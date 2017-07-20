package org.n52.sensorweb.awi.data.entities;

import java.io.Serializable;
import java.util.Date;

/**
 * Entity for the {@code expedition} table.
 *
 * @see ExpeditionGeometry
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

    /**
     * Gets the platform code of this expedition.
     *
     * @return the platform code
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Sets the platform code of this expedition
     *
     * @param platform the platform code
     */
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    /**
     * Get begin date of this expedition.
     *
     * @return the begin date
     */
    public Date getBegin() {
        return begin;
    }

    /**
     * Sets the begin date of this expedition.
     *
     * @param begin the begin date
     */
    public void setBegin(Date begin) {
        this.begin = begin;
    }

    /**
     * Gets the end date of this expedition.
     *
     * @return the end date
     */
    public Date getEnd() {
        return end;
    }

    /**
     * Sets the end date of this expedition.
     *
     * @param end the end date
     */
    public void setEnd(Date end) {
        this.end = end;
    }

    /**
     * Gets the name of this expedition.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this expedition.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Checks if this begin date is before the end date.
     *
     * @return if this expedition is valid
     */
    public boolean isValid() {
        return getBegin().compareTo(getEnd()) <= 0;
    }
}
