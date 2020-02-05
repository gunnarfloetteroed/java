package org.matsim.contrib.ier.emulator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.MatsimServices;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Inject;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet;

/**
 * This is an example implementation of SimulationEmulator. See the interface
 * for more information and comments inline for an explanation what is done
 * here.
 * 
 * @author shoerl
 * @author Gunnar Flötteröd
 */
public class SimulationEmulatorImpl implements SimulationEmulator {

	// -------------------- MEMBERS --------------------

	private final MatsimServices services;

	private final double simEndTime_s;

	private final Set<String> passengerModes;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public SimulationEmulatorImpl(final MatsimServices services) {
		this.services = services;
		this.simEndTime_s = services.getConfig().qsim().getEndTime();

		// Transit-specific parameters and checks below.
		this.passengerModes = new LinkedHashSet<>();
		if (services.getConfig().transit().isUseTransit()) {
			// Only minOfDurationAndEndTime uses transit-compatible
			// TripPlanMutateTimeAllocation.
			if (!ActivityDurationInterpretation.minOfDurationAndEndTime
					.equals(services.getConfig().plans().getActivityDurationInterpretation())) {
				throw new RuntimeException(
						"Only supporting " + ActivityDurationInterpretation.minOfDurationAndEndTime + ".");
			}
			// Emulation is only compatible with checkLineAndStop. This could be improved.
			if (!TransitConfigGroup.BoardingAcceptance.checkLineAndStop
					.equals(services.getConfig().transit().getBoardingAcceptance())) {
				throw new RuntimeException("Only supporting " + TransitConfigGroup.BoardingAcceptance.checkLineAndStop);
			}
			// Extract passenger modes from SwissRailRaptor.
			// raptorConfig.getModeMappingForPassengers() returns an empty
			// collection if mode mappings are not used.
			final SwissRailRaptorConfigGroup raptorConfig = (SwissRailRaptorConfigGroup) this.services.getConfig()
					.getModules().get(SwissRailRaptorConfigGroup.GROUP);
			if (raptorConfig == null) {
				throw new RuntimeException("Only supporting SwissRailRaptor.");
			}
			for (ModeMappingForPassengersParameterSet modeMappingParams : raptorConfig.getModeMappingForPassengers()) {
				this.passengerModes.add(modeMappingParams.getPassengerMode());
			}
		}
	}

	// --------------- IMPLEMENTATION OF SimulationEmulator ---------------

	@Override
	public void emulate(final Person person, final Plan plan, final EventsManager eventsManager) {
		List<? extends PlanElement> elements = plan.getPlanElements();

		double time_s = 0.0;
		boolean stuck = false;

		for (int i = 0; (i < elements.size()) && !stuck; i++) {
			final PlanElement element = elements.get(i);

			final boolean isFirstElement = (i == 0);
			final boolean isLastElement = (i == (elements.size() - 1));

			if (element instanceof Activity) {

				/*
				 * Simulating activities is quite straight-forward. Either they have an end time
				 * set or they have a maximum duration. It only gets a bit more complicated for
				 * public transit. In that case we would need to check if the next leg is a
				 * public transit trip and adjust the end time here according to the schedule.
				 */

				final Activity activity = (Activity) element;
				time_s = (new BasicActivityEmulator(eventsManager, this.simEndTime_s,
						this.services.getConfig().plans().getActivityDurationInterpretation()))
								.emulateActivityAndReturnEndTime_s(activity, person, time_s, isFirstElement,
										isLastElement);

			} else if (element instanceof Leg) {

				/*
				 * Same is true for legs. We need to generate departure and arrival events and
				 * everything in between.
				 */

				final Leg leg = (Leg) element;
				final Activity previousActivity = (Activity) elements.get(i - 1);
				final Activity followingActivity = (Activity) elements.get(i + 1);

				final LegEmulator legEmulator;
				if (TransportMode.car.equals(leg.getMode())) { // TODO Other network modes?
					legEmulator = new CarLegEmulator(eventsManager, this.services.getScenario().getNetwork(),
							this.services.getLinkTravelTimes(), this.services.getScenario().getActivityFacilities(),
							this.simEndTime_s);
				} else if (TransportMode.pt.equals(leg.getMode()) || this.passengerModes.contains(leg.getMode())) {
					legEmulator = new ScheduleBasedTransitLegEmulator(eventsManager, this.services.getScenario());
				} else {
					legEmulator = new OnlyDepartureArrivalLegEmulator(eventsManager,
							this.services.getScenario().getActivityFacilities(), this.simEndTime_s);
				}
				time_s = legEmulator.emulateLegAndReturnEndTime_s(leg, person, previousActivity, followingActivity,
						time_s);
			} else {
				throw new RuntimeException("Unknown instance of " + PlanElement.class.getSimpleName() + ": "
						+ element.getClass().getSimpleName());
			}

			if (time_s > this.simEndTime_s) {
				// A leg/activity emulator indicates "stuck" by exceeding simEndTime_s.
				stuck = true; // Exits the loop, i.e. terminates the emulation of this person.
				eventsManager.processEvent(new PersonStuckEvent(time_s, person.getId(), null, null));
			}
		}
	}
}
