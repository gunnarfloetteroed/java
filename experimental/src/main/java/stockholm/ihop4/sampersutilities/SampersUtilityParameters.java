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

import static java.lang.Double.POSITIVE_INFINITY;
import static org.matsim.api.core.v01.TransportMode.bike;
import static org.matsim.api.core.v01.TransportMode.car;
import static org.matsim.api.core.v01.TransportMode.pt;
import static org.matsim.api.core.v01.TransportMode.walk;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.adultEducation;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.businessFromHome;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.giveARide;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.gymnasium;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.other;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.primarySchool;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.rareShopping;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.recreation;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.regularShopping;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.service;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.visit;
import static stockholm.ihop4.sampersutilities.SampersUtilityParameters.Purpose.work;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import modalsharecalibrator.ModeASCContainer;
import stockholm.ihop2.regent.demandreading.PopulationCreator;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SampersUtilityParameters {

	// -------------------- CONSTANTS --------------------

	public static final Set<String> CONSIDERED_MODES = Collections
			.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(bike, car, pt, walk)));

	public enum Purpose {
		work, recreation, regularShopping, rareShopping, primarySchool, gymnasium, adultEducation, visit, businessFromHome, giveARide, service, other
	};

	// ----- INNER CLASS DEFINING PARAMETERS THROUGH A CHAIN OF COMMAND -----

	private class ParameterPerStratum {

		private ParameterPerStratum next = null;

		public void addNext(final ParameterPerStratum next) {
			if (this.next != null) {
				this.next.addNext(next);
			} else {
				this.next = next;
			}
		}

		protected ParameterPerStratum getNext() {
			return this.next;
		}

		public double getOrZero(final Purpose purpose, final String mode, final double income) {
			if (this.next != null) {
				return this.next.getOrZero(purpose, mode, income);
			} else {
				return 0.0;
			}
		}
	}

	private class ConcreteParameterPerStratum extends ParameterPerStratum {

		private final Purpose purpose;
		private final String mode;
		private final double minIncome;
		private final double maxIncome;
		private final double parameterValue;

		private ConcreteParameterPerStratum(final Purpose purpose, final String mode, final double minIncome,
				final double maxIncome, final double parameterValue) {
			this.purpose = purpose;
			this.mode = mode;
			this.minIncome = minIncome;
			this.maxIncome = maxIncome;
			this.parameterValue = parameterValue;
		}

		@Override
		public double getOrZero(final Purpose purpose, final String mode, final double income) {
			if (this.purpose.equals(purpose) && (this.mode.equals(mode)) && (this.minIncome <= income)
					&& (this.maxIncome > income)) {
				return this.parameterValue;
			} else if (this.getNext() != null) {
				return this.getNext().getOrZero(purpose, mode, income);
			} else {
				return 0.0;
			}
		}
	}

	// -------------------- MEMBERS --------------------

	private final ParameterPerStratum linTimeCoeff_1_min = new ParameterPerStratum();

	private final ParameterPerStratum linCostCoeff_1_SEK = new ParameterPerStratum();
	private final ParameterPerStratum logCostCoeff_lnArgInSEK = new ParameterPerStratum();

	private final ParameterPerStratum linDistanceCoeff_1_km = new ParameterPerStratum();
	private final ParameterPerStratum logDistanceCoeff_lnArgInKm = new ParameterPerStratum();

	private final ParameterPerStratum scheduleDelayCostEarly_1_min = new ParameterPerStratum();
	private final ParameterPerStratum scheduleDelayCostLate_1_min = new ParameterPerStratum();

	private final ParameterPerStratum scheduleDelayTooShort_1_min = new ParameterPerStratum();
	private final ParameterPerStratum scheduleDelayTooLong_1_min = new ParameterPerStratum();

	// TODO Simplified treatment of ASCs, compatible with modal share calibration.
	// private final ParameterPerStratum modeASC = new ParameterPerStratum();
	// private final Map<String, Double> modeASC = new LinkedHashMap<>();
	private final ModeASCContainer modeASCs;

	// -------------------- CONSTRUCTION --------------------

	public SampersUtilityParameters(final ModeASCContainer modeASCs) {
		this.modeASCs = modeASCs;

		if (!Purpose.work.toString().equals(PopulationCreator.WORK)) {
			throw new RuntimeException("Purpose.work has different String representation from PopulationCreator.WORK");
		}
		if (!Purpose.other.toString().equals(PopulationCreator.OTHER)) {
			throw new RuntimeException(
					"Purpose.other has different String representation from PopulationCreator.OTHER");
		}

		// WORK

		this.linDistanceCoeff_1_km.addNext(new ConcreteParameterPerStratum(work, bike, 0, POSITIVE_INFINITY, -0.182));

		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(work, car, 0.0, POSITIVE_INFINITY, -0.039));

		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(work, car, 0, 200 * 1000, -0.019));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(work, pt, 0, 200 * 1000, -0.019));

		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(work, car, 200 * 1000, 300 * 1000, -0.015));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(work, pt, 200 * 1000, 300 * 1000, -0.015));

		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(work, car, 300 * 1000, POSITIVE_INFINITY, -0.005));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(work, pt, 300 * 1000, POSITIVE_INFINITY, -0.005));

		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(work, car, 300 * 1000, POSITIVE_INFINITY, -0.052));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(work, pt, 300 * 1000, POSITIVE_INFINITY, -0.052));

		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(work, pt, 0, POSITIVE_INFINITY, -0.014));

		this.linDistanceCoeff_1_km.addNext(new ConcreteParameterPerStratum(work, walk, 0, POSITIVE_INFINITY, -0.278));

		this.logDistanceCoeff_lnArgInKm
				.addNext(new ConcreteParameterPerStratum(work, walk, 0, POSITIVE_INFINITY, -0.631));

		// this.modeASC.addNext(new ConcreteParameterPerStratum(work, car, 0,
		// POSITIVE_INFINITY, 0.0));
		// this.modeASC.addNext(new ConcreteParameterPerStratum(work, bike, 0,
		// POSITIVE_INFINITY, 0.433));
		// this.modeASC.addNext(new ConcreteParameterPerStratum(work, pt, 0,
		// POSITIVE_INFINITY, -0.758));
		// this.modeASC.addNext(new ConcreteParameterPerStratum(work, walk, 0,
		// POSITIVE_INFINITY, 0.101));

		// OTHER

		this.linDistanceCoeff_1_km.addNext(new ConcreteParameterPerStratum(other, bike, 0, POSITIVE_INFINITY, -0.2442));

		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(other, car, 0.0, POSITIVE_INFINITY, -0.0434));

		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(other, car, 0, 50 * 1000, -0.008927));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(other, pt, 0, 50 * 1000, -0.008927));

		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(other, car, 50 * 1000, 200 * 1000, -0.008269));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(other, pt, 50 * 1000, 200 * 1000, -0.008269));

		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(other, car, 200 * 1000, POSITIVE_INFINITY, -0.004279));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(other, pt, 200 * 1000, POSITIVE_INFINITY, -0.004279));

		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(other, car, 200 * 1000, POSITIVE_INFINITY, +0.3382));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(other, pt, 200 * 1000, POSITIVE_INFINITY, +0.3382));

		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(other, car, 50 * 1000, 200 * 1000, +0.3348));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(other, pt, 50 * 1000, 200 * 1000, +0.3348));

		this.logCostCoeff_lnArgInSEK.addNext(new ConcreteParameterPerStratum(other, car, 0, 50 * 1000, -0.393));
		this.logCostCoeff_lnArgInSEK.addNext(new ConcreteParameterPerStratum(other, pt, 0, 50 * 1000, -0.393));

		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(other, pt, 0, POSITIVE_INFINITY, -0.01325));

		this.linDistanceCoeff_1_km.addNext(new ConcreteParameterPerStratum(other, walk, 0, POSITIVE_INFINITY, -0.3993));

		// this.modeASC.addNext(new ConcreteParameterPerStratum(other, car, 0,
		// POSITIVE_INFINITY, 0.0));
		// this.modeASC.addNext(new ConcreteParameterPerStratum(other, bike, 0,
		// POSITIVE_INFINITY, -1.942));
		// this.modeASC.addNext(new ConcreteParameterPerStratum(other, pt, 0,
		// POSITIVE_INFINITY, -0.6944));
		// this.modeASC.addNext(new ConcreteParameterPerStratum(other, walk, 0,
		// POSITIVE_INFINITY, 0.482));

		// SCHEDULE DELAY COSTS.
		// Should probably be derived from activity-specific travel costs. TODO Revisit!

		for (Purpose purpose : Purpose.values()) {
			for (String mode : CONSIDERED_MODES) {
				this.scheduleDelayCostEarly_1_min
						.addNext(new ConcreteParameterPerStratum(purpose, mode, 0.0, POSITIVE_INFINITY, -1.0));
				this.scheduleDelayCostLate_1_min
						.addNext(new ConcreteParameterPerStratum(purpose, mode, 0.0, POSITIVE_INFINITY, -1.0));
				this.scheduleDelayTooShort_1_min
						.addNext(new ConcreteParameterPerStratum(purpose, mode, 0.0, POSITIVE_INFINITY, -1.0));
				this.scheduleDelayTooLong_1_min
						.addNext(new ConcreteParameterPerStratum(purpose, mode, 0.0, POSITIVE_INFINITY, -1.0));
			}
		}

		// ========== BELOW: PARAMETERS THAT CURRENTLY ARE NOT USED! ==========

		// RECREATION

		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(recreation, bike, 0, POSITIVE_INFINITY, -0.248));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(Purpose.recreation, car, 0.0, POSITIVE_INFINITY, -0.041));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(Purpose.recreation, car, 0, 50 * 1000, -0.030));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(Purpose.recreation, pt, 0, 50 * 1000, -0.030));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(recreation, car, 50 * 1000, POSITIVE_INFINITY, -0.014));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(recreation, pt, 50 * 1000, POSITIVE_INFINITY, -0.014));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(recreation, car, 50 * 1000, POSITIVE_INFINITY, -0.066));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(recreation, pt, 50 * 1000, POSITIVE_INFINITY, -0.066));
		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(recreation, pt, 0, POSITIVE_INFINITY, -0.017));
		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(recreation, walk, 0, POSITIVE_INFINITY, -0.481));

		// REGULAR SHOPPING

		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(regularShopping, bike, 0, POSITIVE_INFINITY, -0.352));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(regularShopping, car, 0.0, POSITIVE_INFINITY, -0.084));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(regularShopping, car, 0.0, POSITIVE_INFINITY, -0.015));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(regularShopping, pt, 0.0, POSITIVE_INFINITY, -0.015));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(regularShopping, car, 0.0, POSITIVE_INFINITY, -0.421));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(regularShopping, pt, 0.0, POSITIVE_INFINITY, -0.421));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(regularShopping, pt, 0, POSITIVE_INFINITY, -0.037));
		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(regularShopping, walk, 0, POSITIVE_INFINITY, -0.560));

		// RARE SHOPPING

		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(rareShopping, bike, 0, POSITIVE_INFINITY, -0.354));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(rareShopping, car, 0.0, POSITIVE_INFINITY, -0.042));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(rareShopping, car, 0.0, POSITIVE_INFINITY, -0.012));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(rareShopping, pt, 0.0, POSITIVE_INFINITY, -0.012));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(rareShopping, car, 0.0, POSITIVE_INFINITY, -0.271));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(rareShopping, pt, 0.0, POSITIVE_INFINITY, -0.271));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(rareShopping, pt, 0, POSITIVE_INFINITY, -0.027));
		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(rareShopping, walk, 0, POSITIVE_INFINITY, -0.546));

		// PRIMARY SCHOOL

		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(primarySchool, bike, 0, POSITIVE_INFINITY, -0.369));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(primarySchool, pt, 0, POSITIVE_INFINITY, -0.026));
		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(primarySchool, walk, 0, POSITIVE_INFINITY, -0.533));

		// GYMNASIUM

		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(gymnasium, bike, 0, POSITIVE_INFINITY, -0.215));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(gymnasium, car, 0.0, POSITIVE_INFINITY, -0.040));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(gymnasium, car, 0.0, POSITIVE_INFINITY, -0.146));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(gymnasium, pt, 0.0, POSITIVE_INFINITY, -0.146));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(gymnasium, car, 0.0, POSITIVE_INFINITY, -0.016));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(gymnasium, pt, 0.0, POSITIVE_INFINITY, -0.016));
		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(gymnasium, pt, 0, POSITIVE_INFINITY, -0.013));
		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(gymnasium, walk, 0, POSITIVE_INFINITY, -0.506));

		// ADULT EDUCATION

		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(adultEducation, bike, 0, POSITIVE_INFINITY, -0.088));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(adultEducation, car, 0.0, POSITIVE_INFINITY, -0.049));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(adultEducation, pt, 0, POSITIVE_INFINITY, -0.213));
		this.linCostCoeff_1_SEK
				.addNext(new ConcreteParameterPerStratum(adultEducation, pt, 0, POSITIVE_INFINITY, -0.012));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(adultEducation, pt, 0, POSITIVE_INFINITY, -0.012));
		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(adultEducation, walk, 0, POSITIVE_INFINITY, -0.209));

		// VISIT

		this.linDistanceCoeff_1_km.addNext(new ConcreteParameterPerStratum(visit, bike, 0, POSITIVE_INFINITY, -0.1989));
		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(visit, car, 0.0, POSITIVE_INFINITY, -0.02973));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(visit, car, 0.0, POSITIVE_INFINITY, -0.00965));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(visit, pt, 0.0, POSITIVE_INFINITY, -0.00965));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(visit, car, 0.0, POSITIVE_INFINITY, -0.2931));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(visit, pt, 0.0, POSITIVE_INFINITY, -0.2931));
		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(visit, pt, 0, POSITIVE_INFINITY, -0.01488));
		this.linDistanceCoeff_1_km.addNext(new ConcreteParameterPerStratum(visit, walk, 0, POSITIVE_INFINITY, -0.4218));

		// BUSINESS FROM HOME

		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(businessFromHome, bike, 0, POSITIVE_INFINITY, -0.1731));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(businessFromHome, car, 0.0, POSITIVE_INFINITY, -0.02843));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(businessFromHome, car, 0.0, POSITIVE_INFINITY, -0.3137));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(businessFromHome, pt, 0.0, POSITIVE_INFINITY, -0.3137));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(businessFromHome, pt, 0, POSITIVE_INFINITY, -0.009709));
		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(businessFromHome, walk, 0, POSITIVE_INFINITY, -0.4896));

		// GIVE A RIDE

		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(giveARide, bike, 0, POSITIVE_INFINITY, -0.2776));
		this.linTimeCoeff_1_min
				.addNext(new ConcreteParameterPerStratum(giveARide, car, 0.0, POSITIVE_INFINITY, -0.06061));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(giveARide, car, 0.0, POSITIVE_INFINITY, -0.6268));
		this.logCostCoeff_lnArgInSEK
				.addNext(new ConcreteParameterPerStratum(giveARide, pt, 0.0, POSITIVE_INFINITY, -0.6268));
		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(giveARide, pt, 0, POSITIVE_INFINITY, -0.02512));
		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(giveARide, walk, 0, POSITIVE_INFINITY, -0.4737));

		// SERVICE

		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(service, bike, 0, POSITIVE_INFINITY, -0.324));
		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(service, car, 0.0, POSITIVE_INFINITY, -0.0868));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(service, car, 0.0, POSITIVE_INFINITY, -0.0174));
		this.linCostCoeff_1_SEK.addNext(new ConcreteParameterPerStratum(service, pt, 0.0, POSITIVE_INFINITY, -0.0174));
		this.linTimeCoeff_1_min.addNext(new ConcreteParameterPerStratum(service, pt, 0, POSITIVE_INFINITY, -0.0301));
		this.linDistanceCoeff_1_km
				.addNext(new ConcreteParameterPerStratum(service, walk, 0, POSITIVE_INFINITY, -0.418));

	}

	// -------------------- PARAMETER GETTERS --------------------

	public double getLinTimeCoeff_1_min(final Purpose purpose, final String mode, final double income_money) {
		return this.linTimeCoeff_1_min.getOrZero(purpose, mode, income_money);
	}

	public double getLinCostCoeff_1_SEK(final Purpose purpose, final String mode, final double income_money) {
		return this.linCostCoeff_1_SEK.getOrZero(purpose, mode, income_money);
	}

	public double getLogCostCoeff_lnArgInSEK(final Purpose purpose, final String mode, final double income_money) {
		return this.logCostCoeff_lnArgInSEK.getOrZero(purpose, mode, income_money);
	}

	public double getLinDistanceCoeff_1_km(final Purpose purpose, final String mode, final double income_money) {
		return this.linDistanceCoeff_1_km.getOrZero(purpose, mode, income_money);
	}

	public double getLogDistanceCoeff_lnArgInKm(final Purpose purpose, final String mode, final double income_money) {
		return this.logDistanceCoeff_lnArgInKm.getOrZero(purpose, mode, income_money);
	}

	public double getScheduleDelayCostEarly_1_min(final Purpose purpose, final String mode, final Double income_money) {
		return this.scheduleDelayCostEarly_1_min.getOrZero(purpose, mode, income_money);
		// return this.linTimeCoeff_1_min.getOrZero(purpose, mode, income_money);
	}

	public double getScheduleDelayCostLate_1_min(final Purpose purpose, final String mode, final Double income_money) {
		return this.scheduleDelayCostLate_1_min.getOrZero(purpose, mode, income_money);
		// return this.linTimeCoeff_1_min.getOrZero(purpose, mode, income_money);
	}

	public double getScheduleDelayCostTooShort_1_min(final Purpose purpose, final String mode,
			final Double income_money) {
		return this.scheduleDelayTooShort_1_min.getOrZero(purpose, mode, income_money);
	}

	public double getScheduleDelayCostTooLong_1_min(final Purpose purpose, final String mode,
			final Double income_money) {
		return this.scheduleDelayTooLong_1_min.getOrZero(purpose, mode, income_money);
	}

	public double getStuckScore(final Purpose purpose, final String mode, final Double income_money) {
		// like being all-day late
		return this.getScheduleDelayCostLate_1_min(purpose, mode, income_money) * 24.0 * 60.0;
	}

	public double getModeASC(final Purpose purpose, final String mode, final Double income_money) {
		// return this.modeASC.getOrZero(purpose, mode, income_money);
		return this.modeASCs.getASC(mode);
	}

	public double getMonetaryDistanceCost_SEK_km() {
		// https://www.skatteverket.se/foretagochorganisationer/arbetsgivare/lonochersattning/traktamente.4.361dc8c15312eff6fd1703e.html?q=milers%C3%A4ttning+bil
		return 1.85;
	}

	public double getScheduleDelaySlack_min() {
		return 60.0;
	}

	public double getPTAccessEgressTimeMultiplier() {
		return 2.0;
	}

	public double getPTFirstWaitingTimeMultiplier() {
		return 1.5;
	}

	public double getPTInVehicleTimeMultiplier() {
		return 1.0;
	}

	public double getPTTransferTimeMultiplier() {
		return 1.5;
	}

	public double getPTTransferPenalty_min() {
		return 5.0;
	}
}
