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
package stockholm.wum.malin;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import stockholm.ihop2.regent.demandreading.ZonalSystem;
import stockholm.ihop2.regent.demandreading.Zone;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class AbstractZoneAnalyzer {

	// -------------------- CONSTANTS --------------------

	private final ZonalSystem zonalSystem;

	private final Scenario scenario;

	// -------------------- CONSTRUCTION --------------------

	AbstractZoneAnalyzer(final ZonalSystem zonalSystem, final Scenario scenario) {
		this.zonalSystem = zonalSystem;
		this.scenario = scenario;
	}

	// -------------------- IMPLEMENTATION --------------------

	String getZoneId(final Id<Link> linkId) {
		final Link link = this.scenario.getNetwork().getLinks().get(linkId);
		if (link == null) {
			return null;
		}
		Zone zone = this.zonalSystem.getZone(link.getFromNode());
		if (zone != null) {
			return zone.getId();
		} else {
			zone = this.zonalSystem.getZone(link.getToNode());
			if (zone != null) {
				return zone.getId();
			} else {
				return null;
			}
		}
	}
}
