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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
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
import org.matsim.vehicles.Vehicle;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class PropModelBuilder {

	// -------------------- MEMBERS --------------------

	// pre-build

	private double regularizationWeight = 0.0;

	private String networkFileName = null;

	private int analysisIntervalStart_s = 0;

	private int analysisIntervalEnd_s = 24 * 3600;

	// post-build

	private Network network = null;

	private SymmetricLinkPairIndexer linkPairIndexer = null;

	private PropModel model = null;

	private ConstantTraveltimeModel constantTTmodel;

	// -------------------- CONSTRUCTION --------------------

	public PropModelBuilder() {
	}

	// -------------------- BUILDING --------------------

	public PropModelBuilder setRegularizationWeight(final double regularizationWeight) {
		this.regularizationWeight = regularizationWeight;
		return this;
	}

	// network

	public PropModelBuilder setNetworkFileName(final String networkFileName) {
		this.networkFileName = networkFileName;
		return this;
	}

	// analysis time interval

	public PropModelBuilder setAnalysisTimeInterval(final int start_s, final int end_s) {
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
				if ((leg.getDepartureTime() >= this.analysisIntervalStart_s) && "car".equals(leg.getMode())) {
					final NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
					int[] linkIndices = new int[1 + networkRoute.getLinkIds().size()];
					int i = 0;
					// first link
					double time_s = leg.getDepartureTime();
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
						personId2cost.put(person.getId(), time_s - leg.getDepartureTime());
						if (personId2linkUsage != null) {
							personId2linkUsage.put(person.getId(), linkIndices);
						}
					}
				}
			}
		}
	}

	// -------------------- BUILDING --------------------

	public PropModel build() {
		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile(this.networkFileName);
		this.network = ScenarioUtils.loadScenario(config).getNetwork();
		this.linkPairIndexer = new SymmetricLinkPairIndexer(this.network);
		this.model = new PropModel(this.linkPairIndexer.getNumberOfLinks(), this.regularizationWeight);

		this.constantTTmodel = new ConstantTraveltimeModel(this.network, this.linkPairIndexer,
				this.analysisIntervalStart_s, this.analysisIntervalEnd_s);

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
		this.constantTTmodel.addTravelTime(travelTime);

		final Population newPopulation;
		{
			final Config config = ConfigUtils.createConfig();
			config.plans().setInputFile(populationFileName);
			newPopulation = ScenarioUtils.loadScenario(config).getPopulation();
		}

		final Map<Id<Person>, int[]> personId2linkUsage = new LinkedHashMap<>();
		final Map<Id<Person>, Double> personId2experiencedCost = new LinkedHashMap<>();
		final Map<Id<Person>, Double> personId2freeFlowCost = new LinkedHashMap<>();
		{
			for (Person person : newPopulation.getPersons().values()) {
				this.evaluateSelectedPlan(person, travelTime, personId2linkUsage, personId2experiencedCost);
				this.evaluateSelectedPlan(person, new TravelTime() {
					@Override
					public double getLinkTravelTime(Link link, double arg1, Person arg2, Vehicle arg3) {
						return link.getLength() / link.getFreespeed();
					}
				}, null, personId2freeFlowCost);
			}
		}

		final List<int[]> allXn = new ArrayList<>(personId2linkUsage.size());
		final List<Double> experiencedCosts = new ArrayList<>(personId2linkUsage.size());
		final List<Double> freeFlowCosts = new ArrayList<>(personId2linkUsage.size());
		for (Id<Person> personId : personId2linkUsage.keySet()) {
			allXn.add(personId2linkUsage.get(personId));
			experiencedCosts.add(personId2experiencedCost.get(personId));
			freeFlowCosts.add(personId2freeFlowCost.get(personId));
		}

		final Plans plans = new Plans(allXn, experiencedCosts, freeFlowCosts, this.model.getLinkCnt());
		this.model.addPlans(plans);
	}

	public PropModel getModel() {
		return this.model;
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		final int startIt = 50;
		final int endIt = 100;

		final PropModelBuilder builder = new PropModelBuilder();
		builder.setNetworkFileName("output_network.xml.gz").setAnalysisTimeInterval(17 * 3600, 18 * 3600);
		builder.build();
		for (int it = startIt; it < endIt; it++) {
			System.out.println("LOADING ITERATION " + it);
			builder.update("output/ITERS/it." + it + "/" + it + ".plans.xml.gz",
					"output/ITERS/it." + it + "/" + it + ".events.xml.gz");
		}
		PropModel model = builder.getModel();

		// System.out.println("TERMINATING");
		// System.exit(0);

		try {
			FileUtils.writeStringToFile(new File("result_propModel.txt"),
					"regularizationWeight" + "\t" + "Q" + "\t" + "fit" + "\n", false);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		final Random rnd = new Random();
		double[] point = new double[model.getLinkCnt()];
		for (int i = 0; i < point.length; i++) {
			point[i] = 2.0 * (rnd.nextDouble() - 0.5);
		}
		
		for (double regularizationWeight : new double[] { 1e-3, 1e-6, 1e-9, 1e-12 }) {
			model = new PropModel(regularizationWeight, model);

			final PropModelEstimator estimator = new PropModelEstimator(model, 1e-8, Integer.MAX_VALUE,
					Integer.MAX_VALUE);
			System.out.println(
					"ESTIMATING: regularizationWeight = " + regularizationWeight);
			point = estimator.run(point);

			model.getObjectiveFunction().value(point);

			try {
				FileUtils.writeStringToFile(new File("result_propModel.txt"), regularizationWeight + "\t"
						+ model.getQ() + "\t" + model.getFitWithoutRegularization() + "\n", true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		System.out.println("... DONE");
	}

}
