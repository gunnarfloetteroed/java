/*
 * Copyright 2018 Mohammad Saleem
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
 * contact: salee@kth.se
 *
 */ 
package stockholm.saleem;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
/**
 * A class to convert coordinates from one coordinate system to
 * another one.
 * 
 * @author Mohammad Saleem
 */
public class StockholmGeotoolTransformation implements CoordinateTransformation{
	private MathTransform transform;

	/**
	 * Creates a new coordinate transformation that makes use of GeoTools.
	 * The coordinate systems to translate from and to can either be specified as
	 * shortened names, as defined in {@link TransformationFactory}, or as
	 * Well-Known-Text (WKT) as supported by the GeoTools.
	 *
	 * @param from Specifies the origin coordinate reference system
	 * @param to Specifies the destination coordinate reference system
	 *
	 * @see <a href="http://geoapi.sourceforge.net/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html">WKT specifications</a>
	 */
	public StockholmGeotoolTransformation(final String from, final String to) {
		CoordinateReferenceSystem sourceCRS = StockholmMGC.getCRS(from);
		CoordinateReferenceSystem targetCRS = StockholmMGC.getCRS(to);

		try {
			this.transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
		}
	}

	@Deprecated
	public Coord transform(final Coord coord) {
		throw new UnsupportedOperationException();
//		Point p = null;
//		try {
//			p = (Point) JTS.transform(StockholmMGC.coord2Point(coord), this.transform);
//		} catch (TransformException e) {
//			throw new RuntimeException(e);
//		}
//		return StockholmMGC.point2Coord(p);
	}

}
