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

import static java.lang.Math.max;
import static java.lang.Math.sqrt;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.matsim.contrib.greedo.trustregion.SlotAnalyzer;
import org.matsim.contrib.greedo.trustregion.TrustRegionBasedReplannerSelector;
import org.matsim.contrib.ier.replannerselection.ReplannerSelector;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import com.google.inject.Inject;
import com.google.inject.Provider;

import floetteroed.utilities.statisticslogging.Statistic;
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

//	private final DisappointmentAnalyzer disappointmentAnalyzer;

//	private StationaryReplanningRegulator stationaryReplanningRegulator;

	private final SlotUsageListener physicalSlotUsageListener;

	private final List<SlotUsageListener> hypotheticalSlotUsageListeners = new LinkedList<>();

	private Plans lastPhysicalPopulationState = null; // reset to this if a replanner has to stick to its old plan

	private Plans secondLastPhysicalPopulationState = null; // reset to this if no improvement step

	private ReplannerIdentifier.SummaryStatistics lastReplanningSummaryStatistics = new ReplannerIdentifier.SummaryStatistics();

//	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> previousCurrentSlotUsages = null;
//	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> previousHypotheticalSlotUsages = null;
//	private Set<Id<Person>> previousReplannerIds = null;

	// private Map<Id<Person>, Double> personId2meanDeltaUn0 = new
	// LinkedHashMap<>();

	// private final NRouteHeuristicReplannerIdentifier nRoute;
	// private final UniformDissapointmentReplannerIdentifier uniformDiss;
	// private final SimpleDissapointmentAnalyzer simpleReplannerSelector;

//	private final ExperimentalCnSetter replannerSelector;

//	private final TrustRegionBasedReplannerSelector trustRegionBasedSelector;

	private MyMeanDeltaUn0 myMeanDeltaUn0 = new MyMeanDeltaUn0();

	static class MyMeanDeltaUn0 implements Statistic<LogDataWrapper> {

		private double value;

		void update(final Collection<Double> values) {
			this.value = values.stream().mapToDouble(x -> x).average().getAsDouble();
		}

		@Override
		public String label() {
			return "<DeltaUn0>";
		}

		@Override
		public String value(LogDataWrapper data) {
			return Double.toString(this.value);
		}
	};

	// private CongestionTracker congestionTracker;

	// private SlotUsageAverager slotUsageAverager = null;

	// private SlotUsageObserver<Id<Person>, Id<?>> slotUsageObserver;

	private SlotUsageAnalyzer slotUsageAnalyzer;

	// private SlotAnalyzer slotAnalyzer;

	private PopulationSampleManager popManager;
	
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

//		this.slotUsageAverager = new SlotUsageAverager(this.greedoConfig);
//		this.slotUsageObserver = new SlotUsageObserver<Id<Person>, Id<?>>(10);
		this.slotUsageAnalyzer = new SlotUsageAnalyzer(this.greedoConfig, services.getScenario().getPopulation());
		// this.slotAnalyzer = new SlotAnalyzer(this.greedoConfig);
		
//		this.trustRegionBasedSelector = new TrustRegionBasedReplannerSelector(this.services.getScenario(),
//				this.greedoConfig, 2);

		this.popManager = new PopulationSampleManager(this.services.getScenario(), this.greedoConfig);

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(), false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new MATSimIteration());
		this.statsWriter.addSearchStatistic(new ReplanningRecipe());

		this.statsWriter.addSearchStatistic(this.myMeanDeltaUn0);

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

	private final List<SlotUsageListener> overrideExperiencedScoresSlotUsageListeners = new LinkedList<>();

	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> secondLastOverwrittenRealizedSlotUsageIndicators = null;

	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastOverwrittenRealizedSlotUsageIndicators = null;

//	private Map<Id<Person>, Double> personId2lastPhysicalScore;

	@Override
	public IEREventHandlerProvider getOverrideExperiencedScoresEventHandlerProvider() {

//		this.personId2lastPhysicalScore = new LinkedHashMap<>();
//		this.services.getScenario().getPopulation().getPersons().values()
//				.forEach(p -> this.personId2lastPhysicalScore.put(p.getId(), p.getSelectedPlan().getScore()));

		this.overrideExperiencedScoresSlotUsageListeners.clear();
		return new IEREventHandlerProvider() {
			@Override
			public synchronized EventHandler get(final Set<Id<Person>> personIds) {
				final SlotUsageListener listener = new SlotUsageListener(greedoConfig.newTimeDiscretization(),
						ages.getWeightsView().entrySet().stream().filter(entry -> personIds.contains(entry.getKey()))
								.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())),
						greedoConfig.getConcurrentLinkWeights(), greedoConfig.getConcurrentTransitVehicleWeights());
				listener.setReactToReset(false); // because many parallel calls per agent might occur
				overrideExperiencedScoresSlotUsageListeners.add(listener);
				return listener;
			}
		};
	}

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

