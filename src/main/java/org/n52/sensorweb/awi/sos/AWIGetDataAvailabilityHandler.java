package org.n52.sensorweb.awi.sos;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;

import org.n52.janmayen.function.Functions;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Expedition;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.sensorweb.awi.util.hibernate.DefaultResultTransfomer;
import org.n52.shetland.ogc.gml.ReferenceType;
import org.n52.shetland.ogc.gml.time.TimePeriod;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sensorML.SensorML20Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityRequest;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse.DataAvailability;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse.FormatDescriptor;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse.ObservationFormatDescriptor;
import org.n52.shetland.ogc.sos.gda.GetDataAvailabilityResponse.ProcedureDescriptionFormatDescriptor;
import org.n52.sos.cache.SosContentCache;
import org.n52.sos.gda.AbstractGetDataAvailabilityHandler;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWIGetDataAvailabilityHandler extends AbstractGetDataAvailabilityHandler {
    private static final ProcedureDescriptionFormatDescriptor PROCEDURE_DESCRIPTION_FORMAT
            = new ProcedureDescriptionFormatDescriptor(SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL);
    private static final ObservationFormatDescriptor OBSERVATION_FORMAT
            = new ObservationFormatDescriptor(OmConstants.RESPONSE_FORMAT_OM_2,
                                              Collections.singleton(OmConstants.OBS_TYPE_MEASUREMENT));
    private static final FormatDescriptor FORMAT_DESCRIPTOR
            = new FormatDescriptor(PROCEDURE_DESCRIPTION_FORMAT, Collections.singleton(OBSERVATION_FORMAT));

    private final SessionFactory sessionFactory;

    @Inject
    public AWIGetDataAvailabilityHandler(SessionFactory sessionFactory) {
        super(SosConstants.SOS);
        this.sessionFactory = sessionFactory;
    }

    @Override
    public GetDataAvailabilityResponse getDataAvailability(GetDataAvailabilityRequest request)
            throws OwsExceptionReport {
        List<DataAvailability> dataAvailabilities = getDataAvailabilities(request);
        GetDataAvailabilityResponse response = new GetDataAvailabilityResponse();
        response.setService(request.getService());
        response.setVersion(request.getVersion());
        response.setOperationName(request.getOperationName());
        response.setDataAvailabilities(dataAvailabilities);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<DataAvailability> getDataAvailabilities(GetDataAvailabilityRequest request) throws OwsExceptionReport {
        QueryContext ctx = QueryContext.forData();

        ObservationFilter filter = ObservationFilter.builder()
                .setProcedures(request.getProcedures())
                .setFeatures(request.getFeaturesOfInterest())
                .setOfferings(request.getOfferings())
                .setProperties(request.getObservedProperties())
                .build();
        SosContentCache cache = getCache();
        DefaultResultTransfomer<DataAvailability> transformer = tuple -> {
            String platform = (String) tuple[0];
            String device = (String) tuple[1];
            String sensor = (String) tuple[2];
            TimePeriod time = new TimePeriod((Date) tuple[3], (Date) tuple[4]);
            long count = (long) tuple[5];
            String feature = tuple.length == 5 ? platform : (String) tuple[6];

            String procedure = String.format("%s:%s", platform, device);

            DataAvailability da
                    = new DataAvailability(new ReferenceType(procedure),
                                           new ReferenceType(sensor),
                                           new ReferenceType(feature),
                                           new ReferenceType(procedure),
                                           time,
                                           count);

            da.setFormatDescriptor(FORMAT_DESCRIPTOR);
            return da;
        };

        Session session = sessionFactory.openSession();
        try {
            Criteria mobile = session.createCriteria(Data.class)
                    .setComment("Getting mobile data availabilities")
                    .createAlias(Data.SENSOR, ctx.getSensor())
                    .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                    .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                    .add(ObservationFilter.isMobile(ctx))
                    .add(filter.getFilterCriterion(cache, ctx))
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.CODE)))
                            .add(Projections.groupProperty(ctx.getSensorPath(Sensor.CODE)))
                            .add(Projections.min(Data.TIME))
                            .add(Projections.max(Data.TIME))
                            .add(Projections.count(Data.VALUE))
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.EXPEDITIONS, Expedition.NAME))))
                    .add(ObservationFilter.getCommonCriteria(ctx));
            Criteria stationary = session.createCriteria(Data.class)
                    .setComment("Getting stationary data availabilities")
                    .createAlias(Data.SENSOR, ctx.getSensor())
                    .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                    .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                    .add(ObservationFilter.isStationary(ctx))
                    .add(filter.getFilterCriterion(cache, ctx))
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.CODE)))
                            .add(Projections.groupProperty(ctx.getSensorPath(Sensor.CODE)))
                            .add(Projections.min(Data.TIME))
                            .add(Projections.max(Data.TIME))
                            .add(Projections.count(Data.VALUE)))
                    .add(ObservationFilter.getCommonCriteria(ctx));
            return Stream.of(mobile, stationary)
                    .map(Functions.currySecond(Criteria::setReadOnly, true))
                    .map(Functions.currySecond(Criteria::setResultTransformer, transformer))
                    .map(Criteria::list)
                    .flatMap(List<DataAvailability>::stream)
                    .collect(toList());
        } catch (HibernateException e) {
            throw new NoApplicableCodeException().causedBy(e);
        } finally {
            session.close();
        }
    }
}
