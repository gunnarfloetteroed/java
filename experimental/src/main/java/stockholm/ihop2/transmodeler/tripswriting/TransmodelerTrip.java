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
package stockholm.ihop2.transmodeler.tripswriting;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerTrip implements Comparable<TransmodelerTrip> {

	// -------------------- CONSTANTS --------------------

	final int id;

	final String fromNodeId;

	final String toNodeId;

	final String fromLinkId;

	final int pathId;

	final String toLinkId;

	final Double dptTime_s;

	final String agentId;

	String oriAct = null;

	String endAct = null;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerTrip(final int id, final String fromNodeId,
			final String toNodeId, final String fromLinkId, final int pathId,
			final String toLinkId, final Double dptTime_s, final String agentId) {
		this.id = id;
		this.fromNodeId = fromNodeId;
		this.toNodeId = toNodeId;
		this.fromLinkId = fromLinkId;
		this.pathId = pathId;
		this.toLinkId = toLinkId;
		this.dptTime_s = dptTime_s;
		this.agentId = agentId;
	}

	void addActivityTypes(final String fromActType, final String toActType) {
		this.oriAct = fromActType;
		this.endAct = toActType;
	}

	// -------------------- IMPLEMENTATION OF Comparable --------------------

	@Override
	public int compareTo(final TransmodelerTrip o) {
		final int timeComp = this.dptTime_s.compareTo(o.dptTime_s);
		if (timeComp != 0) {
			return timeComp;
		} else {
			return Integer.compare(this.id, o.id);
		}		
	}	
}
