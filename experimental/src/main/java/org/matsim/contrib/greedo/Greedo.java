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
package org.matsim.contrib.greedo;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.ier.IERModule;
import org.matsim.contrib.ier.run.IERConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;

import com.google.inject.Singleton;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Greedo {

	// -------------------- CONSTANTS --------------------

	public static final String nullSubpopulationString = "nullSubpopulation";

	private static final Logger log = Logger.getLogger(Greedo.class);

	// -------------------- MEMBERS --------------------

	private Config config = null;

	private Scenario scenario = null;

	// -------------------- CONSTRUCTION --------------------

	public Greedo() {
	}

	// -------------------- WIRE GREEDO INTO MATSim --------------------

	public void meet(final Config config) {

		if (this.config != null) {
			throw new RuntimeException("Have already met a config.");
		}
		if (config.controler().getFirstIteration() != 0) {
			Logger.getLogger(this.getClass()).warn("The simulation does not start at iteration zero.");
			// throw new RuntimeException("Expecting the simulation to start at iteration
			// zero.");
		}
		this.config = config;

		/*
		 * Ensure a valid configuration; fall back to default values if not available.
		 */
		if (!config.getModules().containsKey(IERConfigGroup.GROUP_NAME)) {
			log.warn("Config module " + IERConfigGroup.GROUP_NAME + " is missing, falling back to default values.");
		}
		final IERConfigGroup ierConfig = ConfigUtils.addOrGetModule(config, IERConfigGroup.class);

		if (!config.getModules().containsKey(GreedoConfigGroup.GROUP_NAME)) {
			log.warn("Config module " + GreedoConfigGroup.GROUP_NAME + " is missing, falling back to default values.");
		}
		final GreedoConfigGroup greedoConfig = ConfigUtils.addOrGetModule(config, GreedoConfigGroup.class);

		boolean thereAreCheapStrategies = false;

		if (greedoConfig.getAdjustStrategyWeights()) {

			/*
			 * Preliminary analysis of innovation strategies.
			 */
			final Set<String> allSubpops = new LinkedHashSet<>();
			final Map<String, Integer> subpop2expensiveStrategyCnt = new LinkedHashMap<>();
			final Map<String, Integer> subpop2cheapStrategyCnt = new LinkedHashMap<>();
			final Map<String, Double> subpop2cheapStrategyWeightSum = new LinkedHashMap<>();
			// TODO also use expensive strategy weight sum?
			for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
				final String strategyName = strategySettings.getStrategyName();
				final String subpop = ((strategySettings.getSubpopulation() == null) ? nullSubpopulationString
						: strategySettings.getSubpopulation());
				allSubpops.add(subpop);
				if (strategySettings.getWeight() > 0) {
					if (greedoConfig.getExpensiveStrategyList().contains(strategyName)) {
						subpop2expensiveStrategyCnt.put(subpop,
								1 + subpop2expensiveStrategyCnt.getOrDefault(subpop, 0));
					} else if (greedoConfig.getCheapStrategyList().contains(strategyName)) {
						subpop2cheapStrategyCnt.put(subpop, 1 + subpop2cheapStrategyCnt.getOrDefault(subpop, 0));
						subpop2cheapStrategyWeightSum.put(subpop,
								strategySettings.getWeight() + subpop2cheapStrategyWeightSum.getOrDefault(subpop, 0.0));
						thereAreCheapStrategies = true;
					}
				}
			}

			// >>>>> OLD >>>>>

			// // Preliminary analysis of innovation strategies.
			// int expensiveStrategyCnt = 0;
			// int cheapStrategyCnt = 0;
			// double cheapStrategyWeightSum = 0.0; // TODO also use expensive strategy
			// weight sum?
			// for (StrategySettings strategySettings :
			// config.strategy().getStrategySettings()) {
			// final String strategyName = strategySettings.getStrategyName();
			// if (strategySettings.getWeight() > 0) {
			// if (greedoConfig.getExpensiveStrategyList().contains(strategyName)) {
			// expensiveStrategyCnt++;
			// } else if (greedoConfig.getCheapStrategyList().contains(strategyName)) {
			// cheapStrategyCnt++;
			// cheapStrategyWeightSum += strategySettings.getWeight();
			// }
			// }
			// }

			// <<<<< OLD <<<<<

			/*
			 * Adjust number of emulation iterations.
			 */
			if (!thereAreCheapStrategies) {
				ierConfig.setIterationsPerCycle(1);
			}
			for (String subpop : allSubpops) {
				final int expensiveStrategyCnt = subpop2expensiveStrategyCnt.getOrDefault(subpop, 0);
				final int cheapStrategyCnt = subpop2cheapStrategyCnt.getOrDefault(subpop, 0);

				final int expensiveStrategiesPerCycle;
				if (GreedoConfigGroup.ExpensiveStrategyTreatmentType.allOnce
						.equals(greedoConfig.getExpensiveStrategyTreatment())) {
					expensiveStrategiesPerCycle = expensiveStrategyCnt;
				} else if (GreedoConfigGroup.ExpensiveStrategyTreatmentType.oneInTotal
						.equals(greedoConfig.getExpensiveStrategyTreatment())) {
					expensiveStrategiesPerCycle = 1;
				} else {
					throw new RuntimeException(
							"Unknown expensive strategy treatment: " + greedoConfig.getExpensiveStrategyTreatment());
				}

				if (expensiveStrategiesPerCycle + cheapStrategyCnt > 0) {
					ierConfig.setIterationsPerCycle(Math.max(expensiveStrategiesPerCycle + cheapStrategyCnt,
							ierConfig.getIterationsPerCycle()));
				} else {
					throw new RuntimeException("No innovation strategies recognized for subpopulation " + subpop + ". "
							+ "Greedo expects at least one. See greedo config module for recognized innovation strategies.");
				}
			}
			log.info("Set number of emulated iterations per cycle to " + ierConfig.getIterationsPerCycle() + ".");

			/*
			 * Keep only innovation strategies. Re-weight for max. emulation efficiency.
			 */
			for (String subpop : allSubpops) {
				log.info("Adjusting strategies for subpopulation: " + subpop);

				final int expensiveStrategyCnt = subpop2expensiveStrategyCnt.getOrDefault(subpop, 0);
				final int cheapStrategyCnt = subpop2cheapStrategyCnt.getOrDefault(subpop, 0);
				final double cheapStrategyWeightSum = subpop2cheapStrategyWeightSum.getOrDefault(subpop, 0.0);

				final double singleExpensiveStrategyProba;
				if (GreedoConfigGroup.ExpensiveStrategyTreatmentType.allOnce
						.equals(greedoConfig.getExpensiveStrategyTreatment())) {
					singleExpensiveStrategyProba = 1.0 / ierConfig.getIterationsPerCycle();
				} else if (GreedoConfigGroup.ExpensiveStrategyTreatmentType.oneInTotal
						.equals(greedoConfig.getExpensiveStrategyTreatment())) {
					singleExpensiveStrategyProba = (1.0 / ierConfig.getIterationsPerCycle()) / expensiveStrategyCnt;
				} else {
					throw new RuntimeException(
							"Unknown expensive strategy treatment: " + greedoConfig.getExpensiveStrategyTreatment());
				}

				final double cheapStrategyProbaSum = 1.0
						- ((expensiveStrategyCnt == 0) ? 0.0 : singleExpensiveStrategyProba * expensiveStrategyCnt);
				final double cheapStrategyWeightFactor = cheapStrategyProbaSum / cheapStrategyWeightSum;
				double probaSum = 0;
				for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
					if (subpop.equals((strategySettings.getSubpopulation() == null) ? nullSubpopulationString
							: strategySettings.getSubpopulation())) {
						final String strategyName = strategySettings.getStrategyName();
						if (greedoConfig.getExpensiveStrategyList().contains(strategyName)) {
							strategySettings.setWeight(singleExpensiveStrategyProba);
						} else if (greedoConfig.getCheapStrategyList().contains(strategyName)) {
							strategySettings.setWeight(cheapStrategyWeightFactor * strategySettings.getWeight());
						} else {
							strategySettings.setWeight(0.0); // i.e., dismiss
						}
						log.info("* Setting weight of strategy " + strategyName + " to " + strategySettings.getWeight()
								+ ".");
						probaSum += strategySettings.getWeight();
					}
				}
				if (cheapStrategyCnt == 0) {
					final StrategySettings keepSelected = new StrategySettings();
					keepSelected.setStrategyName(DefaultSelector.KeepLastSelected);
					keepSelected.setSubpopulation(subpop);
					keepSelected.setWeight(1.0 - probaSum);
					config.strategy().addStrategySettings(keepSelected);
					log.info("* Padding with " + DefaultSelector.KeepLastSelected + " and weight="
							+ keepSelected.getWeight()
							+ " because there are no cheap strategies in this subpopulation");
					probaSum += keepSelected.getWeight();
				}
				if (Math.abs(1.0 - probaSum) >= 1e-8) {
					throw new RuntimeException(
							"The sum of all strategy probabilities is " + probaSum + " but should be one.");
				}
			}

			/*
			 * Use minimal choice set and always remove the worse plan. This is probably as
			 * close as it can get to best-response in the presence of random innovation
			 * strategies.
			 */
			config.strategy().setMaxAgentPlanMemorySize(1);
			config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");
			log.info("Approximating a best-response simulation through the following settings:");
			log.info(" * maxAgentPlanMemorySize = 1");
			log.info(" * planSelectorForRemoval = worstPlanSelector");

			/*
			 * Maintain innovation throughout. Greedo step size control replaces the
			 * "fraction".
			 */
			config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);
			log.info("Setting fractionOfIterationsToDisableInnovation to infinity.");

			// >>>>> OLD >>>>>

			// // Adjust number of emulated iterations per cycle to number and type of
			// // innovation strategies.
			// final int expensiveStrategiesPerCycle;
			// if (GreedoConfigGroup.ExpensiveStrategyTreatmentType.allOnce
			// .equals(greedoConfig.getExpensiveStrategyTreatment())) {
			// expensiveStrategiesPerCycle = expensiveStrategyCnt;
			// } else if (GreedoConfigGroup.ExpensiveStrategyTreatmentType.oneInTotal
			// .equals(greedoConfig.getExpensiveStrategyTreatment())) {
			// expensiveStrategiesPerCycle = 1;
			// } else {
			// throw new RuntimeException(
			// "Unknown expensive strategy treatment: " +
			// greedoConfig.getExpensiveStrategyTreatment());
			// }
			//
			// final int originalIterationsPerCycle = ierConfig.getIterationsPerCycle();
			// if (cheapStrategyCnt > 0) {
			// // Make sure that every strategy can be used used on average at least once.
			// ierConfig.setIterationsPerCycle(
			// Math.max(expensiveStrategiesPerCycle + cheapStrategyCnt,
			// originalIterationsPerCycle));
			// } else {
			// if (expensiveStrategyCnt > 0) {
			// // Only best-response strategies: every best-response strategy is used on
			// // average exactly once.
			// ierConfig.setIterationsPerCycle(expensiveStrategiesPerCycle);
			// } else {
			// // No innovation strategies at all!
			// throw new RuntimeException("No innovation strategies recognized. "
			// + "Greedo expects at least one. See greedo config module for recognized
			// innovation strategies.");
			// }
			// }
			// if (ierConfig.getIterationsPerCycle() != originalIterationsPerCycle) {
			// log.info("Adjusted number of emulated iterations per cycle from " +
			// originalIterationsPerCycle + " to "
			// + ierConfig.getIterationsPerCycle() + ".");
			// }
			//
			// /*
			// * Use minimal choice set and always remove the worse plan. This is probably
			// as
			// * close as it can get to best-response in the presence of random innovation
			// * strategies.
			// */
			// config.strategy().setMaxAgentPlanMemorySize(1);
			// config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");
			// log.info("Approximating a best-response simulation through the following
			// settings:");
			// log.info(" * maxAgentPlanMemorySize = 1");
			// log.info(" * planSelectorForRemoval = worstPlanSelector");
			//
			// /*
			// * Maintain innovation throughout. Greedo step size control replaces the
			// * "fraction".
			// */
			// config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);
			// log.info("Setting fractionOfIterationsToDisableInnovation to infinity.");
			//
			// /*
			// * Keep only plan innovation strategies. Re-weight for maximum emulation
			// * efficiency.
			// */
			// final double singleExpensiveStrategyProba;
			// if (GreedoConfigGroup.ExpensiveStrategyTreatmentType.allOnce
			// .equals(greedoConfig.getExpensiveStrategyTreatment())) {
			// singleExpensiveStrategyProba = 1.0 / ierConfig.getIterationsPerCycle();
			// } else if (GreedoConfigGroup.ExpensiveStrategyTreatmentType.oneInTotal
			// .equals(greedoConfig.getExpensiveStrategyTreatment())) {
			// singleExpensiveStrategyProba = (1.0 / ierConfig.getIterationsPerCycle()) /
			// expensiveStrategyCnt;
			// } else {
			// throw new RuntimeException(
			// "Unknown expensive strategy treatment: " +
			// greedoConfig.getExpensiveStrategyTreatment());
			// }
			//
			// final double cheapStrategyProbaSum = 1.0 - singleExpensiveStrategyProba *
			// expensiveStrategyCnt;
			// final double cheapStrategyWeightFactor = cheapStrategyProbaSum /
			// cheapStrategyWeightSum;
			// double probaSum = 0;
			// for (StrategySettings strategySettings :
			// config.strategy().getStrategySettings()) {
			// final String strategyName = strategySettings.getStrategyName();
			// if (greedoConfig.getExpensiveStrategyList().contains(strategyName)) {
			// strategySettings.setWeight(singleExpensiveStrategyProba);
			// } else if (greedoConfig.getCheapStrategyList().contains(strategyName)) {
			// strategySettings.setWeight(cheapStrategyWeightFactor *
			// strategySettings.getWeight());
			// } else {
			// strategySettings.setWeight(0.0); // i.e., dismiss
			// }
			// log.info("Setting weight of strategy " + strategyName + " to " +
			// strategySettings.getWeight() + ".");
			// probaSum += strategySettings.getWeight();
			// }
			// if (Math.abs(1.0 - probaSum) >= 1e-8) {
			// throw new RuntimeException("The sum of all strategy probabilities is " +
			// probaSum + ".");
			// }

			// <<<<< OLD <<<<<
		}
	}

	public void meet(final Scenario scenario) {
		if (this.config == null) {
			throw new RuntimeException("First meet the config.");
		} else if (this.scenario != null) {
			throw new RuntimeException("Have already met the scenario.");
		}
		this.scenario = scenario;
		ConfigUtils.addOrGetModule(this.config, GreedoConfigGroup.class).configure(scenario);
	}

	public void meet(final Controler controler) {
		if (this.scenario == null) {
			throw new RuntimeException("First meet the scenario.");
		}
		for (AbstractModule module : this.getModules()) {
			controler.addOverridingModule(module);
		}
	}

	// TODO Can these be summarized into one module?
	public AbstractModule[] getModules() {
		final AbstractModule greedoModule = new AbstractModule() {
			@Override
			public void install() {
				this.bind(WireGreedoIntoMATSimControlerListener.class).in(Singleton.class);
				this.addEventHandlerBinding().toProvider(WireGreedoIntoMATSimControlerListener.class);
			}
		};
		final AbstractModule ierModule = new IERModule(WireGreedoIntoMATSimControlerListener.class);
		return new AbstractModule[] { greedoModule, ierModule };
	}
}
