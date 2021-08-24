package org.matsim.contrib.greedo.greedoreplanning;

import static java.lang.Math.max;
import static java.util.Collections.shuffle;
import static java.util.Collections.sort;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.matsim.core.config.Config;
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

import floetteroed.utilities.Tuple;
import floetteroed.utilities.statisticslogging.Statistic;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;
import utils.LinkTravelTimeCopy;

/**
 * @author shoerl
 * @author Gunnar Flötteröd
 */
@Singleton
public final class GreedoReplanningMixed implements PlansReplanning, ReplanningListener, AfterMobsimListener {

	// -------------------- CONSTANTS --------------------

	private final static Logger logger = Logger.getLogger(GreedoReplanningMixed.class);

	private final GreedoConfigGroup greedoConfig;
	private final GreedoParameterManager parameterManager;

	private final Provider<ReplanningContext> replanningContextProvider;
	private final StrategyManager strategyManager;
	private final Provider<AgentEmulator> agentEmulatorProvider;

	private final StatisticsWriter<GreedoReplanningMixed> statsWriter;
	private Double meanGap = null;
	private Double replanningRate = null;

	private int callsToReplanning = 0;
	private int realizedReplans = 0;

	// -------------------- MEMBERS --------------------

	private LinkedList<LinkTravelTimeCopy> allTravelTimes = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	@Inject
	GreedoReplanningMixed(final Config config, final StrategyManager strategyManager,
			final Provider<ReplanningContext> replanningContextProvider,
			final Provider<AgentEmulator> agentEmulatorProvider) {
//		this.greedoConfig = ConfigUtils.addOrGetModule(config, GreedoConfigGroup.class);
//		this._K = this.greedoConfig.getIterationReplications();

		this.greedoConfig = ConfigUtils.addOrGetModule(config, GreedoConfigGroup.class);
		this.parameterManager = new GreedoParameterManager(this.greedoConfig.getIterationReplications(),
				this.greedoConfig.getTrustRegion(), this.greedoConfig.getTransientIterations(),
				this.greedoConfig.getStationaryIterations(), tr -> Math.max(1, tr / 2), repl -> repl * 2);

		this.strategyManager = strategyManager;
		this.replanningContextProvider = replanningContextProvider;
		this.agentEmulatorProvider = agentEmulatorProvider;

		this.statsWriter = new StatisticsWriter<>(
				new File(config.controler().getOutputDirectory(), "GreedoReplanning.log").toString(), false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "Iterations";
			}

			@Override
			public String value(GreedoReplanningMixed data) {
				return Integer.toString(data.callsToReplanning);
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MeanGap";
			}

			@Override
			public String value(GreedoReplanningMixed data) {
				return data.meanGap == null ? "" : data.meanGap.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "ReplanningRate";
			}

			@Override
			public String value(GreedoReplanningMixed data) {
				return data.replanningRate == null ? "" : data.replanningRate.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "BetweenIterationVariance";
			}

			@Override
			public String value(GreedoReplanningMixed data) {
				return data.parameterManager.getLastStationaryBetweenIterationVarianceOfMean() == null ? ""
						: data.parameterManager.getLastStationaryBetweenIterationVarianceOfMean().toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "WithinIterationVariance";
			}

			@Override
			public String value(GreedoReplanningMixed data) {
				return data.parameterManager.getLastStationaryWithinIterationVarianceOfMean() == null ? ""
						: data.parameterManager.getLastStationaryWithinIterationVarianceOfMean().toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "TrustRegion";
			}

			@Override
			public String value(GreedoReplanningMixed data) {
				return Integer.toString(data.parameterManager.getTrustRegion());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "Replications";
			}

			@Override
			public String value(GreedoReplanningMixed data) {
				return Integer.toString(data.parameterManager.getReplications());
			}
		});

	}

	// -------------------- AFTER MOBSIM LISTENER --------------------

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		while (this.allTravelTimes.size() + 1 > this.parameterManager.getReplications()) {
			this.allTravelTimes.removeLast();
		}
		this.allTravelTimes.addFirst(new LinkTravelTimeCopy(event.getServices()));
	}

	// -------------------- INTERNALS --------------------

	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> emulateAndReturnSlotUsages(final MatsimServices services,
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

	// -------------------- REPLANNING LISTENER --------------------

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		this.callsToReplanning++;
		final int _R = this.parameterManager.getReplications();

		if ((this.allTravelTimes.size() < _R) || (event.getIteration() % _R != 0)) {
			return;
		}

		// for (int i = 0; i < _K; i++) {
		// logger.info("travel time sum (" + i + ")= " +
		// this.allTravelTimes.get(i).sum());
		// }

		// >>>>>>>>>>>>>>>>>>>> NEW >>>>>>>>>>>>>>>>>>>>

		this.realizedReplans++;
		final Collection<? extends Person> persons = event.getServices().getScenario().getPopulation().getPersons()
				.values();
		final Set<Id<Person>> personIds = event.getServices().getScenario().getPopulation().getPersons().keySet();

		/*
		 * (1) Evaluate old plans against all replications.
		 */
		final Plans oldPlans = new Plans(event.getServices().getScenario().getPopulation());
		final List<Map<Id<Person>, Double>> personId2oldScore = new ArrayList<>();
		final List<Map<Id<Person>, SpaceTimeIndicators<Id<?>>>> personId2oldSlots = new ArrayList<>();
		for (int r = 0; r < _R; r++) {
			personId2oldSlots.add(emulateAndReturnSlotUsages(event.getServices(), this.allTravelTimes.get(r)));
			final Map<Id<Person>, Double> scores = new LinkedHashMap<>();
			for (Person person : persons) {
				scores.put(person.getId(), person.getSelectedPlan().getScore());
			}
			personId2oldScore.add(scores);
		}

		/*
		 * (2) Compute best responses against all replications.
		 */
		final List<Plans> newPlans = new ArrayList<>();
		final List<Map<Id<Person>, SpaceTimeIndicators<Id<?>>>> personId2newSlots = new ArrayList<>();
		for (int r = 0; r < _R; r++) {
			oldPlans.set(event.getServices().getScenario().getPopulation());
			final IERReplanningEngine replanningEngine = new IERReplanningEngine(strategyManager,
					event.getServices().getScenario(), replanningContextProvider, agentEmulatorProvider,
					event.getServices().getConfig());
			replanningEngine.replan(this.realizedReplans, this.allTravelTimes.get(r), null);
			final BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<>();
			for (Person person : persons) {
				person.setSelectedPlan(bestPlanSelector.selectPlan(person));
				PersonUtils.removeUnselectedPlans(person);
			}
			newPlans.add(new Plans(event.getServices().getScenario().getPopulation()));
			personId2newSlots.add(emulateAndReturnSlotUsages(event.getServices(), this.allTravelTimes.get(r)));
		}

		/*
		 * (3) Compute gaps.
		 */
		final Map<Id<Person>, Double> personId2meanGap = new LinkedHashMap<>();
		final double[] replicationMeanGap = new double[_R];
		for (Id<Person> personId : personIds) {
			double newScoreSum = 0.0;
			double oldScoreSum = 0.0;
			for (int r = 0; r < _R; r++) {
				final double newScore = newPlans.get(r).getSelectedPlan(personId).getScore();
				final double oldScore = personId2oldScore.get(r).get(personId);
				newScoreSum += newScore;
				oldScoreSum += oldScore;
				replicationMeanGap[r] += (newScore - oldScore);
			}
			personId2meanGap.put(personId, (newScoreSum - oldScoreSum) / _R);
		}
		for (int r = 0; r < _R; r++) {
			replicationMeanGap[r] /= personIds.size();
		}

		/*
		 * (4) Sort re-planning candidates according to gaps (largest gaps first).
		 */
		final List<Tuple<Id<Person>, Double>> candidateIdsAndGaps = new ArrayList<>();
		for (Map.Entry<Id<Person>, Double> entry : personId2meanGap.entrySet()) {
			candidateIdsAndGaps.add(new Tuple<>(entry.getKey(), entry.getValue()));
		}
		shuffle(candidateIdsAndGaps, MatsimRandom.getRandom()); // to break symmetries
		sort(candidateIdsAndGaps, new Comparator<Tuple<Id<Person>, Double>>() {
			@Override
			public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
				return Double.compare(tuple2.getB(), tuple1.getB()); // largest gap first
			}
		});

		/*
		 * (5) Prepare for tracking slot usage changes per replication.
		 */
		final List<Map<Slot, Integer>> slot2changes = new ArrayList<>();
		final int[] maxSlotChanges = new int[_R];
		for (int r = 0; r < _R; r++) {
			slot2changes.add(new LinkedHashMap<>());
		}

		/*
		 * (6) Greedy heuristic for the re-planner identification problem.
		 */
		// final Set<Id<Person>> tmpReplannerIds = new LinkedHashSet<>();
		// double tmpMeanGapReduction = 0.0;
		final Map<Id<Person>, Integer> replannerId2replanningReplication = new LinkedHashMap<>();

		for (Tuple<Id<Person>, Double> candidateTuple : candidateIdsAndGaps) {
			final Id<Person> candidateId = candidateTuple.getA();
			final double candidateMeanGap = candidateTuple.getB();

			final List<Tuple<Integer, Double>> replicationAndGap = new ArrayList<>();
			for (int r = 0; r < _R; r++) {
				replicationAndGap.add(new Tuple<>(r, newPlans.get(r).getSelectedPlan(candidateId).getScore()
						- personId2oldScore.get(r).get(candidateId)));
			}
			shuffle(replicationAndGap, MatsimRandom.getRandom()); // to break symmetries
			sort(replicationAndGap, new Comparator<Tuple<Integer, Double>>() {
				@Override
				public int compare(Tuple<Integer, Double> tuple1, Tuple<Integer, Double> tuple2) {
					return Double.compare(tuple2.getB(), tuple1.getB()); // largest gap first
				}
			});

			Integer realizedReplanningReplication = null;
			for (int i = 0; (i < replicationAndGap.size()) && (realizedReplanningReplication == null); i++) {
				final int replanningReplication = replicationAndGap.get(i).getA();
				final double gap = replicationAndGap.get(i).getB();

				// Anticipate slot usage change from re-planning.
				final List<List<Slot>> newSlots = new ArrayList<>();
				double newMaxChangeSum = 0.0;
				for (int evaluationReplication = 0; evaluationReplication < _R; evaluationReplication++) {
					double maxChange = maxSlotChanges[evaluationReplication];
					newSlots.add(PopulationSampleManager
							.createSlots(personId2newSlots.get(evaluationReplication).get(candidateId)));
					for (Slot slot : newSlots.get(evaluationReplication)) {
						maxChange = max(maxChange, 1 + slot2changes.get(evaluationReplication).getOrDefault(slot, 0));
					}
					newMaxChangeSum += maxChange;
				}
				final double avgMaxChange = newMaxChangeSum / _R;

				// If there is space, re-plan.
				if (avgMaxChange <= parameterManager.getTrustRegion()) {
					for (int evaluationReplication = 0; evaluationReplication < _R; evaluationReplication++) {
						for (Slot slot : newSlots.get(evaluationReplication)) {
							int newSlotChange = 1 + slot2changes.get(evaluationReplication).getOrDefault(slot, 0);
							slot2changes.get(evaluationReplication).put(slot, newSlotChange);
							maxSlotChanges[evaluationReplication] = max(maxSlotChanges[evaluationReplication],
									newSlotChange);
						}
					}
					realizedReplanningReplication = replanningReplication;
				}
			}

			if (realizedReplanningReplication != null) {
				replannerId2replanningReplication.put(candidateId, realizedReplanningReplication);
			}
		}

		for (Person person : persons) {
			Integer replanningReplication = replannerId2replanningReplication.get(person.getId());
			if (replanningReplication != null) {
				newPlans.get(replanningReplication).set(person);
			} else {
				oldPlans.set(person);
			}
		}

		this.parameterManager.registerGaps(replicationMeanGap);
		this.meanGap = Arrays.stream(replicationMeanGap).average().getAsDouble();
		this.replanningRate = ((double) replannerId2replanningReplication.size()) / persons.size();
		if ((this.callsToReplanning == 1) || this.parameterManager.stationarityDetected()) {
			this.statsWriter.writeToFile(this);
		}
	}
}
