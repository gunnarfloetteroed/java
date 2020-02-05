/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */ 
package stockholm.utils;

import java.util.Random;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.geotools.MGC;

/**
 * 
 * @author Gunnar Flötteröd, based on Patryk Larek
 *
 */
public class ShapeUtils {

	private ShapeUtils() {
	}

	public static Coord drawPointFromGeometry(final Geometry geom) {
		final Random rnd = MatsimRandom.getLocalInstance();
		final double deltaX = geom.getEnvelopeInternal().getMaxX()
				- geom.getEnvelopeInternal().getMinX();
		final double deltaY = geom.getEnvelopeInternal().getMaxY()
				- geom.getEnvelopeInternal().getMinY();
		Point p;
		do {
			final double x = geom.getEnvelopeInternal().getMinX()
					+ rnd.nextDouble() * deltaX;
			final double y = geom.getEnvelopeInternal().getMinY()
					+ rnd.nextDouble() * deltaY;
			p = MGC.xy2Point(x, y);
		} while (!geom.contains(p));
		return new Coord(p.getX(), p.getY());
	}
}
