package org.n52.sensorweb.awi.sos;

import org.hibernate.criterion.CriteriaSpecification;

import org.n52.sensorweb.awi.util.hibernate.PropertyPath;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class QueryContext {
    private static final String SENSOR = "sensor";
    private static final String PLATFORM = "platform";
    private static final String DEVICE = "device";
    private static final String DATA = "data";
    private static final String EXPEDITIONS = "expeditions";

    private final String data;
    private final String sensor;
    private final String platform;
    private final String device;
    private final String expeditions;

    public QueryContext(String data, String sensor, String platform, String device, String expeditions) {
        this.data = data;
        this.sensor = sensor;
        this.platform = platform;
        this.device = device;
        this.expeditions = expeditions;
    }

    public String getDataPath(String... path) {
        return PropertyPath.of(this.data, path);
    }

    public String getSensorPath(String... path) {
        return PropertyPath.of(this.sensor, path);
    }

    public String getPlatformPath(String... path) {
        return PropertyPath.of(this.platform, path);
    }

    public String getDevicePath(String... path) {
        return PropertyPath.of(this.device, path);
    }

    public String getExpeditionsPath(String... path) {
        return PropertyPath.of(this.expeditions, path);
    }

    public String getData() {
        return data;
    }

    public String getSensor() {
        return sensor;
    }

    public String getPlatform() {
        return platform;
    }

    public String getDevice() {
        return device;
    }

    public String getExpeditions() {
        return expeditions;
    }

    public static QueryContext forSensor() {
        return new QueryContext(DATA, CriteriaSpecification.ROOT_ALIAS, PLATFORM, DEVICE, EXPEDITIONS);
    }

    public static QueryContext forPlatform() {
        return new QueryContext(DATA, SENSOR, CriteriaSpecification.ROOT_ALIAS, DEVICE, EXPEDITIONS);
    }

    public static QueryContext forDevice() {
        return new QueryContext(DATA, SENSOR, PLATFORM, CriteriaSpecification.ROOT_ALIAS, EXPEDITIONS);
    }

    public static QueryContext forData() {
        return new QueryContext(CriteriaSpecification.ROOT_ALIAS, SENSOR, PLATFORM, DEVICE, EXPEDITIONS);
    }
}
