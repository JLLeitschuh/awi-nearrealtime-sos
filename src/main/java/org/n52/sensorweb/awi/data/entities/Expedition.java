package org.n52.sensorweb.awi.data.entities;

import java.util.Date;

import com.vividsolutions.jts.geom.Geometry;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class Expedition {
    public static final String NAME = "name";
    public static final String BEGIN = "begin";
    public static final String END = "end";
    public static final String GEOMETRY = "geometry";
    public static final String PLATFORM = "platform";

    private Platform platform;
    private Date begin;
    private Date end;
    private Geometry geometry;
    private String name;

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Date getBegin() {
        return begin;
    }

    public void setBegin(Date begin) {
        this.begin = begin;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
