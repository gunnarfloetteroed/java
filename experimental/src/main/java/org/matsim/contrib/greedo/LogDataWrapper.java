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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LogDataWrapper {

	private final GreedoConfigGroup greedoConfig;

	private final Utilities.SummaryStatistics utilitySummaryStatistics;

	private final ReplannerIdentifier.SummaryStatistics replanningSummaryStatistics;

	private final int iteration;

	public LogDataWrapper(final GreedoConfigGroup greedoConfig,
			final Utilities.SummaryStatistics utilitySummaryStatistics,
			final ReplannerIdentifier.SummaryStatistics replanningSummaryStatistics, final int iteration) {
		this.greedoConfig = greedoConfig;
		this.utilitySummaryStatistics = utilitySummaryStatistics;
		this.replanningSummaryStatistics = replanningSummaryStatistics;
		this.iteration = iteration;
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

	public int getIteration() {
		return this.iteration;
	}

	public ReplannerIdentifier.SummaryStatistics getReplanningSummaryStatistics() {
		return this.replanningSummaryStatistics;
	}
}
