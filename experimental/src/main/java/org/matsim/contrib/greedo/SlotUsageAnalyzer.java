/*
 * Copyright 2021 Gunnar Flötteröd
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

import static floetteroed.utilities.DynamicDataUtils.innerProduct;
import static floetteroed.utilities.DynamicDataUtils.newWeightedSum;
import static floetteroed.utilities.DynamicDataUtils.sumOfEntries2;
import static java.lang.Math.sqrt;
import static org.matsim.contrib.greedo.datastructures.DynamicDataUtils2.getMaxNorm;
import static org.matsim.contrib.greedo.datastructures.SlotUsageUtilities.addIndicatorsToTotalsTreatingNullAsZero;
import static org.matsim.contrib.greedo.datastructures.SlotUsageUtilities.newTotals;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.math.Regression;
import floetteroed.utilities.math.Vector;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SlotUsageAnalyzer {

	private final String logFile = "x.log";

	private final TimeDiscretization timeDiscr;

	private final Population pop;

	private final Regression regr3;
	private final Regression regr2eucl;
	private final Regression regr2inf;

	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastAnticipatedIndicators = null;
	// private Map<Id<Person>,SpaceTimeIndicators<Id<?>>>
	// secondLastAnticipatedIndicators = null;

	private Set<Id<Person>> lastReplannerIds = null;

	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> lastRealizedIndicators = null;
	private Map<Id<Person>, SpaceTimeIndicators<Id<?>>> secondLastRealizedIndicators = null;

	SlotUsageAnalyzer(final GreedoConfigGroup config, final Population pop) {
		this.timeDiscr = config.newTimeDiscretization();
		this.pop = pop;
		try {
			FileUtils.writeStringToFile(new File(this.logFile),
					"|dX_antic|_inf\t|dX_real|_inf\t|dX_antic|_2\t|dX_real|_2\t"
							+ "<dX_antic0,dX_real0>\t|z_antic|_inf\t"
							+ "coeff3(z)\tcoeff3(#repl)\tconst3\tcoeff2a(|dX_antic|_2)\tconst2a\tcoeff2b(z)\tconst2b\n",
					false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.regr3 = new Regression(0.95, 3);
		this.regr2eucl = new Regression(0.95, 2);
		this.regr2inf = new Regression(0.95, 2);
	}

	void setAnticipatedSlotUsages(final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> anticipatedIndicators,
			final Set<Id<Person>> replannerIds) {
		// this.secondLastAnticipatedIndicators = this.lastAnticipatedIndicators;
		this.lastAnticipatedIndicators = new LinkedHashMap<>(anticipatedIndicators);
		this.lastReplannerIds = new LinkedHashSet<>(replannerIds);
	}

	void setRealizedSlotUsages(final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> realizedIndicators) {
		this.secondLastRealizedIndicators = this.lastRealizedIndicators;
		this.lastRealizedIndicators = new LinkedHashMap<>(realizedIndicators);

		if (this.secondLastRealizedIndicators != null) {

			final DynamicData<Id<?>> xReal0 = newTotals(this.timeDiscr, this.secondLastRealizedIndicators.values(),
					false, false);
			final DynamicData<Id<?>> xReal1 = newTotals(this.timeDiscr, this.lastRealizedIndicators.values(), false,
					false);

			final DynamicData<Id<?>> xAntic1 = new DynamicData<>(this.timeDiscr);
			for (Id<Person> personId : this.pop.getPersons().keySet()) {
				if (this.lastReplannerIds.contains(personId)) {
					addIndicatorsToTotalsTreatingNullAsZero(xAntic1, this.lastAnticipatedIndicators.get(personId), 1.0,
							false, false);
				} else {
					addIndicatorsToTotalsTreatingNullAsZero(xAntic1, this.secondLastRealizedIndicators.get(personId),
							1.0, false, false);
				}
			}

			final DynamicData<Id<?>> zAntic1 = new DynamicData<>(this.timeDiscr);
			for (Id<Person> replannerId : this.lastReplannerIds) {
				addIndicatorsToTotalsTreatingNullAsZero(zAntic1, this.lastAnticipatedIndicators.get(replannerId), 1.0,
						false, false);
			}

			final DynamicData<Id<?>> anticipatedDeltaX = newWeightedSum(xAntic1, 1.0, xReal0, -1.0);
			final double anticipatedDeltaXEuclNorm = sqrt(sumOfEntries2(anticipatedDeltaX));
			final double anticipatedDeltaXMaxNorm = getMaxNorm(anticipatedDeltaX);
			final double anticipatedZMaxNorm = getMaxNorm(zAntic1);

			final DynamicData<Id<?>> realizedDeltaX = newWeightedSum(xReal1, 1.0, xReal0, -1.0);
			final double realizedDeltaXEuclNorm = sqrt(sumOfEntries2(realizedDeltaX));
			final double relizedDeltaXMaxNorm = getMaxNorm(realizedDeltaX);

			final double normalizedInnerProd = innerProduct(anticipatedDeltaX, realizedDeltaX)
					/ anticipatedDeltaXEuclNorm / realizedDeltaXEuclNorm;

			this.regr3.update(new Vector(anticipatedZMaxNorm, this.lastReplannerIds.size(), 1.0),
					realizedDeltaXEuclNorm);
			this.regr2eucl.update(new Vector(anticipatedDeltaXEuclNorm, 1.0), realizedDeltaXEuclNorm);
			this.regr2inf.update(new Vector(anticipatedZMaxNorm, 1.0), realizedDeltaXEuclNorm);

			try {
				FileUtils.writeStringToFile(new File(this.logFile), (anticipatedDeltaXMaxNorm + "\t"
						+ relizedDeltaXMaxNorm + "\t" + anticipatedDeltaXEuclNorm + "\t" + realizedDeltaXEuclNorm + "\t"
						+ normalizedInnerProd + "\t" + anticipatedZMaxNorm + "\t" + this.regr3.getCoefficients().get(0)
						+ "\t" + this.regr3.getCoefficients().get(1) + "\t" + this.regr3.getCoefficients().get(2) + "\t"
						+ this.regr2eucl.getCoefficients().get(0) + "\t" + this.regr2eucl.getCoefficients().get(1)
						+ "\t" + this.regr2inf.getCoefficients().get(0) + "\t" + this.regr2inf.getCoefficients().get(1)
						+ "\n").replace('.', ','), true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
