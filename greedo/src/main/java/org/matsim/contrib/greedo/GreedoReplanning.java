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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.emulation.EmulationEngine;
import org.matsim.contrib.emulation.emulators.AgentEmulator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.BasicStatistics;
import floetteroed.utilities.statisticslogging.Statistic;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

/**
 * @author Gunnar Flötteröd
 */
@Singleton
public final class GreedoReplanning implements PlansReplanning, ReplanningListener, AfterMobsimListener {

	// -------------------- CONSTANTS --------------------

	private final static Logger logger = Logger.getLogger(GreedoReplanning.class);

	private final GreedoConfigGroup greedoConfig;

	private final StrategyManager strategyManager;
	private final Provider<ReplanningContext> replanningContextProvider;
	private final Provider<AgentEmulator> agentEmulatorProvider;
	private final MatsimServices services;

	private final PlanChangeAnalyzer planChangeAnalyzer;

	private final StatisticsWriter<GreedoReplanning> statsWriter;

	private Function<Integer, Double> iterationToStepSize = null;

	// -------------------- MEMBERS --------------------

	private final File weightsFile;

	private Integer replanIteration = null;

	private Double replanningRate = null;
	private Double gap = null;
	private Double weightedGap = null;

	private double gapThreshold = 0.0;
	private Double meanReplannerGap = null;

	private final LinkedList<LinkTravelTimeCopy> travelTimes = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	@Inject
	GreedoReplanning(final StrategyManager strategyManager, final Provider<ReplanningContext> replanningContextProvider,
			final Provider<AgentEmulator> agentEmulatorProvider, final MatsimServices services) {

		this.greedoConfig = ConfigUtils.addOrGetModule(services.getConfig(), GreedoConfigGroup.class);
		this.strategyManager = strategyManager;
		this.replanningContextProvider = replanningContextProvider;
		this.agentEmulatorProvider = agentEmulatorProvider;
		this.services = services;

		this.planChangeAnalyzer = new PlanChangeAnalyzer(services.getScenario().getPopulation().getPersons().keySet(),
				this.greedoConfig.getSmoothingInertia(), services.getConfig().controler().getOutputDirectory());

		this.weightsFile = new File(services.getConfig().controler().getOutputDirectory(), "GreedoWeights.log");
		if (this.weightsFile.exists()) {
			this.weightsFile.delete();
		}
		try {
			this.weightsFile.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controler().getOutputDirectory(), "GreedoReplanning.log").toString(),
				false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "ReplanIteration";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.replanIteration.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MemorizedTravelTimes";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Integer.toString(data.travelTimes.size());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "StepSize";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.iterationToStepSize.apply(data.replanIteration).toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "ReplanningRate";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.replanningRate.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "GapThreshold";
			}

