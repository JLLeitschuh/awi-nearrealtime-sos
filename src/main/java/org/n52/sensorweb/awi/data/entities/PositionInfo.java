package org.n52.sensorweb.awi.data.entities;

import java.io.Serializable;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class PositionInfo implements Serializable {
    private static final long serialVersionUID = -6523368294493596888L;

    private Platform platform;
    private Device device;
    private Sensor sensor;
    private Sensor latitude;
    private Sensor longitude;

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Sensor getLatitude() {
        return latitude;
    }

    public void setLatitude(Sensor latitude) {
        this.latitude = latitude;
    }

    public Sensor getLongitude() {
        return longitude;
    }

    public void setLongitude(Sensor longitude) {
        this.longitude = longitude;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }
}
