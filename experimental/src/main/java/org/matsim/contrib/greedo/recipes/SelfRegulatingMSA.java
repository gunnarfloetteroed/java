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
package org.matsim.contrib.greedo.recipes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.LogDataWrapper;
import org.matsim.core.gbl.MatsimRandom;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SelfRegulatingMSA implements ReplannerIdentifierRecipe {

	// -------------------- CONSTANTS --------------------

	private final double denominatorIncreaseSuccess;

	private final double denominatorIncreaseFailure;

	// -------------------- MEMBERS --------------------

	private double denominator = 1.0;

	// -------------------- CONSTRUCTION --------------------

	public SelfRegulatingMSA(final double denominatorIncreaseSuccess, final double denominatorIncreaseFailure) {
		this.denominatorIncreaseSuccess = denominatorIncreaseSuccess;
		this.denominatorIncreaseFailure = denominatorIncreaseFailure;
	}

	// --------------- IMPLEMENTATION OF ReplannerIdentifierRecipe ---------------

	@Override
	public void update(final LogDataWrapper logDataWrapper) {
		final Double lastChange = logDataWrapper.getRealizedUtilityChangeSum();
		if (lastChange != null) {
			this.denominator += ((lastChange > 0) ? this.denominatorIncreaseSuccess : this.denominatorIncreaseFailure);
		}
	}

	@Override
	public boolean isReplanner(final Id<Person> personId, final double anticipatedUtilityChange) {
		return (MatsimRandom.getRandom().nextDouble() < (1.0 / this.denominator));
	}

	@Override
	public String getDeployedRecipeName() {
		return this.getClass().getSimpleName();
	}
}
