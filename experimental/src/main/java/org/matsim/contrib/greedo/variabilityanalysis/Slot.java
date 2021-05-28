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
package org.matsim.contrib.greedo.variabilityanalysis;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Slot<L> {

	public final L loc;

	public final int timeBin;

	public Slot(final L loc, final int timeBin) {
		this.loc = loc;
		this.timeBin = timeBin;
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof Slot) {
			final Slot<?> otherSlot = (Slot<?>) other;
			return (this.loc.equals(otherSlot.loc) && (this.timeBin == otherSlot.timeBin));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return 31 * (31 * this.loc.hashCode() + Integer.hashCode(this.timeBin));
	}
	
	@Override
	public String toString() {
		return this.loc + "(" + this.timeBin + ")";
	}
}
