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
package stockholm.wum.creation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Time;
import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.Units;
import floetteroed.utilities.math.MathHelpers;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AnalyzeBussArrivals {

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		String path = "/Users/GunnarF/OneDrive - VTI/My Data/wum/data/output/";
		String scheduleFile = path + "transitSchedule_reduced.xml.gz";
		// String transitVehiclesFile = path + "transitVehiclesDifferentiated.xml.gz";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile(scheduleFile);
		// new
		// VehicleReaderV1(scenario.getTransitVehicles()).readFile(transitVehiclesFile);

		Id<TransitStopFacility> consideredStopFacilityId = Id.create("740020101", TransitStopFacility.class);
		TransitStopFacility stopFacility = scenario.getTransitSchedule().getFacilities().get(consideredStopFacilityId);
		System.out.println(stopFacility.getName());

		Map<Id<TransitRoute>, List<Double>> relevantRoute2arrivalTimes = new LinkedHashMap<>();

		TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600 / 4, 2 * 24 * 4);
		DynamicData<?> data = new DynamicData<>(timeDiscr);

		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				if ("bus".equals(route.getTransportMode())
						&& !consideredStopFacilityId.equals(route.getStops().get(0).getStopFacility().getId())) {
					for (TransitRouteStop stop : route.getStops()) {
						if (consideredStopFacilityId.equals(stop.getStopFacility().getId())) {
							List<Double> arrivalTimes = relevantRoute2arrivalTimes.get(route.getId());
							if (arrivalTimes == null) {
								arrivalTimes = new ArrayList<>(1);
								relevantRoute2arrivalTimes.put(route.getId(), arrivalTimes);
							}
							for (Departure departure : route.getDepartures().values()) {
								double time = departure.getDepartureTime() + stop.getArrivalOffset();
								if (time < Units.S_PER_D) {
									data.add(null, timeDiscr.getBin(time), 1);
								}
							}
						}
					}
				}
			}
		}

		for (int bin = 0; bin < timeDiscr.getBinCnt(); bin++) {
			System.out.print(Time.strFromSec(timeDiscr.getBinStartTime_s(bin)));
			System.out.print("-");
			System.out.print(Time.strFromSec(timeDiscr.getBinEndTime_s(bin)));
			System.out.print("\t");
			System.out.println(MathHelpers.round(data.getBinValue(null, bin)));
		}

		System.out.println("... END");
	}
}
