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
package stockholm.ihop4;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.greedo.Greedo;
import org.matsim.contrib.greedo.GreedoConfigGroup;
import org.matsim.contrib.opdyts.MATSimOpdytsRunner;
import org.matsim.contrib.opdyts.OpdytsConfigGroup;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.LinkEntryCountDeviationObjectiveFunction;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.TotalDeviationObjectiveFunctionEXPERIMENTAL;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.CountTrajectorySummarizer;
import org.matsim.contrib.opdyts.buildingblocks.calibration.plotting.TrajectoryPlotter;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes.ActivityTimesUtils;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.behavioralparameters.PerformingCoefficient;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.capacityscaling.SimulatedDemandShare;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariable;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariableBuilder;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar.ScalarRandomizer;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.utils.EveryIterationScoringParameters;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.microstate.MATSimStateFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.Units;
import modalsharecalibrator.ModeASCContainer;
import stockholm.ihop4.sampersutilities.SampersScoringFunctionModule;
import stockholm.ihop4.tollzonepassagedata.TollZoneMeasurementReader;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class IHOP4ProductionRunner {

	private static final Logger log = Logger.getLogger(Greedo.class);

	static void keepOnlyStrictCarUsers(final Scenario scenario) {
		final Set<Id<Person>> remove = new LinkedHashSet<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement planEl : person.getSelectedPlan().getPlanElements()) {
				if (planEl instanceof Leg && !TransportMode.car.equals(((Leg) planEl).getMode())) {
					remove.add(person.getId());
				}
			}
		}
		log.info("before strict-car filter: " + scenario.getPopulation().getPersons().size());
		for (Id<Person> removeId : remove) {
			scenario.getPopulation().getPersons().remove(removeId);
		}
		log.info("after strict-car filter: " + scenario.getPopulation().getPersons().size());
	}

	// // ==================== SIMULATE ====================
	//
	// static void simulate(final Config config) {
	//
	// // Greedo.
	//
	// final Greedo greedo;
	// if (config.getModules().containsKey(AccelerationConfigGroup.GROUP_NAME)) {
	// greedo = new Greedo();
	// greedo.setAdjustStrategyWeights(true);
	// greedo.meet(config);
	// } else {
	// greedo = null;
	// }
	//
	// // Trajectory plotting.
	//
	// final TrajectoryPlotter trajectoryPlotter = new TrajectoryPlotter(config, 1);
	// final TollZoneMeasurementReader measReader = new
	// TollZoneMeasurementReader(config);
	// measReader.run();
	// for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent :
	// measReader.getAllDayMeasurements()
	// .getObjectiveFunctions()) {
	// trajectoryPlotter.addDataSource(objectiveFunctionComponent);
	// }
	// for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent :
	// measReader
	// .getOnlyTollTimeMeasurements().getObjectiveFunctions()) {
	// trajectoryPlotter.addDataSource(objectiveFunctionComponent);
	// }
	// trajectoryPlotter.addSummarizer(new CountTrajectorySummarizer(new
	// TimeDiscretization(0, 1800, 48)));
	//
	// // Objective function, ONLY FOR EVALUATION.
	//
	// final double normalizingFactor = 1.0
	// /
	// (measReader.getAllDayMeasurements().getSumOfEvaluatdResidualsAtZeroSimulation()
	// +
	// measReader.getOnlyTollTimeMeasurements().getSumOfEvaluatdResidualsAtZeroSimulation());
	//
	// final MATSimObjectiveFunctionSum<MATSimState> overallObjectiveFunction = new
	// MATSimObjectiveFunctionSum<>();
	// for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent :
	// measReader.getAllDayMeasurements()
	// .getObjectiveFunctions()) {
	// overallObjectiveFunction.add(objectiveFunctionComponent, normalizingFactor);
	// }
	// for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent :
	// measReader
	// .getOnlyTollTimeMeasurements().getObjectiveFunctions()) {
	// overallObjectiveFunction.add(objectiveFunctionComponent, normalizingFactor);
	// }
	//
	// // Scenario.
	//
	// final Scenario scenario = ScenarioUtils.loadScenario(config);
	// keepOnlyStrictCarUsers(scenario);
	// if (greedo != null) {
	// greedo.meet(scenario);
	// }
	//
	// // Controler.
	//
	// final Controler controler = new Controler(scenario);
	//
	// for (AbstractModule module : measReader.getAllDayMeasurements().getModules())
	// {
	// controler.addOverridingModule(module);
	// }
	// for (AbstractModule module :
	// measReader.getOnlyTollTimeMeasurements().getModules()) {
	// controler.addOverridingModule(module);
	// }
	// controler.addOverridingModule(new AbstractModule() {
	// @Override
	// public void install() {
	// this.addControlerListenerBinding().toInstance(trajectoryPlotter);
	// }
	// });
	// if (greedo != null) {
	// controler.addOverridingModule(greedo);
	// }
	//
	// for (AbstractModule module : measReader.getAllDayMeasurements().getModules())
	// {
	// controler.addOverridingModule(module);
	// }
	// for (AbstractModule module :
	// measReader.getOnlyTollTimeMeasurements().getModules()) {
	// controler.addOverridingModule(module);
	// }
	// controler.addOverridingModule(new AbstractModule() {
	// @Override
	// public void install() {
	// this.addControlerListenerBinding().toInstance(new StartupListener() {
	// @Override
	// public void notifyStartup(StartupEvent event) {
	// FileUtils.deleteQuietly(new File(config.controler().getOutputDirectory(),
	// "objfct.log"));
	// }
	// });
	// this.addControlerListenerBinding().toInstance(new BeforeMobsimListener() {
	// @Override
	// public void notifyBeforeMobsim(BeforeMobsimEvent event) {
	// if ((event.getIteration() > 0) && (event.getIteration() % ConfigUtils
	// .addOrGetModule(config, PSimConfigGroup.class).getIterationsPerCycle() == 0))
	// {
	// try {
	// FileUtils.writeStringToFile(
	// new File(config.controler().getOutputDirectory(), "objfct.log"),
	// overallObjectiveFunction.value(null) + "\n", true);
	// } catch (IOException e) {
	// throw new RuntimeException(e);
	// }
	// }
	// }
	// });
	// }
	// });
	//
	// controler.setModules(new ControlerDefaultsWithRoadPricingModule());
	//
	// // ... and run.
	//
	// controler.run();
	//
	// }

	// ==================== CALIBRATE ====================

	static void readAndAddData(final int startTime_s, final int endTime_s,
			// MATSimObjectiveFunctionSum<MATSimState> overallObjectiveFunction,
			final TotalDeviationObjectiveFunctionEXPERIMENTAL overallObjectiveFunction,
			final List<AbstractModule> modules, final TrajectoryPlotter trajectoryPlotter, final Config config) {

		if (ConfigUtils.addOrGetModule(config, IhopConfigGroup.class).getTollZoneCountsFolder() != null) {

			System.err.println("Need to update this code to new Greedo.");
			System.exit(0);
			// final int simulatedSensorDataExtractionInterval;
			// if (config.getModules().containsKey(PSimConfigGroup.GROUP_NAME)) {
			// simulatedSensorDataExtractionInterval = ConfigUtils.addOrGetModule(config,
			// PSimConfigGroup.class)
			// .getIterationsPerCycle();
			// } else {
			// simulatedSensorDataExtractionInterval = 1;
			// }

			// final TollZoneMeasurementReader measReader = new
			// TollZoneMeasurementReader(config);
			final TollZoneMeasurementReader measReader = new TollZoneMeasurementReader(
					ConfigUtils.addOrGetModule(config, IhopConfigGroup.class).getTollZoneCountsFolder(), config, 20,
					new TimeDiscretization(0, 1800, 48), new TimeDiscretization(6 * 3600 + 30 * 60, 1800, 24), 1); // simulatedSensorDataExtractionInterval);

			measReader.setStartEndTime_s(startTime_s, endTime_s);
			measReader.run();

			// final double normalizingFactor = 1.0
			// /
			// (measReader.getAllDayMeasurements().getSumOfEvaluatdResidualsAtZeroSimulation()
			// +
			// measReader.getOnlyTollTimeMeasurements().getSumOfEvaluatdResidualsAtZeroSimulation());

			for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent : measReader
					.getAllDayMeasurements().getObjectiveFunctions()) {
				// overallObjectiveFunction.add(objectiveFunctionComponent, normalizingFactor);
				overallObjectiveFunction.addDataSource(objectiveFunctionComponent);
				trajectoryPlotter.addDataSource(objectiveFunctionComponent);
			}
			for (AbstractModule module : measReader.getAllDayMeasurements().getModules()) {
				modules.add(module);
			}

			for (LinkEntryCountDeviationObjectiveFunction objectiveFunctionComponent : measReader
					.getOnlyTollTimeMeasurements().getObjectiveFunctions()) {
				// overallObjectiveFunction.add(objectiveFunctionComponent, normalizingFactor);
				overallObjectiveFunction.addDataSource(objectiveFunctionComponent);
				trajectoryPlotter.addDataSource(objectiveFunctionComponent);
			}
			for (AbstractModule module : measReader.getOnlyTollTimeMeasurements().getModules()) {
				modules.add(module);
			}

		}
	}

	// static void calibrate(final Config config) {
	//
	// final OpdytsGreedoProgressListener progressListener = new
	// OpdytsGreedoProgressListener("progress.log");
	//
	// // Greedo
	//
	// final Greedo greedo;
	// if (config.getModules().containsKey(AccelerationConfigGroup.GROUP_NAME)) {
	// greedo = new Greedo();
	// greedo.setAdjustStrategyWeights(true);
	// greedo.setGreedoProgressListener(progressListener);
	// greedo.meet(config);
	// } else {
	// greedo = null;
	// }
	//
	// // Configuration
	//
	// final OpdytsConfigGroup opdytsConfig = ConfigUtils.addOrGetModule(config,
	// OpdytsConfigGroup.class);
	// if (greedo != null) {
	// opdytsConfig.setEnBlockSimulationIterations(
	// ConfigUtils.addOrGetModule(config,
	// PSimConfigGroup.class).getIterationsPerCycle());
	// }
	// final IhopConfigGroup ihopConfig = ConfigUtils.addOrGetModule(config,
	// IhopConfigGroup.class);
	//
	// // Scenario
	//
	// final Scenario scenario = ScenarioUtils.loadScenario(config);
	// keepOnlyStrictCarUsers(scenario);
	// if (greedo != null) {
	// greedo.meet(scenario);
	// }
	//
	// // -------------------- DECISION VARIABLES --------------------
	//
	// final CompositeDecisionVariableBuilder builder = new
	// CompositeDecisionVariableBuilder();
	//
	// // Performing
	// if (ihopConfig.getPerformingStepSize_utils_hr() != null) {
	// builder.add(new PerformingCoefficient(config,
	// config.planCalcScore().getPerforming_utils_hr()),
	// new ScalarRandomizer<>(ihopConfig.getPerformingStepSize_utils_hr()));
	// }
	//
	// // Activity times
	//
	// if (ihopConfig.getActivityTimeStepSize_s() != null) {
	// builder.add(ActivityTimesUtils.newAllActivityTimesDecisionVariable(config,
	// ihopConfig.getActivityTimeStepSize_s(), 0.0));
	// }
	//
	// // SimulatedPopulationShare
	//
	// if (ihopConfig.getSimulatedPopulationShareStepSize() != null) {
	// builder.add(
	// new SimulatedDemandShare(config, ihopConfig.getSimulatedPopulationShare(),
	// new Consumer<Double>() {
	// @Override
	// public void accept(Double simulatedPopulationShare) {
	// ihopConfig.setSimulatedPopulationShare(simulatedPopulationShare);
	// }
	// }), new
	// ScalarRandomizer<>(ihopConfig.getSimulatedPopulationShareStepSize()));
	// }
	//
	// // Build decision variable and matching randomizer.
	//
	// final CompositeDecisionVariable decisionVariable =
	// builder.buildDecisionVariable();
	//
	// final DecisionVariableRandomizer<CompositeDecisionVariable>
	// decisionVariableRandomizer = ConfigUtils
	// .addOrGetModule(config,
	// IhopConfigGroup.class).newDecisionVariableRandomizer();
	//
	// // --------------- OBJECTIVE FUNCTION & TRAJECTORY PLOTTING ---------------
	//
	// final int morningPeakStart_s = 0;
	// final int morningPeakEnd_s = 10 * 3600 - 1;
	//
	// final int eveningPeakStart_s = 15 * 3600;
	// final int eveningPeakEnd_s = (int) Units.S_PER_D - 1;
	//
	// final MATSimObjectiveFunctionSum<MATSimState> overallObjectiveFunction = new
	// MATSimObjectiveFunctionSum<>();
	// final List<AbstractModule> modules = new LinkedList<>();
	// final TrajectoryPlotter trajectoryPlotter = new TrajectoryPlotter(config, 1);
	//
	// if
	// (IhopConfigGroup.TollZoneTimeIntervallType.allDay.equals(ihopConfig.getTollZoneTimeIntervall()))
	// {
	// readAndAddData(0, (int) Units.S_PER_D, overallObjectiveFunction, modules,
	// trajectoryPlotter, config);
	// } else {
	// if
	// (IhopConfigGroup.TollZoneTimeIntervallType.morningPeak.equals(ihopConfig.getTollZoneTimeIntervall())
	// || IhopConfigGroup.TollZoneTimeIntervallType.bothPeaks
	// .equals(ihopConfig.getTollZoneTimeIntervall())) {
	// readAndAddData(morningPeakStart_s, morningPeakEnd_s,
	// overallObjectiveFunction, modules,
	// trajectoryPlotter, config);
	// }
	// if
	// (IhopConfigGroup.TollZoneTimeIntervallType.eveningPeak.equals(ihopConfig.getTollZoneTimeIntervall())
	// || IhopConfigGroup.TollZoneTimeIntervallType.bothPeaks
	// .equals(ihopConfig.getTollZoneTimeIntervall())) {
	// readAndAddData(eveningPeakStart_s, eveningPeakEnd_s,
	// overallObjectiveFunction, modules,
	// trajectoryPlotter, config);
	// }
	// }
	//
	// trajectoryPlotter.addSummarizer(new CountTrajectorySummarizer(new
	// TimeDiscretization(0, 1800, 48)));
	//
	// // -------------------- OPDYTS RUNNER --------------------
	//
	// final MATSimOpdytsRunner<CompositeDecisionVariable, MATSimState> runner = new
	// MATSimOpdytsRunner<>(scenario,
	// new MATSimStateFactoryImpl<>());
	// runner.addOverridingModule(new AbstractModule() {
	// @Override
	// public void install() {
	// bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
	// }
	// });
	// // for (AbstractModule module :
	// measReader.getAllDayMeasurements().getModules())
	// // {
	// // runner.addOverridingModule(module);
	// // }
	// // for (AbstractModule module :
	// // measReader.getOnlyTollTimeMeasurements().getModules()) {
	// // runner.addOverridingModule(module);
	// // }
	// for (AbstractModule module : modules) {
	// runner.addOverridingModule(module);
	// }
	// runner.addOverridingModule(new AbstractModule() {
	// @Override
	// public void install() {
	// this.addControlerListenerBinding().toInstance(trajectoryPlotter);
	// }
	// });
	// if (greedo != null) {
	// // runner.addWantsControlerReferenceBeforeInjection(greedo);
	// runner.addOverridingModule(greedo);
	// }
	//
	// runner.setReplacingModules(new ControlerDefaultsWithRoadPricingModule());
	//
	// runner.setOpdytsProgressListener(progressListener);
	//
	// // runner.setConvergenceCriterion(new AR1ConvergenceCriterion(1e5)); //
	// square
	// // runner.setConvergenceCriterion(new AR1ConvergenceCriterion(1000.0)); //
	// // absolute
	// // runner.setConvergenceCriterion(new
	// // FixedIterationNumberConvergenceCriterion(2, 1));
	//
	// runner.run(decisionVariableRandomizer, decisionVariable,
	// overallObjectiveFunction);
	// }

	static void run(final Config config) {

		final boolean optimize = config.getModules().containsKey(OpdytsConfigGroup.GROUP_NAME);

		final OpdytsGreedoProgressListener progressListener = new OpdytsGreedoProgressListener(
				new File(config.controler().getOutputDirectory(), "progress.log").toString());

		// Greedo

		final Greedo greedo;
		if (config.getModules().containsKey(GreedoConfigGroup.GROUP_NAME)) {
			greedo = new Greedo();
			// greedo.setGreedoProgressListener(progressListener);
			greedo.meet(config);
		} else {
			greedo = null;
		}

		// Configuration

		// if (optimize) {
		// final OpdytsConfigGroup opdytsConfig = ConfigUtils.addOrGetModule(config,
		// OpdytsConfigGroup.class);
		// if (greedo != null) {
		// opdytsConfig.setEnBlockSimulationIterations(
		// ConfigUtils.addOrGetModule(config,
		// PSimConfigGroup.class).getIterationsPerCycle());
		// }
		// }

		final IhopConfigGroup ihopConfig = ConfigUtils.addOrGetModule(config, IhopConfigGroup.class);

		// Scenario

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		keepOnlyStrictCarUsers(scenario);
		if (greedo != null) {
			greedo.meet(scenario);
		}

		// -------------------- DECISION VARIABLES --------------------

		final CompositeDecisionVariable decisionVariable;
		final DecisionVariableRandomizer<CompositeDecisionVariable> decisionVariableRandomizer;

		if (optimize) {

			final CompositeDecisionVariableBuilder builder = new CompositeDecisionVariableBuilder();

			// Performing

			if (ihopConfig.getPerformingStepSize_utils_hr() != null) {
				PerformingCoefficient perfCoeff = new PerformingCoefficient(config,
						config.planCalcScore().getPerforming_utils_hr());
				// No technical reason to add bound constraints.
				builder.add(perfCoeff, new ScalarRandomizer<>(ihopConfig.getPerformingStepSize_utils_hr()));
			}

			// Activity times

			if (ihopConfig.getActivityTimeStepSize_s() != null) {
				builder.add(ActivityTimesUtils.newAllActivityTimesDecisionVariable(config,
						ihopConfig.getActivityTimeStepSize_s(), 0.0));
			}

			// SimulatedPopulationShare

			if (ihopConfig.getSimulatedPopulationShareStepSize() != null) {
				SimulatedDemandShare share = new SimulatedDemandShare(config, ihopConfig.getSimulatedPopulationShare(),
						new Consumer<Double>() {
							@Override
							public void accept(Double simulatedPopulationShare) {
								ihopConfig.setSimulatedPopulationShare(simulatedPopulationShare);
							}
						});
				share.setMinValue_s(0.001);
				builder.add(share, new ScalarRandomizer<>(ihopConfig.getSimulatedPopulationShareStepSize()));
			}

			// Build decision variable and matching randomizer.

			decisionVariable = builder.buildDecisionVariable();

			decisionVariableRandomizer = ConfigUtils.addOrGetModule(config, IhopConfigGroup.class)
					.newDecisionVariableRandomizer();

		} else {

			decisionVariable = null;
			decisionVariableRandomizer = null;

		}

		// --------------- OBJECTIVE FUNCTION & TRAJECTORY PLOTTING ---------------

		final int morningPeakStart_s = 0;
		final int morningPeakEnd_s = 10 * 3600 - 1;

		final int eveningPeakStart_s = 15 * 3600;
		final int eveningPeakEnd_s = (int) Units.S_PER_D - 1;

		// final MATSimObjectiveFunctionSum<MATSimState> overallObjectiveFunction = new
		// MATSimObjectiveFunctionSum<>();
		final TotalDeviationObjectiveFunctionEXPERIMENTAL overallObjectiveFunction = new TotalDeviationObjectiveFunctionEXPERIMENTAL(
				new TimeDiscretization(0, 1800, 48));

		final List<AbstractModule> modules = new LinkedList<>();
		final TrajectoryPlotter trajectoryPlotter = new TrajectoryPlotter(config, 1);

		if (IhopConfigGroup.TollZoneTimeIntervallType.allDay.equals(ihopConfig.getTollZoneTimeIntervall())) {
			readAndAddData(0, (int) Units.S_PER_D, overallObjectiveFunction, modules, trajectoryPlotter, config);
		} else {
			if (IhopConfigGroup.TollZoneTimeIntervallType.morningPeak.equals(ihopConfig.getTollZoneTimeIntervall())
					|| IhopConfigGroup.TollZoneTimeIntervallType.bothPeaks
							.equals(ihopConfig.getTollZoneTimeIntervall())) {
				readAndAddData(morningPeakStart_s, morningPeakEnd_s, overallObjectiveFunction, modules,
						trajectoryPlotter, config);
			}
			if (IhopConfigGroup.TollZoneTimeIntervallType.eveningPeak.equals(ihopConfig.getTollZoneTimeIntervall())
					|| IhopConfigGroup.TollZoneTimeIntervallType.bothPeaks
							.equals(ihopConfig.getTollZoneTimeIntervall())) {
				readAndAddData(eveningPeakStart_s, eveningPeakEnd_s, overallObjectiveFunction, modules,
						trajectoryPlotter, config);
			}
		}

		trajectoryPlotter.addSummarizer(new CountTrajectorySummarizer(new TimeDiscretization(0, 1800, 48)));

		// -------------------- OPDYTS / PLAIN RUNNER --------------------

		if (optimize) {

			final MATSimOpdytsRunner<CompositeDecisionVariable, MATSimState> runner = new MATSimOpdytsRunner<>(scenario,
					new MATSimStateFactoryImpl<>());
			runner.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
				}
			});
			// for (AbstractModule module : measReader.getAllDayMeasurements().getModules())
			// {
			// runner.addOverridingModule(module);
			// }
			// for (AbstractModule module :
			// measReader.getOnlyTollTimeMeasurements().getModules()) {
			// runner.addOverridingModule(module);
			// }
			for (AbstractModule module : modules) {
				runner.addOverridingModule(module);
			}
			runner.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addControlerListenerBinding().toInstance(trajectoryPlotter);
				}
			});
			if (greedo != null) {
				// runner.addWantsControlerReferenceBeforeInjection(greedo);
				for (AbstractModule module : greedo.getModules()) {
					runner.addOverridingModule(module);
				}
			}

			// >>>> TODO FOR TESTING >>>>

			runner.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addControlerListenerBinding().toInstance(new StartupListener() {
						@Override
						public void notifyStartup(StartupEvent event) {
							FileUtils.deleteQuietly(new File(config.controler().getOutputDirectory(), "objfct.log"));
						}
					});
					this.addControlerListenerBinding().toInstance(new BeforeMobsimListener() {
						@Override
						public void notifyBeforeMobsim(BeforeMobsimEvent event) {
							if ((event.getIteration() > 0)
							// && (event.getIteration() % ConfigUtils
							// .addOrGetModule(config, PSimConfigGroup.class).getIterationsPerCycle() == 0)
							) {
								try {
									FileUtils.writeStringToFile(
											new File(config.controler().getOutputDirectory(), "objfct.log"),
											overallObjectiveFunction.value(null) + "\n", true);
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							}
						}
					});
				}
			});

			// runner.addOverridingModule(new AbstractModule() {
			// @Override
			// public void install() {
			// this.addControlerListenerBinding().toInstance(new StartupListener() {
			// @Override
			// public void notifyStartup(StartupEvent event) {
			// FileUtils.deleteQuietly(new File(config.controler().getOutputDirectory(),
			// "planstats.log"));
			// }
			// });
			// this.addControlerListenerBinding().toInstance(new BeforeMobsimListener() {
			// @Override
			// public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			// try {
			//
			// int planCnt = 0;
			// int selectedPlanElementCnt = 0;
			// double scoreSum = 0;
			// for (Person person : scenario.getPopulation().getPersons().values()) {
			// planCnt += person.getPlans().size();
			// selectedPlanElementCnt += person.getSelectedPlan().getPlanElements().size();
			// scoreSum += person.getSelectedPlan().getScore();
			// }
			// FileUtils.writeStringToFile(
			// new File(config.controler().getOutputDirectory(), "planstats.log"),
			// "it=" + event.getIteration() + " planCnt=" + planCnt
			// + " selectedPlanElementCnt=" + selectedPlanElementCnt + " scoreSum="
			// + scoreSum + "\n",
			// true);
			//
			// } catch (IOException e) {
			// throw new RuntimeException(e);
			// }
			// }
			// });
			// }
			// });

			// <<<< TODO FOR TESTING <<<<

			runner.setReplacingModules(new ControlerDefaultsWithRoadPricingModule());

			runner.setOpdytsProgressListener(progressListener);

			// runner.setConvergenceCriterion(new AR1ConvergenceCriterion(1e5)); // square
			// runner.setConvergenceCriterion(new AR1ConvergenceCriterion(1000.0)); //
			// absolute
			// runner.setConvergenceCriterion(new
			// FixedIterationNumberConvergenceCriterion(2, 1));

			runner.run(decisionVariableRandomizer, decisionVariable, overallObjectiveFunction);

		} else {

			final Controler controler = new Controler(scenario);

			// for (AbstractModule module : measReader.getAllDayMeasurements().getModules())
			// {
			// controler.addOverridingModule(module);
			// }
			// for (AbstractModule module :
			// measReader.getOnlyTollTimeMeasurements().getModules()) {
			// controler.addOverridingModule(module);
			// }
			for (AbstractModule module : modules) {
				controler.addOverridingModule(module);
			}

			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addControlerListenerBinding().toInstance(trajectoryPlotter);
				}
			});
			if (greedo != null) {
				for (AbstractModule module : greedo.getModules()) {
					controler.addOverridingModule(module);
				}
			}

			// for (AbstractModule module : measReader.getAllDayMeasurements().getModules())
			// {
			// controler.addOverridingModule(module);
			// }
			// for (AbstractModule module :
			// measReader.getOnlyTollTimeMeasurements().getModules()) {
			// controler.addOverridingModule(module);
			// }
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addControlerListenerBinding().toInstance(new StartupListener() {
						@Override
						public void notifyStartup(StartupEvent event) {
							FileUtils.deleteQuietly(new File(config.controler().getOutputDirectory(), "objfct.log"));
						}
					});
					this.addControlerListenerBinding().toInstance(new BeforeMobsimListener() {
						@Override
						public void notifyBeforeMobsim(BeforeMobsimEvent event) {
							System.err.println("Need to update this code");
							System.exit(0);
							// if ((event.getIteration() > 0) && (event.getIteration() % ConfigUtils
							// .addOrGetModule(config, PSimConfigGroup.class).getIterationsPerCycle() == 0))
							// {
							// try {
							// FileUtils.writeStringToFile(
							// new File(config.controler().getOutputDirectory(), "objfct.log"),
							// overallObjectiveFunction.value(null) + "\n", true);
							// } catch (IOException e) {
							// throw new RuntimeException(e);
							// }
							// }
						}
					});
				}
			});

			controler.setModules(new ControlerDefaultsWithRoadPricingModule());

			// ... and run.

			controler.run();

		}
	}

	static void runWithSampersDynamics(final Config config) {

		Logger.getLogger(IHOP4ProductionRunner.class).warn("Overriding plans input file in-codes.");
		config.plans().setInputFile("1PctAllModes_enriched.xml");

		config.getModules().remove(IhopConfigGroup.GROUP_NAME);
		// config.getModules().remove(PSimConfigGroup.GROUP_NAME);
		// config.getModules().remove(GreedoConfigGroup.GROUP_NAME);

		final Greedo greedo = new Greedo();
		greedo.meet(config);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		keepOnlyStrictCarUsers(scenario);
		greedo.meet(scenario);

		final Controler controler = new Controler(scenario);
		controler.setModules(new ControlerDefaultsWithRoadPricingModule());
		controler.addOverridingModule(new SampersScoringFunctionModule());

		for (AbstractModule module : greedo.getModules()) {
			controler.addOverridingModule(module);
		}
		
		// The following because Sampers scoring requires it
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ModeASCContainer.class);
			}
		});
		
		controler.run();
	}

	public static void main(String[] args) {

		final Config config = ConfigUtils.loadConfig(args[0], new RoadPricingConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// if (!config.getModules().containsKey(IhopConfigGroup.GROUP_NAME)) {
		// throw new RuntimeException(IhopConfigGroup.GROUP_NAME + " config module is
		// missing.");
		// }
		// run(config);

		runWithSampersDynamics(config);
	}
}
