package org.n52.sensorweb.awi.data.entities;

import java.io.Serializable;
import java.util.Date;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class Expedition implements Serializable {
    public static final String NAME = "name";
    public static final String BEGIN = "begin";
    public static final String END = "end";
    public static final String PLATFORM = "platform";
    private static final long serialVersionUID = -7996822347864213643L;

    private String platform;
    private Date begin;
    private Date end;
    private String name;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isValid() {
        return getBegin().compareTo(getEnd()) <= 0;
    }
}
