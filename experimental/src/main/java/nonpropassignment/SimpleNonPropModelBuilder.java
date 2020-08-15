/*
 * Copyright 2020 Gunnar Flötteröd
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
package nonpropassignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SimpleNonPropModelBuilder {

	// -------------------- MEMBERS --------------------

	// pre-build

	private String networkFileName = null;

	private int analysisIntervalStart_s = 0;

	private int analysisIntervalEnd_s = 24 * 3600;

	// post-build

	private Network network = null;

	private SymmetricLinkPairIndexer linkPairIndexer = null;

	private SimpleNonPropModel model = null;

	// -------------------- CONSTRUCTION --------------------

	public SimpleNonPropModelBuilder() {
	}

	// -------------------- BUILDING --------------------

	// network

	public SimpleNonPropModelBuilder setNetworkFileName(final String networkFileName) {
		this.networkFileName = networkFileName;
		return this;
	}

	// analysis time interval

	public SimpleNonPropModelBuilder setAnalysisTimeInterval(final int start_s, final int end_s) {
		this.analysisIntervalStart_s = start_s;
		this.analysisIntervalEnd_s = end_s;
		return this;
	}

	// -------------------- INTERNALS --------------------

	private void evaluateSelectedPlan(final Person person, final TravelTime travelTime,
			final Map<Id<Person>, int[]> personId2linkUsage, final Map<Id<Person>, Double> personId2cost) {
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				final Leg leg = (Leg) pe;
				if ((leg.getDepartureTime().seconds() >= this.analysisIntervalStart_s) && "car".equals(leg.getMode())) {
					final NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
					int[] linkIndices = new int[1 + networkRoute.getLinkIds().size()];
					int i = 0;
					// first link
					double time_s = leg.getDepartureTime().seconds();
					Link link = this.network.getLinks().get(networkRoute.getStartLinkId());
					linkIndices[i++] = this.linkPairIndexer.getLinkIndex(link.getId());
					time_s += travelTime.getLinkTravelTime(link, time_s, person, null);
					// intermediate links
					for (Id<Link> linkId : networkRoute.getLinkIds()) {
						link = this.network.getLinks().get(linkId);
						linkIndices[i++] = this.linkPairIndexer.getLinkIndex(link.getId());
						time_s += travelTime.getLinkTravelTime(link, time_s, person, null);
					}
					// last link ignored since not traversed
					// include only if completely within analysis time interval
					if (time_s < this.analysisIntervalEnd_s) {
						personId2cost.put(person.getId(), time_s);
						if (personId2linkUsage != null) {
							personId2linkUsage.put(person.getId(), linkIndices);
						}
					}
				}
			}
		}
	}

	// -------------------- BUILDING --------------------

	public SimpleNonPropModel build() {

		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile(this.networkFileName);
		this.network = ScenarioUtils.loadScenario(config).getNetwork();
		this.linkPairIndexer = new SymmetricLinkPairIndexer(this.network);

		final double[] minC = new double[this.linkPairIndexer.getNumberOfLinks()];
		for (Map.Entry<Id<Link>, Integer> entry : this.linkPairIndexer.getLink2IndexView().entrySet()) {
			final Link link = this.network.getLinks().get(entry.getKey());
			minC[entry.getValue()] = link.getLength() / link.getFreespeed();
		}

		this.model = new SimpleNonPropModel(minC);
		return this.model;
	}

	public void update(final String populationFileName, final String eventFileName) {

		final TravelTime travelTime;
		{
			final TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(this.network);
			builder.setAnalyzedModes(new LinkedHashSet<>(Arrays.asList("car")));
			builder.setCalculateLinkToLinkTravelTimes(false);
			builder.setCalculateLinkTravelTimes(true);
			builder.setFilterModes(false); // TODO What does this mean?
			builder.setMaxTime(36 * 3600);
			builder.setTimeslice(3600); // for the time being, hourly time slices
			final TravelTimeCalculator travelTimeCalculator = builder.build();
			final EventsManager eventsManager = EventsUtils.createEventsManager();
			eventsManager.addHandler(travelTimeCalculator);
			EventsUtils.readEvents(eventsManager, eventFileName);
			travelTime = travelTimeCalculator.getLinkTravelTimes();
		}

		final Population newPopulation;
		{
			final Config config = ConfigUtils.createConfig();
			config.plans().setInputFile(populationFileName);
			newPopulation = ScenarioUtils.loadScenario(config).getPopulation();
		}

		final Map<Id<Person>, int[]> personId2linkUsage = new LinkedHashMap<>();
		final Map<Id<Person>, Double> personId2experiencedCost = new LinkedHashMap<>();
		{
			for (Person person : newPopulation.getPersons().values()) {
				this.evaluateSelectedPlan(person, travelTime, personId2linkUsage, personId2experiencedCost);
			}
		}

		final List<int[]> allXn = new ArrayList<>(personId2linkUsage.size());
		final List<Double> realizedCost = new ArrayList<>(personId2linkUsage.size());

		for (Id<Person> personId : personId2linkUsage.keySet()) {
			allXn.add(personId2linkUsage.get(personId));
			realizedCost.add(personId2experiencedCost.get(personId));
		}

		
		final List<Double> freeFlowCost = this.model.predictFreeFlowCost(allXn);
		final double[] x = this.model.sumIntoX(allXn);
		
		double[] g = this.model.predictG(x);
		double gSum = Utils.sum(g);
		List<Double> predictedCongestionCost = this.model.predictCongestionCost(allXn, g, gSum);
		final double beforeQ = this.model.evaluate(allXn, realizedCost, freeFlowCost, predictedCongestionCost);

		this.model.update(allXn, realizedCost, freeFlowCost, predictedCongestionCost, x, g, gSum);

		g = this.model.predictG(x);
		gSum = Utils.sum(g);
		predictedCongestionCost = this.model.predictCongestionCost(allXn, g, gSum);
		final double afterQ = this.model.evaluate(allXn, realizedCost, freeFlowCost, predictedCongestionCost);

		System.out.println("BEFORE\t" + beforeQ + "\tAFTER:\t" + afterQ);
	}

	public SimpleNonPropModel getModel() {
		return this.model;
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		final String path = "/Users/GunnarF/NoBackup/data-workspace/laplace/sioux-falls/";
		final SimpleNonPropModelBuilder builder = new SimpleNonPropModelBuilder();
		builder.setNetworkFileName(path + "Siouxfalls_network_PT.xml").setAnalysisTimeInterval(7 * 3600 + 1800,
				8 * 3600 + 1800);
		builder.build();
		for (int it = 0; it < 100; it++) {
			System.out.println("LOADING ITERATION " + it);
			builder.update(path + "2020-01-27_output/ITERS/it." + it + "/" + it + ".plans.xml.gz",
					path + "2020-01-27_output/ITERS/it." + it + "/" + it + ".events.xml.gz");
		}

		System.out.println("... DONE");
	}
}
