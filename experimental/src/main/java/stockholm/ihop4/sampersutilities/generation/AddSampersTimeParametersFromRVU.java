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
package stockholm.ihop4.sampersutilities.generation;

import static stockholm.ihop4.sampersutilities.SampersAttributeUtils.setPlannedActivityDuration_h;
import static stockholm.ihop4.sampersutilities.SampersAttributeUtils.setPlannedActivityStart_h;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.utilities.Units;
import stockholm.ihop2.regent.demandreading.PopulationCreator;
import stockholm.ihop4.rvu2013.RVU2013Analyzer;
import stockholm.ihop4.rvu2013.TourSequenceTimeStructures.TimeStructure;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AddSampersTimeParametersFromRVU {

	// -------------------- MEMBERS --------------------

	private final RVU2013Analyzer rvuAnalyzer;

	private final Map<List<String>, LinkedList<TimeStructure>> purposes2timeStructures;

	// -------------------- CONSTRUCTION --------------------

	public AddSampersTimeParametersFromRVU(final String rvuFile) {
		this.rvuAnalyzer = new RVU2013Analyzer(rvuFile);
		this.purposes2timeStructures = new LinkedHashMap<>();
	}

	// ---------- INTERNALS: VARIANCE-REDUCED TIME STRUCTURE SAMPLING ----------

	private TimeStructure drawTimeStructure(final String... purposesArray) {
		final List<String> purposesList = Arrays.asList(purposesArray);
		LinkedList<TimeStructure> timeStructures = this.purposes2timeStructures.get(purposesList);
		if (timeStructures == null || timeStructures.size() == 0) {
			timeStructures = new LinkedList<>(this.rvuAnalyzer.getTimeStructures().getTimeStructures(purposesArray));
			Collections.shuffle(timeStructures);
			this.purposes2timeStructures.put(purposesList, timeStructures);
		}
		return timeStructures.removeFirst();
	}

	// -------------------- IMPLEMENTATION --------------------

	public void enrich(final Plan plan) {

		/*
		 * The following is hard-coded for only the following tour sequences:
		 * 
		 * work, other, work-other.
		 * 
		 * Will most likely go wrong if the plan contains a different travel pattern.
		 */

		// Figure out what tours are contained in the plan. Ordering is unique anyway.
		boolean containsWork = false;
		boolean containsOther = false;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				final Activity act = (Activity) pe;
				containsWork |= PopulationCreator.WORK.equals(act.getType());
				containsOther |= PopulationCreator.OTHER.equals(act.getType());
			}
		}

		// Draw a suitable time structure.
		final TimeStructure timeStructure;
		{
			if (containsWork && !containsOther) {
				timeStructure = this.drawTimeStructure(PopulationCreator.WORK);
			} else if (!containsWork && containsOther) {
				timeStructure = this.drawTimeStructure(PopulationCreator.OTHER);
			} else if (containsWork && containsOther) {
				timeStructure = this.drawTimeStructure(PopulationCreator.WORK, PopulationCreator.OTHER);
			} else {
				throw new RuntimeException("containsWork = " + containsWork + ", containsOther = " + containsOther);
			}
		}

		// Insert the time structure into the plan.
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				final Activity act = (Activity) pe;
				if (PopulationCreator.INTERMEDIATE_HOME.equals(act.getType())) {
					final double dur_s = timeStructure.intermedHomeDur_s(0);
					setPlannedActivityStart_h(act, null);
					setPlannedActivityDuration_h(act, Units.H_PER_S * dur_s);
				} else if (PopulationCreator.WORK.equals(act.getType())) {
					setPlannedActivityStart_h(act, Units.H_PER_S * timeStructure.start_s(0));
					final double dur_s = timeStructure.duration_s(0);
					setPlannedActivityDuration_h(act, Units.H_PER_S * dur_s);
				} else if (PopulationCreator.OTHER.equals(act.getType())) {
					final int otherTourIndex = (containsWork ? 1 : 0);
					final double dur_s = timeStructure.duration_s(otherTourIndex);
					setPlannedActivityStart_h(act, Units.H_PER_S * timeStructure.start_s(otherTourIndex));
					setPlannedActivityDuration_h(act, Units.H_PER_S * dur_s);
				} else {
					setPlannedActivityStart_h(act, null);
					setPlannedActivityDuration_h(act, null);
				}
			}
		}
	}

	public void enrich(final Population population) {
		for (Person person : population.getPersons().values()) {
			// Ensure a non-null income.
			if (person.getAttributes().getAttribute(PopulationCreator.income_SEK_yr) == null) {
				person.getAttributes().putAttribute(PopulationCreator.income_SEK_yr, 0);
			}
			// Keep and enrich only the selected plan.
			PersonUtils.removeUnselectedPlans(person);
			this.enrich(person.getSelectedPlan());
		}
	}

	public static void main(String[] args) {

		// final Config config = ConfigUtils
		// .loadConfig("/Users/GunnarF/NoBackup/data-workspace/ihop4/production-scenario/config.xml");
		// final Scenario scenario = ScenarioUtils.loadScenario(config);
		//
		// final AddSampersTimeParametersFromRVU enricher = new
		// AddSampersTimeParametersFromRVU(
		// "/Users/GunnarF/OneDrive - VTI/My
		// Data/ihop4/rvu2013/MDRE_1113_original.csv");
		// enricher.enrich(scenario.getPopulation());
		//
		// PopulationUtils.writePopulation(scenario.getPopulation(),
		// "/Users/GunnarF/NoBackup/data-workspace/ihop4/production-scenario/enriched-population_TMP.xml");

		// for (String frac : new String[] { "1", "5", "25" }) {

			Config config = ConfigUtils.createConfig();
			config.plans().setInputFile("/Users/GunnarF/NoBackup/data-workspace/wum/production-scenario/25PctAllModes.xml");
			Scenario scenario = ScenarioUtils.loadScenario(config);

			AddSampersTimeParametersFromRVU enricher = new AddSampersTimeParametersFromRVU(
					"/Users/GunnarF/OneDrive - VTI/My Data/ihop4/rvu2013/MDRE_1113_original.csv");
			enricher.enrich(scenario.getPopulation());

			PopulationUtils.writePopulation(scenario.getPopulation(),
					"/Users/GunnarF/NoBackup/data-workspace/wum/production-scenario/25PctAllModes_enriched.xml");
		// }
	}
}
