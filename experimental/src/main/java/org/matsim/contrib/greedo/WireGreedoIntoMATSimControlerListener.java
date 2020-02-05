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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.listeners.SlotUsageListener;
import org.matsim.contrib.greedo.logging.AsymptoticAgeLogger;
import org.matsim.contrib.greedo.logging.AvgAnticipatedDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgAnticipatedReplannerDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgNonReplannerSize;
import org.matsim.contrib.greedo.logging.AvgRealizedDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgRealizedUtility;
import org.matsim.contrib.greedo.logging.AvgReplannerSize;
import org.matsim.contrib.greedo.logging.Beta;
import org.matsim.contrib.greedo.logging.Beta0;
import org.matsim.contrib.greedo.logging.LambdaBar;
import org.matsim.contrib.greedo.logging.LambdaRealized;
import org.matsim.contrib.greedo.logging.MATSimIteration;
import org.matsim.contrib.greedo.logging.NormalizedWeightedCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedWeightedNonReplannerCountDifferences2;
import org.matsim.contrib.greedo.logging.NormalizedWeightedReplannerCountDifferences2;
import org.matsim.contrib.greedo.logging.ReplanningRecipe;
import org.matsim.contrib.ier.replannerselection.ReplannerSelector;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import com.google.inject.Inject;
import com.google.inject.Provider;

import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
@Singleton
public class WireGreedoIntoMATSimControlerListener implements Provider<EventHandler>, ReplannerSelector {

	// -------------------- MEMBERS --------------------

	private final MatsimServices services;

	private final GreedoConfigGroup greedoConfig;

	private final StatisticsWriter<LogDataWrapper> statsWriter;

	private final Utilities utilities;

	private final Ages ages;

	// private final ReplanningEfficiencyEstimator2 replanningEfficiencyEstimator;

	private final AsymptoticAgeLogger asymptoticAgeLogger;

	private final SlotUsageListener physicalSlotUsageListener;

	private final List<SlotUsageListener> hypotheticalSlotUsageListeners = new LinkedList<>();

	private double betaScale = 1.0;

	private Plans lastPhysicalPopulationState = null;

