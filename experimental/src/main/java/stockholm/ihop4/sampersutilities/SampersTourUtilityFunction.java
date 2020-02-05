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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;

import floetteroed.utilities.Units;
import stockholm.StockholmConfigGroup;
import stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SampersTourUtilityFunction {

	// -------------------- CONSTANTS --------------------

	private final SampersUtilityParameters utlParams;
	
	// -------------------- CONSTRUCTION --------------------

	SampersTourUtilityFunction(final SampersUtilityParameters utlParams) {
		this.utlParams = utlParams;
	}

	// -------------------- INTERNALS --------------------

	double getScheduleDelayCost(final SampersTour tour, final Double income_SEK_yr) {
		final Purpose purpose = tour.getPurpose();
		final String mode = tour.getFirstLeg().getMode();
		double result = 0.0;

		/* Start time */ {
			final Double plannedStart_h = SampersAttributeUtils.getPlannedActivityStart_h(tour);
			if (plannedStart_h != null) {
				final double plannedStart_min = Units.MIN_PER_H * plannedStart_h;
				final double realizedStart_min = Units.MIN_PER_S * tour.getRealizedStartTime_s();
				result += this.utlParams.getScheduleDelayCostEarly_1_min(purpose, mode, income_SEK_yr)
						* Math.max(0.0,
								(plannedStart_min - realizedStart_min) - this.utlParams.getScheduleDelaySlack_min())
						+ this.utlParams.getScheduleDelayCostLate_1_min(purpose, mode, income_SEK_yr) * Math.max(0.0,
								(realizedStart_min - plannedStart_min) - this.utlParams.getScheduleDelaySlack_min());
			}
		}

		/* Duration */ {
			final Double plannedDuration_h = SampersAttributeUtils.getPlannedActivityDuration_h(tour);
			if (plannedDuration_h != null) {
				final double plannedDuration_min = Units.MIN_PER_H * plannedDuration_h;
				final double realizedDuration_min = Units.MIN_PER_S * (tour.getRealizedActivityDuration_s());
				result += this.utlParams.getScheduleDelayCostTooShort_1_min(purpose, mode, income_SEK_yr)
						* Math.max(0.0, plannedDuration_min - realizedDuration_min)
						+ this.utlParams.getScheduleDelayCostTooLong_1_min(purpose, mode, income_SEK_yr)
								* Math.max(0.0, realizedDuration_min - plannedDuration_min);
			}
		}

		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	protected double getGeneralizedPTTravelTime_s(final double accessEgressTime_s, final double firstWaitingTime_s,
			final double inVehicleTime_s, final double transferTime_s, final int numberOfTransfers) {
		return (this.utlParams.getPTAccessEgressTimeMultiplier() * accessEgressTime_s
				+ this.utlParams.getPTFirstWaitingTimeMultiplier() * firstWaitingTime_s
				+ this.utlParams.getPTInVehicleTimeMultiplier() * inVehicleTime_s
				+ this.utlParams.getPTTransferTimeMultiplier() * transferTime_s
				+ Units.S_PER_MIN * this.utlParams.getPTTransferPenalty_min() * numberOfTransfers);
	}

	double getStuckScore(final Person person) {
		return this.utlParams.getStuckScore(SampersUtilityParameters.Purpose.work, TransportMode.car,
				SampersAttributeUtils.getIncome_SEK_yr(person));
	}

	double getUtility(final SampersTour tour, final Person person) {

		final String mode = tour.getFirstLeg().getMode();
		if (!SampersUtilityParameters.CONSIDERED_MODES.contains(mode)) {
			throw new RuntimeException("Mode " + mode + " is not one of the supported modes: " + SampersUtilityParameters.CONSIDERED_MODES);
		}
		
		final Purpose purpose = tour.getPurpose();
		final double income_SEK_yr = SampersAttributeUtils.getIncome_SEK_yr(person);

		/* Mode ASC */
		double result = this.utlParams.getModeASC(purpose, mode, income_SEK_yr);
		
		/* Schedule delay */
		result += this.getScheduleDelayCost(tour, income_SEK_yr);

		/* Time */ {
			double travelTime_min = tour.getRealizedTravelTime_min();
			if (travelTime_min < 0) {
				Logger.getLogger(this.getClass()).warn("tour travel time = " + travelTime_min + " min");
				travelTime_min = 0.0;
			}
			result += this.utlParams.getLinTimeCoeff_1_min(purpose, mode, income_SEK_yr)
					* tour.getRealizedTravelTime_min();
		}

		/* Monetary cost */ {
			double cost_SEK = tour.getEventBasedCost_SEK()
					+ this.utlParams.getMonetaryDistanceCost_SEK_km() * tour.getRealizedTravelDistance_km();
			if (cost_SEK < 0) {
				Logger.getLogger(this.getClass()).warn("tour cost = " + cost_SEK + " SEK");
				cost_SEK = 0.0;
			}
			result += this.utlParams.getLinCostCoeff_1_SEK(purpose, mode, income_SEK_yr) * cost_SEK
					+ this.utlParams.getLogCostCoeff_lnArgInSEK(purpose, mode, income_SEK_yr)
							* Math.log(0.01 + cost_SEK); // Yes, +0.01 it is.
		}

		/* Distance */ {
			double dist_km = tour.getRealizedTravelDistance_km();
			if (dist_km < 0) {
				Logger.getLogger(this.getClass()).warn("tour distance = " + dist_km + " km");
				dist_km = 0.0;
			}
			result += this.utlParams.getLinDistanceCoeff_1_km(purpose, mode, income_SEK_yr) * dist_km
					+ this.utlParams.getLogDistanceCoeff_lnArgInKm(purpose, mode, income_SEK_yr) * Math.log(0.01 + dist_km);
		}

		return result;
	}
}
