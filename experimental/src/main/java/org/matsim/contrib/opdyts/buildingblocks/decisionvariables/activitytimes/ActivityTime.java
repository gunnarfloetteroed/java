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
package org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes;

import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar.AbstractScalarDecisionVariable;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

import floetteroed.utilities.Time;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public abstract class ActivityTime<U extends ActivityTime<U>> extends AbstractScalarDecisionVariable<U> {

	// -------------------- CONSTANTS --------------------

	private final Config config;

	private final String activityType;

	// -------------------- MEMBERS --------------------

	// private double value_s;

	// -------------------- CONSTRUCTION --------------------

	public ActivityTime(final Config config, final String activityType, final double value_s) {
		super(value_s);
		this.config = config;
		this.activityType = activityType;
		// this.value_s = value_s;
	}

	// -------------------- GETTERS AND SETTERS --------------------

	public Config getConfig() {
		return this.config;
	}

	public String getActivityType() {
		return this.activityType;
	}

	public ActivityParams getActivityParams() {
		return this.getConfig().planCalcScore().getActivityParams(this.getActivityType());
	}

	// ---------- PARTIAL IMPLEMENTATION OF ScalarDecisionVariable ----------

	// @Override
	// public void setValue(double value_s) {
	// this.value_s = value_s;
	// }

	// @Override
	// public double getValue() {
	// return this.value_s;
	// }

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + this.getActivityType() + ","
				+ Time.strFromSec((int) this.getValue()) + ")";
	}
}