	private ReplannerIdentifier.SummaryStatistics lastReplanningSummaryStatistics = new ReplannerIdentifier.SummaryStatistics();

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public WireGreedoIntoMATSimControlerListener(final MatsimServices services) {

		this.services = services;
		this.greedoConfig = ConfigUtils.addOrGetModule(this.services.getConfig(), GreedoConfigGroup.class);
		this.utilities = new Utilities();
		this.ages = new Ages(services.getScenario().getPopulation().getPersons().keySet(), this.greedoConfig);
		this.physicalSlotUsageListener = new SlotUsageListener(this.greedoConfig.newTimeDiscretization(),
				this.ages.getWeightsView(), this.greedoConfig.getConcurrentLinkWeights(),
				this.greedoConfig.getConcurrentTransitVehicleWeights());
		// this.replanningEfficiencyEstimator = new
		// ReplanningEfficiencyEstimator2(this.greedoConfig);

		this.asymptoticAgeLogger = new AsymptoticAgeLogger(this.greedoConfig.getMaxRelativeMemoryLength(),
				new File("./output/"), "asymptoticAges.", ".txt");

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(), false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new MATSimIteration());
		this.statsWriter.addSearchStatistic(new ReplanningRecipe());
		this.statsWriter.addSearchStatistic(new LambdaRealized());
		this.statsWriter.addSearchStatistic(new LambdaBar());
		this.statsWriter.addSearchStatistic(new Beta());
		this.statsWriter.addSearchStatistic(new Beta0());
		// this.statsWriter.addSearchStatistic(this.replanningEfficiencyEstimator.newBetaStatistic());
		// this.statsWriter.addSearchStatistic(this.replanningEfficiencyEstimator.newDeltaStatistic());
		this.statsWriter.addSearchStatistic(this.ages.newAvgAgeStatistic());
		this.statsWriter.addSearchStatistic(this.ages.newAvgAgeWeightStatistic());
		// this.statsWriter.addSearchStatistic(this.replanningEfficiencyEstimator.newDeltaX2vsDeltaDeltaUStatistic());
		// this.statsWriter
		// .addSearchStatistic(this.asymptoticAgeLogger.newAgeVsSimilarityByExpDeltaUtilityCorrelationStatistic());
		// this.statsWriter.addSearchStatistic(
		// this.asymptoticAgeLogger.newAvgAgeVsAvgSimilarityByAvgExpDeltaUtilityCorrelationStatistic());
		this.statsWriter.addSearchStatistic(
				this.asymptoticAgeLogger.newAgeTimesExpDeltaUtilityVsSimilarityCorrelationStatistic());
		// this.statsWriter.addSearchStatistic(
		// this.asymptoticAgeLogger.newAvgAgeTimesAvgExpDeltaUtilityVsAvgSimilarityCorrelationStatistic());
		this.statsWriter.addSearchStatistic(new AvgReplannerSize());
		this.statsWriter.addSearchStatistic(new AvgNonReplannerSize());
		this.statsWriter.addSearchStatistic(new NormalizedWeightedReplannerCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedWeightedNonReplannerCountDifferences2());
		this.statsWriter.addSearchStatistic(new NormalizedWeightedCountDifferences2());
		// this.statsWriter.addSearchStatistic(new AvgNonReplannerUtilityChange());
		this.statsWriter.addSearchStatistic(new AvgRealizedUtility());
		this.statsWriter.addSearchStatistic(new AvgRealizedDeltaUtility());
		this.statsWriter.addSearchStatistic(new AvgAnticipatedReplannerDeltaUtility());
		this.statsWriter.addSearchStatistic(new AvgAnticipatedDeltaUtility());
		// this.statsWriter.addSearchStatistic(this.replanningEfficiencyEstimator.newAvgPredictedDeltaUtility());
		for (int percent = 5; percent <= 95; percent += 5) {
			this.statsWriter.addSearchStatistic(this.ages.newAgePercentile(percent));
		}
		// for (int percent = 0; percent <= 100; percent += 10) {
		// this.statsWriter.addSearchStatistic(this.replanningEfficiencyEstimator.newAgeWeightStatistic(percent));
		// }
	}

	// -------------------- INTERNALS --------------------

	private int iteration() {
		return this.physicalSlotUsageListener.getLastResetIteration();
	}

	// -------------------- IMPLEMENTATION OF ReplannerSelector --------------------

	/*-
	 * The ReplannerSelector functionality is called at the beginning respectively
	 * end of the "replanning" step in the "MATSim loop". "Replanning" is (i) called
	 * at the beginning of each iteration (apart from the first) and (ii) is always
	 * preceded by "execution (mobsim)" and "scoring".
	 * 
	 * One hence deals with the realized network experience of the previous
	 * iteration. At the beginning of "replanning" (i.e. in
	 * beforeReplanningAndGetEventHandlerProvider()), the expectations also apply to
	 * the previous iteration. At the end of "replanning" (i.e. in
	 * afterReplanning()), the expectation applies to the upcoming iteration.
	 * However, given that expectations are differentiated wrt. replanners and
	 * non-replanners, they are only available at the end of "replanning", and hence
	 * only for the upcoming iteration.
	 * 
	 * 			 |	experience 	 |	expectation  |	expectation 
	 * 			 |	before/after |	before		 |	after
	 * 	it.		 |	replanning	 |	replanning	 |	replanning
	 * ----------+---------------+---------------+---------------
	 * 	k		 |	k-1			 |	k-1			 |	k
	 * 	k+1	 	 |	k			 |	k			 |	k+1
	 * 
	 * Experience and expectation are hence consistent at the beginning of "replanning", 
	 * where they apply to the previous (just completed) iteration.
	 */

	@Override
	public IEREventHandlerProvider beforeReplanningAndGetEventHandlerProvider() {

//		if (IERChecker.hasHypotheticalPlans()) {
//			IERChecker.initRealized();
//			for (Id<Person> personId : IERChecker.hypotheticalIds()) {
//				IERChecker.registerRealizedPlan(personId,
//						this.services.getScenario().getPopulation().getPersons().get(personId).getSelectedPlan());
//			}
//			IERChecker.analyze("ierAnalysis_" + this.iteration() + ".txt");
//		}
//		IERChecker.clearHypothetical();
//		IERChecker.clearRealized();

		// TODO NEW 2019-07-21
		if (this.greedoConfig.getAdjustStrategyWeights()) {
			final BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<>();
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				person.setSelectedPlan(bestPlanSelector.selectPlan(person));
				PersonUtils.removeUnselectedPlans(person);
			}
		}

		this.lastPhysicalPopulationState = new Plans(this.services.getScenario().getPopulation());
		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			this.utilities.updateRealizedUtility(person.getId(),
					this.lastPhysicalPopulationState.getSelectedPlan(person.getId()).getScore());
		}

		final LogDataWrapper logDataWrapper = new LogDataWrapper(this.greedoConfig,
				this.utilities.newSummaryStatistics(), this.lastReplanningSummaryStatistics,
				this.services.getIterationNumber() - 1);
		this.statsWriter.writeToFile(logDataWrapper);
		this.greedoConfig.getReplannerIdentifierRecipe().update(logDataWrapper);
		// this.replanningEfficiencyEstimator.update(logDataWrapper);
		this.asymptoticAgeLogger.update(logDataWrapper);

		this.hypotheticalSlotUsageListeners.clear();
		return new IEREventHandlerProvider() {
			@Override
			public synchronized EventHandler get(final Set<Id<Person>> personIds) {
				final SlotUsageListener listener = new SlotUsageListener(greedoConfig.newTimeDiscretization(),
						ages.getWeightsView().entrySet().stream().filter(entry -> personIds.contains(entry.getKey()))
								.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())),
						greedoConfig.getConcurrentLinkWeights(), greedoConfig.getConcurrentTransitVehicleWeights());
				listener.setReactToReset(false); // because many parallel calls per agent might occur
				hypotheticalSlotUsageListeners.add(listener);
				return listener;
			}
		};
	}

	@Override
	public void afterReplanning() {

		if (this.greedoConfig.getAdjustStrategyWeights()) {
			final BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<>();
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				person.setSelectedPlan(bestPlanSelector.selectPlan(person));
				PersonUtils.removeUnselectedPlans(person);
				this.utilities.updateExpectedUtility(person.getId(), person.getSelectedPlan().getScore());
			}
		}

		final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> hypotheticalSlotUsageIndicators = new LinkedHashMap<>();
		for (SlotUsageListener listener : this.hypotheticalSlotUsageListeners) {
			hypotheticalSlotUsageIndicators.putAll(listener.getIndicatorView());
		}
		this.hypotheticalSlotUsageListeners.clear();

		final Utilities.SummaryStatistics utilityStats = this.utilities.newSummaryStatistics();
		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(
				// this.replanningEfficiencyEstimator.getBeta(),
				this.greedoConfig, this.iteration(), this.physicalSlotUsageListener.getIndicatorView(),
				hypotheticalSlotUsageIndicators, utilityStats.personId2expectedUtilityChange,
				utilityStats.personId2experiencedUtility,
				// this.realizedToTargetLambdaRatio,
				this.betaScale);
		final Set<Id<Person>> replannerIds = replannerIdentifier.drawReplanners();

