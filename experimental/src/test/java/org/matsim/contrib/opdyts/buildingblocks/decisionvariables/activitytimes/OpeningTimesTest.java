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

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class OpeningTimesTest {

	static Config newInitializedTestConfig() {
		Config config = ConfigUtils.createConfig();
		{
			ActivityParams actParams = new ActivityParams("h");
			// actParams.setOpeningTime(Time.getUndefinedTime());
			// actParams.setClosingTime(Time.getUndefinedTime());
			config.planCalcScore().addActivityParams(actParams);
		}
		{
			ActivityParams actParams = new ActivityParams("w");
			actParams.setOpeningTime(6 * 3600);
			actParams.setClosingTime(18 * 3600);
			config.planCalcScore().addActivityParams(actParams);
		}
		{
			ActivityParams actParams = new ActivityParams("o1");
			// actParams.setOpeningTime(Time.getUndefinedTime());
			actParams.setClosingTime(20 * 3600);
			config.planCalcScore().addActivityParams(actParams);
		}
		{
			ActivityParams actParams = new ActivityParams("o2");
			actParams.setOpeningTime(8 * 3600);
			// actParams.setClosingTime(Time.getUndefinedTime());
			config.planCalcScore().addActivityParams(actParams);
		}
		return config;
	}

}
