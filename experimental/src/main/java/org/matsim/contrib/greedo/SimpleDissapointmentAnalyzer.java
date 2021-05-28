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

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import floetteroed.utilities.math.BasicStatistics;
import floetteroed.utilities.math.Regression;
import floetteroed.utilities.math.Vector;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SimpleDissapointmentAnalyzer {

	private final static String logFile = "accLog.txt";

	private Regression regr = null;

	private Map<Id<Person>, Double> personId2meanDeltaUn0 = new LinkedHashMap<>();
	private Map<Id<Person>, Double> replannerId2meanDeltaUn0 = new LinkedHashMap<>();
	private Map<Id<Person>, Double> replannerId2meanDeltaUn = new LinkedHashMap<>();

	public SimpleDissapointmentAnalyzer() {
		try {
			FileUtils.writeStringToFile(new File(this.logFile),
					"<deltaUn0>\treplanningRate\td1\ta\tb\t<err>\tstdev<err>\n", false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void updateMeanMap(final Id<Person> personId, final double innovation, final double innoWeight,
			final Map<Id<Person>, Double> meanMap) {
		meanMap.put(personId, innoWeight * innovation + (1.0 - innoWeight) * meanMap.getOrDefault(personId, 0.0));
	}

	void update(final Set<Id<Person>> replanners, final Map<Id<Person>, Double> personId2DeltaUn0,
			final Map<Id<Person>, Double> personId2DeltaUn, final double innoWeight) {
		this.regr = new Regression(1.0, 2);

		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
			updateMeanMap(entry.getKey(), entry.getValue(), innoWeight, this.personId2meanDeltaUn0);
		}

		for (Id<Person> replannerId : replanners) {
			final Double deltaUn0 = personId2DeltaUn0.get(replannerId);
			final Double deltaUn = personId2DeltaUn.get(replannerId);
			updateMeanMap(replannerId, deltaUn0, innoWeight, this.replannerId2meanDeltaUn0);
			updateMeanMap(replannerId, deltaUn, innoWeight, this.replannerId2meanDeltaUn);
			this.regr.update(new Vector(deltaUn0, 1.0), deltaUn0 - deltaUn);
		}

		final BasicStatistics d1Error = new BasicStatistics();
		for (Id<Person> replannerId : replanners) {
			final Double deltaUn0 = personId2DeltaUn0.get(replannerId);
			final Double deltaUn = personId2DeltaUn.get(replannerId);
			d1Error.add((deltaUn0 - deltaUn) - this.regr.predict(new Vector(deltaUn0, 1.0)));
		}

		final OptionalDouble meanDeltaUn0 = personId2DeltaUn0.values().stream().mapToDouble(x -> x).average();
		final double meanLambda = ((double) replanners.size()) / personId2DeltaUn0.size();

		final OptionalDouble meanMeanDeltaUn0 = this.replannerId2meanDeltaUn0.values().stream().mapToDouble(x -> x)
				.average();
		final OptionalDouble meanMeanDeltaUn = this.replannerId2meanDeltaUn.values().stream().mapToDouble(x -> x)
				.average();

		try {
			FileUtils.writeStringToFile(new File(this.logFile),
					meanDeltaUn0.getAsDouble() + "\t" + meanLambda + "\t"
							+ (meanMeanDeltaUn0.getAsDouble() - meanMeanDeltaUn.getAsDouble()) + "\t"
							+ this.regr.getCoefficients().get(0) + "\t" + this.regr.getCoefficients().get(1) + "\t"
							+ d1Error.getAvg() + "\t" + d1Error.getStddev() + "\n",
					true);
		} catch (IOException e) {
			Logger.getLogger(this.getClass()).warn(e.toString());
		}

	}

//	private double d1(final Id<Person> personId) {
//		return (this.replannerId2meanDeltaUn0.getOrDefault(personId, 0.0)
//				- this.replannerId2meanDeltaUn.getOrDefault(personId, 0.0));
//	}

	Set<Id<Person>> selectReplanners(final Map<Id<Person>, Double> personId2DeltaUn0) {
		final Set<Id<Person>> result = new LinkedHashSet<>();
		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
			final Id<Person> personId = entry.getKey();
//			if (entry.getValue() >= ((this.regr == null) ? 0.0
//					: this.regr.predict(new Vector(this.personId2meanDeltaUn0.getOrDefault(personId, 0.0), 1.0)))) {
			if (entry.getValue() >= this.personId2meanDeltaUn0.getOrDefault(personId, 0.0) - 1.0) {
				result.add(personId);
			}
		}
		return result;
	}

}