//		{
//			final BasicStatistics overriddenMinusPhysical = new BasicStatistics();
//			this.personId2lastPhysicalScore.entrySet()
//					.forEach(e -> overriddenMinusPhysical.add(this.services.getScenario().getPopulation().getPersons()
//							.get(e.getKey()).getSelectedPlan().getScore() - e.getValue()));
//			Logger.getLogger(this.getClass()).info("overriden minus physical stats: mean = "
//					+ overriddenMinusPhysical.getAvg() + ", stddev = " + overriddenMinusPhysical.getStddev());
//		}

		this.secondLastOverwrittenRealizedSlotUsageIndicators = lastOverwrittenRealizedSlotUsageIndicators;
		this.lastOverwrittenRealizedSlotUsageIndicators = new LinkedHashMap<>();
		for (SlotUsageListener listener : this.overrideExperiencedScoresSlotUsageListeners) {
			this.lastOverwrittenRealizedSlotUsageIndicators.putAll(listener.getIndicatorView());
		}
		this.overrideExperiencedScoresSlotUsageListeners.clear();

		this.slotUsageAnalyzer.setRealizedSlotUsages(this.lastOverwrittenRealizedSlotUsageIndicators);
//		this.slotAnalyzer.registerRealizedSlotUsages(this.lastOverwrittenRealizedSlotUsageIndicators, iteration(),
//				0.05); // TODO hard-wired innovation weight

//		if (this.secondLastOverwrittenRealizedSlotUsageIndicators != null) {
//			this.trustRegionBasedSelector.updatePersonSizes(this.lastOverwrittenRealizedSlotUsageIndicators,
//					this.secondLastOverwrittenRealizedSlotUsageIndicators);
//		}

		if (this.greedoConfig.getAdjustStrategyWeights()) {
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				if (person.getPlans().size() != 1) {
					throw new RuntimeException("Person " + person.getId() + " has " + person.getPlans().size()
							+ " plans, expected was one.");
				}
				if (person.getSelectedPlan() == null) {
					throw new RuntimeException("Person " + person.getId() + " has no selected plan.");
				}
			}
		}

		this.secondLastPhysicalPopulationState = this.lastPhysicalPopulationState;
		this.lastPhysicalPopulationState = new Plans(this.services.getScenario().getPopulation());
		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			this.utilities.updateRealizedUtility(person.getId(),
					this.lastPhysicalPopulationState.getSelectedPlan(person.getId()).getScore());
		}

		if (this.secondLastPhysicalPopulationState != null /* has replanned before */) {
			final Utilities.SummaryStatistics utilityStats = this.utilities.newSummaryStatistics();
			if (this.greedoConfig.getRejectFailure() && (utilityStats.personId2expectedUtilityChange.values().stream()
					.mapToDouble(s -> s).sum() < 0.0)) {
				this.lastPhysicalPopulationState = this.secondLastPhysicalPopulationState;
				this.lastOverwrittenRealizedSlotUsageIndicators = this.secondLastOverwrittenRealizedSlotUsageIndicators;
			}
//			this.trustRegionBasedSelector
//					.adjustTrustRegionAndIsImprovementStep(utilityStats.personId2realizedUtilityChange);
		}

		final LogDataWrapper logDataWrapper = new LogDataWrapper(this.greedoConfig,
				this.utilities.newSummaryStatistics(), this.lastReplanningSummaryStatistics,
				new DisappointmentAnalyzer.SummaryStatistics(), this.services.getIterationNumber() - 1,
				Arrays.asList(0.0));
		this.statsWriter.writeToFile(logDataWrapper);

		this.greedoConfig.getReplannerIdentifierRecipe().update(logDataWrapper);

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
		for (Id<Person> personId : this.services.getScenario().getPopulation().getPersons().keySet()) {
			this.popManager.addVisits(personId, 
					hypotheticalSlotUsageIndicators.get(personId),
					this.lastOverwrittenRealizedSlotUsageIndicators.get(personId),
					utilityStats.personId2expectedUtilityChange.get(personId));			
		}
		this.popManager.registerCompletedReplication();
		
