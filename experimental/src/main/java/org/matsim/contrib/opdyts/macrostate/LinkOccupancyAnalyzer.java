package org.matsim.contrib.opdyts.macrostate;

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

import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkOccupancyAnalyzer extends CountingStateAnalyzer<Id<Link>>
		implements LinkLeaveEventHandler, LinkEnterEventHandler, VehicleEntersTrafficEventHandler,
		VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler {

	// -------------------- MEMBERS --------------------

	private final Set<Id<Link>> relevantLinks;

	// -------------------- CONSTRUCTION --------------------

	public LinkOccupancyAnalyzer(final TimeDiscretization timeDiscretization, final Set<Id<Link>> relevantLinks) {
		super(timeDiscretization);
		this.relevantLinks = relevantLinks;
	}

	public LinkOccupancyAnalyzer(final int startTime_s, final int binSize_s, final int binCnt,
			final Set<Id<Link>> relevantLinks) {
		this(new TimeDiscretization(startTime_s, binSize_s, binCnt), relevantLinks);
	}

	// -------------------- INTERNALS --------------------

	private boolean relevant(Id<Link> link) {
		return ((this.relevantLinks == null) || this.relevantLinks.contains(link));
	}

	// ---------- IMPLEMENTATION OF *EventHandler INTERFACES ----------

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerIncrease(event.getLinkId(), (int) event.getTime());
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerIncrease(event.getLinkId(), (int) event.getTime());
		}
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerDecrease(event.getLinkId(), (int) event.getTime());
		}
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerDecrease(event.getLinkId(), (int) event.getTime());
		}
	}

	@Override
	public void handleEvent(final VehicleAbortsEvent event) {
		if (this.relevant(event.getLinkId())) {
			this.registerDecrease(event.getLinkId(), (int) event.getTime());
		}
	}
}
