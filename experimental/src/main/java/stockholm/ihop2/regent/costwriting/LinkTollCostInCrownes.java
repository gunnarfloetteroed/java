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
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.vehicles.Vehicle;

/**
 * This would be much nicer if I could directly access the roadpricing contrib
 * somehow.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkTollCostInCrownes implements TravelDisutility {

	private final RoadPricingSchemeImpl roadPricingScheme;

	public LinkTollCostInCrownes(final String tollFileName) {
		this.roadPricingScheme = new RoadPricingSchemeImpl();
		final RoadPricingReaderXMLv1 reader = new RoadPricingReaderXMLv1(
				this.roadPricingScheme);
		reader.readFile(tollFileName);
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time,
			Person person, Vehicle vehicle) {
		final RoadPricingSchemeImpl.Cost cost = this.roadPricingScheme
				.getLinkCostInfo(link.getId(), time,
						(person != null) ? person.getId() : null,
						(vehicle != null) ? vehicle.getId() : null);
		if (cost == null) {
			return 0.0;
		} else {
			return cost.amount;
		}
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0.0;
	}

}