//		final double lambdaBar;
//		if ((this.lastReplanningSummaryStatistics.numberOfReplanners != null)
//				&& (this.lastReplanningSummaryStatistics.numberOfNonReplanners != null)) {
//			lambdaBar = ((double) this.lastReplanningSummaryStatistics.numberOfReplanners)
//					/ (this.lastReplanningSummaryStatistics.numberOfReplanners
//							+ this.lastReplanningSummaryStatistics.numberOfNonReplanners);
//		} else {
//			lambdaBar = 1.0;
//		}

//		final Utilities.SummaryStatistics utilityStats = this.utilities.newSummaryStatistics();
//		this.secondLastPersonId2expectedUtilityChange = this.lastPersonId2expectedUtilityChange;
//		this.lastPersonId2expectedUtilityChange = this.utilities.newSummaryStatistics().personId2expectedUtilityChange;
//		if (this.secondLastPersonId2expectedUtilityChange != null) {
//			
//		}

//		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(
//				this.physicalSlotUsageListener.getIndicatorView(), hypotheticalSlotUsageIndicators,
//				utilityStats.personId2expectedUtilityChange, utilityStats.personId2experiencedUtility,
//				this.greedoConfig, this.disappointmentAnalyzer.getBView(), lambdaBar,
//				this.services.getScenario().getNetwork(), this.stationaryReplanningRegulator.getCnView());
//		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(
//				this.physicalSlotUsageListener.getIndicatorView(), hypotheticalSlotUsageIndicators,
//				this.personId2meanDeltaUn0, utilityStats.personId2experiencedUtility, this.greedoConfig,
//				this.disappointmentAnalyzer.getBView(), lambdaBar, this.services.getScenario().getNetwork(),
//				this.stationaryReplanningRegulator.getCnView());
//		final Set<Id<Person>> replannerIds = replannerIdentifier.drawReplanners();

//		final Set<Id<Person>> replannerIds = this.nRoute
//				.identifyReplanners(this.utilities.newSummaryStatistics().personId2expectedUtilityChange, this.greedoConfig.getMSAReplanningRate(this.iteration(), true));
//		this.nRoute.update(replannerIds, this.utilities.newSummaryStatistics().personId2expectedUtilityChange,
//				this.greedoConfig.getMSAReplanningRate(this.iteration(), false),
//				this.greedoConfig.getMSAReplanningRate(this.iteration(), false));

		final Set<Id<Person>> replannerIds;
		if (GreedoConfigGroup.ReplannerIdentifierType.accelerate
				.equals(this.greedoConfig.getReplannerIdentifierType())) {
//			replannerIds = this.uniformDiss.identifyReplanners(
//					this.utilities.newSummaryStatistics().personId2expectedUtilityChange,
//					this.greedoConfig.getMSAReplanningRate(this.iteration(), true));
//			this.uniformDiss.update(replannerIds, this.utilities.newSummaryStatistics().personId2expectedUtilityChange,
//					1.0 / Math.sqrt(1.0 + this.iteration()), // TODO hardcoded stepsize!
//					this.greedoConfig.getMSAReplanningRate(this.iteration(), true), hypotheticalSlotUsageIndicators);
//			replannerIds = this.simpleReplannerSelector
//					.selectReplanners(this.utilities.newSummaryStatistics().personId2expectedUtilityChange);
//			replannerIds = this.replannerSelector
//					.selectReplanners(this.utilities.newSummaryStatistics().personId2expectedUtilityChange);
//			replannerIds = this.slotUsageAverager
//					.identifyReplanners(this.services.getScenario().getPopulation().getPersons().keySet());
//			replannerIds = this.slotUsageAverager.selectReplanners(this.overwrittenRealizedSlotUsageIndicators,
//					hypotheticalSlotUsageIndicators, utilityStats.personId2expectedUtilityChange.values().stream()
//							.mapToDouble(x -> x).average().getAsDouble(),
//					1.0 / (1.0 + this.iteration()));

//			// 2021-03-18 functional impl
//			final SlotPacker slotPacker = new SlotPacker(this.services.getScenario().getNetwork(),
//					this.greedoConfig.newTimeDiscretization());
//			utilityStats.personId2expectedUtilityChange.forEach((id, deltaUn0) -> {
//				slotPacker.add(id, deltaUn0,
//						hypotheticalSlotUsageIndicators.getOrDefault(id,
//								new SpaceTimeIndicators<>(this.greedoConfig.getBinCnt())),
//						this.overwrittenRealizedSlotUsageIndicators.getOrDefault(id,
//								new SpaceTimeIndicators<>(this.greedoConfig.getBinCnt())));
//			});
//			replannerIds = slotPacker.pack(this.greedoConfig.getTrustRegion());

//			this.trustRegionBasedSelector.clear();
//			utilityStats.personId2expectedUtilityChange.forEach((id, deltaUn0) -> {
//				this.trustRegionBasedSelector.add(id, deltaUn0, hypotheticalSlotUsageIndicators.getOrDefault(id,
//						new SpaceTimeIndicators<>(this.greedoConfig.getBinCnt())));
//			});

//			this.utilities.newSummaryStatistics().personId2expectedUtilityChange.forEach((id, deltaUn0) -> {
//				this.trustRegionBasedSelector.add(id, deltaUn0,
//						hypotheticalSlotUsageIndicators.getOrDefault(id,
//								new SpaceTimeIndicators<>(this.greedoConfig.getBinCnt())),
//						this.lastOverwrittenRealizedSlotUsageIndicators.getOrDefault(id,
//								new SpaceTimeIndicators<>(this.greedoConfig.getBinCnt())));
//			});
//			replannerIds = this.trustRegionBasedSelector.selectReplanners(1.0 / sqrt(max(1.0, this.iteration())),
//					this.slotAnalyzer.getSlot2sizeView());

			replannerIds = this.popManager.selectReplanners();			

		} else {
			replannerIds = new LinkedHashSet<>();
			final Map<Id<Person>, Double> personId2DeltaUn0 = this.utilities
					.newSummaryStatistics().personId2expectedUtilityChange;
			final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(personId2DeltaUn0.keySet());
			Collections.shuffle(allPersonIdsShuffled);
			for (Id<Person> personId : allPersonIdsShuffled) {
				final boolean isReplanner = this.greedoConfig.getReplannerIdentifierRecipe().isReplanner(personId,
						personId2DeltaUn0.get(personId));
				if (isReplanner) {
					replannerIds.add(personId);
				}
			}
		}
		this.myMeanDeltaUn0.update(this.utilities.newSummaryStatistics().personId2expectedUtilityChange.values());

		this.slotUsageAnalyzer.setAnticipatedSlotUsages(hypotheticalSlotUsageIndicators, replannerIds);