			@Override
			public String value(GreedoReplanning data) {
				return Double.toString(data.gapThreshold);
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MeanReplannerGap";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.meanReplannerGap == null ? "" : data.meanReplannerGap.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "Gap";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.gap.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "WeightedGap";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.weightedGap.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MinKernelMu";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.planChangeAnalyzer.getMinMu() == null ? ""
						: Double.toString(data.planChangeAnalyzer.getMinMu());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MaxKernelMu";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.planChangeAnalyzer.getMaxMu() == null ? ""
						: Double.toString(data.planChangeAnalyzer.getMaxMu());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "OptKernelMu";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.planChangeAnalyzer.getOptimalMu() == null ? ""
						: Double.toString(data.planChangeAnalyzer.getOptimalMu());
			}
		});
	}

	// -------------------- INTERNALS --------------------

	private void emulate(final MatsimServices services, final TravelTime travelTimes) {
		final EmulationEngine emulationEngine = new EmulationEngine(this.strategyManager, services.getScenario(),
				this.replanningContextProvider, this.agentEmulatorProvider, services.getConfig());

		try {
			emulationEngine.emulate(new LinkedHashSet<>(services.getScenario().getPopulation().getPersons().values()),
					services.getIterationNumber(), travelTimes);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void emulateAgainstAllTravelTimes(final List<Map<Id<Person>, Double>> personId2scorePerReplication,
			final boolean logError) {

		// >>>>> Checking emulation precision >>>>>
		final Map<Id<Person>, Double> personId2oldScore = new LinkedHashMap<>();
		if (logError) {
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				// TODO stream!
				personId2oldScore.put(person.getId(), person.getSelectedPlan().getScore());
			}
		}
		// <<<<< Checking emulation precision <<<<<

		for (LinkTravelTimeCopy travelTime : this.travelTimes) {
			this.emulate(this.services, travelTime);
			final Map<Id<Person>, Double> scores = new LinkedHashMap<>();
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				scores.put(person.getId(), person.getSelectedPlan().getScore());
			}
			personId2scorePerReplication.add(scores);
		}

		// >>>>> Checking emulation precision >>>>>
		if (logError) {
			final BasicStatistics stats = new BasicStatistics();
			for (Id<Person> personId : this.services.getScenario().getPopulation().getPersons().keySet()) {
				stats.add(personId2scorePerReplication.get(0).get(personId) - personId2oldScore.get(personId));
			}
			Logger.getLogger(this.getClass()).info("Emulation score error: mean = " + stats.getAvg() + ", stddev = "
					+ stats.getStddev() + ", min = " + stats.getMin() + ", max = " + stats.getMax());
		}
		// <<<<< Checking emulation precision <<<<<

	}

	// -------------------- AFTER MOBSIM LISTENER --------------------

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
//		while (this.travelTimes.size() >= Math.min(this.recommendedMemory, this.greedoConfig.getMaxMemory())) {

