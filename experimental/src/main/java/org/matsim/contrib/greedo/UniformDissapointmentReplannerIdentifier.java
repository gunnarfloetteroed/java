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
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class UniformDissapointmentReplannerIdentifier {

	// private final double filtration = 0.1;

	private class ScoreHistogram {

		private static final String histFileName = "scoreHist.txt";

		private int[] frequencies = null;

		ScoreHistogram() {
			this.clear();
			try {
				FileUtils.writeStringToFile(new File(histFileName), "", false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		void clear() {
			this.frequencies = new int[10];
		}

		void add(final double deltaScore) {
			if (deltaScore < 0) {
				Logger.getLogger(this.getClass()).warn("negative deltaScore: " + deltaScore);
				return;
			}

			final int bin = (int) deltaScore;
			if ((this.frequencies.length - 1) < bin) {
				final int[] newFrequencies = new int[bin + 10];
				System.arraycopy(this.frequencies, 0, newFrequencies, 0, this.frequencies.length);
				this.frequencies = newFrequencies;
			}
			this.frequencies[bin]++;
		}

		void dumpAndClear() {
			if (this.frequencies.length > 0) {
				final StringBuffer line = new StringBuffer("" + this.frequencies[0]);
				for (int i = 1; i < this.frequencies.length; i++) {
					line.append(",");
					line.append(this.frequencies[i]);
				}
				line.append("\n");
				try {
					FileUtils.writeStringToFile(new File(histFileName), line.toString(), true);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			this.frequencies = new int[0];
		}
	}

	private final static String logFile = "accLog.txt";

	private ScoreHistogram hist = null;

	private Map<Id<Person>, Double> personId2meanDeltaUn0 = new LinkedHashMap<>();
//	private Map<Id<Person>, Double> personId2cn = new LinkedHashMap<>();

	private double mu = 0.0;

	UniformDissapointmentReplannerIdentifier() {
		try {
			FileUtils.writeStringToFile(new File(this.logFile),
					"<deltaUn0>\t<deltaXn(repl)>\t<deltaXn(non-repl)>\treplanningRate\ttargetRate\tmu\n", false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.hist = new ScoreHistogram();
	}

	public void update(final Set<Id<Person>> replanners, final Map<Id<Person>, Double> personId2DeltaUn0,
			final double stepSize, final double lambdaTarget,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> hypotheticalSlotUsageIndicators) {

//		double meanDn = 0.0;
//		for (Id<Person> replannerId : replanners) {
//			meanDn += personId2DeltaUn0.get(replannerId);
//		}
//		meanDn /= personId2DeltaUn0.size();
		final double meanLambda = ((double) replanners.size()) / personId2DeltaUn0.size();
		this.mu = stepSize * (meanLambda - lambdaTarget) + this.mu;
//		this.mu = (this.mu + 1.0) * Math.exp(stepSize * (meanLambda - lambdaTarget)) - 1.0;

//		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
//			final Id<Person> personId = entry.getKey();
//			final double deltaUn0 = entry.getValue();
//			final double _Dn = (replanners.contains(personId) ? deltaUn0 : 0.0);
//			final double deltaCn = cnStepSize * (_Dn - meanDn);
//			this.personId2cn.put(personId, Math.max(this.personId2cn.getOrDefault(personId, 0.0) + deltaCn, 0));
//		}

		int replannerCnt = 0;
		int nonReplannerCnt = 0;
		double meanSizeReplanner = 0.0;
		double meanSizeNonReplanner = 0.0;
		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
			final Id<Person> personId = entry.getKey();
			final double newDeltaUn0 = entry.getValue();
			final Double meanDeltaUn0 = this.personId2meanDeltaUn0.get(personId);
			if (meanDeltaUn0 == null) {
				this.personId2meanDeltaUn0.put(personId, newDeltaUn0);
			} else {
				this.personId2meanDeltaUn0.put(personId, stepSize * newDeltaUn0 + (1.0 - stepSize) * meanDeltaUn0);
			}
			this.hist.add(newDeltaUn0);

			if (hypotheticalSlotUsageIndicators != null) {
				if (replanners.contains(personId)) {
					if (hypotheticalSlotUsageIndicators.get(personId) != null) {
						meanSizeReplanner += hypotheticalSlotUsageIndicators.get(personId).size();
					}
					replannerCnt++;
				} else {
					if (hypotheticalSlotUsageIndicators.get(personId) != null) {
						meanSizeNonReplanner += hypotheticalSlotUsageIndicators.get(personId).size();
					}
					nonReplannerCnt++;
				}
			}
		}
		meanSizeReplanner /= replannerCnt;
		meanSizeNonReplanner /= nonReplannerCnt;

//		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
//			final Id<Person> personId = entry.getKey();
//			final double _Lambdan = (replanners.contains(personId) ? 1.0 : 0.0);
//			final double deltaCn = cnStepSize * (_Lambdan - lambdaTarget);
//			this.personId2cn.put(personId, Math.max(this.personId2cn.getOrDefault(personId, 0.0) + deltaCn, 0));
//		}

		final OptionalDouble meanDeltaUn0 = personId2DeltaUn0.values().stream().mapToDouble(x -> x).average();
//		final BasicStatistics cnStats = new BasicStatistics(this.personId2cn.values());

		try {
			FileUtils.writeStringToFile(
					new File(this.logFile), meanDeltaUn0.getAsDouble() + "\t" + meanSizeReplanner + "\t"
							+ meanSizeNonReplanner + "\t" + meanLambda + "\t" + lambdaTarget + "\t" + this.mu + "\n",
					true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.hist.dumpAndClear();
	}

	public Set<Id<Person>> identifyReplanners(final Map<Id<Person>, Double> personId2DeltaUn0,
			final double lambdaTarget) {
		final Set<Id<Person>> replanners = new LinkedHashSet<>();
		for (Map.Entry<Id<Person>, Double> entry : personId2DeltaUn0.entrySet()) {
			final Id<Person> personId = entry.getKey();
			final double deltaUn0 = entry.getValue();
//			if (deltaUn0 >= lambdaTarget / (1.0 - lambdaTarget) * this.personId2meanDeltaUn0.getOrDefault(personId, 0.0)
//					+ this.mu) {
			if (deltaUn0 >= this.personId2meanDeltaUn0.getOrDefault(personId, 0.0) + this.mu) {
				replanners.add(personId);
			}
		}
		return replanners;
	}

//	static Map<Id<Person>, Double> newMap(Id<Person> id, double val) {
//		Map<Id<Person>, Double> result = new LinkedHashMap<>(1);
//		result.put(id, val);
//		return result;
//	}
//
//	public static void main(String[] args) {
//		Id<Person> id = Id.createPersonId("dummy");
//		UniformDissapointmentReplannerIdentifier nRoute = new UniformDissapointmentReplannerIdentifier(2.0, 0.5);
//		for (int k = 1; k < 1000; k++) {
//			Map<Id<Person>, Double> deltaUn0 = newMap(id, Math.random() * 10);
//			Set<Id<Person>> replanners = nRoute.identifyReplanners(deltaUn0, 1.0 / Math.sqrt(k));
//			nRoute.update(replanners, deltaUn0, 1.0 / Math.sqrt(k), 0.5);
//			System.out.println(("" + nRoute.personId2cn.get(id)).replace(".", ","));
//		}
//	}
}
