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
package org.matsim.contrib.greedo.analysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ScoreDistributionAnalyzer {

	public Map<Id<Person>, Double> loadScores(final String populationFile) {
		final Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(populationFile);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario.getPopulation().getPersons().entrySet().stream().collect(
				Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().getSelectedPlan().getScore()));
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		Map<Id<Person>, Double> id2score = new ScoreDistributionAnalyzer().loadScores(
				"/Users/GunnarF/NoBackup/data-workspace/ihop4/test/greedo.xml.gz");

		List<Double> scores = id2score.values().stream().collect(Collectors.toList());
		Collections.sort(scores);
		scores.stream().forEach(s -> System.out.println(s));
		
		System.out.println("... DONE");
	}

}
