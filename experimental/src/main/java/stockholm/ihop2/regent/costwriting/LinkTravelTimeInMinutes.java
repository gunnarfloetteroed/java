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
package stockholm.ihop2.regent.costwriting;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkTravelTimeInMinutes implements TravelDisutility {

	private final TravelTime travelTimeInSeconds;

	public LinkTravelTimeInMinutes(final TravelTime travelTimeInSeconds) {
		this.travelTimeInSeconds = travelTimeInSeconds;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time,
			final Person person, final Vehicle vehicle) {
		return Units.MIN_PER_S
				* this.travelTimeInSeconds.getLinkTravelTime(link, time,
						person, vehicle);
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return Units.MIN_PER_S
				* this.travelTimeInSeconds.getLinkTravelTime(link,
						Time.UNDEFINED_TIME, null, null);
	}
}
