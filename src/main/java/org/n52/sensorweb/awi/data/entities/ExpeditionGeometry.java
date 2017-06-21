package org.n52.sensorweb.awi.data.entities;

import com.vividsolutions.jts.geom.Geometry;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class ExpeditionGeometry extends Expedition {
    public static final String GEOMETRY = "geometry";
    private static final long serialVersionUID = 7674672063221970106L;
    private Geometry geometry;

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }
}
