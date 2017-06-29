package org.n52.sensorweb.awi.data.entities;

import java.io.Serializable;
import java.util.Date;

import com.vividsolutions.jts.geom.Geometry;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class Data implements Serializable {

    public static final String TIME = "time";
    public static final String VALUE = "value";
    public static final String SENSOR = "sensor";
    public static final String GEOMETRY = "geometry";

    private static final long serialVersionUID = 2023558780951333945L;
    private String code;
    private Date time;
    private double value;
    private Sensor sensor;
    private Geometry geometry;

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public Device getDevice() {
        return getSensor().getDevice();
    }

    public Platform getPlatform() {
        return getDevice().getPlatform();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public boolean isMobile() {
        return getSensor().getDevice().getPlatform().isMobile();
    }
}
