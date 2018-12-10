package org.n52.sensorweb.awi.data.entities;

import java.io.Serializable;
import java.util.Date;

import com.vividsolutions.jts.geom.Geometry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Entity for the {@code dataview} table.
 *
 * @author Christian Autermann
 */
public class Data implements Serializable {

    public static final String TIME = "time";
    public static final String VALUE = "value";
    public static final String SENSOR = "sensor";
    public static final String GEOMETRY = "geometry";

    private static final long serialVersionUID = 2023558780951333945L;
    private Date time;
    private double value;
    private Sensor sensor;
    private Geometry geometry;

    /**
     * Get the time of this data point.
     *
     * @return the time
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Date getTime() {
        return time;
    }

    /**
     * Set the time of this data point.
     *
     * @param time the time
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public void setTime(Date time) {
        this.time = time;
    }

    /**
     * Get the value of this data point.
     *
     * @return the value
     */
    public double getValue() {
        return value;
    }

    /**
     * Set the value of this data point.
     *
     * @param value the value
     */
    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Get the sensor of this data point.
     *
     * @return the sensor
     */
    public Sensor getSensor() {
        return sensor;
    }

    /**
     * Set the sensor of this data point
     *
     * @param sensor the sensor
     */
    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    /**
     * Get the device of this data point.
     *
     * @return the device
     */
    public Device getDevice() {
        return getSensor().getDevice();
    }

    /**
     * Get the platform of this data point.
     *
     * @return the platform
     */
    public Platform getPlatform() {
        return getDevice().getPlatform();
    }

    /**
     * Get the geometry of this data point.
     *
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Set the geometry of this data point
     *
     * @param geometry the geometry
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Check if this data points is observed by a mobile platform.
     *
     * @return if this data points is observed by a mobile platform.
     */
    public boolean isMobile() {
        return getSensor().getDevice().getPlatform().isMobile();
    }
}
