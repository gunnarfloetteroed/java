package org.matsim.contrib.ier;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ier.emulator.AgentEmulator;
import org.matsim.contrib.ier.emulator.SimulationEmulator;
import org.matsim.contrib.ier.replannerselection.ReplannerSelector;
import org.matsim.contrib.ier.replannerselection.ReplannerSelector.IEREventHandlerProvider;
import org.matsim.contrib.ier.run.IERConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import floetteroed.utilities.math.BasicStatistics;

/**
 * This class replaces the standard MATSim replanning. It fullfills a number of
 * tasks:
 * 
 * <ul>
 * <li>For each agent, perform the MATSIm-configured replanning multiple times
 * to arrive at a "best-response" for the agent plan at the momentary system
 * state.</li>
 * <li>Select whether each agent should switch from the currently used plan to
 * the new optimized plan.</li>
 * </ul>
 * 
 * @author shoerl
 */
@Singleton
public final class IERReplanning implements PlansReplanning, ReplanningListener {
	private final static Logger logger = Logger.getLogger(IERReplanning.class);

	private final int numberOfEmulationThreads;

	private final IERConfigGroup ierConfig;

	private final Provider<ReplanningContext> replanningContextProvider;
	private final StrategyManager strategyManager;
	private final Scenario scenario;
	private final Provider<AgentEmulator> agentEmulatorProvider;
	private final ReplannerSelector replannerSelector;

