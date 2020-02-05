package org.matsim.contrib.opdyts.macrostate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.math.Vector;

/**
 * Keeps track of link occupancies per time bin and network mode.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class DifferentiatedLinkOccupancyAnalyzer
		implements SimulationMacroStateAnalyzer, LinkLeaveEventHandler, LinkEnterEventHandler,
		VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler {

	// -------------------- CONSTANTS --------------------

	private final TimeDiscretization timeDiscretization;

	private final Set<Id<Link>> relevantLinks;

	// -------------------- MEMBERS --------------------

	// one occupancy analyzer per mode. package private for unit testing.
	/* package */ final Map<String, CountingStateAnalyzer<Id<Link>>> mode2stateAnalyzer;

	// fast occupancy analyzer lookup for each currently traveling vehicle
	private final Map<Id<Vehicle>, CountingStateAnalyzer<Id<Link>>> vehicleId2stateAnalyzer = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public DifferentiatedLinkOccupancyAnalyzer(final TimeDiscretization timeDiscretization,
			final Set<String> relevantModes, final Set<Id<Link>> relevantLinks) {
		this.timeDiscretization = timeDiscretization;
		this.relevantLinks = relevantLinks;
		this.mode2stateAnalyzer = new LinkedHashMap<>();
		for (String mode : relevantModes) {
			this.mode2stateAnalyzer.put(mode, null);
		}
		this.clear();
	}

	// --------------- IMPLEMENTATION OF SimulationStateAnalyzer ---------------

	@Override
	public void clear() {
		for (String mode : new ArrayList<>(this.mode2stateAnalyzer.keySet())) {
			this.mode2stateAnalyzer.put(mode, new CountingStateAnalyzer<Id<Link>>(this.timeDiscretization));
		}
		this.vehicleId2stateAnalyzer.clear();
	}

	@Override
	public Vector newStateVectorRepresentation() {
		final Vector result = new Vector(
				this.mode2stateAnalyzer.size() * this.relevantLinks.size() * this.timeDiscretization.getBinCnt());
		int i = 0;
		for (String mode : this.mode2stateAnalyzer.keySet()) {
			final CountingStateAnalyzer<Id<Link>> analyzer = this.mode2stateAnalyzer.get(mode);
			for (Id<Link> linkId : this.relevantLinks) {
				for (int bin = 0; bin < this.timeDiscretization.getBinCnt(); bin++) {
					result.set(i++, analyzer.getCount(linkId, bin));
				}
			}
		}
		return result;
	}

	// ---------- IMPLEMENTATION OF *EventHandler INTERFACES ----------

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		final CountingStateAnalyzer<Id<Link>> stateAnalyzer = this.mode2stateAnalyzer.get(event.getNetworkMode());
		if (stateAnalyzer != null) { // relevantMode
			this.vehicleId2stateAnalyzer.put(event.getVehicleId(), stateAnalyzer);
			if (this.relevantLinks.contains(event.getLinkId())) {
				stateAnalyzer.registerIncrease(event.getLinkId(), (int) event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		final CountingStateAnalyzer<Id<Link>> stateAnalyzer = this.vehicleId2stateAnalyzer.get(event.getVehicleId());
		if (stateAnalyzer != null) { // relevant mode
			if (this.relevantLinks.contains(event.getLinkId())) {
				stateAnalyzer.registerDecrease(event.getLinkId(), (int) event.getTime());
			}
			this.vehicleId2stateAnalyzer.remove(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		final CountingStateAnalyzer<Id<Link>> stateAnalyzer = this.vehicleId2stateAnalyzer.get(event.getVehicleId());
		if (stateAnalyzer != null) { // relevant mode
			if (this.relevantLinks.contains(event.getLinkId())) {
				stateAnalyzer.registerIncrease(event.getLinkId(), (int) event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		final CountingStateAnalyzer<Id<Link>> stateAnalyzer = this.vehicleId2stateAnalyzer.get(event.getVehicleId());
		if (stateAnalyzer != null) { // relevant mode
			if (this.relevantLinks.contains(event.getLinkId())) {
				stateAnalyzer.registerDecrease(event.getLinkId(), (int) event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(final VehicleAbortsEvent event) {
		final CountingStateAnalyzer<Id<Link>> stateAnalyzer = this.vehicleId2stateAnalyzer.get(event.getVehicleId());
		if (stateAnalyzer != null) { // relevant mode
			if (this.relevantLinks.contains(event.getLinkId())) {
				stateAnalyzer.registerDecrease(event.getLinkId(), (int) event.getTime());
			}
			// TODO: Based on the assumption "abort = abort trip".
			this.vehicleId2stateAnalyzer.remove(event.getVehicleId());
		}
	}
}
