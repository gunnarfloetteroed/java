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
package org.matsim.contrib.greedo;

import static java.lang.Math.ceil;
import static java.lang.Math.min;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.DynamicData;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinkTravelTimeCopy implements TravelTime {

	private final DynamicData<Id<Link>> data_s;

	public LinkTravelTimeCopy(final MatsimServices services) {

		final TravelTime travelTimes = services.getLinkTravelTimes();
		final Config config = services.getConfig();

		final int binSize_s = config.travelTimeCalculator().getTraveltimeBinSize();
		final int binCnt = (int) ceil(((double) config.travelTimeCalculator().getMaxTime()) / binSize_s);

		this.data_s = new DynamicData<Id<Link>>(0, binSize_s, binCnt);

		for (Link link : services.getScenario().getNetwork().getLinks().values()) {
			for (int bin = 0; bin < binCnt; bin++) {
				this.data_s.put(link.getId(), bin,
						travelTimes.getLinkTravelTime(link, (bin + 0.5) * binSize_s, null, null));
			}
		}
	}

	@Override
	public synchronized double getLinkTravelTime(Link link, double time_s, Person person, Vehicle vehicle) {
		final int bin = min(this.data_s.getBinCnt() - 1, (int) (time_s / this.data_s.getBinSize_s()));
		return this.data_s.getBinValue(link.getId(), bin);
	}

	// for testing
	public double sum() {
		double result = 0.0;
		for (Id<Link> link : this.data_s.keySet()) {
			for (int bin = 0; bin < this.data_s.getBinCnt(); bin++) {
				result += this.data_s.getBinValue(link, bin);
			}
		}
		return result;
	}
}
