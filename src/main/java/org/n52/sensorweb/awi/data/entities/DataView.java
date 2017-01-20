package org.n52.sensorweb.awi.data.entities;

import java.io.Serializable;

import org.joda.time.DateTime;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class DataView implements Serializable {
    private static final long serialVersionUID = 2023558780951333945L;
    private int id;
    private String code;
    private DateTime time;
    private double value;
    private Sensor sensor;
    private double longitude;
    private double latitude;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public DateTime getTime() {
        return time;
    }

    public void setTime(DateTime time) {
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


    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
