package org.n52.sensorweb.awi.sos;


import org.n52.sensorweb.awi.data.PropertyPath;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class QueryContext {

    private final String data;
    private final String sensor;
    private final String expedition;
    private final String platform;
    private final String device;

    public QueryContext(String data, String sensor, String expedition, String platform, String device) {
        this.data = data;
        this.sensor = sensor;
        this.expedition = expedition;
        this.platform = platform;
        this.device = device;
    }

    public QueryContext() {
        this("data", "sensor", "expedition", "platform", "device");
    }

    public String getDataPath(String... path) {
        return PropertyPath.of(this.data, path);
    }

    public String getSensorPath(String... path) {
        return PropertyPath.of(this.sensor, path);
    }

    public String getExpeditionPath(String... path) {
        return PropertyPath.of(this.expedition, path);
    }

    public String getPlatformPath(String... path) {
        return PropertyPath.of(this.platform, path);
    }

    public String getDevicePath(String... path) {
        return PropertyPath.of(this.device, path);
    }

    public String getData() {
        return data;
    }

    public String getSensor() {
        return sensor;
    }

    public String getExpedition() {
        return expedition;
    }

    public String getPlatform() {
        return platform;
    }

    public String getDevice() {
        return device;
    }

}
