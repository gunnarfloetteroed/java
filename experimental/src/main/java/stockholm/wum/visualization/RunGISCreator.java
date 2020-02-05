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
package stockholm.wum.visualization;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;

import stockholm.saleem.StockholmTransformationFactory;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RunGISCreator {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String path = "/Users/GunnarF/NoBackup/data-workspace/pt/results/2018-07-31a/output";
		final String configFileName = path + "/output_config2.xml";
		final String eventsFileName = path + "/output_events.xml.gz";
		final String nodesFileName = path + "/nodes.shp";
		final String linksFileName = path + "/links.shp";

		Config config = ConfigUtils.loadConfig(configFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// new Nodes2ESRIShape(scenario.getNetwork(), nodesFileName, TransformationFactory.WGS84).write();

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(scenario.getNetwork(), linksFileName, builder).write();

		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(),
				MGC.getCRS(TransformationFactory.WGS84), path);
		sp.setOutputSample(0.05);
		sp.setActBlurFactor(100);
		sp.setLegBlurFactor(100);
		sp.setWriteActs(true);
		sp.setWriteLegs(true);

		sp.write();

		System.out.println("... DONE");

	}

}
