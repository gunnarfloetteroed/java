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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import floetteroed.utilities.Units;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
abstract class AbstractTollZonePassageDataHandler extends AbstractTabularFileHandlerWithHeaderLine {

	// CONSTANTS

	protected static final String PASSAGE_TIME = "passage_time";
	protected static final String CHARGING_POINT = "charging_point";
	protected static final String VEHICLE_LENGTH_CM = "vehicle_length";

	protected static final String VEHICLE_TYPE = "vtr_vehicle_type"; // "PB" is "personbil"
	protected static final String PROFESSIONAL_USE = "vtr_professional_use"; // not private
	protected static final String PERSON_TYPE = "vtr_person_type"; // "F" is "fysisk person"

	// CONTROL MEMBERS

	protected final Set<String> feasibleVehicleTypes = new LinkedHashSet<>(Arrays.asList("PB"));
	protected final Set<String> feasibleProfessionalUses = new LinkedHashSet<>(Arrays.asList(""));
	protected final Set<String> feasiblePersonTypes = new LinkedHashSet<>(Arrays.asList("F"));

	// DATA MEMBERS

	protected Integer time_s = null;
	protected String chargingPointStr = null;
	protected String linkStr = null;
	protected Integer vehicleLength_cm = null;

	protected Boolean containsRegisterData = null;
	protected Boolean registerDataFeasible = null;

	protected String vehicleType = null;
	protected String professionalUse = null;
	protected String personType = null;

	AbstractTollZonePassageDataHandler() {
	}

	@Override
	public final void startDataRow(String[] args) {

		this.time_s = null;
		this.chargingPointStr = null;
		this.linkStr = null;
		this.vehicleLength_cm = null;

		this.containsRegisterData = null;
		this.registerDataFeasible = null;

		this.vehicleType = null;
		this.professionalUse = null;
		this.personType = null;

		final List<String> timeData = Arrays.asList(this.getStringValue(PASSAGE_TIME).split("[\\ \\.\\:]"));
		final int hours = Integer.parseInt(timeData.get(1));
		final int minutes = Integer.parseInt(timeData.get(2));
		final int seconds = Integer.parseInt(timeData.get(3));
		this.time_s = 3600 * hours + 60 * minutes + seconds;
		if ((this.time_s < 0) || (this.time_s >= Units.S_PER_D)) {
			return;
		}

		this.chargingPointStr = this.getStringValue(CHARGING_POINT);
		this.linkStr = TollZonePassageDataSpecification.chargingPoint2link.get(this.chargingPointStr);
		if (this.linkStr == null) {
			return;
		}

		this.vehicleLength_cm = Integer.parseInt(this.getStringValue(VEHICLE_LENGTH_CM));
		if (this.vehicleLength_cm < 0) {
			return;
		}

		if (args.length == 8) {

			this.containsRegisterData = false;
			this.processFields();

		} else if (args.length == 20) {

			this.containsRegisterData = true;
			this.vehicleType = this.getStringValue(VEHICLE_TYPE);
			this.professionalUse = this.getStringValue(PROFESSIONAL_USE);
			this.personType = this.getStringValue(PERSON_TYPE);
			this.registerDataFeasible = (this.feasibleVehicleTypes.contains(this.vehicleType)
					&& this.feasibleProfessionalUses.contains(this.professionalUse)
					&& this.feasiblePersonTypes.contains(this.personType));
			this.processFields();

		} else {

			throw new RuntimeException("This row contains " + args.length + " columns.");

		}
	}

	protected abstract void processFields();

}
