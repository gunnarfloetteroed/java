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
import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.attributable.Attributes;

import stockholm.ihop2.regent.demandreading.PopulationCreator;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SampersAttributeUtils {

	// -------------------- CONSTANTS --------------------

	public enum ActivityAttribute {
		start_h, duration_h
	}

	// -------------------- DO NOT INSTANTIATE --------------------

	private SampersAttributeUtils() {
	}

	// -------------------- GETTERS AND SETTERS --------------------

	// INCOME

	public static double getIncome_SEK_yr(final Attributes attrs) {
		return (Integer) attrs.getAttribute(PopulationCreator.income_SEK_yr);
	}

	public static void setIncome_SEK_yr(final Attributes attrs, final Integer income_SEK_yr) {
		if (income_SEK_yr == null) {
			attrs.removeAttribute(PopulationCreator.income_SEK_yr);
		} else {
			attrs.putAttribute(PopulationCreator.income_SEK_yr, income_SEK_yr);
		}
	}

	public static double getIncome_SEK_yr(final Person person) {
		return getIncome_SEK_yr(person.getAttributes());
	}

	public static void setIncome_SEK_yr(final Person person, int income_SEK_yr) {
		setIncome_SEK_yr(person.getAttributes(), income_SEK_yr);
	}

	// PLANNED START

	public static Double getPlannedActivityStart_h(final Attributes attrs) {
		return (Double) attrs.getAttribute(ActivityAttribute.start_h.toString());
	}

	public static void setPlannedActivityStart_h(final Attributes attrs, final Double start_h) {
		if (start_h == null) {
			attrs.removeAttribute(ActivityAttribute.start_h.toString());
		} else {
			attrs.putAttribute(ActivityAttribute.start_h.toString(), start_h);
		}
	}

	public static Double getPlannedActivityStart_h(final Activity act) {
		return getPlannedActivityStart_h(act.getAttributes());
	}

	public static void setPlannedActivityStart_h(final Activity act, final Double start_h) {
		setPlannedActivityStart_h(act.getAttributes(), start_h);
	}

	public static Double getPlannedActivityStart_h(final SampersTour tour) {
		return getPlannedActivityStart_h(tour.getActivityAttributes());
	}

	public static void setPlannedActivityStart_h(final SampersTour tour, final Double start_h) {
		setPlannedActivityStart_h(tour.getActivityAttributes(), start_h);
	}

	// PLANNED DURATION

	public static Double getPlannedActivityDuration_h(final Attributes attrs) {
		return (Double) attrs.getAttribute(ActivityAttribute.duration_h.toString());
	}

	public static void setPlannedActivityDuration_h(final Attributes attrs, final Double duration_h) {
		if (duration_h == null) {
			attrs.removeAttribute(ActivityAttribute.duration_h.toString());
		} else {
			attrs.putAttribute(ActivityAttribute.duration_h.toString(), duration_h);
		}
	}

	public static Double getPlannedActivityDuration_h(final Activity act) {
		return getPlannedActivityDuration_h(act.getAttributes());
	}

	public static void setPlannedActivityDuration_h(final Activity act, final Double duration_h) {
		setPlannedActivityDuration_h(act.getAttributes(), duration_h);
	}

	public static Double getPlannedActivityDuration_h(final SampersTour tour) {
		return getPlannedActivityDuration_h(tour.getActivityAttributes());
	}

	public static void setPlannedActivityDuration_h(final SampersTour tour, final Double duration_h) {
		setPlannedActivityDuration_h(tour.getActivityAttributes(), duration_h);
	}

}
