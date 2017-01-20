package org.n52.sensorweb.awi.data.entities;

import java.io.Serializable;
import java.util.Objects;

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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.platform);
        hash = 41 * hash + Objects.hashCode(this.device);
        hash = 41 * hash + Objects.hashCode(this.sensor);
        hash = 41 * hash + Objects.hashCode(this.latitude);
        hash = 41 * hash + Objects.hashCode(this.longitude);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PositionInfo other = (PositionInfo) obj;
        if (!Objects.equals(this.platform, other.platform)) {
            return false;
        }
        if (!Objects.equals(this.device, other.device)) {
            return false;
        }
        if (!Objects.equals(this.sensor, other.sensor)) {
            return false;
        }
        if (!Objects.equals(this.latitude, other.latitude)) {
            return false;
        }
        if (!Objects.equals(this.longitude, other.longitude)) {
            return false;
        }
        return true;
    }
}
