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
package stockholm.ihop2.transmodeler.networktransformation;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerSegment extends TransmodelerElement implements
		Comparable<TransmodelerSegment> {

	// -------------------- MEMBERS --------------------

	private final int lanes;

	private final double length;

	// using negative positions in BA direction
	private final Integer position;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerSegment(final String id, final int lanes, final double length,
			final Integer position) {
		super(id);
		this.lanes = lanes;
		this.length = length;
		this.position = position;
	}

	// -------------------- GETTERS --------------------

	int getLanes() {
		return lanes;
	}

	double getLength() {
		return length;
	}

	// -------------------- IMPLEMENTATION OF Comparable --------------------

	@Override
	public int compareTo(final TransmodelerSegment o) {
		return (this.position).compareTo(o.position);
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(lanes=" + this.lanes
				+ ", length=" + this.length + ", position=" + this.position
				+ ")";
	}
}