//		while (this.travelTimes.size() >= this.greedoConfig.getMaxMemory()) {
//			this.travelTimes.removeFirst();
//		}
//		this.travelTimes.addLast(new LinkTravelTimeCopy(event.getServices()));
		while (this.travelTimes.size() >= this.greedoConfig.getMaxMemory()) {
			this.travelTimes.removeLast();
		}
		this.travelTimes.addFirst(new LinkTravelTimeCopy(event.getServices()));
	}

	// -------------------- REPLANNING LISTENER --------------------

	@Override
	public void notifyReplanning(final ReplanningEvent event) {

		if (this.replanIteration == null) {
			this.replanIteration = 0;
		} else {
			this.replanIteration++;
		}

		final Set<Id<Person>> personIds = this.services.getScenario().getPopulation().getPersons().keySet();
		final Collection<? extends Person> persons = this.services.getScenario().getPopulation().getPersons().values();

		/*
		 * (1) Extract old plans and compute new plans. Evaluate both old and new plans.
		 */

		final Plans oldPlans = new Plans(this.services.getScenario().getPopulation());
		final List<Map<Id<Person>, Double>> personId2oldScoreOverReplications = new ArrayList<>(
				this.travelTimes.size());
		this.emulateAgainstAllTravelTimes(personId2oldScoreOverReplications, true);

		final Plans newPlans;
		{
			final EmulationEngine replanningEngine = new EmulationEngine(this.strategyManager,
					event.getServices().getScenario(), this.replanningContextProvider, this.agentEmulatorProvider,
					event.getServices().getConfig());
			replanningEngine.replan(event.getIteration(), this.travelTimes.getFirst());
			final BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<>();
			for (Person person : persons) {
				person.setSelectedPlan(bestPlanSelector.selectPlan(person));
				PersonUtils.removeUnselectedPlans(person);
			}
			newPlans = new Plans(event.getServices().getScenario().getPopulation());
		}
		final List<Map<Id<Person>, Double>> personId2newScoreOverReplications = new ArrayList<>(
				this.travelTimes.size());
		this.emulateAgainstAllTravelTimes(personId2newScoreOverReplications, false);

		/*
		 * (2) Compute intermediate statistics.
		 */

		this.gap = personId2newScoreOverReplications.get(0).values().stream().mapToDouble(s -> s).average()
				.getAsDouble()
				- personId2oldScoreOverReplications.get(0).values().stream().mapToDouble(s -> s).average()
						.getAsDouble();

		final List<Double> kernelWeights = this.planChangeAnalyzer
				.updateMuAndCalculateWeights(personId2newScoreOverReplications, personId2oldScoreOverReplications);

		final StringBuffer line = new StringBuffer();
		for (Double weight : kernelWeights) {
			line.append(weight + "\t");
		}
		line.append("\n");
		try {
			FileUtils.write(this.weightsFile, line.toString(), true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		final Map<Id<Person>, Double> personId2weightedOldScore = new LinkedHashMap<>(personIds.size());
		final List<Tuple<Id<Person>, Double>> personIdAndGap = new ArrayList<>(personIds.size());
		for (Id<Person> personId : personIds) {
			double weightedGap = 0.0;
			double weightedOldScore = 0.0;
			for (int lag = 0; lag < kernelWeights.size(); lag++) {
				weightedGap += kernelWeights.get(lag) * (personId2newScoreOverReplications.get(lag).get(personId)
						- personId2oldScoreOverReplications.get(lag).get(personId));
				weightedOldScore += kernelWeights.get(lag) * personId2oldScoreOverReplications.get(lag).get(personId);
			}
			personIdAndGap.add(new Tuple<>(personId, weightedGap));
			personId2weightedOldScore.put(personId, weightedOldScore);
		}
		Collections.shuffle(personIdAndGap, MatsimRandom.getRandom()); // to break symmetries
		Collections.sort(personIdAndGap, new Comparator<Tuple<Id<Person>, Double>>() {
			@Override
			public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
				return Double.compare(tuple2.getB(), tuple1.getB()); // largest gap first
			}
		});

		this.weightedGap = personIdAndGap.stream().map(t -> t.getB()).mapToDouble(g -> g).average().getAsDouble();

		/*
		 * (3) Step size initialization.
		 */

		if (this.iterationToStepSize == null) {

			if (GreedoConfigGroup.StepControlType.EXP.equals(this.greedoConfig.getStepControl())) {

				final double initialStepSize = this.greedoConfig.getInitialStepSizeFactor() * persons.size();
				final int firstIteration = this.services.getConfig().controler().getFirstIteration();
				final double stepSizeShrinkageRate = Math.pow(initialStepSize,
						(-1.0) / (this.services.getConfig().controler().getLastIteration() - firstIteration));
				this.iterationToStepSize = new Function<>() {
					@Override
					public Double apply(Integer iteration) {
						return initialStepSize * Math.pow(stepSizeShrinkageRate, iteration - firstIteration);
					}
				};

			} else if (GreedoConfigGroup.StepControlType.MSA.equals(this.greedoConfig.getStepControl())) {

				this.iterationToStepSize = new Function<>() {
					@Override
					public Double apply(Integer iteration) {
						return greedoConfig.getInitialStepSizeFactor() * persons.size()
								* Math.pow(iteration + 1, greedoConfig.getReplanningRateIterationExponent());
					}
				};

			} else {
				throw new RuntimeException("Unknown step control: " + this.greedoConfig.getStepControl());
			}
		}

		/*
		 * (4) Identify re-planners.
		 */

		final Set<Id<Person>> replannerIds = new LinkedHashSet<>();
		final double lambdaBar = this.iterationToStepSize.apply(this.replanIteration) / persons.size();
		double replannerGapSum = 0.0;

		if (GreedoConfigGroup.ReplannerIdentifierType.MSA.equals(this.greedoConfig.getReplannerIdentifier())) {

			// final List<Id<Person>> personIdList = new ArrayList<>(personIds);
			// Collections.shuffle(personIdList);
			// replannerIds.addAll(personIdList.subList(0, (int) (lambdaBar *
			// personIdList.size())));

			final List<Tuple<Id<Person>, Double>> candidatePersonIdAndGapList = new ArrayList<>(personIdAndGap);
			Collections.shuffle(candidatePersonIdAndGapList);
			for (Tuple<Id<Person>, Double> tuple : candidatePersonIdAndGapList.subList(0,
					(int) (lambdaBar * candidatePersonIdAndGapList.size()))) {
				replannerIds.add(tuple.getA());
				replannerGapSum += tuple.getB();
			}

		} else if (GreedoConfigGroup.ReplannerIdentifierType.SBAYTI2007
				.equals(this.greedoConfig.getReplannerIdentifier())) {

			if (this.greedoConfig.isGapWeighting()) {

//					final double allGapSum = personIdAndGap.stream().map(tuple -> tuple.getB()).mapToDouble(g -> g)
//							.sum();
//					final double gapSumThreshold = lambdaBar * (allGapSum + 1e-8);
//					double gapSum = 0;
//					int n = 0;
//					while ((gapSum < gapSumThreshold) && (n < personIdAndGap.size())) {
//						final Tuple<Id<Person>, Double> replannerIdAndGap = personIdAndGap.get(n++);
//						replannerIds.add(replannerIdAndGap.getA());
//						gapSum += replannerIdAndGap.getB();
//					}
				throw new RuntimeException("gapWeighting is disregarded");

			} else {
				if (this.greedoConfig.isUsingThreshold()) {
					for (Tuple<Id<Person>, Double> tuple : personIdAndGap) {
						if (tuple.getB() >= this.gapThreshold) {
							replannerIds.add(tuple.getA());
							replannerGapSum += tuple.getB();
						}
					}
				} else {
					for (int n = 0; n < lambdaBar * personIdAndGap.size(); n++) {
						replannerIds.add(personIdAndGap.get(n).getA());
						replannerGapSum += personIdAndGap.get(n).getB();
					}
				}
			}

		} else if (GreedoConfigGroup.ReplannerIdentifierType.GAPPROP
				.equals(this.greedoConfig.getReplannerIdentifier())) {

			for (Tuple<Id<Person>, Double> tuple : personIdAndGap) {
				final Id<Person> candidateId = tuple.getA();
				final double gap = tuple.getB();
				final double oldCost = (-1.0) * personId2weightedOldScore.get(candidateId);
				if (oldCost < 0) {
					logger.warn("Old cost " + oldCost + " < 0.0 for person " + candidateId);
				}
				if (MatsimRandom.getRandom().nextDouble() < lambdaBar * gap / Math.max(1e-8, oldCost)) {
					replannerIds.add(candidateId);
					replannerGapSum += gap;
				}
			}

		} else {
			throw new RuntimeException("Unsupported replanning recipe: " + this.greedoConfig.getReplannerIdentifier());
		}

		for (Person person : persons) {
			if (replannerIds.contains(person.getId())) {
				newPlans.set(person);
			} else {
				oldPlans.set(person);
			}
		}
		this.replanningRate = ((double) replannerIds.size()) / personIds.size();

		if (replannerIds.size() > 0) {
			if (this.meanReplannerGap == null) {
				this.meanReplannerGap = replannerGapSum / replannerIds.size();
			} else {
				this.meanReplannerGap = (1.0 - lambdaBar) * this.meanReplannerGap
						+ lambdaBar * replannerGapSum / replannerIds.size();
			}
			final double frac = ((double) (event.getIteration()
					- this.services.getConfig().controler().getFirstIteration()))
					/ (this.services.getConfig().controler().getLastIteration()
							- this.services.getConfig().controler().getFirstIteration());
			this.gapThreshold = frac * this.meanReplannerGap;
//			this.gapThreshold = (1.0 - lambdaBar) * this.gapThreshold
//					+ lambdaBar * replannerGapSum / replannerIds.size();
		}

		this.planChangeAnalyzer.registerReplanners(replannerIds, this.travelTimes.size());

		this.statsWriter.writeToFile(this);
	}
}
