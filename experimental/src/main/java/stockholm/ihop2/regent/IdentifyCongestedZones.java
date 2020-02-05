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
package stockholm.ihop2.regent;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;

import stockholm.saleem.StockholmTransformationFactory;
import stockholm.ihop2.regent.demandreading.ZonalSystem;
import stockholm.ihop2.regent.demandreading.Zone;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class IdentifyCongestedZones {

	IdentifyCongestedZones() {
	}

	public static void main(String[] args) {

		final String freeFlowMatrixFile = "./test/matsim-testrun/freeflow-traveltimes.xml";
		final String congestedMatrixFile = "./test/matsim-testrun/tourtts.xml";
		final String freeFlowMatrixId = null;
		final String congestedMatrixID = "WORK";

		final Matrix freeFlowMatrix;
		{
			final Matrices m1 = new Matrices();
			final MatsimMatricesReader r1 = new MatsimMatricesReader(m1, null);
			r1.readFile(freeFlowMatrixFile);
			if (freeFlowMatrixId == null) {
				freeFlowMatrix = m1.getMatrices().values().iterator().next();
			} else {
				freeFlowMatrix = m1.getMatrix(freeFlowMatrixId);
			}
		}

		final Matrix congestedMatrix;
		{
			final Matrices m2 = new Matrices();
			final MatsimMatricesReader r2 = new MatsimMatricesReader(m2, null);
			r2.readFile(congestedMatrixFile);
			if (congestedMatrixID == null) {
				congestedMatrix = m2.getMatrices().values().iterator().next();
			} else {
				congestedMatrix = m2.getMatrix(congestedMatrixID);
			}
		}

		final String networkFileName = "./test/matsim-testrun/input/network-plain.xml";
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final String zonesShapeFileName = "./test/regentmatsim/input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);
		// final CoordinateTransformation zone2netTrafo =
		// StockholmTransformationFactory
		// .getCoordinateTransformation(
		// StockholmTransformationFactory.WGS84_EPSG3857,
		// StockholmTransformationFactory.WGS84_SWEREF99);

		for (Map.Entry<String, ArrayList<Entry>> cellId2column : congestedMatrix
				.getToLocations().entrySet()) {
			double ratioSum = 0;
			int posValCnt = 0;
			for (Entry entry1 : cellId2column.getValue()) {
				final double cong = entry1.getValue();
				final double free = freeFlowMatrix.getEntry(
						entry1.getFromLocation(), entry1.getToLocation())
						.getValue();
				if (cong > 0 && free > 0) {
					// System.out.println(cong + ", " + free);
					ratioSum += entry1.getValue()
							/ freeFlowMatrix.getEntry(entry1.getFromLocation(),
									entry1.getToLocation()).getValue();
					posValCnt++;
				}
			}
			if (posValCnt > 0) {
				final double avgRatio = ratioSum / posValCnt;
				if (avgRatio >= 5.0) {
					// System.out.println(cellId2row.getKey() + "\t" +
					// avgRatio);
					 final Zone zone =
					 zonalSystem.getZone(cellId2column.getKey());
					// System.out.println(zone.getId() + "\t" + avgRatio);
					System.out.println(zone.getGeometry().getCentroid().getX()
							+ ";" + zone.getGeometry().getCentroid().getY());
				}
			}
		}
	}
}
