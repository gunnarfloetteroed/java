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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.listeners.SlotUsageListener;
import org.matsim.contrib.greedo.logging.AvgAnticipatedDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgAnticipatedReplannerDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgNonReplannerSize;
import org.matsim.contrib.greedo.logging.AvgRealizedDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgRealizedUtility;
import org.matsim.contrib.greedo.logging.AvgReplannerSize;
import org.matsim.contrib.greedo.logging.CnMean;
import org.matsim.contrib.greedo.logging.CnStddev;
import org.matsim.contrib.greedo.logging.DoesNothingShare;
import org.matsim.contrib.greedo.logging.EEstim2Mean;
import org.matsim.contrib.greedo.logging.EEstimMean;
import org.matsim.contrib.greedo.logging.ENaive2Mean;
import org.matsim.contrib.greedo.logging.ENaiveMean;
import org.matsim.contrib.greedo.logging.ENull2Mean;
import org.matsim.contrib.greedo.logging.ENullMean;
import org.matsim.contrib.greedo.logging.LambdaRealized;
import org.matsim.contrib.greedo.logging.MATSimIteration;
import org.matsim.contrib.greedo.logging.ReplanningRecipe;
import org.matsim.contrib.ier.replannerselection.ReplannerSelector;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import com.google.inject.Inject;
import com.google.inject.Provider;

import floetteroed.utilities.math.BasicStatistics;
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

	private final DisappointmentAnalyzer disappointmentAnalyzer;

	private StationaryReplanningRegulator stationaryReplanningRegulator;

	private final SlotUsageListener physicalSlotUsageListener;

	private final List<SlotUsageListener> hypotheticalSlotUsageListeners = new LinkedList<>();

	private Plans lastPhysicalPopulationState = null;

	private ReplannerIdentifier.SummaryStatistics lastReplanningSummaryStatistics = new ReplannerIdentifier.SummaryStatistics();

	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> previousCurrentSlotUsages = null;
	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> previousAnticipatedSlotUsages = null;
	private Set<Id<Person>> previousReplannerIds = null;

	private Map<Id<Person>, Double> personId2meanDeltaUn0 = new LinkedHashMap<>();

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

		this.disappointmentAnalyzer = new DisappointmentAnalyzer(this.greedoConfig);
		this.stationaryReplanningRegulator = new StationaryReplanningRegulator(
				this.greedoConfig.getPercentileStepSize(),
				this.greedoConfig.getMSAReplanningRate(services.getConfig().controler().getFirstIteration(), true));

		// TODO CONTINUE HERE

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(), false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new MATSimIteration());
		this.statsWriter.addSearchStatistic(new ReplanningRecipe());
		this.statsWriter.addSearchStatistic(new LambdaRealized());
		this.statsWriter.addSearchStatistic(this.ages.newAvgAgeStatistic());
		this.statsWriter.addSearchStatistic(this.ages.newAvgAgeWeightStatistic());

		this.statsWriter.addSearchStatistic(new DoesNothingShare());

		this.statsWriter.addSearchStatistic(new ENullMean());
		this.statsWriter.addSearchStatistic(new ENaiveMean());
		this.statsWriter.addSearchStatistic(new EEstimMean());
		this.statsWriter.addSearchStatistic(new ENull2Mean());
		this.statsWriter.addSearchStatistic(new ENaive2Mean());
		this.statsWriter.addSearchStatistic(new EEstim2Mean());

		this.statsWriter.addSearchStatistic(new CnMean());
		this.statsWriter.addSearchStatistic(new CnStddev());
		this.statsWriter.addSearchStatistic(new AvgReplannerSize());
		this.statsWriter.addSearchStatistic(new AvgNonReplannerSize());
		this.statsWriter.addSearchStatistic(new AvgRealizedUtility());
		this.statsWriter.addSearchStatistic(new AvgRealizedDeltaUtility());
		this.statsWriter.addSearchStatistic(new AvgAnticipatedReplannerDeltaUtility());
		this.statsWriter.addSearchStatistic(new AvgAnticipatedDeltaUtility());
		for (int percent = 5; percent <= 95; percent += 5) {
			this.statsWriter.addSearchStatistic(this.ages.newAgePercentile(percent));
		}
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

		if (this.previousReplannerIds != null /* has replanned before */) {
			final Utilities.SummaryStatistics utilityStats = this.utilities.newSummaryStatistics();
			
// TODO EXPERIMENTAL, TEMPORARILY REMOVED
//			this.disappointmentAnalyzer.update(this.previousCurrentSlotUsages, this.previousAnticipatedSlotUsages,
//					utilityStats.personId2realizedUtilityChange, utilityStats.personId2expectedUtilityChange,
//					this.previousReplannerIds, this.lastReplanningSummaryStatistics.getPersonId2InteractionsView(),
//					this.greedoConfig.getMSAReplanningRate(this.iteration(), false));

			this.stationaryReplanningRegulator
					.setMeanReplanningRate(this.greedoConfig.getMSAReplanningRate(this.iteration(), true));
			this.stationaryReplanningRegulator.setStepSize(this.greedoConfig.getPercentileStepSize());
			// ORIGINAL BELOW
			this.stationaryReplanningRegulator.update(utilityStats.personId2expectedUtilityChange,
					this.lastReplanningSummaryStatistics.getPersonId2Dn0View(),
					this.lastReplanningSummaryStatistics.getPersonId2TnView());
			// EXPERIMENTAL VARIATIONS BELOW
//			this.stationaryReplanningRegulator.update(utilityStats.personId2expectedUtilityChange,
//					this.lastReplanningSummaryStatistics.getPersonId2Dn1MinusDn0View(),
//					this.lastReplanningSummaryStatistics.getPersonId2TnView());

			BasicStatistics stats = new BasicStatistics();
			for (Double cn : this.stationaryReplanningRegulator.getCnView().values()) {
				stats.add(cn);
			}
			Logger.getLogger(this.getClass())
					.info("CN STATS: mean = " + stats.getAvg() + "  stddev = " + stats.getStddev());

		}

		final LogDataWrapper logDataWrapper = new LogDataWrapper(this.greedoConfig,
				this.utilities.newSummaryStatistics(), this.lastReplanningSummaryStatistics,
				this.disappointmentAnalyzer.getSummaryStatistics(), this.services.getIterationNumber() - 1,
				this.stationaryReplanningRegulator.getCnView().values());
		this.statsWriter.writeToFile(logDataWrapper);

		// ORIGINAL
		// this.greedoConfig.getReplannerIdentifierRecipe().update(logDataWrapper);
		// EXPERIMENTAL BELOW
		final double innoWeight = this.greedoConfig.getMSAReplanningRate(this.iteration(), false);
		for (Map.Entry<Id<Person>, Double> entry : this.utilities.newSummaryStatistics().personId2expectedUtilityChange
				.entrySet()) {
			final Double oldValue = this.personId2meanDeltaUn0.get(entry.getKey());
			if (oldValue == null) {
				this.personId2meanDeltaUn0.put(entry.getKey(), entry.getValue());
			} else {
				this.personId2meanDeltaUn0.put(entry.getKey(),
						(1.0 - innoWeight) * oldValue + innoWeight * entry.getValue());
			}
		}
		this.greedoConfig.getReplannerIdentifierRecipe().update(logDataWrapper, this.personId2meanDeltaUn0);

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

		final double lambdaBar;
		if ((this.lastReplanningSummaryStatistics.numberOfReplanners != null)
				&& (this.lastReplanningSummaryStatistics.numberOfNonReplanners != null)) {
			lambdaBar = ((double) this.lastReplanningSummaryStatistics.numberOfReplanners)
					/ (this.lastReplanningSummaryStatistics.numberOfReplanners
							+ this.lastReplanningSummaryStatistics.numberOfNonReplanners);
		} else {
			lambdaBar = 1.0;
		}

		final Utilities.SummaryStatistics utilityStats = this.utilities.newSummaryStatistics();

