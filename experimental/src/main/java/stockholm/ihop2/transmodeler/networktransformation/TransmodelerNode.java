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
class TransmodelerNode extends TransmodelerElement {

	private final double longitude;

	private final double latitude;

	TransmodelerNode(final String id, final double longitude,
			final double latitude) {
		super(id);
		this.longitude = longitude;
		this.latitude = latitude;
	}

	double getLongitude() {
		return this.longitude;
	}

	double getLatitude() {
		return this.latitude;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(id=" + this.getId()
				+ ", lon=" + this.longitude + ", lat=" + this.latitude + ")";
	}
}
