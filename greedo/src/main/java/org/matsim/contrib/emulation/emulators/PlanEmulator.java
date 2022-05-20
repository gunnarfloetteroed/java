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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.config.TransitConfigGroup;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet;

/**
 * 
 * @author shoerl
 * @author Gunnar Flötteröd
 */
public class PlanEmulator {

	// -------------------- MEMBERS --------------------

	private final MatsimServices services;

	private final double simEndTime_s;

	private final Set<String> passengerModes;

	private final Map<String, LegEmulator> mode2emulator;

	private final Map<String, LegDecomposer> mode2decomposer;

	// -------------------- CONSTRUCTION --------------------

	public PlanEmulator(final MatsimServices services, final Map<String, LegEmulator> mode2emulator,
			final Map<String, LegDecomposer> mode2decomposer) {
		this.services = services;
		this.mode2emulator = mode2emulator;
		this.mode2decomposer = mode2decomposer;

		this.simEndTime_s = services.getConfig().qsim().getEndTime().seconds();

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

	public void emulate(final Person person, final Plan plan, final EventsManager eventsManager,
			final TravelTime overridingCarTravelTime) {
		final List<PlanElement> planElements = new ArrayList<>();
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				planElements.add(planElement);
			} else if (planElement instanceof Leg) {
				final Leg compositeLeg = (Leg) planElement;
				final LegDecomposer legDecomposer = this.mode2decomposer.getOrDefault(compositeLeg.getMode(),
						this.mode2decomposer.get(TransportMode.other));
				legDecomposer.setCompositeLeg(compositeLeg, plan);
				planElements.addAll(legDecomposer.getAtomicPlanElementsView());
			} else {
				throw new RuntimeException("Plan element with unknown type: " + planElement.getClass().getName());
			}
		}
		this.emulate(person, planElements, eventsManager, overridingCarTravelTime);
	}

	public void emulate(final Person person, List<PlanElement> planElements, final EventsManager eventsManager,
			final TravelTime overridingCarTravelTime) {

		double time_s = 0.0;
		boolean stuck = false;

		for (int planElementIndex = 0; (planElementIndex < planElements.size()) && !stuck; planElementIndex++) {
			final PlanElement element = planElements.get(planElementIndex);

			final boolean isFirstElement = (planElementIndex == 0);
			final boolean isLastElement = (planElementIndex == (planElements.size() - 1));

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
				final LegEmulator legEmulator = this.mode2emulator.getOrDefault(leg.getMode(),
						this.mode2emulator.get(TransportMode.other));
				legEmulator.setEventsManager(eventsManager);
				legEmulator.setOverridingTravelTime(
						overridingCarTravelTime != null ? overridingCarTravelTime : this.services.getLinkTravelTimes());

				time_s = legEmulator.emulateLegAndReturnEndTime_s(planElementIndex, planElements, person, time_s);
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
