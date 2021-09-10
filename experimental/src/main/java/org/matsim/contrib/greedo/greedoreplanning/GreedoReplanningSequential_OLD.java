package org.matsim.contrib.greedo.greedoreplanning;

import static java.lang.Math.min;
import static java.lang.Math.round;

import java.io.File;
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
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.greedo.GreedoConfigGroup;
import org.matsim.contrib.greedo.Plans;
import org.matsim.contrib.greedo.PopulationSampleManager;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.listeners.SlotUsageListener;
import org.matsim.contrib.greedo.trustregion.Slot;
import org.matsim.contrib.ier.IERReplanningEngine;
import org.matsim.contrib.ier.emulator.AgentEmulator;
import org.matsim.contrib.ier.replannerselection.ReplannerSelector.IEREventHandlerProvider;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import floetteroed.utilities.SetUtils;
import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.BasicStatistics;
import floetteroed.utilities.statisticslogging.Statistic;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;
import utils.LinkTravelTimeCopy;

/**
 * @author Gunnar Flötteröd
 */
@Singleton
public final class GreedoReplanningSequential_OLD implements PlansReplanning, ReplanningListener, AfterMobsimListener {

	// -------------------- CONSTANTS --------------------

//	private final String deltaSFile = "deltaS.log";
//	private final String deltaZFile = "deltaZ.log";

	private final static Logger logger = Logger.getLogger(GreedoReplanningSequential.class);

	private final MatsimServices services;
	private final GreedoConfigGroup greedoConfig;

	private final StrategyManager strategyManager;
	private final Provider<ReplanningContext> replanningContextProvider;
	private final Provider<AgentEmulator> agentEmulatorProvider;

	private final StatisticsWriter<GreedoReplanningSequential> statsWriter;

	// -------------------- MEMBERS --------------------

	// private BasicStatistics initialTrustRegionEstimator = new BasicStatistics();
	private Integer callsToReplan = 0;

	private int travelTimeInnovationCnt = 0;
	private List<LinkTravelTimeCopy> travelTimes = new ArrayList<>();

	private int convergenceStatInnovationCnt = 0;

	// private Double randomCongestionChange = null;
	private Double trustRegion = null;

	private List<Double> gaps = new ArrayList<>();
	private Double gapTStat = null;
	private Double gapCV = null;

	private List<Double> minMemoryBackcastGaps = new ArrayList<>();
	private Double minMemoryBackcastGapTStat = null;
	private Double minMemoryBackcastGapCV = null;

	private List<Double> replanningRates = new ArrayList<>();
	private Double replanningRatesTStat = null;

