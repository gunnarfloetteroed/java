package org.matsim.contrib.ier.emulator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.roadpricing.MyRoadPricingUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingScheme;

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

	private final SimulationEmulator simulationEmulator;
	private final int iteration;

	private final ScoringFunctionFactory scoringFunctionFactory;
	private final Scenario scenario;

	@Inject
	public AgentEmulator(Scenario scenario, ScoringFunctionFactory scoringFunctionFactory,
			SimulationEmulator simulationEmulator, ReplanningContext context) {
		this.simulationEmulator = simulationEmulator;
		this.iteration = context.getIteration();
		this.scenario = scenario;
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	/**
	 * Emulates an agent's plan given the SimulationEmulator that has been defined
	 * previously. In particular, the whole class encapsulates one iteration of
	 * scoring while events are generated per agent. The reason this works is that
	 * the scoring doesn't care about the timing of events of independent agents.
	 */
	public void emulate(Person person, Plan plan, EventHandler eventHandler) {

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(eventHandler);

		if (this.scenario.getConfig().getModules().containsKey(RoadPricingConfigGroup.GROUP_NAME)) {
			// RoadPricingTollCalculator ADDS ITSELF as a handler to the EventsManager.
			// Creates then suitable PersonMoneyEvents and passes them to the manager.
			RoadPricingScheme scheme = (RoadPricingScheme) this.scenario
					.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
			Network network = this.scenario.getNetwork();
			MyRoadPricingUtils.newInstance(network, scheme, eventsManager);
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

		this.simulationEmulator.emulate(person, plan, eventsManager);

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
