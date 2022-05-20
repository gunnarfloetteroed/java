/*
 * Greedo -- Equilibrium approximation for general-purpose multi-agent simulations.
 *
 * Copyright 2022 Gunnar Flötteröd
 * 
 *
 * This file is part of Greedo.
 *
 * Greedo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Greedo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Greedo.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@gmail.com
 *
 */
package org.matsim.contrib.greedo;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emulation.emulators.ScheduleBasedTransitLegEmulator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class GreedoExample {

	static void configureGreedo(Greedo greedo) {

		/*
		 * Configurations beyond automagic. This is fairly stable but not yet well
		 * documented. Talk to Gunnar if you want to use this.
		 */

		// To emulate public transport according to schedule and not by teleportation.
		// Allows to plug in custom emulators.
		greedo.setEmulator("pt", ScheduleBasedTransitLegEmulator.class);

		// Allows to emulate "complicated" modes (e.g. carsharing) where one leg
		// consists of multiple elements (e.g. walk to station, rent, drive, etc)
		greedo.setDecomposer("mode to be decomposed", null /* put class for decomposition here */);
	}

	public static void main(String[] args) {

		Greedo greedo = new Greedo();

		configureGreedo(greedo);

		Config config = ConfigUtils.loadConfig("path to your config");
		greedo.meet(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		greedo.meet(controler);

		controler.run();
	}

}
