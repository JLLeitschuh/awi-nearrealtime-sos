/*
 * Copyright 2016 52°North GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.sensorweb.awi.sos;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Projections;

import org.n52.janmayen.function.Functions;
import org.n52.sensorweb.awi.data.DefaultResultTransfomer;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Expedition;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.shetland.ogc.gml.AbstractFeature;
import org.n52.shetland.ogc.gml.CodeWithAuthority;
import org.n52.shetland.ogc.om.features.FeatureCollection;
import org.n52.shetland.ogc.om.features.SfConstants;
import org.n52.shetland.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.request.GetFeatureOfInterestRequest;
import org.n52.shetland.ogc.sos.response.GetFeatureOfInterestResponse;
import org.n52.sos.ds.AbstractGetFeatureOfInterestHandler;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWIGetFeatureOfInterestHandler extends AbstractGetFeatureOfInterestHandler {

    private static final GeometryFactory GEOMETRY_FACTORY
            = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);

    private final SessionFactory sessionFactory;

    public AWIGetFeatureOfInterestHandler(SessionFactory sessionFactory) {
        super(SosConstants.SOS);
        this.sessionFactory = sessionFactory;
    }

    @Override
    public GetFeatureOfInterestResponse getFeatureOfInterest(GetFeatureOfInterestRequest request)
            throws OwsExceptionReport {

        ObservationFilter filter = ObservationFilter.builder()
                .setFeatures(request.getFeatureIdentifiers())
                .setProperties(request.getObservedProperties())
                .setProcedures(request.getProcedures())
                .setSpatialFilter(request.getSpatialFilters())
                .build();

        GetFeatureOfInterestResponse response = new GetFeatureOfInterestResponse(request.getService(),
                                                                                 request.getVersion());
        response.setAbstractFeature(getFeatureCollection(filter));
        return response;

    }

    private SamplingFeature createSamplingFeature(Object[] tuple) {
        return createFeature((String) tuple[0], (Geometry) tuple[1]);
    }

    private SamplingFeature createFeature(String id, Geometry geometry) {
        SamplingFeature feature = new SamplingFeature(new CodeWithAuthority(id));
        feature.setFeatureType(getFeatureType(geometry));
        feature.setGeometry(geometry);
        return feature;
    }

    private String getFeatureType(Geometry geometry) {
        if (geometry instanceof Point || geometry instanceof MultiPoint) {
            return SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT;
        } else if (geometry instanceof LineString || geometry instanceof MultiLineString) {
            return SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_CURVE;
        } else if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
            return SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_SURFACE;
        } else {
            return SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_FEATURE;
        }
    }

    private FeatureCollection getFeatureCollection(ObservationFilter filter) throws OwsExceptionReport {
        FeatureCollection collection = new FeatureCollection();
        getFeatures(filter).forEach(collection::addMember);
        return collection;
    }

    private Stream<AbstractFeature> getFeatures(ObservationFilter filter) throws OwsExceptionReport {
        Session session = sessionFactory.openSession();
        try {
            QueryContext ctx
                    = new QueryContext(CriteriaSpecification.ROOT_ALIAS, "sensor", "expedition", "platform", "device");
            DefaultResultTransfomer<SamplingFeature> transformer = this::createSamplingFeature;
            return Stream.of(
                    session.createCriteria(Data.class)
                            .createAlias(Data.SENSOR, ctx.getSensor())
                            .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                            .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                            .createAlias(ctx.getPlatformPath(Platform.EXPEDITION), ctx.getExpedition())
                            .add(filter.isStationary(ctx))
                            .setProjection(Projections.projectionList()
                                    .add(Projections.property(ctx.getPlatformPath(Platform.CODE)))
                                    .add(Projections.property(ctx.getPlatformPath(Platform.GEOMETRY)))),
                    session.createCriteria(Data.class)
                            .createAlias(Data.SENSOR, ctx.getSensor())
                            .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                            .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                            .createAlias(ctx.getPlatformPath(Platform.EXPEDITION), ctx.getExpedition())
                            .add(filter.isMobile(ctx))
                            .setProjection(Projections.projectionList()
                                    .add(Projections.property(ctx.getExpeditionPath(Expedition.NAME)))
                                    .add(Projections.property(ctx.getExpeditionPath(Expedition.GEOMETRY)))))
                    .map(Functions.currySecond(Criteria::setReadOnly, true))
                    .map(Functions
                            .currySecond(Criteria::setResultTransformer, transformer))
                    .map(Criteria::list)
                    .flatMap(List<AbstractFeature>::stream);
        } catch (HibernateException e) {
            throw new NoApplicableCodeException().causedBy(e);
        } finally {
            session.close();
        }
    }
}
