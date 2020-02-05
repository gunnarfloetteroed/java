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
package stockholm.ihop4.sampersutilities;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.utils.objectattributes.attributable.Attributes;

import floetteroed.utilities.Units;
import stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SampersTour {

	// -------------------- MEMBERS --------------------

	private Leg firstLeg = null;

	private Activity activity = null;

	private Attributes activityAttributes = null;

	private Purpose purpose = null;

	private Leg secondLeg = null;

	private double money_SEK = 0.0;

	// -------------------- CONSTRUCTION / BUILDING --------------------

	SampersTour() {
	}

	private String buildStatus() {
		return "First leg is " + (this.firstLeg == null ? "" : "not ") + "null, activity is "
				+ (this.activity == null ? "" : "not ") + "null, second leg is "
				+ (this.secondLeg == null ? "" : "not ") + "null.";
	}

	void addLeg(final Leg leg) {
		if ((this.firstLeg == null) && (this.activity == null) && (this.secondLeg == null)) {
			this.firstLeg = leg;
		} else if ((this.firstLeg != null) && (this.activity != null) && (this.secondLeg == null)) {
			this.secondLeg = leg;
		} else {
			throw new RuntimeException("Cannot add a leg: " + this.buildStatus());
		}
	}

	void addActivity(final Activity activity, final Attributes activityAttributes) {
		if ((this.firstLeg != null) && (this.activity == null) && (this.secondLeg == null)) {
			this.activity = activity;
			this.activityAttributes = activityAttributes;
			this.purpose = SampersUtilityParameters.Purpose.valueOf(activity.getType());
		} else {
			throw new RuntimeException("Cannot add an activity: " + this.buildStatus());
		}
	}

	void addMoney_SEK(final double money_SEK) {
		if (money_SEK > 0) {
			// we only allow for costs
			throw new RuntimeException("money = " + money_SEK + " SEK");
		}
		this.money_SEK += money_SEK;
	}

	boolean isComplete() {
		return ((this.firstLeg != null) && (this.activity != null) && (this.secondLeg != null));
	}

	// -------------------- GETTERS --------------------

	Leg getFirstLeg() {
		return this.firstLeg;
	}

	Leg getSecondLeg() {
		return this.secondLeg;
	}

	Attributes getActivityAttributes() {
		return this.activityAttributes;
	}

	Purpose getPurpose() {
		return this.purpose;
	}

	Double getRealizedStartTime_s() {
		return this.activity.getStartTime();
	}

	double getRealizedActivityDuration_s() {
		return (this.activity.getEndTime() - this.activity.getStartTime());
	}

	double getRealizedTravelTime_min() {
		// legs contain travel time in seconds
		return (this.firstLeg.getTravelTime() + this.secondLeg.getTravelTime()) / 60.0;
	}

	double getRealizedTravelDistance_km() {
		// TODO Make sure that the distance unit is indeed meters!
		return Units.KM_PER_M * (this.firstLeg.getRoute().getDistance() + this.secondLeg.getRoute().getDistance());
	}

	double getEventBasedCost_SEK() {
		return -this.money_SEK; // a cost, hence positive
	}
}
