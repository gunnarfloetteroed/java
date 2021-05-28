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
package org.matsim.contrib.greedo.trustregion;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Neighborhoods {

	public static Set<Slot> getSpaceExpanded(final Set<Slot> startRegion, final Network network) {
		final Set<Slot> result = new LinkedHashSet<>(startRegion);
		for (Slot slot : startRegion) {
			final Link link = network.getLinks().get(slot.loc);
			if (link != null) {
				link.getFromNode().getInLinks().keySet().forEach(id -> result.add(new Slot(id, slot.timeBin)));
				link.getFromNode().getOutLinks().keySet().forEach(id -> result.add(new Slot(id, slot.timeBin)));
				link.getToNode().getInLinks().keySet().forEach(id -> result.add(new Slot(id, slot.timeBin)));
				link.getToNode().getOutLinks().keySet().forEach(id -> result.add(new Slot(id, slot.timeBin)));
			}
		}
		return result;
	}

	public static Set<Slot> getSpaceExpanded(final Set<Slot> startRegion, final Network network, final int depth) {
		if (depth == 0) {
			return new LinkedHashSet<>(startRegion);
		} else {
			return getSpaceExpanded(getSpaceExpanded(startRegion, network), network, depth - 1);
		}
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("./scenarios/EkoCS-Trans/input/network.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		LinkedList<Id<Link>> linkIds = new LinkedList<>(scenario.getNetwork().getLinks().keySet());
		Collections.shuffle(linkIds);
		System.out.println();
		for (int depth = 0; depth <= 200; depth++) {
			Set<Slot> start = new LinkedHashSet<>(1);
			start.add(new Slot(linkIds.removeFirst(), 0));
			System.out.println(depth + "\t" + getSpaceExpanded(start, scenario.getNetwork(), depth).size());
		}
	}

}
