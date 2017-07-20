package org.n52.sensorweb.awi.sos;

import org.hibernate.criterion.CriteriaSpecification;

import org.n52.sos.ds.hibernate.util.PropertyPath;

/**
 * Convenient class to build hibernate property paths.
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

    /**
     * Create a new {@code QueryContext}.
     *
     * @param data        the data alias
     * @param sensor      the sensor alias
     * @param platform    the platform alias
     * @param device      the device alias
     * @param expeditions the expeditions alias
     */
    public QueryContext(String data, String sensor, String platform, String device, String expeditions) {
        this.data = data;
        this.sensor = sensor;
        this.platform = platform;
        this.device = device;
        this.expeditions = expeditions;
    }

    /**
     * Get a path relative to the data alias.
     *
     * @param path the path componenents
     *
     * @return the path
     */
    public String getDataPath(String... path) {
        return PropertyPath.of(this.data, path);
    }

    /**
     * Get a path relative to the sensor alias.
     *
     * @param path the path componenents
     *
     * @return the path
     */
    public String getSensorPath(String... path) {
        return PropertyPath.of(this.sensor, path);
    }

    /**
     * Get a path relative to the platform alias.
     *
     * @param path the path componenents
     *
     * @return the path
     */
    public String getPlatformPath(String... path) {
        return PropertyPath.of(this.platform, path);
    }

    /**
     * Get a path relative to the device alias.
     *
     * @param path the path componenents
     *
     * @return the path
     */
    public String getDevicePath(String... path) {
        return PropertyPath.of(this.device, path);
    }

    /**
     * Get a path relative to the expeditions alias.
     *
     * @param path the path componenents
     *
     * @return the path
     */
    public String getExpeditionsPath(String... path) {
        return PropertyPath.of(this.expeditions, path);
    }

    /**
     * Get the data alias
     *
     * @return the alias
     */
    public String getData() {
        return data;
    }

    /**
     * Get the sensor alias.
     *
     * @return the alias
     */
    public String getSensor() {
        return sensor;
    }

    /**
     * Get the platform alias.
     *
     * @return the alias.
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Get the device alias.
     *
     * @return the alias
     */
    public String getDevice() {
        return device;
    }

    /**
     * Get the expedition alias.
     *
     * @return the alias
     */
    public String getExpeditions() {
        return expeditions;
    }

    /**
     * Create a new {@code QueryContext} with the sensor as the root entity.
     *
     * @return the query context
     */
    public static QueryContext forSensor() {
        return new QueryContext(DATA, CriteriaSpecification.ROOT_ALIAS, PLATFORM, DEVICE, EXPEDITIONS);
    }

    /**
     * Create a new {@code QueryContext} with the platform as the root entity.
     *
     * @return the query context
     */
    public static QueryContext forPlatform() {
        return new QueryContext(DATA, SENSOR, CriteriaSpecification.ROOT_ALIAS, DEVICE, EXPEDITIONS);
    }

    /**
     * Create a new {@code QueryContext} with the device as the root entity.
     *
     * @return the query context
     */
    public static QueryContext forDevice() {
        return new QueryContext(DATA, SENSOR, PLATFORM, CriteriaSpecification.ROOT_ALIAS, EXPEDITIONS);
    }

    /**
     * Create a new {@code QueryContext} with the data as the root entity.
     *
     * @return the query context
     */
    public static QueryContext forData() {
        return new QueryContext(CriteriaSpecification.ROOT_ALIAS, SENSOR, PLATFORM, DEVICE, EXPEDITIONS);
    }
}
