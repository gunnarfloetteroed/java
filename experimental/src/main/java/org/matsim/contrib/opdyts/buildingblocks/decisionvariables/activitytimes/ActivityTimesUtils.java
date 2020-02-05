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

import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariable;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariableBuilder;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.scalar.ScalarRandomizer;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.utils.misc.Time;

import floetteroed.utilities.Units;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ActivityTimesUtils {

	public static CompositeDecisionVariable newAllActivityTimesDecisionVariable(final Config config,
			final double timeVariationStepSize_s, final double searchStageExponent) {
		final CompositeDecisionVariableBuilder builder = new CompositeDecisionVariableBuilder();
		for (ActivityParams actParams : config.planCalcScore().getActivityParams()) {
			if (!"dummy".equals(actParams.getActivityType())) { // magic act in PlanCalcScoreConfigGroup
				if (!Time.isUndefinedTime(actParams.getOpeningTime())) {
					final OpeningTime openingTime = new OpeningTime(config, actParams.getActivityType(),
							actParams.getOpeningTime());
					openingTime.setMinValue_s(0);
					openingTime.setMaxValue_s(Units.S_PER_D);
					builder.add(openingTime,
							new ScalarRandomizer<OpeningTime>(timeVariationStepSize_s, searchStageExponent));
				}
				if (!Time.isUndefinedTime(actParams.getClosingTime())) {
					final ClosingTime closingTime = new ClosingTime(config, actParams.getActivityType(),
							actParams.getClosingTime());
					closingTime.setMinValue_s(0);
					closingTime.setMaxValue_s(Units.S_PER_D);
					builder.add(closingTime,
							new ScalarRandomizer<ClosingTime>(timeVariationStepSize_s, searchStageExponent));
				}
				if (!Time.isUndefinedTime(actParams.getTypicalDuration())) {
					final TypicalDuration typicalDuration = new TypicalDuration(config, actParams.getActivityType(),
							actParams.getTypicalDuration());
					typicalDuration.setMinValue_s(15 * Units.S_PER_MIN);
					builder.add(typicalDuration,
							new ScalarRandomizer<TypicalDuration>(timeVariationStepSize_s, searchStageExponent));
				}
			}
		}
		return builder.buildDecisionVariable();
	}

}
