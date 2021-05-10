/*
 * Copyright 2021 Gunnar Flötteröd
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
package samgods;

import static java.lang.Math.random;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.Units;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SamgodsTrainPopulationCreator {

	public static final String ENTRY_ACT = "entry";
	public static final String EXIT_ACT = "exit";

	// No travel before 8pm on day 1.
	public static final double EARLIEST_ENTRY_TIME_S = 20.0 * Units.S_PER_H;

	// No travel after 6am on day 2.
	public static final double LATEST_EXIT_TIME_S = (24.0 + 6.0) * Units.S_PER_H;

	private final Population population;

	private Link getEntryLink(Node node, Network network) {
		final String idStr = node.getId().toString() + "_ENTRY";
		final Id<Link> entryLinkId = Id.createLinkId(idStr);
		Link result = network.getLinks().get(entryLinkId);
		if (result == null) {
			final Node entryNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(idStr), node.getCoord());
			result = NetworkUtils.createAndAddLink(network, entryLinkId, entryNode, node, 1.0, 1000.0, 1e8, 1e8, null,
					null);
		}
		return result;
	}

	private Link getExitLink(Node node, Network network) {
		final String idStr = node.getId().toString() + "_EXIT";
		final Id<Link> exitLinkId = Id.createLinkId(idStr);
		Link result = network.getLinks().get(exitLinkId);
		if (result == null) {
			final Node exitNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(idStr), node.getCoord());
			result = NetworkUtils.createAndAddLink(network, exitLinkId, node, exitNode, 1.0, 1000.0, 1000.0, 1.0, null,
					null);
		}
		return result;
	}

	public SamgodsTrainPopulationCreator(final Network network,
			final Map<Tuple<Id<Node>, Id<Node>>, List<Vehicle>> nodeIds2trains) {
		this.population = PopulationUtils.createPopulation(ConfigUtils.createConfig(), network);

		for (Map.Entry<Tuple<Id<Node>, Id<Node>>, List<Vehicle>> entry : nodeIds2trains.entrySet()) {
			final Id<Node> fromNode = entry.getKey().getA();
			final Id<Node> toNode = entry.getKey().getB();
			final List<Vehicle> vehicles = entry.getValue();
			if (!network.getNodes().containsKey(fromNode) || !network.getNodes().containsKey(toNode)) {
				System.out.println("Lost OD pair (" + fromNode + "," + toNode + ").");
			} else {
				for (Vehicle trainVehicle : vehicles) {
					final Person trainPerson = this.population.getFactory()
							.createPerson(Id.createPersonId(trainVehicle.getId()));
					this.population.addPerson(trainPerson);

					final Plan plan = this.population.getFactory().createPlan();
					trainPerson.addPlan(plan);

					final Link entryLink = this.getEntryLink(network.getNodes().get(fromNode), network);
					final Link exitLink = this.getExitLink(network.getNodes().get(toNode), network);

					final double dptTime_s = EARLIEST_ENTRY_TIME_S
							+ random() * (LATEST_EXIT_TIME_S - EARLIEST_ENTRY_TIME_S);

					final Activity entryAct = this.population.getFactory().createActivityFromLinkId(ENTRY_ACT,
							entryLink.getId());
					entryAct.setEndTime(dptTime_s);
					plan.addActivity(entryAct);

					final Leg leg = this.population.getFactory().createLeg("car"); // TODO all params. relative to car
					leg.setDepartureTime(dptTime_s);
					plan.addLeg(leg);

					final Activity exitAct = this.population.getFactory().createActivityFromLinkId(EXIT_ACT,
							exitLink.getId());
					plan.addActivity(exitAct);
				}
			}
		}
		System.out.println("Created " + this.population.getPersons().size() + " trains.");
	}

	public Population getPopulation() {
		return this.population;
	}
}
