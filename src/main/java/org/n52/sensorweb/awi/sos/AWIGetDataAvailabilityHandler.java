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
import org.n52.sos.ds.AbstractGetDataAvailabilityHandler;
import org.n52.sos.ds.hibernate.util.DefaultResultTransfomer;
import org.n52.sos.ds.hibernate.util.MoreRestrictions;

/**
 * {@code GetDataAvailability} handler for the AWI Nearrealtime database.
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

    /**
     * Create a new {@code AWIGetDataAvailabilityHandler}.
     *
     * @param sessionFactory the session factory
     */
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

    /**
     * Get the data availabilities for the specified request.
     *
     * @param request the request
     *
     * @return the data availabilities
     *
     * @throws OwsExceptionReport in case an error occurs
     */
    @SuppressWarnings("unchecked")
    private List<DataAvailability> getDataAvailabilities(GetDataAvailabilityRequest request)
            throws OwsExceptionReport {
        QueryContext ctx = QueryContext.forData();

        ObservationFilter filter = ObservationFilter.builder()
                .setProcedures(request.getProcedures())
                .setFeatures(request.getFeaturesOfInterest())
                .setOfferings(request.getOfferings())
                .setProperties(request.getObservedProperties())
                .build();

        SosContentCache cache = getCache();

        Set<String> features = filter.getFeatures().isEmpty()
                                       ? cache.getFeaturesOfInterest()
                                       : filter.getFeatures();
        Set<String> procedures = filter.getProcedures().isEmpty()
                                         ? cache.getProcedures()
                                         : filter.getProcedures();
        Set<String> offerings = filter.getOfferings().isEmpty()
                                        ? cache.getOfferings()
                                        : filter.getOfferings();
        Set<String> properties = filter.getProperties().isEmpty()
                                         ? cache.getObservableProperties()
                                         : filter.getProperties();

        DefaultResultTransfomer<DataAvailability> transformer = tuple -> {
            return createDataAvailability((String) tuple[0], // platform
                                          (String) tuple[1], // platformName
                                          (String) tuple[2], // device
                                          (String) tuple[3], // device
                                          (String) tuple[4], // sensor
                                          (String) tuple[5], // sensorName
                                          (String) tuple[6], // feature
                                          (String) tuple[6], // featureName
                                          (Date) tuple[7], // begin
                                          (Date) tuple[8], // end
                                          (long) tuple[9]);  // count
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
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.NAME)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.CODE)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.NAME)))
                            .add(Projections.groupProperty(ctx.getSensorPath(Sensor.CODE)))
                            .add(Projections.groupProperty(ctx.getSensorPath(Sensor.NAME)))
                            .add(Projections.groupProperty(ctx.getExpeditionsPath(Expedition.NAME)))
                            .add(Projections.min(Data.TIME))
                            .add(Projections.max(Data.TIME))
                            .add(Projections.count(Data.VALUE)))
                    .add(Restrictions.isNull(ctx.getPlatformPath(Platform.GEOMETRY)))
                    .add(Restrictions.leProperty(ctx.getExpeditionsPath(Expedition.BEGIN),
                                                 ctx.getExpeditionsPath(Expedition.END)))
                    .add(Restrictions.geProperty(ctx.getDataPath(Data.TIME),
                                                 ctx.getExpeditionsPath(Expedition.BEGIN)))
                    .add(Restrictions.leProperty(ctx.getDataPath(Data.TIME),
                                                 ctx.getExpeditionsPath(Expedition.END)));

            Criteria stationary = session.createCriteria(Data.class)
                    .setComment("Getting stationary data availabilities")
                    .createAlias(Data.SENSOR, ctx.getSensor())
                    .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                    .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.NAME)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.CODE)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.NAME)))
                            .add(Projections.groupProperty(ctx.getSensorPath(Sensor.CODE)))
                            .add(Projections.groupProperty(ctx.getSensorPath(Sensor.NAME)))
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.min(Data.TIME))
                            .add(Projections.max(Data.TIME))
                            .add(Projections.count(Data.VALUE)))
                    .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.GEOMETRY)));

            Stream.of(mobile, stationary).forEach(criteria
                    -> criteria.add(Restrictions.isNotNull(ctx.getSensorPath(Sensor.CODE)))
                            .add(Restrictions.isNotNull(ctx.getDevicePath(Device.CODE)))
                            .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.CODE)))
                            .add(Restrictions.eq(ctx.getPlatformPath(Platform.PUBLISHED), true))
                            .add(getProcedureCriterion(procedures, ctx))
                            .add(getProcedureCriterion(offerings, ctx))
                            .add(Restrictions.in(ctx.getSensorPath(Sensor.CODE), properties)));

            stationary.add(Restrictions.in(ctx.getPlatformPath(Platform.CODE), features));
            mobile.add(Restrictions.in(ctx.getExpeditionsPath(Expedition.NAME), features));

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
     * @param platform     the platform code
     * @param platformName the platform name
     * @param device       the device code
     * @param deviceName   the device name
     * @param sensor       the sensor code
     * @param sensorName   the sensor name
     * @param feature      the feature code
     * @param featureName  the feature name
     * @param begin        the begin time
     * @param end          the end time
     * @param count        the count of observations
     *
     * @return the data availability
     */
    private DataAvailability createDataAvailability(String platform, String platformName,
                                                    String device, String deviceName,
                                                    String sensor, String sensorName,
                                                    String feature, String featureName,
                                                    Date begin, Date end, long count) {
        TimePeriod time = new TimePeriod(begin, end);
        String procedure = String.format("%s:%s", platform, device);
        String procedureName = String.format("%s - %s", platformName, deviceName);
        DataAvailability da = new DataAvailability(new ReferenceType(procedure, procedureName),
                                                   new ReferenceType(sensor, sensorName),
                                                   new ReferenceType(feature, featureName),
                                                   new ReferenceType(procedure, procedureName),
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
                        Optional.ofNullable(x.group(2))
                                .map(device -> Restrictions.eq(ctx.getDevicePath(Device.CODE), device))))
                .filter(Optional::isPresent).map(Optional::get).collect(toDisjunction());
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
