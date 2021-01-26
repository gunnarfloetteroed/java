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

import static java.lang.Math.max;
import static java.lang.Math.pow;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class NRouteHeuristicReplannerIdentifier {

	private final String logFile = "nRoute.log";

	private final double costExponent;

	private final double randomReplanningProba;

	private double _J = 1.0; // aka "competition size"

	private Map<Id<Person>, Double> personId2cn = new LinkedHashMap<>();

	NRouteHeuristicReplannerIdentifier(final double costExponent, final double randomReplanningProba) {
		this.costExponent = costExponent;
		this.randomReplanningProba = randomReplanningProba;
		try {
			FileUtils.writeStringToFile(new File(logFile), "<deltaUn0>\treplanningRate\t<cn>\tL\n", false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void update(final Set<Id<Person>> replanners, final Map<Id<Person>, Double> personId2DeltaUn0,
			final double cnStepSize, final double lambdaTarget) {

		final double lambdaRealized = ((double) replanners.size()) / personId2DeltaUn0.size();
		this._J = Math.max(1.0, this._J + (lambdaRealized - lambdaTarget));

		final double eps = this.randomReplanningProba;
		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
			final Id<Person> personId = entry.getKey();
			final double deltaUn0Raised = pow(max(entry.getValue(), 0.0), 1.0 / this.costExponent);
			final double _Dn = (replanners.contains(personId) ? deltaUn0Raised : 0.0);
			final double deltaCn = cnStepSize
					* ((1.0 - eps) * _Dn + eps * lambdaTarget * deltaUn0Raised - deltaUn0Raised / this._J);
			this.personId2cn.put(personId, this.personId2cn.getOrDefault(personId, 0.0) + deltaCn);
		}

		final OptionalDouble deltaUn0 = personId2DeltaUn0.values().stream().mapToDouble(x -> x).average();
		final OptionalDouble cn = this.personId2cn.values().stream().mapToDouble(x -> x).average();

		try {
			FileUtils.writeStringToFile(new File("nRoute.log"),
					deltaUn0.getAsDouble() + "\t" + lambdaRealized + "\t" + cn.getAsDouble() + "\t" + this._J + "\n",
					true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Set<Id<Person>> identifyReplanners(final Map<Id<Person>, Double> personId2DeltaUn0,
			final double lambdaTarget) {
		final Set<Id<Person>> replanners = new LinkedHashSet<>();
		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
			final Id<Person> personId = entry.getKey();
			final double deltaUn0 = entry.getValue();
			if (MatsimRandom.getRandom().nextDouble() < this.randomReplanningProba) {
				if (MatsimRandom.getRandom().nextDouble() < lambdaTarget) {
					replanners.add(personId);
				}
			} else {
				if (deltaUn0 >= this.personId2cn.getOrDefault(personId, 0.0)) {
					replanners.add(personId);
				}
			}
		}
		return replanners;
	}

	static Map<Id<Person>, Double> newMap(Id<Person> id, double val) {
		Map<Id<Person>, Double> result = new LinkedHashMap<>(1);
		result.put(id, val);
		return result;
	}

	public static void main(String[] args) {
		Id<Person> id = Id.createPersonId("dummy");
		NRouteHeuristicReplannerIdentifier nRoute = new NRouteHeuristicReplannerIdentifier(2.0, 0.5);
		for (int k = 1; k < 1000; k++) {
			Map<Id<Person>, Double> deltaUn0 = newMap(id, Math.random() * 10);
			Set<Id<Person>> replanners = nRoute.identifyReplanners(deltaUn0, 1.0 / Math.sqrt(k));
			nRoute.update(replanners, deltaUn0, 1.0 / Math.sqrt(k), 0.5);
			System.out.println(("" + nRoute.personId2cn.get(id)).replace(".", ","));
		}
	}
}