	@Inject
	IERReplanning(StrategyManager strategyManager, Scenario scenario,
			Provider<ReplanningContext> replanningContextProvider, Config config,
			Provider<AgentEmulator> agentEmulatorProvider, Provider<SimulationEmulator> simulationEmulatorProvider,
			ReplannerSelector replannerSelector, ScoringFunctionFactory scoringFunctionFactory) {
		this.strategyManager = strategyManager;
		this.scenario = scenario;
		this.replanningContextProvider = replanningContextProvider;
		this.numberOfEmulationThreads = config.global().getNumberOfThreads();
		this.ierConfig = ConfigUtils.addOrGetModule(config, IERConfigGroup.class);
		this.agentEmulatorProvider = agentEmulatorProvider;
		this.replannerSelector = replannerSelector;
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {

		try {

			if (this.ierConfig.getWritePerformanceOutput()) {
				this.writePerformanceOutput(event.getIteration());
			}

			final IEREventHandlerProvider handlerForLastReplanningIterationProvider = this.replannerSelector
					.beforeReplanningAndGetEventHandlerProvider();
			final IEREventHandlerProvider handlerForOtherReplanningIterationsProvider = new IEREventHandlerProvider() {
				@Override
				public EventHandler get(Set<Id<Person>> personIds) {
					return new EventHandler() {
					};
				}
			};

			final ReplanningContext replanningContext = this.replanningContextProvider.get();

			for (int i = 0; i < this.ierConfig.getIterationsPerCycle(); i++) {
				logger.info(String.format("Started replanning iteration %d/%d", i + 1,
						this.ierConfig.getIterationsPerCycle()));

				// NEW MEMORIZE OLD SELECTED PLANS
				final Map<Id<Person>, Plan> personId2oldSelectedPlan = new LinkedHashMap<>();
				for (Person person : this.scenario.getPopulation().getPersons().values()) {
					personId2oldSelectedPlan.put(person.getId(), person.getSelectedPlan());
				}

				// We run replanning on all agents (exactly as it is defined in the config)
				this.strategyManager.run(this.scenario.getPopulation(), replanningContext);

				// NEW ONLY EMULATE AND SCORE PLANS THAT HAVE CHANGED
				final Set<Person> personsToEmulate = new LinkedHashSet<>();
				final IEREventHandlerProvider currentEventHandlerProvider;
				if (i == this.ierConfig.getIterationsPerCycle() - 1) {
					currentEventHandlerProvider = handlerForLastReplanningIterationProvider;
					personsToEmulate.addAll(this.scenario.getPopulation().getPersons().values());
				} else {
					currentEventHandlerProvider = handlerForOtherReplanningIterationsProvider;
					for (Person person : this.scenario.getPopulation().getPersons().values()) {
						if (person.getSelectedPlan() != personId2oldSelectedPlan.get(person.getId())) {
							personsToEmulate.add(person);
						}
					}
				}

				// if (this.ierConfig.getParallel()) {
				// emulateInParallel(this.scenario.getPopulation(), event.getIteration(),
				// currentEventHandlerProvider);
				emulateInParallel(personsToEmulate, event.getIteration(), currentEventHandlerProvider);
				// } else {
				// emulateSequentially(this.scenario.getPopulation(), event.getIteration(),
				// currentEventHandlerProvider.get(this.scenario.getPopulation().getPersons().keySet()));
				// }

				logger.info(String.format("Finished replanning iteration %d/%d", i + 1,
						this.ierConfig.getIterationsPerCycle()));
			}

			this.replannerSelector.afterReplanning();

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void writePerformanceOutput(final int iteration) throws InterruptedException {
		final Map<Id<Person>, Double> personId2OriginalScore = new LinkedHashMap<>();
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			personId2OriginalScore.put(person.getId(), person.getSelectedPlan().getScore());
		}
		this.emulateInParallel(this.scenario.getPopulation(), iteration, new IEREventHandlerProvider() {
			@Override
			public EventHandler get(Set<Id<Person>> personIds) {
				return new EventHandler() {
				};
			}
		});
		final BasicStatistics scoreErrorStats = new BasicStatistics();
		final List<Tuple<Double, List<String>>> scoreErrorAndModesList = new ArrayList<>();
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			final double scoreError = person.getSelectedPlan().getScore() - personId2OriginalScore.get(person.getId());
			scoreErrorStats.add(scoreError);
			List<String> modes = new ArrayList<>();
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Leg) {
					modes.add(((Leg) pe).getMode());
				}
			}
			scoreErrorAndModesList.add(new Tuple<>(scoreError, modes));
			person.getSelectedPlan().setScore(personId2OriginalScore.get(person.getId()));
		}
		Collections.sort(scoreErrorAndModesList, new Comparator<Tuple<Double, List<String>>>() {
			@Override
			public int compare(Tuple<Double, List<String>> o1, Tuple<Double, List<String>> o2) {
				return o1.getFirst().compareTo(o2.getFirst());
			}
		});
		try {
			final PrintWriter writer = new PrintWriter(new File(
					this.scenario.getConfig().controler().getOutputDirectory(), "scoreErrors_" + iteration + ".txt"));
			for (int i = 0; i < scoreErrorAndModesList.size(); i++) {
				writer.println(i + "\t" + scoreErrorAndModesList.get(i).getFirst() + "\t"
						+ scoreErrorAndModesList.get(i).getSecond().size() + "\t"
						+ scoreErrorAndModesList.get(i).getSecond());
			}
			final String msg = "Mean score error: " + scoreErrorStats.getAvg() + "; stddev = "
					+ scoreErrorStats.getStddev();
			Logger.getLogger(this.getClass()).info(msg);
			writer.println(msg);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// @Deprecated
	// private void emulateSequentially(Population population, int iteration,
	// EventHandler eventHandler)
	// throws InterruptedException {
	//
	// Logger.getLogger(this.getClass()).warn("Scoring in serial emulation is not
	// maintained.");
	// System.exit(0);
	//
	// final EventsManager eventsManager = EventsUtils.createEventsManager();
	// final EventsToScore eventsToScore =
	// EventsToScore.createWithScoreUpdating(this.scenario,
	// this.scoringFunctionFactory, eventsManager);
	// eventsToScore.beginIteration(iteration);
	//
	// eventsManager.addHandler(eventHandler);
	// eventsManager.resetHandlers(iteration);
	//
	// for (Person person : population.getPersons().values()) {
	// this.simulationEmulatorProvider.get().emulate(person,
	// person.getSelectedPlan(), eventsManager);
	// }
	//
	// eventsManager.finishProcessing();
	// eventsToScore.finish();
	// }

	private void emulateInParallel(Population population, int iteration,
			final IEREventHandlerProvider eventHandlerProvider) throws InterruptedException {
		this.emulateInParallel(population.getPersons().values(), iteration, eventHandlerProvider);
	}

	private void emulateInParallel(Collection<? extends Person> persons, int iteration,
			final IEREventHandlerProvider eventHandlerProvider) throws InterruptedException {

		// Iterator<? extends Person> personIterator =
		// population.getPersons().values().iterator();
		Iterator<? extends Person> personIterator = persons.iterator();
		List<Thread> threads = new LinkedList<>();

		// long totalNumberOfPersons = population.getPersons().size();
		long totalNumberOfPersons = persons.size();
		AtomicLong processedNumberOfPersons = new AtomicLong(0);
		AtomicBoolean finished = new AtomicBoolean(false);

		// Maybe there is a better way to do this... Turn logging off.
		Logger.getLogger("org.matsim").setLevel(Level.ERROR); // WARN);

		// Here we set up all the runner threads and start them
		for (int i = 0; i < this.numberOfEmulationThreads; i++) {
			Thread thread = new Thread(() -> {

				final AgentEmulator agentEmulator;
				synchronized (this.agentEmulatorProvider) {
					agentEmulator = this.agentEmulatorProvider.get();
				}

				Map<Id<Person>, Person> batch = new LinkedHashMap<>();

				do {
					batch.clear();

					// Here we create our batch
					synchronized (personIterator) {
						while (personIterator.hasNext() && batch.size() < this.ierConfig.getBatchSize()) {
							final Person person = personIterator.next();
							batch.put(person.getId(), person);
						}
					}

					final EventHandler eventHandler;
					synchronized (eventHandlerProvider) {
						eventHandler = eventHandlerProvider.get(batch.keySet());
					}

					// And here we send all the agents to the emulator. The score will be written to
					// the plan directly.
					for (Person person : batch.values()) {
						agentEmulator.emulate(person, person.getSelectedPlan(), eventHandler);
					}

					processedNumberOfPersons.addAndGet(batch.size());
				} while (batch.size() > 0);
			});

			threads.add(thread);
			thread.start();
		}

		// We want one additional thread to track progress and output some information
		Thread progressThread = new Thread(() -> {
			long currentProcessedNumberOfPersons = 0;
			long lastProcessedNumberOfPersons = -1;

			while (!finished.get()) {
				try {
					currentProcessedNumberOfPersons = processedNumberOfPersons.get();

					if (currentProcessedNumberOfPersons > lastProcessedNumberOfPersons) {
						logger.info(String.format("Emulating... %d / %d (%.2f%%)", currentProcessedNumberOfPersons,
								totalNumberOfPersons, 100.0 * currentProcessedNumberOfPersons / totalNumberOfPersons));
					}

					lastProcessedNumberOfPersons = currentProcessedNumberOfPersons;

					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});

		progressThread.start();

		// Wait for all the runners to finish
		for (Thread thread : threads) {
			thread.join();
		}

		// Wait for the progress thread to finish
		finished.set(true);
		progressThread.join();

		logger.info("Emulation finished.");

		// Maybe there is a better way to do this... Turn logging on.
		Logger.getLogger("org.matsim").setLevel(null);
	}
}
