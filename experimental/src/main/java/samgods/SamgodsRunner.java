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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

import floetteroed.utilities.Units;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SamgodsRunner {

	static void createScenario() {
		final SamgodsNetworkCreator networkReader = new SamgodsNetworkCreator("./scenarios/samgods/Kapacitet.txt",
				"./scenarios/samgods/Network_Node.txt", "./scenarios/samgods/Network_Link.txt", true);
		NetworkUtils.runNetworkCleaner(networkReader.getNetwork());

		final SamgodsODCreator odReader = new SamgodsODCreator("./scenarios/samgods/Nyckel_Emme_Voyager.csv",
				"./scenarios/samgods/train-ods/");
		final SamgodsVehiclesCreator vehCreator = new SamgodsVehiclesCreator(odReader.getZone2nodeIds(), 200.0);
		vehCreator.writeVehicles("./scenarios/samgods/vehicles.xml");

		final SamgodsTrainPopulationCreator popCreator = new SamgodsTrainPopulationCreator(networkReader.getNetwork(),
				vehCreator.getNodeIds2trains());
		PopulationUtils.writePopulation(popCreator.getPopulation(), "./scenarios/samgods/population.xml");
		NetworkUtils.writeNetwork(networkReader.getNetwork(), "./scenarios/samgods/network.xml");

	}

	static void createConfigAndInitialRun() {
		final double simEndTime_s = 48.0 * Units.S_PER_H;

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("population.xml");
		config.vehicles().setVehiclesFile("vehicles.xml");
		config.qsim().setStartTime(0.0);
		config.qsim().setEndTime(1e8);
		config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.minOfEndtimeAndMobsimFinished);
		config.qsim().setFlowCapFactor(10.0);
		config.qsim().setStorageCapFactor(10.0);
		config.qsim().setUsePersonIdForMissingVehicleId(true);
		config.controler().setLastIteration(0);
		config.controler().setDumpDataAtEnd(true);
		config.controler().setOutputDirectory("./scenarios/samgods/output/");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		{
			final StrategySettings reRoute = new StrategySettings();
			reRoute.setStrategyName(DefaultStrategy.ReRoute);
			reRoute.setSubpopulation(null);
			reRoute.setWeight(1.0);
			config.strategy().addStrategySettings(reRoute);
		}
		{
			final ActivityParams entryParams = new ActivityParams(SamgodsTrainPopulationCreator.ENTRY_ACT);
			entryParams.setLatestStartTime(0.0);
			entryParams.setEarliestEndTime(SamgodsTrainPopulationCreator.EARLIEST_ENTRY_TIME_S);
			entryParams.setTypicalDuration(SamgodsTrainPopulationCreator.EARLIEST_ENTRY_TIME_S);
			config.planCalcScore().addActivityParams(entryParams);
		}
		{
			final ActivityParams exitParams = new ActivityParams(SamgodsTrainPopulationCreator.EXIT_ACT);
			exitParams.setLatestStartTime(SamgodsTrainPopulationCreator.LATEST_EXIT_TIME_S);
			// exitParams.setEarliestEndTime(simEndTime_s);
			exitParams.setTypicalDuration(simEndTime_s - SamgodsTrainPopulationCreator.LATEST_EXIT_TIME_S);
			config.planCalcScore().addActivityParams(exitParams);
		}
		ConfigUtils.writeConfig(config, "./scenarios/samgods/config.xml");

		config = ConfigUtils.loadConfig("./scenarios/samgods/config.xml");
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controller = new Controler(scenario);
		controller.addControlerListener(new StartupListener() {
			@Inject
			private Provider<ReplanningContext> replanningContextProvider;

			@Override
			public void notifyStartup(StartupEvent event) {
				final StrategyManager strategyManager = event.getServices().getStrategyManager();
				strategyManager.run(event.getServices().getScenario().getPopulation(),
						this.replanningContextProvider.get());
			}
		});
		controller.run();
	}

	static void run() {
		final Config config = ConfigUtils.loadConfig("./scenarios/samgods/output_config.xml");
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controller = new Controler(scenario);
		controller.run();
	}

	public static void main(String[] args) {
		createScenario();
//		createConfigAndInitialRun();
		run();
	}

}
