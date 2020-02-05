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

import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;

import stockholm.wum.analysis.DifferentiatedPTUsageAnalyzer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TravelersPerMode2GIS {

	private final Scenario scenario;

	private final Map<String, Set<Id<Person>>> mode2userIds;

	public TravelersPerMode2GIS(final Scenario scenario, final Map<String, Set<Id<Person>>> mode2userIds) {
		this.scenario = scenario;
		this.mode2userIds = mode2userIds;
	}

	public void run(final String path) {

		for (Map.Entry<String, Set<Id<Person>>> entry : this.mode2userIds.entrySet()) {

			System.out.println("processing mode " + entry.getKey() + " with " + entry.getValue().size() + " users");

//			final List<Person> persons = new ArrayList<>(entry.getValue().size());
			Population tmpPop = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
			for (Id<Person> personId : entry.getValue()) {
//				persons.add(this.scenario.getPopulation().getPersons().get(personId));
				tmpPop.addPerson( this.scenario.getPopulation().getPersons().get( personId ) );
			}

			final String outPrefix = FilenameUtils.concat(path, entry.getKey());
			
//			final SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(persons, this.scenario.getNetwork(),
//					MGC.getCRS(TransformationFactory.WGS84), outPrefix);
			final SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(tmpPop, this.scenario.getNetwork(),
					MGC.getCRS(TransformationFactory.WGS84), outPrefix);
			
			// I replaced persons in the code above by tmpPop to make the code compile.  Have not tested what it does.  kai, aug'18
			
			sp.setOutputSample(1.0);
//			sp.setActBlurFactor(100);
//			sp.setLegBlurFactor(100);
			sp.setActBlurFactor(0);
			sp.setLegBlurFactor(0);
			sp.setWriteActs(true);
			sp.setWriteLegs(true);
			sp.write();
		}
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		final String path = "/Users/GunnarF/NoBackup/data-workspace/pt/2018-08-09_scenario/";
		final String outPath = path + "output/";
		final String configFileName = path + "config.xml";

		final Config config = ConfigUtils.loadConfig(configFileName);
		config.network().addParam("inputNetworkFile", "output/output_network.xml.gz");
		config.plans().addParam("inputPlansFile", "output/output_plans.xml.gz");
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final DifferentiatedPTUsageAnalyzer ptAnalyzer = new DifferentiatedPTUsageAnalyzer(scenario);
		ptAnalyzer.run();

		final TravelersPerMode2GIS travelerWriter = new TravelersPerMode2GIS(scenario, ptAnalyzer.getMode2userIds());
		travelerWriter.run(outPath);

		System.out.println("... DONE");

	}

}