package org.matsim.contrib.greedo.greedoreplanning;

import static java.lang.Math.max;
import static java.util.Collections.shuffle;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableSet;

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
public final class GreedoReplanning implements PlansReplanning, ReplanningListener, AfterMobsimListener {

	// -------------------- CONSTANTS --------------------

	private final static Logger logger = Logger.getLogger(GreedoReplanning.class);

	private final GreedoConfigGroup greedoConfig;
	private final GreedoParameterManager parameterManager;

	private final Provider<ReplanningContext> replanningContextProvider;
	private final StrategyManager strategyManager;
	private final Provider<AgentEmulator> agentEmulatorProvider;

	private final StatisticsWriter<GreedoReplanning> statsWriter;
	private Double meanGap = null;
	private Double replanningRate = null;

	private int callsToReplanning = 0;

	// -------------------- MEMBERS --------------------

	private LinkedList<LinkTravelTimeCopy> allTravelTimes = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	@Inject
	GreedoReplanning(final Config config, final StrategyManager strategyManager,
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
			public String value(GreedoReplanning data) {
				return Integer.toString(data.callsToReplanning);
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "MeanGap";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.meanGap == null ? "" : data.meanGap.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "ReplanningRate";
			}

			@Override
			public String value(GreedoReplanning data) {
				return data.replanningRate == null ? "" : data.replanningRate.toString();
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "BetweenIterationVariance";
			}

			@Override
			public String value(GreedoReplanning data) {
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
			public String value(GreedoReplanning data) {
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
			public String value(GreedoReplanning data) {
				return Integer.toString(data.parameterManager.getTrustRegion());
			}
		});
		this.statsWriter.addSearchStatistic(new Statistic<>() {
			@Override
			public String label() {
				return "Replications";
			}

			@Override
			public String value(GreedoReplanning data) {
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

		if ((this.allTravelTimes.size() < this.parameterManager.getReplications())
				|| (event.getIteration() % this.parameterManager.getReplications() != 0)) {
			return;
		}

		// for (int i = 0; i < _K; i++) {
		// logger.info("travel time sum (" + i + ")= " +
		// this.allTravelTimes.get(i).sum());
		// }

		final Plans oldPlans = new Plans(event.getServices().getScenario().getPopulation());
		double bestScoreImprovement = Double.NEGATIVE_INFINITY;
		ReplannerIdentifier bestReplannerIdentifier = null;
		final double[] gaps = new double[this.parameterManager.getReplications()];
		for (int replanningReplication = 0; replanningReplication < this.parameterManager
				.getReplications(); replanningReplication++) {
			final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(oldPlans, replanningReplication,
					event.getServices());
			gaps[replanningReplication] = replannerIdentifier.gaps[replanningReplication];
			if (replannerIdentifier.meanGapReduction > bestScoreImprovement) {
				bestReplannerIdentifier = replannerIdentifier;
			}
		}

		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			if (bestReplannerIdentifier.replannerIds.contains(person.getId())) {
				bestReplannerIdentifier.newPlans.set(person);
			} else {
				oldPlans.set(person);
			}
		}

		this.parameterManager.registerGaps(gaps);
		this.meanGap = Arrays.stream(gaps).average().getAsDouble();

		this.replanningRate = ((double) bestReplannerIdentifier.replannerIds.size())
				/ event.getServices().getScenario().getPopulation().getPersons().size();
		if ((this.callsToReplanning == 1) || this.parameterManager.stationarityDetected()) {
			this.statsWriter.writeToFile(this);
		}
	}

	// -------------------- INNER CLASS --------------------

	private class ReplannerIdentifier {

		private final int replanningReplication;
		private final Plans newPlans; // BR against travel times with re-planning replication index
		private final double meanGap;

		private final double[] gaps;

		private final Set<Id<Person>> replannerIds;
		private final double meanGapReduction; // evaluation over all other replications

		private ReplannerIdentifier(final Plans oldPlans, final int replanningReplication,
				final MatsimServices services) {

			this.replanningReplication = replanningReplication;
			final Set<Id<Person>> personIds = services.getScenario().getPopulation().getPersons().keySet();
			final Collection<? extends Person> persons = services.getScenario().getPopulation().getPersons().values();

			/*
			 * (1) Compute statistics over relevant replications, do book-keeping.
			 */

			// Set the (unique) old plans and extract old scores.
			oldPlans.set(services.getScenario().getPopulation());
			final Map<Id<Person>, Double> personId2oldMeanScore = new LinkedHashMap<>();
			this.gaps = new double[parameterManager.getReplications()];
			for (int evaluationReplication = 0; evaluationReplication < parameterManager
					.getReplications(); evaluationReplication++) {
				emulateAndReturnSlotUsages(services, allTravelTimes.get(evaluationReplication));
				for (Person person : persons) {
					personId2oldMeanScore.put(person.getId(), personId2oldMeanScore.getOrDefault(person.getId(), 0.0)
							+ person.getSelectedPlan().getScore() / parameterManager.getReplications());
					this.gaps[evaluationReplication] -= person.getSelectedPlan().getScore();
				}
				// personId2oldScore.add(extractScores(services.getScenario().getPopulation()));
				// logger.info("old mean score (" + replanningReplication + "/" +
				// evaluationReplication + ") = "
				// +
				// personId2oldScore.get(evaluationReplication).values().stream().mapToDouble(s
				// -> s).average()
				// .getAsDouble());
			}

			// Compute and extract to-plans.
			final IERReplanningEngine replanningEngine = new IERReplanningEngine(strategyManager,
					services.getScenario(), replanningContextProvider, agentEmulatorProvider, services.getConfig());
			replanningEngine.replan(services.getIterationNumber(), allTravelTimes.get(replanningReplication), null);
			final BestPlanSelector<Plan, Person> bestPlanSelector = new BestPlanSelector<>();
			for (Person person : persons) {
				person.setSelectedPlan(bestPlanSelector.selectPlan(person));
				PersonUtils.removeUnselectedPlans(person);
			}
			this.newPlans = new Plans(services.getScenario().getPopulation());

//			for (Person person : services.getScenario().getPopulation().getPersons().values()) {
//				if (person.getPlans().size() > 1) {
//					logger.info("person " + person + " has " + person.getPlans().size() + " plans.");
//					for (Plan plan : person.getPlans()) {
//						logger.info("  plan with score " + plan.getScore() + ", selected = "
//								+ (plan == person.getSelectedPlan()));
//					}
//				}
//			}

			// Evaluate to-states and extract scores.
			final List<Map<Id<Person>, SpaceTimeIndicators<Id<?>>>> personId2newSlots = new ArrayList<>();
			// final List<Map<Id<Person>, Double>> personId2newScore = new ArrayList<>(_K);
			final Map<Id<Person>, Double> personId2newMeanScore = new LinkedHashMap<>();
			for (int evaluationReplication = 0; evaluationReplication < parameterManager
					.getReplications(); evaluationReplication++) {
				personId2newSlots.add(emulateAndReturnSlotUsages(services, allTravelTimes.get(evaluationReplication)));
				for (Person person : persons) {
					this.gaps[evaluationReplication] += person.getSelectedPlan().getScore();
				}
				if (evaluationReplication != replanningReplication) {
					for (Person person : persons) {
						personId2newMeanScore.put(person.getId(),
								personId2newMeanScore.getOrDefault(person.getId(), 0.0)
										+ person.getSelectedPlan().getScore()
												/ (parameterManager.getReplications() - 1.0));
					}
				}
				// personId2newScore.add(extractScores(services.getScenario().getPopulation()));
				// logger.info("new mean score (" + replanningReplication + "/" +
				// evaluationReplication + ") = "
				// +
				// personId2newScore.get(evaluationReplication).values().stream().mapToDouble(s
				// -> s).average()
				// .getAsDouble());
			}
			for (int r = 0; r < this.gaps.length; r++) {
				this.gaps[r] /= persons.size();
			}

			// For book-keeping only.
			this.meanGap = personId2newMeanScore.values().stream().mapToDouble(s -> s).average().getAsDouble()
					- personId2oldMeanScore.values().stream().mapToDouble(s -> s).average().getAsDouble();

			/*
			 * (2) TR-based re-planner selection.
			 * 
			 * Use the BR-plans against replication i and evaluate against replications j.
			 */

			// Sort re-planning candidates according to gap (largest gap first).
			final List<Tuple<Id<Person>, Double>> candidateIdsAndGaps = new ArrayList<>();
			for (Id<Person> candidateId : personIds) {
				final double gap = personId2newMeanScore.get(candidateId) - personId2oldMeanScore.get(candidateId);
				candidateIdsAndGaps.add(new Tuple<>(candidateId, gap));
			}
			shuffle(candidateIdsAndGaps, MatsimRandom.getRandom()); // to break symmetries
			sort(candidateIdsAndGaps, new Comparator<Tuple<Id<Person>, Double>>() {
				@Override
				public int compare(Tuple<Id<Person>, Double> tuple1, Tuple<Id<Person>, Double> tuple2) {
					return Double.compare(tuple2.getB(), tuple1.getB()); // largest gap first
				}
			});

			// Prepare for checking change slot usage change norms individually per
			// evaluation replication.
			final List<Map<Slot, Integer>> slot2changes = new ArrayList<>();
			final List<Integer> maxSlotChanges = new ArrayList<>();
			for (int evaluationReplication = 0; evaluationReplication < parameterManager
					.getReplications(); evaluationReplication++) {
				slot2changes.add(new LinkedHashMap<>());
				maxSlotChanges.add(0);
			}

			// Greedy heuristic for the re-planner identification problem.

			final Set<Id<Person>> tmpReplannerIds = new LinkedHashSet<>();
			double tmpMeanGapReduction = 0.0;

			for (Tuple<Id<Person>, Double> candidateTuple : candidateIdsAndGaps) {
				final Id<Person> candidateId = candidateTuple.getA();
				final double candidateGap = candidateTuple.getB();

				// Average (over replications) slot usage change norm if candidate replans?
				double newMaxChangeSum = 0.0;
				for (int evaluationReplication = 0; evaluationReplication < parameterManager
						.getReplications(); evaluationReplication++) {
					if (evaluationReplication != replanningReplication) {
						double maxChange = maxSlotChanges.get(evaluationReplication);
						final List<Slot> newSlots = PopulationSampleManager
								.createSlots(personId2newSlots.get(evaluationReplication).get(candidateId));
						for (Slot slot : newSlots) {
							maxChange = max(maxChange,
									1 + slot2changes.get(evaluationReplication).getOrDefault(slot, 0));
						}
						newMaxChangeSum += maxChange;
					}
				}
				final double avgMaxChange = newMaxChangeSum / (parameterManager.getReplications() - 1.0);

				// If there is space, re-plan.
				if (avgMaxChange <= parameterManager.getTrustRegion()) {
					for (int evaluationReplication = 0; evaluationReplication < parameterManager
							.getReplications(); evaluationReplication++) {
						if (evaluationReplication != replanningReplication) {
							final List<Slot> newSlots = PopulationSampleManager
									.createSlots(personId2newSlots.get(evaluationReplication).get(candidateId));
							for (Slot slot : newSlots) {
								int newSlotChange = 1 + slot2changes.get(evaluationReplication).getOrDefault(slot, 0);
								slot2changes.get(evaluationReplication).put(slot, newSlotChange);
								maxSlotChanges.set(evaluationReplication,
										Math.max(maxSlotChanges.get(evaluationReplication), newSlotChange));
							}
						}
					}
					tmpReplannerIds.add(candidateId);
					tmpMeanGapReduction += candidateGap;
					// for (int evaluationReplication = 0; evaluationReplication < _K;
					// evaluationReplication++) {
					// if (evaluationReplication != replanningReplication) {
					// tmpMeanGapReduction +=
					// (personId2newScore.get(evaluationReplication).get(candidateId)
					// - personId2oldScore.get(evaluationReplication).get(candidateId) / (_K -
					// 1.0));
					// }
					// }
				}
			}
			this.replannerIds = unmodifiableSet(tmpReplannerIds);
			this.meanGapReduction = tmpMeanGapReduction;
		}
	}
}
