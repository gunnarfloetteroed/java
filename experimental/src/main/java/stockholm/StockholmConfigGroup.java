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
package stockholm;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class StockholmConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "stockholm";

	public StockholmConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- boatFactor --------------------

	private double boatFactor = 1.0;

	@StringGetter("boatFactor")
	public double getBoatFactor() {
		return boatFactor;
	}

	@StringSetter("boatFactor")
	public void setBoatFactor(final double boatFactor) {
		this.boatFactor = boatFactor;
	}

	// -------------------- freeOfCharge --------------------
	//
	// private boolean freeOfCharge = false;
	//
	// @StringGetter("freeOfCharge")
	// public boolean getFreeOfCharge() {
	// return freeOfCharge;
	// }
	//
	// @StringSetter("freeOfCharge")
	// public void setFreeOfCharge(final boolean freeOfCharge) {
	// this.freeOfCharge = freeOfCharge;
	// }

	// -------------------- adjustRoutingParameters --------------------
	//
	// private boolean adjustRoutingParameters = true;
	//
	// @StringGetter("adjustRoutingParameters")
	// public boolean getAdjustRoutingParameters() {
	// return adjustRoutingParameters;
	// }
	//
	// @StringSetter("adjustRoutingParameters")
	// public void setAdjustRoutingParameters(final boolean adjustRoutingParameters)
	// {
	// this.adjustRoutingParameters = adjustRoutingParameters;
	// }

	// -------------------- ferryPassengerMode --------------------

	private String ferryPassengerMode = "ferryPassenger";

	@StringGetter("ferryPassengerMode")
	public String getFerryPassengerMode() {
		return this.ferryPassengerMode;
	}

	@StringSetter("ferryPassengerMode")
	public void setFerryPassengerMode(final String ferryPassengerMode) {
		this.ferryPassengerMode = ferryPassengerMode;
	}

	public boolean isFerryPassengerMode(final String passengerMode) {
		if ((this.ferryPassengerMode != null) && (passengerMode != null)) {
			return this.ferryPassengerMode.equals(passengerMode);
		} else {
			return false;
		}
	}
	
	// -------------------- car ASC --------------------

	private double carASC = 0.0;

	@StringGetter("carASC")
	public double getCarASC() {
		return this.carASC;
	}

	@StringSetter("carASC")
	public void setCarASC(final double carASC) {
		this.carASC = carASC;
	}

	// -------------------- pt ASC --------------------

	private double ptASC = 0.0;

	@StringGetter("ptASC")
	public double getPtASC() {
		return this.ptASC;
	}

	@StringSetter("ptASC")
	public void setPtASC(final double ptASC) {
		this.ptASC = ptASC;
	}

	// -------------------- walk ASC --------------------

	private double walkASC = 0.0;

	@StringGetter("walkASC")
	public double getWalkASC() {
		return this.walkASC;
	}

	@StringSetter("walkASC")
	public void setWalkASC(final double walkASC) {
		this.walkASC = walkASC;
	}

	// -------------------- walk ASC --------------------

	private double bikeASC = 0.0;

	@StringGetter("bikeASC")
	public double getBikeASC() {
		return this.bikeASC;
	}

	@StringSetter("bikeASC")
	public void setBikeASC(final double bikeASC) {
		this.bikeASC = bikeASC;
	}


}
