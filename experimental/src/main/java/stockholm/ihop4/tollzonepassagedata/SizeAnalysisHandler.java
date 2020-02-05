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

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SizeAnalysisHandler extends AbstractTollZonePassageDataHandler {

	private final int[] relevantIdentifiedVehiclesPerMeterLengthClass;
	private final int[] identifiedVehiclesPerMeterLengthClass;

	SizeAnalysisHandler(final int[] relevantIdentifiedVehiclesPerMeterLengthClass,
			final int[] identifiedVehiclesPerMeterLengthClass) {
		this.relevantIdentifiedVehiclesPerMeterLengthClass = relevantIdentifiedVehiclesPerMeterLengthClass;
		this.identifiedVehiclesPerMeterLengthClass = identifiedVehiclesPerMeterLengthClass;
	}

	@Override
	protected void processFields() {

		final int lengthClass = super.vehicleLength_cm / 100;
		if ((lengthClass < 0) || (lengthClass >= this.identifiedVehiclesPerMeterLengthClass.length)) {
			return;
		}

		if (super.containsRegisterData) {
			this.identifiedVehiclesPerMeterLengthClass[lengthClass]++;
			if (super.registerDataFeasible) {
				this.relevantIdentifiedVehiclesPerMeterLengthClass[lengthClass]++;
			}
		}
	}
}
