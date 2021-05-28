/*
 * Copyright 2021 Gunnar Flötteröd
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
package org.matsim.contrib.greedo;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.DynamicDataXMLFileIO;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class CongestionTracker {

	private DynamicData<Id<Link>> relTTs;

	CongestionTracker(int startIteration, int endIteration) {
		this.relTTs = new DynamicData<>(startIteration, 1, endIteration + 1);
	}

	void register(int iteration, TravelTime travelTime, Network network) {
		if (iteration >= this.relTTs.getBinCnt()) {
			return;
		}
		for (Link link : network.getLinks().values()) {
			double freeFlowTT = link.getLength() / link.getFreespeed();
			this.relTTs.add(link.getId(), iteration,
					travelTime.getLinkTravelTime(link, 18 * 3600, null, null) / freeFlowTT);
		}
		@SuppressWarnings("serial")
		DynamicDataXMLFileIO<Id<Link>> io = new DynamicDataXMLFileIO<>() {
			@Override
			protected String key2attrValue(Id<Link> key) {
				return key.toString();
			}

			@Override
			protected Id<Link> attrValue2key(String string) {
				return Id.createLinkId(string);
			}
		};
		try {
			io.write("cong.new.xml", this.relTTs);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
