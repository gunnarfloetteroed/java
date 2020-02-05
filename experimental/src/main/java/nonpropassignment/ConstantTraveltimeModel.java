/*
 * Copyright 2020 Gunnar Flötteröd
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
package nonpropassignment;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class ConstantTraveltimeModel {

	double cnt = 0;

	private Map<Link, Double> link2ttSum = new LinkedHashMap<>();

	private final int startTime_s;

	private final int endTime_s;

	private final Network network;
	private final SymmetricLinkPairIndexer indexer;

	ConstantTraveltimeModel(Network network, final SymmetricLinkPairIndexer indexer, int startTime_s, int endTime_s) {
		this.network = network;
		this.indexer = indexer;
		for (Id<Link> linkId : indexer.getLink2IndexView().keySet()) {
			this.link2ttSum.put(network.getLinks().get(linkId), 0.0);
		}
		this.startTime_s = startTime_s;
		this.endTime_s = endTime_s;
	}

	void addTravelTime(TravelTime travelTime) {
		this.cnt++;
		for (Map.Entry<Link, Double> entry : this.link2ttSum.entrySet()) {
			entry.setValue(entry.getValue() + travelTime.getLinkTravelTime(entry.getKey(),
					0.5 * (this.startTime_s + this.endTime_s), null, null));
		}
	}

	double[] getMeanTTs() {
		double[] result = new double[this.indexer.getNumberOfLinks()];
		final double fact = 1.0 / this.cnt;
		for (Map.Entry<Id<Link>, Integer> entry : this.indexer.getLink2IndexView().entrySet()) {
			result[entry.getValue()] = fact * this.link2ttSum.get(this.network.getLinks().get(entry.getKey()));
		}
		return result;
	}

	double[] getFreeTTs() {
		double[] result = new double[this.indexer.getNumberOfLinks()];
		for (Map.Entry<Id<Link>, Integer> entry : this.indexer.getLink2IndexView().entrySet()) {
			final Link link = this.network.getLinks().get(entry.getKey());
			result[entry.getValue()] = link.getLength() / link.getFreespeed();
		}
		return result;
	}

}
