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
package org.matsim.contrib.opdyts.buildingblocks;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Rule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.MATSimOpdytsRunner;
import org.matsim.contrib.opdyts.OpdytsConfigGroup;
import org.matsim.contrib.opdyts.buildingblocks.convergencecriteria.AR1ConvergenceCriterion;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes.ClosingTime;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes.OpeningTime;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes.TypicalDuration;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariable;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariableBuilder;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.OneAtATimeRandomizer;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar.ScalarRandomizer;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.utils.EveryIterationScoringParameters;
import org.matsim.contrib.opdyts.buildingblocks.objectivefunctions.calibration.LegHistogramObjectiveFunction;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.microstate.MATSimStateFactoryImpl;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunction;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunctionSum;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CalibrateOpeningTimesFromDepartureHistogram {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private static URL EQUIL_DIR = ExamplesUtils.getTestScenarioURL("equil");

	public void test() {

		// Config config = ConfigUtils.loadConfig(IOUtils.newUrl(EQUIL_DIR, "config.xml"));
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(EQUIL_DIR, "config.xml"));
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(1000);
		config.plans().setInputFile(
				"C:/Users/GunnarF/OneDrive - VTI/My Code/git-2018/Opdyts-MATSim-Integration/output_plans.xml.gz");

		StrategySettings timeChoice = new StrategySettings();
		timeChoice.setStrategyName("TimeAllocationMutator");
		timeChoice.setWeight(0.1);
		config.strategy().addStrategySettings(timeChoice);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.run();
	}

	public void test2() {

		// Config config = ConfigUtils.loadConfig(IOUtils.newUrl(EQUIL_DIR, "config.xml"));
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(EQUIL_DIR, "config.xml"));
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(3);

		config.plans().setInputFile(
				"C:/Users/GunnarF/OneDrive - VTI/My Code/git-2018/Opdyts-MATSim-Integration/output_plans.xml.gz");

		StrategySettings timeChoice = new StrategySettings();
		timeChoice.setStrategyName("TimeAllocationMutator");
		timeChoice.setWeight(0.1);
		config.strategy().addStrategySettings(timeChoice);

		OpdytsConfigGroup opdytsConfig = ConfigUtils.addOrGetModule(config, OpdytsConfigGroup.class);
		opdytsConfig.setBinCount(24 * 12);
		opdytsConfig.setBinSize(3600 / 12);
		opdytsConfig.setInertia(0.9);
		opdytsConfig.setInitialEquilibriumGapWeight(0.0);
		opdytsConfig.setInitialUniformityGapWeight(0.0);
		opdytsConfig.setMaxIteration(100);
		opdytsConfig.setMaxMemoryPerTrajectory(Integer.MAX_VALUE);
		opdytsConfig.setMaxTotalMemory(Integer.MAX_VALUE);
		opdytsConfig.setMaxTransition(Integer.MAX_VALUE);
		opdytsConfig.setNoisySystem(true);
		opdytsConfig.setNumberOfIterationsForAveraging(50);
		opdytsConfig.setNumberOfIterationsForConvergence(100);
		opdytsConfig.setSelfTuningWeightScale(1.0);
		opdytsConfig.setStartTime(0);
		opdytsConfig.setUseAllWarmUpIterations(true);
		opdytsConfig.setWarmUpIterations(1);

		// OBJECTIVE FUNCTION

		Set<String> modes = new LinkedHashSet<>(Arrays.asList("car"));

		double[] realDepartures = new double[362];
		// Everybody departs at 4 to work. Bin size 300 sec -> bin nr = 4 * 3600 / 300 =
		// 48
		realDepartures[48] = 100;
		// Everybody departs at 5 from work. Bin size 300 sec -> bin nr = 5 * 3600 / 300
		// = 60
		realDepartures[60] = 100;
		MATSimObjectiveFunction<MATSimState> dptObjFct = LegHistogramObjectiveFunction.newDepartures(modes,
				realDepartures);

		// double[] realArrivals = new double[362];
		// // Everybody arrives one bin later
		// realArrivals[61] = 100;
		// MATSimObjectiveFunction<MATSimState> arrObjFct =
		// LegHistogramObjectiveFunction.newDepartures(modes,
		// realArrivals);

		MATSimObjectiveFunctionSum<MATSimState> objFct = new MATSimObjectiveFunctionSum<>();
		objFct.add(dptObjFct, 1.0);
		// objFct.add(arrObjFct, 1.0);

		// WIRE EVERYTHING TOGETHER

		Scenario scenario = ScenarioUtils.loadScenario(config);
		MATSimOpdytsRunner<CompositeDecisionVariable, MATSimState> runner = new MATSimOpdytsRunner<>(scenario,
				new MATSimStateFactoryImpl<>());
		runner.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
				// this.addControlerListenerBinding().toInstance(stateFactory);
			}
		});
		runner.setConvergenceCriterion(new AR1ConvergenceCriterion(2.5));

		// >>>>>>>>>> TODO SPEC OF COMPOSITE (TIME) DECISION VARIABLES >>>>>>>>>>

		CompositeDecisionVariableBuilder builder = new CompositeDecisionVariableBuilder();
		double deltaTime_s = 15 * 60;
//		for (ActivityParams actParams : config.planCalcScore().getActivityParams()) {
//			if (!Time.isUndefinedTime(actParams.getOpeningTime())) {
//				builder.add(new OpeningTime(config, actParams.getActivityType(), actParams.getOpeningTime()),
//						new ScalarRandomizer<OpeningTime>(deltaTime_s, 0.0));
//			}
//			if (!Time.isUndefinedTime(actParams.getClosingTime())) {
//				builder.add(new ClosingTime(config, actParams.getActivityType(), actParams.getClosingTime()),
//						new ScalarRandomizer<ClosingTime>(deltaTime_s, 0.0));
//			}
//			if (!Time.isUndefinedTime(actParams.getTypicalDuration())) {
//				builder.add(new TypicalDuration(config, actParams.getActivityType(), actParams.getTypicalDuration()),
//						new ScalarRandomizer<TypicalDuration>(deltaTime_s, 0.0));
//			}
//		}
		for (ActivityParams actParams : config.planCalcScore().getActivityParams()) {
			if (actParams.getOpeningTime().isDefined()) {
				builder.add(new OpeningTime(config, actParams.getActivityType(), actParams.getOpeningTime().seconds()),
						new ScalarRandomizer<OpeningTime>(deltaTime_s, 0.0));
			}
			if (actParams.getClosingTime().isDefined()) {
				builder.add(new ClosingTime(config, actParams.getActivityType(), actParams.getClosingTime().seconds()),
						new ScalarRandomizer<ClosingTime>(deltaTime_s, 0.0));
			}
			if (actParams.getTypicalDuration().isDefined()) {
				builder.add(new TypicalDuration(config, actParams.getActivityType(), actParams.getTypicalDuration().seconds()),
						new ScalarRandomizer<TypicalDuration>(deltaTime_s, 0.0));
			}
		}
		CompositeDecisionVariable initialDecisionVariable = builder.buildDecisionVariable();

		// <<<<<<<<<< TODO SPEC OF COMPOSITE (TIME) DECISION VARIABLES <<<<<<<<<<

		DecisionVariableRandomizer<CompositeDecisionVariable> randomizer = new OneAtATimeRandomizer();

		runner.run(randomizer, initialDecisionVariable, objFct);
	}
}
