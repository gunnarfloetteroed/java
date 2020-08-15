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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import floetteroed.utilities.Units;
import floetteroed.utilities.math.BasicStatistics;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AdjustPseudoNetwork {

	private final TransitSchedule transitSchedule;

	private final Network network;

	private final String prefix;

	public AdjustPseudoNetwork(final TransitSchedule transitSchedule, final Network network, final String prefix) {
		this.transitSchedule = transitSchedule;
		this.network = network;
		this.prefix = prefix;
	}

	public void run() {

		Set<String> modes = new LinkedHashSet<>();
		for (String mode : new String[] {"pt"}) { // "bus","tram","subway","rail","ferry"}) {
			modes.add(mode);
		}
		
		Map<Link, BasicStatistics> link2tts = new LinkedHashMap<>();
		for (Link link : this.network.getLinks().values()) {
			if (link.getId().toString().startsWith(this.prefix)) {
				link.setCapacity(1e6);
				link.setNumberOfLanes(1e3);
				link.setLength(Math.max(link.getLength(), 50.0));
				link2tts.put(link, new BasicStatistics());
				link.setAllowedModes(modes);
			}
		}

		for (TransitLine line : this.transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {

					final double routeDptTime_s = departure.getDepartureTime();
					Node prevStopNode = null;
					double prevStopDptTime_s = Double.NaN;

					for (TransitRouteStop stop : route.getStops()) {

						final Link thisLink = this.network.getLinks().get(stop.getStopFacility().getLinkId());
						final Node thisStopNode = thisLink.getToNode();
						final double thisStopArrTime_s = routeDptTime_s + stop.getArrivalOffset().seconds();

						if (prevStopNode != null) {

							if ((thisLink.getFromNode() != prevStopNode) || (thisLink.getToNode() != thisStopNode)) {
								throw new RuntimeException();
							}

							final double stop2stopTime_s = thisStopArrTime_s - prevStopDptTime_s;
							link2tts.get(thisLink).add(stop2stopTime_s);
						}

						prevStopNode = thisStopNode;
						prevStopDptTime_s = thisStopArrTime_s;
					}
				}
			}
		}

		for (Map.Entry<Link, BasicStatistics> entry : link2tts.entrySet()) {
			final Link link = entry.getKey();
			final BasicStatistics stats = entry.getValue();
			final double oldSpeed_m_s = link.getFreespeed();
			final double newSpeed_m_s;
			if ((stats.size() > 0) && (stats.getAvg() > 0)) {
				newSpeed_m_s = Math.max(link.getLength() / stats.getAvg(), 1000.0 * Units.M_S_PER_KM_H);
			} else {
				newSpeed_m_s = 1000.0 * Units.M_S_PER_KM_H;
			}
			link.setFreespeed(newSpeed_m_s);
			System.out.println("changing speed on link " + link.getId() + " from " + Units.KM_H_PER_M_S * oldSpeed_m_s
					+ " km/h to " + Units.KM_H_PER_M_S * newSpeed_m_s + " km/h.");
		}

	}

}
