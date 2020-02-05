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
package stockholm.ihop4.tollzonepassagedata;

import floetteroed.utilities.DynamicData;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class PassageDataHandler extends AbstractTollZonePassageDataHandler {

	private final DynamicData<String> data;

	private final double[] weightPerMeterLengthClass;

	PassageDataHandler(final DynamicData<String> data, final double[] weightPerMeterLengthClass) {
		this.data = data;
		this.weightPerMeterLengthClass = weightPerMeterLengthClass;
	}

	@Override
	protected void processFields() {

		if (this.containsRegisterData) {
			if (this.registerDataFeasible) {
				this.data.add(super.linkStr, this.data.bin((int) this.time_s), 1.0);
			}
		} else { // contains no register data -> weight by length class
			int lengthClass = this.vehicleLength_cm / 100;
			if (lengthClass < this.weightPerMeterLengthClass.length) {
				this.data.add(super.linkStr, this.data.bin((int) this.time_s),
						this.weightPerMeterLengthClass[lengthClass]);
			}
		}
	}
}
