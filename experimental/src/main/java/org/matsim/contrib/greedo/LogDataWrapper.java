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
package org.matsim.contrib.greedo;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import floetteroed.utilities.math.BasicStatistics;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LogDataWrapper {

	private final GreedoConfigGroup greedoConfig;

	private final Utilities.SummaryStatistics utilitySummaryStatistics;

	private final ReplannerIdentifier.SummaryStatistics replanningSummaryStatistics;

	private final DisappointmentAnalyzer.SummaryStatistics disappointmentSummaryStatistics;

	private final BasicStatistics cnStats;

	private final int iteration;

	public LogDataWrapper(final GreedoConfigGroup greedoConfig,
			final Utilities.SummaryStatistics utilitySummaryStatistics,
			final ReplannerIdentifier.SummaryStatistics replanningSummaryStatistics,
			final DisappointmentAnalyzer.SummaryStatistics disappointmentSummaryStatistics, final int iteration,
			final Collection<Double> cnValues) {
		this.greedoConfig = greedoConfig;
		this.utilitySummaryStatistics = utilitySummaryStatistics;
		this.replanningSummaryStatistics = replanningSummaryStatistics;
		this.disappointmentSummaryStatistics = disappointmentSummaryStatistics;
		this.iteration = iteration;

		this.cnStats = new BasicStatistics();
		this.cnStats.addAll(cnValues);
	}

	public GreedoConfigGroup getGreedoConfig() {
		return this.greedoConfig;
	}

	// The return type is package private.
	Utilities.SummaryStatistics getUtilitySummaryStatistics() {
		return this.utilitySummaryStatistics;
	}

	public Map<Id<Person>, Double> getPersonId2expectedUtilityChange() {
		return this.utilitySummaryStatistics.personId2expectedUtilityChange;
	}

	public Double getRealizedUtilitySum() {
		return this.utilitySummaryStatistics.realizedUtilitySum;
	}

	public Double getRealizedUtilityChangeSum() {
		return this.utilitySummaryStatistics.realizedUtilityChangeSum;
	}

	public ReplannerIdentifier.SummaryStatistics getReplanningSummaryStatistics() {
		return this.replanningSummaryStatistics;
	}

	public DisappointmentAnalyzer.SummaryStatistics getDisappoinmentSummaryStatistics() {
		return this.disappointmentSummaryStatistics;
	}

	public int getIteration() {
		return this.iteration;
	}

	public Double getCnMean() {
		if (this.cnStats.size() > 0) {
			return this.cnStats.getAvg();
		} else {
			return null;
		}
	}

	public Double getCnStddev() {
		if (this.cnStats.size() > 1) {
			return this.cnStats.getStddev();
		} else {
			return null;
		}
	}
}