	private List<Double> netvarStats = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	@Inject
	GreedoReplanningSequential_OLD(final StrategyManager strategyManager,
			final Provider<ReplanningContext> replanningContextProvider,
			final Provider<AgentEmulator> agentEmulatorProvider, final MatsimServices services) {

		this.greedoConfig = ConfigUtils.addOrGetModule(services.getConfig(), GreedoConfigGroup.class);
		this.strategyManager = strategyManager;
		this.replanningContextProvider = replanningContextProvider;
		this.agentEmulatorProvider = agentEmulatorProvider;
		this.services = services;

//		FileUtils.deleteQuietly(new File(this.deltaSFile));
//		try {
//			new File(this.deltaSFile).createNewFile();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		FileUtils.deleteQuietly(new File(this.deltaZFile));
//		try {
//			new File(this.deltaZFile).createNewFile();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		this.statsWriter = new StatisticsWriter<>(
				new File(services.getConfig().controler().getOutputDirectory(), "GreedoReplanning.log").toString(),
				false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "CallsToReplan";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.callsToReplan.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MemorizedTravelTimes";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return Integer.toString(data.travelTimes.size());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "RandomCongestionChange";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				// return data.randomCongestionChange == null ? "" :
				// data.randomCongestionChange.toString();
				return data.netvarStats.size() == 0 ? "" : data.netvarStats.get(data.netvarStats.size() - 1).toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "TrustRegion";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.trustRegion == null ? "" : data.trustRegion.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "ReplanningRate";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.replanningRates.size() == 0 ? ""
						: data.replanningRates.get(data.replanningRates.size() - 1).toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "ReplanningRateTStat";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.replanningRatesTStat == null ? "" : data.replanningRatesTStat.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "Gap";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.gaps.size() == 0 ? "" : data.gaps.get(data.gaps.size() - 1).toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "GapTStat";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.gapTStat == null ? "" : data.gapTStat.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "GapCV";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.gapCV == null ? "" : data.gapCV.toString();
			}
		});

		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MinMemoryBackcastGap";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.minMemoryBackcastGaps.size() == 0 ? ""
						: data.minMemoryBackcastGaps.get(data.minMemoryBackcastGaps.size() - 1).toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MinMemoryBackcapstGapTStat";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.minMemoryBackcastGapTStat == null ? "" : data.minMemoryBackcastGapTStat.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MinMemoryBackcastGapCV";
			}

			@Override
			public String value(GreedoReplanningSequential data) {
				return data.minMemoryBackcastGapCV == null ? "" : data.minMemoryBackcastGapCV.toString();
			}
		});

	}

	// -------------------- INTERNALS --------------------

	private <T> List<T> newShortenedToMemory(final List<T> original, final int totalCnt) {
		final int newSize = min(original.size(),
				min(this.greedoConfig.getMaxTravelTimeMemory(),
						this.greedoConfig.getMinTravelTimeMemory()
								+ (int) round((totalCnt - this.greedoConfig.getMinTravelTimeMemory())
										* this.greedoConfig.getRelTravelTimeMemory())));
		return original.subList(original.size() - newSize, original.size());
	}

	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> emulateAndReturnIndicators(final MatsimServices services,
			final TravelTime travelTimes) {
		final Map<Id<Person>, Double> personWeights = services.getScenario().getPopulation().getPersons().keySet()
				.stream().collect(Collectors.toMap(id -> id, id -> 1.0));
		final List<SlotUsageListener> emulatedSlotUsageListeners = new LinkedList<>();
		final IEREventHandlerProvider emulatedEventHandlerProvider = new IEREventHandlerProvider() {
			@Override
			public synchronized EventHandler get(final Set<Id<Person>> personIds) {
				final SlotUsageListener listener = new SlotUsageListener(greedoConfig.newTimeDiscretization(),
						personWeights, greedoConfig.getConcurrentLinkWeights(),
						greedoConfig.getConcurrentTransitVehicleWeights());
				listener.setReactToReset(false); // because many parallel calls per agent might occur
				emulatedSlotUsageListeners.add(listener);
				return listener;
			}
		};
		final IERReplanningEngine emulationEngine = new IERReplanningEngine(this.strategyManager,
				services.getScenario(), this.replanningContextProvider, this.agentEmulatorProvider,
				services.getConfig());
		try {
			emulationEngine.emulate(new LinkedHashSet<>(services.getScenario().getPopulation().getPersons().values()),
					services.getIterationNumber(), emulatedEventHandlerProvider, travelTimes);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> result = new LinkedHashMap<>();
		for (SlotUsageListener listener : emulatedSlotUsageListeners) {
			result.putAll(listener.getIndicatorView());
		}
		return result;
	}

	private void emulateAgainstAllTravelTimes(final List<Map<Id<Person>, Double>> personId2score,
			final List<Map<Id<Person>, SpaceTimeIndicators<Id<?>>>> personId2indicatorsPerReplication) {
		for (LinkTravelTimeCopy travelTime : this.travelTimes) {
			personId2indicatorsPerReplication.add(emulateAndReturnIndicators(this.services, travelTime));
			final Map<Id<Person>, Double> scores = new LinkedHashMap<>();
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				scores.put(person.getId(), person.getSelectedPlan().getScore());
			}
			personId2score.add(scores);
		}
	}

	// -------------------- AFTER MOBSIM LISTENER --------------------

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
//		if (GreedoConfigGroup.ReplannerIdentifierType.accelerate
//				.equals(this.greedoConfig.getReplannerIdentifierType())) {
		this.travelTimes.add(new LinkTravelTimeCopy(event.getServices()));
		this.travelTimes = this.newShortenedToMemory(this.travelTimes, ++this.travelTimeInnovationCnt);
