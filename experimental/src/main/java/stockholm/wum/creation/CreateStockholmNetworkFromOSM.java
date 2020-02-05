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
package stockholm.wum.creation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;

import stockholm.saleem.StockholmTransformationFactory;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CreateStockholmNetworkFromOSM {

	public static void main(String[] args) {

		System.exit(0);
		
		Scenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network nw = sc.getNetwork();
		CoordinateTransformation ct = StockholmTransformationFactory.getCoordinateTransformation(
				StockholmTransformationFactory.WGS84, StockholmTransformationFactory.WGS84_SWEREF99);
		OsmNetworkReader onr = new OsmNetworkReader(nw, ct);

		onr.parse("/Users/GunnarF/OneDrive - VTI/My Data/sweden/sweden-latest.osm");
		// onr.parse("/Users/GunnarF/OneDrive - VTI/My Data/ihop4/Stockholm.osm.gz");

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(nw);
		
		NetworkWriter writer = new NetworkWriter(nw);
		writer.write("/Users/GunnarF/OneDrive - VTI/My Data/sweden/sweden-latest.xml.gz");
	}

}
