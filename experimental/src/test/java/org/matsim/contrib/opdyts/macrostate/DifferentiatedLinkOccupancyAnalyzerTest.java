/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.opdyts.macrostate;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.math.Vector;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class DifferentiatedLinkOccupancyAnalyzerTest {

	static final double eps = 1e-8;

	@Test
	public void test() {

		final int startTime_s = 0;
		final int binSize_s = 10;
		final int binCnt = 10;

		final Id<Link> link1 = Id.createLinkId("link1");
		final Id<Link> link2 = Id.createLinkId("link2");
		final Id<Link> link3 = Id.createLinkId("link3");
		final Id<Link> link4 = Id.createLinkId("link4");

		final Set<Id<Link>> relevantLinks = new TreeSet<>(Arrays.asList(link1, link2, link3, link4));
		final Set<String> relevantModes = new LinkedHashSet<>(Arrays.asList("car"));
		final TimeDiscretization timeDiscr = new TimeDiscretization(startTime_s, binSize_s, binCnt);

		final DifferentiatedLinkOccupancyAnalyzer analyzer = new DifferentiatedLinkOccupancyAnalyzer(timeDiscr,
				relevantModes, relevantLinks);
		// final .Factory provider = new DifferentiatedLinkOccupancyAnalyzer.Factory(
		// timeDiscr, relevantModes, relevantLinks);
		// final DifferentiatedLinkOccupancyAnalyzer analyzer =
		// (DifferentiatedLinkOccupancyAnalyzer) provider
		// .newEventHandler();
		// provider.beforeIteration();
		analyzer.clear();

		final Id<Vehicle> veh1 = Id.createVehicleId("veh1");
		final Id<Vehicle> veh2 = Id.createVehicleId("veh2");
		final Id<Vehicle> veh3 = Id.createVehicleId("veh3");
		final Id<Vehicle> veh4 = Id.createVehicleId("veh4");

		final Id<Vehicle> noCar = Id.createVehicleId("noCar");

		final List<Event> events = new ArrayList<>();

		/*-
		 *   link1    link2    link3    link4
		 * o -----> o -----> o -----> o -----> o
		 * 
		 *  00.0  15.0
		 *  ~~~~~~~~~~
		 *     veh1
		 * 
		 *           10.0  25.0 
		 *           ~~~~~~~~~~
		 *              veh2
		 *              
		 *                    20.0  35.0
		 *                    ~~~~~~~~~~
		 *                       veh3
		 *                       
		 *  05.0    20.0    35.0    50.0
		 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~   
		 *               veh4                  
		 *               
		 *  05.0    20.0    35.0    50.0
		 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~   
		 *               noCar                  
		 */

		events.add(new VehicleEntersTrafficEvent(0.0, null, link1, veh1, "car", 0.0));
		events.add(new LinkLeaveEvent(15.0, veh1, link1));
		events.add(new LinkEnterEvent(15.0, veh1, link2));
		events.add(new VehicleLeavesTrafficEvent(15.0, null, link2, veh1, "car", 0.0));

		events.add(new VehicleEntersTrafficEvent(10.0, null, link2, veh2, "car", 0.0));
		events.add(new LinkLeaveEvent(25.0, veh2, link2));
		events.add(new LinkEnterEvent(25.0, veh2, link3));
		events.add(new VehicleLeavesTrafficEvent(25.0, null, link3, veh2, "car", 0.0));

		events.add(new VehicleEntersTrafficEvent(20.0, null, link3, veh3, "car", 0.0));
		events.add(new LinkLeaveEvent(35.0, veh3, link3));
		events.add(new LinkEnterEvent(35.0, veh3, link4));
		events.add(new VehicleLeavesTrafficEvent(35.0, null, link4, veh3, "car", 0.0));

		events.add(new VehicleEntersTrafficEvent(5.0, null, link1, veh4, "car", 0.0));
		events.add(new LinkLeaveEvent(20.0, veh4, link1));
		events.add(new LinkEnterEvent(20.0, veh4, link2));
		events.add(new LinkLeaveEvent(35.0, veh4, link2));
		events.add(new LinkEnterEvent(35.0, veh4, link3));
		events.add(new LinkLeaveEvent(50.0, veh4, link3));
		events.add(new LinkEnterEvent(50.0, veh4, link4));
		events.add(new VehicleLeavesTrafficEvent(50.0, null, link4, veh4, "car", 0.0));

		events.add(new VehicleEntersTrafficEvent(5.0, null, link1, noCar, "noCar", 0.0));
		events.add(new LinkLeaveEvent(20.0, noCar, link1));
		events.add(new LinkEnterEvent(20.0, noCar, link2));
		events.add(new LinkLeaveEvent(35.0, noCar, link2));
		events.add(new LinkEnterEvent(35.0, noCar, link3));
		events.add(new LinkLeaveEvent(50.0, noCar, link3));
		events.add(new LinkEnterEvent(50.0, noCar, link4));
		events.add(new VehicleLeavesTrafficEvent(50.0, null, link4, noCar, "noCar", 0.0));

		Collections.sort(events, new Comparator<Event>() {
			@Override
			public int compare(Event o1, Event o2) {
				return Double.compare(o1.getTime(), o2.getTime());
			}
		});
		for (Event event : events) {
			if (event instanceof VehicleEntersTrafficEvent) {
				((VehicleEntersTrafficEventHandler) analyzer).handleEvent((VehicleEntersTrafficEvent) event);
			} else if (event instanceof LinkLeaveEvent) {
				((LinkLeaveEventHandler) analyzer).handleEvent((LinkLeaveEvent) event);
			}
			if (event instanceof LinkEnterEvent) {
				((LinkEnterEventHandler) analyzer).handleEvent((LinkEnterEvent) event);
			}
			if (event instanceof VehicleLeavesTrafficEvent) {
				((VehicleLeavesTrafficEventHandler) analyzer).handleEvent((VehicleLeavesTrafficEvent) event);
			}
		}

		Vector x1 = new Vector(1.5, 1.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		for (int bin = 0; bin < 10; bin++) {
			assertEquals(x1.get(bin), analyzer.mode2stateAnalyzer.get("car").getCount(link1, bin), eps);
		}

		Vector x2 = new Vector(0.0, 1.0, 1.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		for (int bin = 0; bin < 10; bin++) {
			assertEquals(x2.get(bin), analyzer.mode2stateAnalyzer.get("car").getCount(link2, bin), eps);
		}

		Vector x3 = new Vector(0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		for (int bin = 0; bin < 10; bin++) {
			assertEquals(x3.get(bin), analyzer.mode2stateAnalyzer.get("car").getCount(link3, bin), eps);
		}

		Vector x4 = new Vector(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
		for (int bin = 0; bin < 10; bin++) {
			assertEquals(x4.get(bin), analyzer.mode2stateAnalyzer.get("car").getCount(link4, bin), eps);
		}

//		Vector state = provider.newStateVectorRepresentation();
		Vector state = analyzer.newStateVectorRepresentation();
		int i = 0;
		for (Vector exp : Arrays.asList(x1, x2, x3, x4)) {
			for (int j = 0; j < 10; j++) {
				assertEquals(exp.get(j), state.get(i++), eps);
			}
		}
	}
}
