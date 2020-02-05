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

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.AtlantisToWGS84;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.geometry.transformations.WGS84toAtlantis;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;
/**
 * A factory to instantiate a specific coordinate transformation.
 *
 * @author Mohammad Saleem
 *
 */
public class StockholmTransformationFactory extends TransformationFactory{
	public final static String WGS84_RT90 = "WGS84toRT90"; // FIXME there is no "to" here
	public final static String WGS84_SWEREF99 = "WGS84toSWEREF99"; // FIXME there is no "to" here
	public final static String WGS84_EPSG3857 = "WGS84_EPSG3857";
	/**
	 * Returns a coordinate transformation to transform coordinates from one
	 * coordinate system to another one.
	 *
	 * @param fromSystem The source coordinate system.
	 * @param toSystem The destination coordinate system.
	 * @return Coordinate Transformation
	 */
	public static CoordinateTransformation getCoordinateTransformation(final String fromSystem, final String toSystem) {
		if (fromSystem.equals(toSystem)) return new IdentityTransformation();
		if (WGS84.equals(fromSystem)) {
			if (CH1903_LV03.equals(toSystem)) return new WGS84toCH1903LV03();
			if (CH1903_LV03_Plus.equals(toSystem)) return new WGS84toCH1903LV03Plus();
			if (ATLANTIS.equals(toSystem)) return new WGS84toAtlantis();
		}
		if (WGS84.equals(toSystem)) {
			if (CH1903_LV03.equals(fromSystem)) return new CH1903LV03toWGS84();
			if (CH1903_LV03_Plus.equals(fromSystem)) return new CH1903LV03PlustoWGS84();
			if (GK4.equals(fromSystem)) return new GK4toWGS84();
			if (ATLANTIS.equals(fromSystem)) return new AtlantisToWGS84();
		}
		
		// return new StockholmGeotoolTransformation(fromSystem, toSystem);
		return new GeotoolsTransformation(fromSystem, toSystem);
	}
}