//		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(
//				this.physicalSlotUsageListener.getIndicatorView(), hypotheticalSlotUsageIndicators,
//				utilityStats.personId2expectedUtilityChange, utilityStats.personId2experiencedUtility,
//				this.greedoConfig, this.disappointmentAnalyzer.getBView(), lambdaBar,
//				this.services.getScenario().getNetwork(), this.stationaryReplanningRegulator.getCnView());
		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(
				this.physicalSlotUsageListener.getIndicatorView(), hypotheticalSlotUsageIndicators,
				this.personId2meanDeltaUn0, utilityStats.personId2experiencedUtility, this.greedoConfig,
				this.disappointmentAnalyzer.getBView(), lambdaBar, this.services.getScenario().getNetwork(),
				this.stationaryReplanningRegulator.getCnView());

		final Set<Id<Person>> replannerIds = replannerIdentifier.drawReplanners();

		this.previousAnticipatedSlotUsages = new LinkedHashMap<>(hypotheticalSlotUsageIndicators);
		this.previousCurrentSlotUsages = new LinkedHashMap<>(this.physicalSlotUsageListener.getIndicatorView());
		this.previousReplannerIds = new LinkedHashSet<>(replannerIds);

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
			} else {
				// Non-replanners return to the old plan.
				this.lastPhysicalPopulationState.set(person); // old
			}
		}
		this.lastReplanningSummaryStatistics = replannerIdentifier.getSummaryStatistics(replannerIds,
				this.ages.getAgesView());

		this.ages.update(replannerIds);
		this.physicalSlotUsageListener.updatePersonWeights(this.ages.getWeightsView());
	}

	// --------------- IMPLEMENTATION OF Provider<EventHandler> ---------------

	@Override
	public EventHandler get() {
		return this.physicalSlotUsageListener;
	}
}
