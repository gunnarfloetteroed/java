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
package stockholm.ihop2.transmodeler.eventsreading;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

class LinkLeaveCounter implements LinkLeaveEventHandler {

	final Map<Id<Link>, Long> id2cnt = new LinkedHashMap<Id<Link>, Long>();

	long totalLeaves = 0;

	LinkLeaveCounter() {
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		if (this.id2cnt.containsKey(event.getLinkId())) {
			this.id2cnt.put(event.getLinkId(),
					this.id2cnt.get(event.getLinkId()) + 1);
		} else {
			this.id2cnt.put(event.getLinkId(), 1l);
		}
		this.totalLeaves++;
	}

}
