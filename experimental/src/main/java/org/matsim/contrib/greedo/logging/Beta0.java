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
package org.matsim.contrib.greedo.logging;

import org.matsim.contrib.greedo.LogDataWrapper;

import floetteroed.utilities.statisticslogging.Statistic;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Beta0 implements Statistic<LogDataWrapper> {

	@Override
	public String label() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String value(LogDataWrapper arg0) {
		final Double lambdaBar = arg0.getReplanningSummaryStatistics().lambdaBar;
		final Double deltaX2 = arg0.getReplanningSummaryStatistics().sumOfWeightedReplannerCountDifferences2;
		final Double deltaU = arg0.getReplanningSummaryStatistics().sumOfReplannerUtilityChanges;
		final Double deltaUStar = arg0.getRealizedUtilityChangeSum();
		if ((lambdaBar != null) && (deltaX2 != null) && (deltaU != null) && (deltaUStar != null)) {
			return Statistic.toString(deltaX2 / lambdaBar / (deltaU - deltaUStar));
		} else {
			return "";
		}
	}

}