//		} else {
//			this.travelTimes.clear();
//			this.travelTimes.add(new LinkTravelTimeCopy(event.getServices()));
//		}
	}

	// -------------------- REPLANNING LISTENER --------------------

	@Override
	public void notifyReplanning(final ReplanningEvent event) {

		this.callsToReplan++;
		// final int _R = this.travelTimes.size();
		final Set<Id<Person>> personIds = event.getServices().getScenario().getPopulation().getPersons().keySet();
		final Collection<? extends Person> persons = event.getServices().getScenario().getPopulation().getPersons()
				.values();

		/*
		 * (1) Extract old plans and compute new plans. Evaluate both old and new plans
		 * against all old replications.
		 */

		final Plans oldPlans = new Plans(event.getServices().getScenario().getPopulation());
		final List<Map<Id<Person>, Double>> personId2oldScore = new ArrayList<>();
		final List<Map<Id<Person>, SpaceTimeIndicators<Id<?>>>> personId2oldIndicatorsOverReplications = new ArrayList<>();
		this.emulateAgainstAllTravelTimes(personId2oldScore, personId2oldIndicatorsOverReplications);

		final Plans newPlans;
		{
			final IERReplanningEngine replanningEngine = new IERReplanningEngine(this.strategyManager,
					event.getServices().getScenario(), this.replanningContextProvider, this.agentEmulatorProvider,
					event.getServices().getConfig());
			replanningEngine.replan(event.getIteration(), travelTimes.get(travelTimes.size() - 1), null);
			final BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<>();
			for (Person person : persons) {
				person.setSelectedPlan(bestPlanSelector.selectPlan(person));
				PersonUtils.removeUnselectedPlans(person);
			}
			newPlans = new Plans(event.getServices().getScenario().getPopulation());
		}
		final List<Map<Id<Person>, SpaceTimeIndicators<Id<?>>>> personId2newIndicatorsOverReplications = new ArrayList<>();
		final List<Map<Id<Person>, Double>> personId2newScore = new ArrayList<>();
		this.emulateAgainstAllTravelTimes(personId2newScore, personId2newIndicatorsOverReplications);

		// >>> TMP >>>
		//
		// List<Double> gaps = new ArrayList<>();
		// String line = "";
		// for (int k = this.travelTimes.size() - 1; k >= 0; k--) {
		// // final double gap =
		// personId2newScore.get(k).values().stream().mapToDouble(s
		// // -> s).average().getAsDouble()
		// // - personId2oldScore.get(k).values().stream().mapToDouble(s ->
		// // s).average().getAsDouble();
		// final double gap =
		// this.personId2BRScores.get(k).values().stream().mapToDouble(s -> s).average()
		// .getAsDouble()
		// - personId2oldScore.get(k).values().stream().mapToDouble(s ->
		// s).average().getAsDouble();
		// line += gap + "\t";
		// gaps.add(gap);
		// }
		// try {
		// FileUtils.write(new File(this.deltaSFile), line.replace('.', ',') + "\n",
		// true);
		// } catch (IOException e) {
		// throw new RuntimeException(e);
		// }
		//
		// <<< TMP <<<

		/*
		 * (3) Compute gap-related statistics.
		 */

		final List<Double> backcastGaps = new ArrayList<>(this.travelTimes.size());
		for (int k = 0; k < this.travelTimes.size(); k++) {
			backcastGaps.add(personId2newScore.get(k).values().stream().mapToDouble(s -> s).average().getAsDouble()
					- personId2oldScore.get(k).values().stream().mapToDouble(s -> s).average().getAsDouble());
		}

		final List<Tuple<Id<Person>, Double>> personIdAndGap = new ArrayList<>(personIds.size());
		for (Id<Person> personId : personIds) {
			double newScoreSum = 0.0;
			double oldScoreSum = 0.0;
			final int k0 = Math.max(0, this.travelTimes.size() - this.greedoConfig.getMaxEvaluatedGaps());
			final int k1 = this.travelTimes.size() - this.greedoConfig.getSkipInitialGaps();
			for (int k = k0; k < k1; k++) {
				final double newScore = personId2newScore.get(k).get(personId);
				final double oldScore = personId2oldScore.get(k).get(personId);
				newScoreSum += newScore;
				oldScoreSum += oldScore;
			}
			personIdAndGap.add(new Tuple<>(personId, (newScoreSum - oldScoreSum) / (k1 - k0)));
		}
		Collections.shuffle(personIdAndGap, MatsimRandom.getRandom()); // to break symmetries
		Collections.sort(personIdAndGap, new Comparator<Tuple<Id<Person>, Double>>() {
			@Override
			public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
				return Double.compare(tuple2.getB(), tuple1.getB()); // largest gap first
			}
		});

		/*
		 * (4) Evaluation of random slot variability.
		 */

		if (this.travelTimes.size() >= 2) {

			// Evaluating *old* slot usages given current and previous travel times.
			final Map<Slot, Integer> slot2previousUsage = new LinkedHashMap<>();
			final Map<Slot, Integer> slot2currentUsage = new LinkedHashMap<>();
			for (Id<Person> personId : personIds) {
				for (Slot slot : PopulationSampleManager
						.createSlots(personId2oldIndicatorsOverReplications.get(this.travelTimes.size() - 2).get(personId))) {
					slot2previousUsage.put(slot, 1 + slot2previousUsage.getOrDefault(slot, 0));
				}
				for (Slot slot : PopulationSampleManager
						.createSlots(personId2oldIndicatorsOverReplications.get(this.travelTimes.size() - 1).get(personId))) {
					slot2currentUsage.put(slot, 1 + slot2currentUsage.getOrDefault(slot, 0));
				}
			}

			double congestionChange = 0.0;
			for (Slot slot : SetUtils.union(slot2previousUsage.keySet(), slot2currentUsage.keySet())) {
				congestionChange = Math.max(congestionChange,
						Math.abs(slot2currentUsage.getOrDefault(slot, 0) - slot2previousUsage.getOrDefault(slot, 0)));
			}
			this.netvarStats.add(congestionChange);
			if (this.trustRegion != null) {
				this.netvarStats = this.newShortenedToMemory(this.netvarStats, this.convergenceStatInnovationCnt);
			}
			// if (this.trustRegion == null) {
			// 	this.initialTrustRegionEstimator.add(this.randomCongestionChange);
			// }
		}

		/*
		 * (5) Identify replanners.
		 */

		final Set<Id<Person>> replannerIds = new LinkedHashSet<>();

		if (GreedoConfigGroup.ReplannerIdentifierType.MSA.equals(this.greedoConfig.getReplannerIdentifierType())) {

			// >>>>>>>>>>>>>>>>>>>> MSA >>>>>>>>>>>>>>>>>>>>

			for (Id<Person> personId : personIds) {
				if (MatsimRandom.getRandom().nextDouble() < this.greedoConfig
						.getMSAReplanningRate(this.callsToReplan - 1, false)) {
					replannerIds.add(personId);
				}
			}

			// <<<<<<<<<<<<<<<<<<<< MSA <<<<<<<<<<<<<<<<<<<<

		} else if (GreedoConfigGroup.ReplannerIdentifierType.Sbayti2007
				.equals(this.greedoConfig.getReplannerIdentifierType())) {

			// >>>>>>>>>>>>>>>>>>>> Sbayti >>>>>>>>>>>>>>>>>>>>

			final int replannerCnt = (int) (this.greedoConfig.getMSAReplanningRate(this.callsToReplan - 1, false)
					* personIdAndGap.size());
			for (int i = 0; i < replannerCnt; i++) {
				replannerIds.add(personIdAndGap.get(i).getA());
			}

			// <<<<<<<<<<<<<<<<<<<< Sbayti <<<<<<<<<<<<<<<<<<<<

		} else if (GreedoConfigGroup.ReplannerIdentifierType.accelerate
				.equals(this.greedoConfig.getReplannerIdentifierType())) {

			// >>>>>>>>>>>>>>>>>>>> GREEDO >>>>>>>>>>>>>>>>>>>>

			if (this.travelTimes.size() < this.greedoConfig.getMinTravelTimeMemory()) {

				if (this.trustRegion != null) {
					throw new RuntimeException();
				}

				for (Id<Person> personId : personIds) {
					if (MatsimRandom.getRandom().nextDouble() < this.greedoConfig.getInitialReplanProba()) {
						replannerIds.add(personId);
					}
				}

			} else {
				
				// >>>>> MOVED HERE >>>>>
				
				{
					final double gap = personId2newScore.get(this.travelTimes.size() - 1).values().stream().mapToDouble(s -> s).average()
							.getAsDouble()
							- personId2oldScore.get(this.travelTimes.size() - 1).values().stream().mapToDouble(s -> s).average().getAsDouble();
					if (this.trustRegion != null) {
						this.gaps.add(gap);
						this.gaps = this.newShortenedToMemory(this.gaps, ++this.convergenceStatInnovationCnt);
					}
				}
				
				// <<<<< MOVED HERE <<<<<

//				if (this.trustRegion == null) {
//					this.trustRegion = this.initialTrustRegionEstimator.getAvg();
//					this.initialTrustRegionEstimator = null;
//				}

				{
					double backcastGap = 0.0;
					for (int k = personId2newScore.size()
							- this.greedoConfig.getMinTravelTimeMemory(); k < personId2newScore.size(); k++) {
						backcastGap += (personId2newScore.get(k).values().stream().mapToDouble(s -> s).average()
								.getAsDouble()
								- personId2oldScore.get(k).values().stream().mapToDouble(s -> s).average()
										.getAsDouble());
					}
					this.minMemoryBackcastGaps.add(backcastGap / this.greedoConfig.getMinTravelTimeMemory());
				}

				final int[] maxChangeOverReplications = new int[this.travelTimes.size()];
				final List<Map<Slot, Integer>> slot2changeOverReplications = new ArrayList<>(this.travelTimes.size());
				for (int r = 0; r < this.travelTimes.size(); r++) {
					slot2changeOverReplications.add(new LinkedHashMap<>());
				}

				for (Tuple<Id<Person>, Double> candidateTuple : personIdAndGap) {
					final Id<Person> candidateId = candidateTuple.getA();
					final double candidateGap = candidateTuple.getB();
					if (this.greedoConfig.getReplanNegativeGaps() || (candidateGap > 0)) {

						final List<Set<Slot>> changedSlotsOverReplications = new ArrayList<>(this.travelTimes.size());
						for (int r = 0; r < this.travelTimes.size(); r++) {
							if (r >= this.travelTimes.size() - this.greedoConfig.getMaxEvaluatedNetstates()) {
								final Set<Slot> changedSlots = new LinkedHashSet<>(PopulationSampleManager
										.createSlots(personId2newIndicatorsOverReplications.get(r).get(candidateId)));
								if (this.greedoConfig.getPenalizeDepartures()) {
									changedSlots.addAll(PopulationSampleManager.createSlots(
											personId2oldIndicatorsOverReplications.get(r).get(candidateId)));
								}
								changedSlotsOverReplications.add(changedSlots);
							} else {
								changedSlotsOverReplications.add(new LinkedHashSet<>(0));
							}
						}

						final double anticipatedNewOverallChange;
						{
							double changeSum = 0.0;
							for (int r = 0; r < this.travelTimes.size(); r++) {
								double change = maxChangeOverReplications[r];
								for (Slot slot : changedSlotsOverReplications.get(r)) {
									change = Math.max(change,
											1 + slot2changeOverReplications.get(r).getOrDefault(slot, 0));
								}
								changeSum += change;
							}
							// anticipatedNewOverallChange = changeSum / _R;
							anticipatedNewOverallChange = changeSum
									/ Math.min(this.travelTimes.size(), this.greedoConfig.getMaxEvaluatedNetstates());
						}

						if (anticipatedNewOverallChange <= this.trustRegion) {
							for (int r = 0; r < this.travelTimes.size(); r++) {
								for (Slot slot : changedSlotsOverReplications.get(r)) {
									int newSlotChange = 1 + slot2changeOverReplications.get(r).getOrDefault(slot, 0);
									slot2changeOverReplications.get(r).put(slot, newSlotChange);
									maxChangeOverReplications[r] = Math.max(maxChangeOverReplications[r],
											newSlotChange);
								}
							}
							replannerIds.add(candidateId);
						}
					}
				}

//				line = "";
//				for (int k = maxChangeOverReplications.length - 1; k >= 0; k--) {
//					line += maxChangeOverReplications[k] + "\t";
//				}
//				try {
//					FileUtils.write(new File(this.deltaZFile), line.replace('.', ',') + "\n", true);
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}

			}

			// <<<<<<<<<<<<<<<<<<<< GREEDO <<<<<<<<<<<<<<<<<<<<

		} else {

			throw new RuntimeException(
					"Unsupported replanning recipe: " + this.greedoConfig.getReplannerIdentifierType());

		}

		for (Person person : persons) {
			if (replannerIds.contains(person.getId())) {
				newPlans.set(person);
			} else {
				oldPlans.set(person);
			}
		}
		// this.replanningRate = ((double) replannerIds.size()) / personIds.size();
		this.replanningRates.add(((double) replannerIds.size()) / personIds.size());
		this.replanningRates = this.newShortenedToMemory(this.replanningRates, this.convergenceStatInnovationCnt);

		if (this.gaps.size() >= this.greedoConfig.getMinTravelTimeMemory()) {

			final DickeyFullerTest gapTest = new DickeyFullerTest(this.gaps,
					this.greedoConfig.getDickeyFullerThreshold());
			this.gapTStat = gapTest.tStatistic;
			this.gapCV = Math.sqrt(gapTest.varianceOfMean) / gapTest.mean;

			final DickeyFullerTest backcastGapTest = new DickeyFullerTest(this.minMemoryBackcastGaps,
					this.greedoConfig.getDickeyFullerThreshold());
			this.minMemoryBackcastGapTStat = backcastGapTest.tStatistic;
			this.minMemoryBackcastGapCV = Math.sqrt(backcastGapTest.varianceOfMean) / backcastGapTest.mean;

			final DickeyFullerTest replanningRatesTest = new DickeyFullerTest(this.replanningRates,
					this.greedoConfig.getDickeyFullerThreshold());
			this.replanningRatesTStat = replanningRatesTest.tStatistic;

			this.statsWriter.writeToFile(this);

			if ((this.gaps.size() >= this.greedoConfig.getMaxTravelTimeMemory())
					|| (gapTest.getStationary() && replanningRatesTest.getStationary())) {

				this.travelTimes = new LinkedList<>(this.travelTimes.subList(
						this.travelTimes.size() - (this.greedoConfig.getMinTravelTimeMemory() - 1),
						this.travelTimes.size()));
				this.travelTimeInnovationCnt = this.travelTimes.size();

				this.gaps.clear();
				this.minMemoryBackcastGaps.clear();
				this.replanningRates.clear();
				this.convergenceStatInnovationCnt = 0;

				this.gapTStat = null;
				this.gapCV = null;
				this.minMemoryBackcastGapTStat = null;
				this.minMemoryBackcastGapCV = null;
				this.replanningRatesTStat = null;
				this.trustRegion *= this.greedoConfig.getTrustRegionReductionFactor();
			}

		} else {
			this.statsWriter.writeToFile(this);
		}
	}
}
