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
package org.matsim.contrib.emulation;

import java.util.Collection;
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emulation.emulators.AgentEmulator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;

/**
 * Carved out of IERReplanning.
 * 
 * @author shoerl
 * @author Gunnar Flötteröd
 * 
 */
public class EmulationEngine {
	private final static Logger logger = Logger.getLogger(EmulationEngine.class);

	private final Provider<ReplanningContext> replanningContextProvider;
	private final StrategyManager strategyManager;
	private final Scenario scenario;
	private final Provider<AgentEmulator> agentEmulatorProvider;

	private final EmulationConfigGroup ierConfig;
	private final int numberOfEmulationThreads;

	public EmulationEngine(StrategyManager strategyManager, Scenario scenario,
			Provider<ReplanningContext> replanningContextProvider, Provider<AgentEmulator> agentEmulatorProvider,
			Config config) {
		this.strategyManager = strategyManager;
		this.scenario = scenario;
		this.replanningContextProvider = replanningContextProvider;
		this.agentEmulatorProvider = agentEmulatorProvider;
		this.ierConfig = ConfigUtils.addOrGetModule(config, EmulationConfigGroup.class);
		this.numberOfEmulationThreads = config.global().getNumberOfThreads();
	}

	public void replan(final int matsimIteration, final TravelTime carTravelTime) {

		final ReplanningContext replanningContext = this.replanningContextProvider.get();

		for (int i = 0; i < this.ierConfig.getIterationsPerCycle(); i++) {
			logger.info(
					String.format("Started replanning iteration %d/%d", i + 1, this.ierConfig.getIterationsPerCycle()));
			logger.info("mean score of selected plan = " + this.scenario.getPopulation().getPersons().values().stream()
					.mapToDouble(p -> p.getSelectedPlan().getScore()).average().getAsDouble());

			this.strategyManager.run(this.scenario.getPopulation(), replanningContext);

			final Set<Person> personsToEmulate = new LinkedHashSet<>(
					this.scenario.getPopulation().getPersons().values());
			try {
				this.emulate(personsToEmulate, matsimIteration, carTravelTime);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			logger.info(String.format("Finished replanning iteration %d/%d", i + 1,
					this.ierConfig.getIterationsPerCycle()));
		}
	}

	public void emulate(Collection<? extends Person> persons, int iteration,
			final TravelTime carTravelTime)
			throws InterruptedException {

		Iterator<? extends Person> personIterator = persons.iterator();
		List<Thread> threads = new LinkedList<>();

		long totalNumberOfPersons = persons.size();
		AtomicLong processedNumberOfPersons = new AtomicLong(0);
		AtomicBoolean finished = new AtomicBoolean(false);

		// Maybe there is a better way to do this... Turn logging off.
		final Level originalLogLevel = Logger.getLogger("org.matsim").getLevel();
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

					// And here we send all the agents to the emulator. The score will be written to
					// the plan directly.
					for (Person person : batch.values()) {
						agentEmulator.emulate(person, person.getSelectedPlan(), carTravelTime);
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
		Logger.getLogger("org.matsim").setLevel(originalLogLevel);
	}
}
