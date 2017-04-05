package org.n52.sensorweb.awi.sos;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;

import org.n52.janmayen.function.Predicates;
import org.n52.sensorweb.awi.data.DefaultResultTransfomer;
import org.n52.sensorweb.awi.data.PropertyPath;
import org.n52.sensorweb.awi.data.entities.DataView;
import org.n52.sensorweb.awi.data.entities.Device;
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
import org.n52.shetland.util.CollectionHelper;
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
        Session session = sessionFactory.openSession();
        try {
            return session.createCriteria(DataView.class)
                    .createAlias(DataView.SENSOR, DataView.SENSOR)
                    .createAlias(PropertyPath.of(DataView.SENSOR, Sensor.DEVICE), Sensor.DEVICE)
                    .createAlias(PropertyPath.of(Sensor.DEVICE, Device.PLATFORM), Device.PLATFORM)
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty(PropertyPath.of(DataView.SENSOR, Sensor.CODE)))
                            .add(Projections.groupProperty(PropertyPath.of(Sensor.DEVICE, Device.CODE)))
                            .add(Projections.groupProperty(PropertyPath.of(Device.PLATFORM, Platform.CODE)))
                            .add(Projections.groupProperty(PropertyPath.of(Device.PLATFORM, Platform.TYPE)))
                            .add(Projections.min(DataView.TIME))
                            .add(Projections.max(DataView.TIME))
                            .add(Projections.count(DataView.VALUE)))
                    .add(getFilterCriterion(asSet(request.getProcedures()),
                                            asSet(request.getFeaturesOfInterest()),
                                            asSet(request.getOfferings()),
                                            asSet(request.getObservedProperties())))
                    .add(Restrictions.isNotNull(PropertyPath.of(DataView.SENSOR, Sensor.CODE)))
                    .setResultTransformer((DefaultResultTransfomer<DataAvailability>) this::transformResultColumn)
                    .list();
        } catch (HibernateException e) {
            throw new NoApplicableCodeException().causedBy(e);
        } finally {
            session.close();
        }
    }

    private DataAvailability transformResultColumn(Object[] tuple) {
        String sensor = (String) tuple[0];
        String device = (String) tuple[1];
        String platform = (String) tuple[2];
        String platformType = (String) tuple[3];
        DateTime minTime = (DateTime) tuple[4];
        DateTime maxTime = (DateTime) tuple[5];
        long count = (long) tuple[6];

        String procedure = String.format("%s:%s:%s", platformType, platform, device);
        DataAvailability da
                = new DataAvailability(new ReferenceType(procedure),
                                       new ReferenceType(sensor),
                                       new ReferenceType(procedure),
                                       new ReferenceType(procedure),
                                       new TimePeriod(minTime, maxTime),
                                       count);

        da.setFormatDescriptor(FORMAT_DESCRIPTOR);

        return da;
    }

    private Junction getFilterCriterion(Set<String> procedures, Set<String> features, Set<String> offerings,
                                        Set<String> properties) throws OwsExceptionReport {
        SosContentCache cache = getCache();
        Junction conjunction = Restrictions.conjunction();
        // features, procedures and offerings are all the same for this service
        List<Set<String>> nonEmptyProcedureFilters = Stream.of(procedures, features, offerings)
                .filter(Predicates.not(Set::isEmpty)).collect(toList());
        final Set<String> urns;
        // do we have any procedure filters?
        if (!nonEmptyProcedureFilters.isEmpty()) {
            // create the intersection of the filters
            Set<String> intersection = CollectionHelper.intersection(nonEmptyProcedureFilters);
            if (properties.isEmpty()) {
                // create URNs from the supplied observable properties
                urns = intersection.stream()
                        .flatMap(procedure -> cache.getObservablePropertiesForProcedure(procedure).stream()
                        .map(property -> String.format("%s:%s", procedure, property)))
                        .collect(toSet());
            } else {
                // create URNs from all cached observable properties
                urns = intersection.stream()
                        .flatMap(procedure -> properties.stream()
                        .filter(property -> cache.hasObservablePropertyForProcedure(procedure, property))
                        .map(property -> String.format("%s:%s", procedure, property)))
                        .collect(toSet());
            }
        } else if (!properties.isEmpty()) {
            // create the URNs from all procedures matching the observable properties
            urns = properties.stream()
                    .flatMap(property -> cache.getProceduresForObservableProperty(property).stream()
                    .map(procedure -> String.format("%s:%s", procedure, property)))
                    .collect(toSet());
        } else {
            urns = null;
        }

        if (urns != null) {
            conjunction.add(Restrictions.in(DataView.CODE, urns));
        }

        return conjunction;
    }

    private static <T> Set<T> asSet(List<T> list) {
        return new HashSet<>(Optional.ofNullable(list).orElseGet(Collections::emptyList));
    }
}
