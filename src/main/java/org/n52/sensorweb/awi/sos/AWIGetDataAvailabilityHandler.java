package org.n52.sensorweb.awi.sos;

import static java.util.stream.Collectors.toList;
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toDisjunction;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import org.n52.janmayen.function.Functions;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Expedition;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.sensorweb.awi.util.hibernate.DefaultResultTransfomer;
import org.n52.sensorweb.awi.util.hibernate.MoreRestrictions;
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
            return createDataAvailability((String) tuple[0], // platform
                                          (String) tuple[1], // device
                                          (String) tuple[2], // sensor
                                          (String) tuple[3], // feature
                                          (Date) tuple[4], // begin
                                          (Date) tuple[5], // end
                                          (long) tuple[6]);  // count
        };

        Session session = sessionFactory.openSession();
        try {
            Criteria mobile = session.createCriteria(Data.class)
                    .setComment("Getting mobile data availabilities")
                    .createAlias(Data.SENSOR, ctx.getSensor())
                    .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                    .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                    .createAlias(ctx.getPlatformPath(Platform.EXPEDITIONS), ctx.getExpeditions())
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.CODE)))
                            .add(Projections.groupProperty(ctx.getSensorPath(Sensor.CODE)))
                            .add(Projections.groupProperty(ctx.getExpeditionsPath(Expedition.NAME)))
                            .add(Projections.min(Data.TIME))
                            .add(Projections.max(Data.TIME))
                            .add(Projections.count(Data.VALUE)))
                    .add(Restrictions.isNull(ctx.getPlatformPath(Platform.GEOMETRY)))
                    .add(Restrictions.leProperty(ctx.getExpeditionsPath(Expedition.BEGIN), ctx.getExpeditionsPath(Expedition.END)))
                    .add(Restrictions.geProperty(ctx.getDataPath(Data.TIME), ctx.getExpeditionsPath(Expedition.BEGIN)))
                    .add(Restrictions.leProperty(ctx.getDataPath(Data.TIME), ctx.getExpeditionsPath(Expedition.END)));

            Criteria stationary = session.createCriteria(Data.class)
                    .setComment("Getting stationary data availabilities")
                    .createAlias(Data.SENSOR, ctx.getSensor())
                    .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                    .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.CODE)))
                            .add(Projections.groupProperty(ctx.getSensorPath(Sensor.CODE)))
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.min(Data.TIME))
                            .add(Projections.max(Data.TIME))
                            .add(Projections.count(Data.VALUE)))
                    .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.GEOMETRY)));

            Stream.of(mobile, stationary).forEach(criteria ->
                criteria.add(Restrictions.isNotNull(ctx.getSensorPath(Sensor.CODE)))
                        .add(Restrictions.isNotNull(ctx.getDevicePath(Device.CODE)))
                        .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.CODE)))
                        .add(Restrictions.eq(ctx.getPlatformPath(Platform.PUBLISHED), true)));

            if (!filter.getFeatures().isEmpty()) {
                stationary.add(Restrictions.in(ctx.getPlatformPath(Platform.CODE), filter.getFeatures()));
                mobile.add(Restrictions.in(ctx.getExpeditionsPath(Expedition.NAME), filter.getFeatures()));
            }

            if (!filter.getProcedures().isEmpty()) {
                Stream.of(mobile, stationary).forEach(c -> c.add(getProcedureCriterion(filter.getProcedures(), ctx)));
            }

            if (!filter.getOfferings().isEmpty()) {
                Stream.of(mobile, stationary).forEach(c -> c.add(getProcedureCriterion(filter.getOfferings(), ctx)));
            }

            if (!filter.getProperties().isEmpty()) {
                Stream.of(mobile, stationary).forEach(c -> c.add(Restrictions.in(ctx.getSensorPath(Sensor.CODE), filter.getProperties())));
            }

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

    /**
     * Create a data availability from the supplied values.
     *
     * @param platform the platform code
     * @param device   the device code
     * @param sensor   the sensor code
     * @param feature  the feature code
     * @param begin    the begin time
     * @param end      the end time
     * @param count    the count of observations
     *
     * @return the data availability
     */
    private DataAvailability createDataAvailability(String platform, String device, String sensor,
                                                    String feature, Date begin, Date end, long count) {
        TimePeriod time = new TimePeriod(begin, end);
        String procedure = String.format("%s:%s", platform, device);
        DataAvailability da = new DataAvailability(new ReferenceType(procedure),
                                                   new ReferenceType(sensor),
                                                   new ReferenceType(feature),
                                                   new ReferenceType(procedure),
                                                   time,
                                                   count);

        da.setFormatDescriptor(FORMAT_DESCRIPTOR);
        return da;
    }

    /**
     * Get a criterion for the supplied procedure identifiers.
     *
     * @param urns the procedure identifiers
     * @param ctx  the query context
     *
     * @return the criterion
     */
    private static Criterion getProcedureCriterion(Set<String> urns, QueryContext ctx) {
        Pattern pattern = Pattern.compile("^([^:]+:[^:]+)(?::(.+))?$");
        return urns.stream()
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(x -> MoreRestrictions.and(
                Optional.of(Restrictions.eq(ctx.getPlatformPath(Platform.CODE), x.group(1))),
                Optional.ofNullable(x.group(2)).map(device -> Restrictions.eq(ctx.getDevicePath(Device.CODE), device))))
                .filter(Optional::isPresent).map(Optional::get).collect(toDisjunction());
    }
}
