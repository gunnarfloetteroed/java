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
package org.matsim.contrib.emulation.emulators;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingTollCalculator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Inject;

/**
 * This class has the purpose to emulate MATSim for a single agent's plan. All
 * the relevant parts to perform the emulation and the scoring are completely
 * encapsulated, such that each agent can be simulated without interference on
 * one independent thread.
 * 
 * @author shoerl
 * @author Gunnar Flötteröd
 */
public final class AgentEmulator {

	private final PlanEmulator simulationEmulator;
	private final int iteration;

	private final ScoringFunctionFactory scoringFunctionFactory;
	private final Scenario scenario;
	
	@Inject
	public AgentEmulator(MatsimServices services, Scenario scenario, ScoringFunctionFactory scoringFunctionFactory,
			ReplanningContext context, Map<String, LegEmulator> mode2emulator, Map<String, LegDecomposer> mode2decomposer) {
//		this.iteration = context.getIteration();
//		this.scenario = scenario;
//		this.scoringFunctionFactory = scoringFunctionFactory;

		this.iteration = context.getIteration();
		this.scenario = services.getScenario();
		this.scoringFunctionFactory = scoringFunctionFactory;

		this.simulationEmulator = new PlanEmulator(services, mode2emulator, mode2decomposer);
	}

	static int warnCnt = 0;

	/**
	 * Emulates an agent's plan given the SimulationEmulator that has been defined
	 * previously. In particular, the whole class encapsulates one iteration of
	 * scoring while events are generated per agent. The reason this works is that
	 * the scoring doesn't care about the timing of events of independent agents.
	 */
	public void emulate(Person person, Plan plan, TravelTime overridingCarTravelTime) {
		
		if (warnCnt++ < 10) {
			Logger.getLogger(this.getClass())
					.warn("Removed road pricing related code. (2020-08-14: changed while moving to MATSim 12)");
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();

//		if (this.scenario.getConfig().getModules().containsKey(RoadPricingConfigGroup.GROUP_NAME)) {
//			// RoadPricingTollCalculator ADDS ITSELF as a handler to the EventsManager.
//			// Creates then suitable PersonMoneyEvents and passes them to the manager.
//			RoadPricingScheme scheme = (RoadPricingScheme) this.scenario
//					.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
//			Network network = this.scenario.getNetwork();
//			MyRoadPricingUtils.newInstance(network, scheme, eventsManager);
//		}
		
		final RoadPricingScheme roadPricingScheme = (RoadPricingScheme) this.scenario.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
		if (roadPricingScheme != null) {
			// RoadPricingTollCalculator ADDS ITSELF as a handler to the EventsManager, nothing else to do.
			new RoadPricingTollCalculator(this.scenario.getNetwork(), roadPricingScheme, eventsManager);
		}

		EventsToActivities eventsToActivities = new EventsToActivities();
		EventsToLegs eventsToLegs = new EventsToLegs(this.scenario);
		eventsManager.addHandler(eventsToActivities);
		eventsManager.addHandler(eventsToLegs);

		ScoringFunction scoringFunction = this.scoringFunctionFactory.createNewScoringFunction(person);
		ScoringFunctionWrapper scoringFunctionWrapper = new ScoringFunctionWrapper(scoringFunction);
		eventsManager.addHandler(scoringFunctionWrapper);

		eventsToActivities.addActivityHandler(scoringFunctionWrapper);
		eventsToLegs.addLegHandler(scoringFunctionWrapper);

		eventsManager.resetHandlers(this.iteration);

		this.simulationEmulator.emulate(person, plan, eventsManager, overridingCarTravelTime);

		eventsManager.finishProcessing();
		eventsToActivities.finish();
		scoringFunction.finish();

		plan.setScore(scoringFunction.getScore());		
	}

	static private class ScoringFunctionWrapper
			implements EventsToActivities.ActivityHandler, EventsToLegs.LegHandler, BasicEventHandler {
		private final ScoringFunction scoringFunction;

		private ScoringFunctionWrapper(ScoringFunction scoringFunction) {
			this.scoringFunction = scoringFunction;
		}

		@Override
		public void handleLeg(PersonExperiencedLeg leg) {
			this.scoringFunction.handleLeg(leg.getLeg());
		}

		@Override
		public void handleActivity(PersonExperiencedActivity activity) {
			this.scoringFunction.handleActivity(activity.getActivity());
		}

		@Override
		synchronized public void handleEvent(Event event) {
			if (event instanceof PersonStuckEvent) {
				this.scoringFunction.agentStuck(event.getTime());
			} else if (event instanceof PersonMoneyEvent) {
				this.scoringFunction.addMoney(((PersonMoneyEvent) event).getAmount());
			}
			this.scoringFunction.handleEvent(event);
		}
	}
}