//		this.slotAnalyzer.registerAnticipatedSlotUsages(hypotheticalSlotUsageIndicators);

		// this.previousAnticipatedSlotUsages = new
		// LinkedHashMap<>(hypotheticalSlotUsageIndicators);
//		this.previousCurrentSlotUsages = new LinkedHashMap<>(this.physicalSlotUsageListener.getIndicatorView());
//		this.previousReplannerIds = new LinkedHashSet<>(replannerIds);

		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
			if (person.getPlans().size() != 1) {
				throw new RuntimeException("Person " + person.getId() + " has " + person.getPlans().size() + " plans.");
			}

			/*
			 * 2021-02-03: Removed this again, we want to keep the replanning logic simple
			 * (a better-response, no matter what). So just reset the non-replanners.
			 * 
			 * 2019-07-20. What happens here is that non-replanners are re-set to their old
			 * plans. Fine. But replanners should have a chance to switch back to their
			 * previous plan in case it turns out that the new choice was bad. So the new
			 * plan needs to be ADDED as SELECTED to the choice set (then of size 2). Then
			 * comes the network loading, the chosen plan gets scored, and only then the
			 * worst of the two (both now based on physical network loadings) gets removed,
			 * and the better one enters the next better-response replanning round.
			 */
			if (!replannerIds.contains(person.getId())) {
				this.lastPhysicalPopulationState.set(person);
			}
//			if (replannerIds.contains(person.getId())) {
//				// Replanners select the new plan for execution and keep the old plan.
//				person.setSelectedPlan(person.getPlans().get(0)); // new
//				this.lastPhysicalPopulationState.add(person); // old
//			} else {
//				// Non-replanners return to the old plan.
//				this.lastPhysicalPopulationState.set(person); // old
//			}
		}

//		this.lastReplanningSummaryStatistics = replannerIdentifier.getSummaryStatistics(replannerIds,
//				this.ages.getAgesView());

		this.ages.update(replannerIds);
		this.physicalSlotUsageListener.updatePersonWeights(this.ages.getWeightsView());
	}

	// --------------- IMPLEMENTATION OF Provider<EventHandler> ---------------

	@Override
	public EventHandler get() {
		return this.physicalSlotUsageListener;
	}
}
