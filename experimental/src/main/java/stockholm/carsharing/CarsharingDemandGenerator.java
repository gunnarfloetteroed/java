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
package stockholm.carsharing;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CarsharingDemandGenerator {

	private final Scenario scenario;

	private final Function<Person, String> membershipAssignment;

	private final BiFunction<Person, String, String> subscriptionAssignment;

	CarsharingDemandGenerator(final String populationFile, final Function<Person, String> membershipAssignment,
			BiFunction<Person, String, String> subscriptionAssignment) {
		this.membershipAssignment = membershipAssignment;
		this.subscriptionAssignment = subscriptionAssignment;
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(populationFile);
		this.scenario = ScenarioUtils.loadScenario(config);
	}

	void keepOnlyStrictCarUsers() {
		keepOnlyStrictCarUsers(this.scenario.getPopulation());
	}

	public static void keepOnlyStrictCarUsers(final Population population) {
		final Set<Id<Person>> remove = new LinkedHashSet<>();
		for (Person person : population.getPersons().values()) {
			for (PlanElement planEl : person.getSelectedPlan().getPlanElements()) {
				if (planEl instanceof Leg && !TransportMode.car.equals(((Leg) planEl).getMode())) {
					remove.add(person.getId());
				}
			}
		}
		for (Id<Person> removeId : remove) {
			population.getPersons().remove(removeId);
		}
	}

	void createMemberships(final String membershipFile) {
		try {
			final PrintWriter writer = new PrintWriter(membershipFile);
			writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
			writer.println("<!DOCTYPE memberships SYSTEM \"src/main/resources/dtd/CSMembership.dtd\">");
			writer.println("<memberships>");
			for (Person person : scenario.getPopulation().getPersons().values()) {
//				final String company = this.membershipAssignment.apply(person);
//				final String type = this.subscriptionAssignment.apply(person, company);
//				if ((company != null) && (type != null)) {
//					writer.println("  <person id=\"" + person.getId() + "\">");
//					writer.println("    <company id=\"" + company + "\">");
//					writer.println("      <carsharing name=\"" + type + "\"/>");
//					writer.println("    </company>");
//					writer.println("  </person>");
//				}
				writer.println("  <person id=\"" + person.getId() + "\">");
				final String company = this.membershipAssignment.apply(person);
				if (company != null) {
					writer.println("    <company id=\"" + company + "\">");
					final String type = this.subscriptionAssignment.apply(person, company);
					// if (type != null) {
					writer.println("      <carsharing name=\"" + type + "\"/>");
					// }
					writer.println("    </company>");
				}
				writer.println("  </person>");
			}
			writer.println("</memberships>");
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		// TODO Problem with people not having a membership. Check what is needed.

		final String path = "./scenarios/EkoCS-Trans/input/";
		CarsharingDemandGenerator gen = new CarsharingDemandGenerator(path + "1PctAllModes_enriched.xml",
				person -> (Math.random() < 1.0) ? "m" : null,
				(person, company) -> (Math.random() < 0.5) ? "oneway" : "twoway");
		System.out.println("Loaded " + gen.scenario.getPopulation().getPersons().size() + " persons.");

		gen.keepOnlyStrictCarUsers();
		System.out.println("Kept " + gen.scenario.getPopulation().getPersons().size() + " strict car users.");

		gen.createMemberships(path + "memberships.xml");

		System.out.println("... DONE");
	}

}
