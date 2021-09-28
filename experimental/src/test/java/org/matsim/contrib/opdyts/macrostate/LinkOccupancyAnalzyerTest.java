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

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinkOccupancyAnalzyerTest {

	static final double eps = 1e-8;
	
	@Test
	public void test() {
		final int startTime_s = 0;
		final int binSize_s = 10;
		final int binCnt = 10;
		final LinkOccupancyAnalyzer analyzer = new LinkOccupancyAnalyzer(startTime_s, binSize_s, binCnt, null);

		final Id<Link> id1 = Id.createLinkId("1");
		final Id<Vehicle> veh1 = Id.createVehicleId("1");

		// [0,10): avg. occupancy = 0, last occupancy = 0

		analyzer.handleEvent(new LinkEnterEvent(10.0, veh1, id1));
		// time = 10, in = 1, out = 0
		analyzer.handleEvent(new VehicleEntersTrafficEvent(10.0, null, id1, veh1, null, 0.0));
		// time = 10, in = 2, out = 0
		analyzer.handleEvent(new LinkEnterEvent(14.0, veh1, id1));
		// time = 14, in = 3, out = 0
		analyzer.handleEvent(new LinkLeaveEvent(19.0, veh1, id1));
		// time = 19, in = 3, out = 1

		// [10,20): avg. occupancy = 2.5, last occupancy = 2

		analyzer.handleEvent(new VehicleEntersTrafficEvent(20.0, null, id1, veh1, "car", 0.0));
		// time = 20, in = 4, out = 1
		analyzer.handleEvent(new LinkEnterEvent(29.0, veh1, id1));
		// time = 29, in = 5, out = 1

		// [20,30): avg. occupancy = 3.1, last occupancy = 4

		analyzer.handleEvent(new VehicleLeavesTrafficEvent(30.0, null, id1, veh1, "car", 0.0));
		// time = 30, in = 5, out = 2
		analyzer.handleEvent(new LinkLeaveEvent(30.0, veh1, id1));
		// time = 30, in = 5, out = 3
		analyzer.handleEvent(new LinkEnterEvent(30.0, veh1, id1));
		// time = 30, in = 6, out = 3
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39, null, id1, veh1, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39, null, id1, veh1, "car", 0.0));
		analyzer.handleEvent(new VehicleLeavesTrafficEvent(39, null, id1, veh1, "car", 0.0));
		// time = 39, in = 6, out = 6

		// [30,39): avg. occupancy: 0.9*3+0.1*0=2.7, last occupancy = 0

		analyzer.handleEvent(new LinkEnterEvent(40.0, veh1, id1));
		analyzer.handleEvent(new LinkEnterEvent(40.0, veh1, id1));
		analyzer.handleEvent(new LinkLeaveEvent(40.0, veh1, id1));

		// time = 40, in = 8, out = 7

		// [40,49): avg. occupancy: 1.0, last occupancy = 0

		analyzer.handleEvent(new LinkLeaveEvent(90.0, veh1, id1));

		// time = 90, in = 8, out = 8

		analyzer.finalizeAndLock();

		assertEquals(0.0, analyzer.getCount(id1, 0), eps);
		assertEquals(2.5, analyzer.getCount(id1, 1), eps);
		assertEquals(3.1, analyzer.getCount(id1, 2), eps);
		assertEquals(2.7, analyzer.getCount(id1, 3), eps);
		assertEquals(1.0, analyzer.getCount(id1, 4), eps);
		assertEquals(1.0, analyzer.getCount(id1, 5), eps);
		assertEquals(1.0, analyzer.getCount(id1, 6), eps);
		assertEquals(1.0, analyzer.getCount(id1, 7), eps);
		assertEquals(1.0, analyzer.getCount(id1, 8), eps);
		assertEquals(0.0, analyzer.getCount(id1, 9), eps);
	}
}