//		IERChecker.initHypothetical();
//		IERChecker.clearRealized();

		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			if (person.getPlans().size() != 1) {
				throw new RuntimeException("Person " + person.getId() + " has " + person.getPlans().size() + " plans.");
			}
			/*
			 * 2019-07-20. What happens here is that non-replanners are re-set to their old
			 * plans. Fine. But replanners should have a chance to switch back to their
			 * previous plan in case it turns out that the new choice was bad. So the new
			 * plan needs to be ADDED as SELECTED to the choice set (then of size 2). Then
			 * comes the network loading, the chosen plan gets scored, and only then the
			 * worst of the two (both now based on physical network loadings) gets removed,
			 * and the better one enters the next better-response replanning round.
			 */
			if (replannerIds.contains(person.getId())) {
				// Replanners select the new plan for execution and keep the old plan.
				person.setSelectedPlan(person.getPlans().get(0)); // new
				this.lastPhysicalPopulationState.add(person); // old

//				IERChecker.registerHypotheticalPlan(person.getId(), person.getSelectedPlan());

			} else {
				// Non-replanners return to the old plan.
				this.lastPhysicalPopulationState.set(person); // old
			}
		}
		this.lastReplanningSummaryStatistics = replannerIdentifier.getSummaryStatistics(replannerIds,
				this.ages.getAgesView());

		this.ages.update(replannerIds);
		this.physicalSlotUsageListener.updatePersonWeights(this.ages.getWeightsView());

		// TODO 2019-07-08 NEW
		if (this.greedoConfig.getEnforceMeanReplanningRate()) {
			final double lambdaRealized = ((double) this.lastReplanningSummaryStatistics.numberOfReplanners)
					/ this.lastReplanningSummaryStatistics.getNumberOfReplanningCandidates();
			final double lambdaBar = this.lastReplanningSummaryStatistics.lambdaBar;
			// this.realizedToTargetLambdaRatio.add(lambdaRealized / lambdaBar);
			this.betaScale *= (lambdaBar / lambdaRealized);
		}
	}

	// --------------- IMPLEMENTATION OF Provider<EventHandler> ---------------

	@Override
	public EventHandler get() {
		return this.physicalSlotUsageListener;
	}
}
