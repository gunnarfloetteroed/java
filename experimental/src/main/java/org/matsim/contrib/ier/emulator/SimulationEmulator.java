package org.matsim.contrib.ier.emulator;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * The SimulationEmulator has the purpose to create events for an agent's plan.
 * While the standard network simulation in MATSim performs a detailed
 * queue-based simulation with interacting agents the idea is here to create
 * events as quickly as possible, potentially loosing some of the dynamics.
 * However, this is meant to be a fast approximation of the actual network
 * simulation in MATSim.
 * 
 * @author shoerl
 */
public interface SimulationEmulator {
	void emulate(Person person, Plan plan, EventsManager eventsManager);
}
