package org.matsim.contrib.greedo.logging;

import org.matsim.contrib.greedo.LogDataWrapper;

public class LambdaRealized extends PopulationAverageStatistic {

	@Override
	public String value(LogDataWrapper arg0) {
		return this.averageOrEmpty(new Double(arg0.getReplanningSummaryStatistics().numberOfReplanners), arg0.getReplanningSummaryStatistics().getNumberOfReplanningCandidates());
	}

}
