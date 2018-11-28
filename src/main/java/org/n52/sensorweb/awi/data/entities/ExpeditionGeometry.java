package org.n52.sensorweb.awi.data.entities;

import org.locationtech.jts.geom.Geometry;

/**
 * Entity for the {@code expedtion} table that also includes the geometry.
 *
 * @see Expedition
 * @author Christian Autermann
 */
public class ExpeditionGeometry extends Expedition {
    public static final String GEOMETRY = "geometry";
    private static final long serialVersionUID = 7674672063221970106L;
    private Geometry geometry;

    /**
     * Get the geomtry of this expedition.
     *
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Sets the geometry of this expedition.
     *
     * @param geometry the geometry
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
}
