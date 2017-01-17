package org.n52.sensorweb.awi.data.entities;

import org.joda.time.DateTime;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class Observation {
    private DateTime time;
    private String urn;
    private double latitude;
    private double longitude;
    private double value;
    private String unit;
}
