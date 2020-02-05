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
package org.matsim.roadpricing;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class MyRoadPricingUtils {

	/*
	 * Don't know how to get my own instance of CalcPaidToll otherwise, with its
	 * constructor being package private and the class being final. Will eventually
	 * switch to a more recent version of the roadpricing contrib, hence living with
	 * this hack for the time being.
	 */
	public static CalcPaidToll newInstance(final Network network, final RoadPricingScheme scheme,
			EventsManager events) {
		final Level level = Logger.getLogger("org.matsim").getLevel();
		Logger.getLogger("org.matsim").setLevel(Level.OFF);
		final CalcPaidToll result = new CalcPaidToll(network, scheme, events);
		Logger.getLogger("org.matsim").setLevel(level);
		return result;
	}

}
