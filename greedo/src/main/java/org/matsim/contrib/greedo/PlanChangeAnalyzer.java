/*
 * Greedo -- Equilibrium approximation for general-purpose multi-agent simulations.
 *
 * Copyright 2022 Gunnar Flötteröd
 * 
 *
 * This file is part of Greedo.
 *
 * Greedo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Greedo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Greedo.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@gmail.com
 *
 */
package org.matsim.contrib.greedo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class PlanChangeAnalyzer {

	// -------------------- CONSTANTS --------------------

	private boolean verbose = true;

	private final File weightsFile;

	private final double muInertia;

	// -------------------- MEMBERS --------------------

	private int iteration = -1;

	private final Map<Id<Person>, LinkedList<Integer>> personId2lastReplan;

	private double[][] lagIndexedDistance = new double[0][0];

	private Double optimalMu = null;

	private Double minMu = null;

	private Double maxMu = null;

	// -------------------- CONSTRUCTION --------------------

	PlanChangeAnalyzer(final Set<Id<Person>> allPersonIds, final double muInertia, final String outputFolder) {
		this.muInertia = muInertia;

		this.weightsFile = new File(outputFolder, "distances.log");
		if (this.weightsFile.exists()) {
			this.weightsFile.delete();
		}
		try {
			this.weightsFile.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.personId2lastReplan = new LinkedHashMap<>(
				allPersonIds.stream().collect(Collectors.toMap(id -> id, id -> new LinkedList<>())));
	}

	// -------------------- INTERNALS --------------------

	private void toFile(String line) {
		try {
			FileUtils.write(this.weightsFile, line.toString(), true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private double[] newReducedToDim(double[] vector, Integer dim) {
		int effDim = (dim == null ? vector.length : dim);
		return Arrays.copyOf(vector, effDim);
	}

	private double[][] newReducedToDims(double[][] matrix, Integer dim1, Integer dim2) {
		int effDim1 = (dim1 == null ? matrix.length : dim1);
		double[][] result = new double[effDim1][];
		for (int i = 0; i < effDim1; i++) {
			result[i] = this.newReducedToDim(matrix[i], dim2);
		}
		return result;
	}

	private Set<Id<Person>> allPersonIds() {
		return this.personId2lastReplan.keySet();
	}

	// -------------------- IMPLEMENTATION --------------------

	Double getOptimalMu() {
		return optimalMu;
	}

	Double getMinMu() {
		return minMu;
	}

	Double getMaxMu() {
		return maxMu;
	}

//	List<Double> updateMuAndCalculateWeights(final List<Map<Id<Person>, Double>> personId2newScoreOverReplications,
//			final List<Map<Id<Person>, Double>> personId2oldScoreOverReplications) {
//		final int _R = personId2newScoreOverReplications.size();
//		final int _N = personId2newScoreOverReplications.get(0).size();
//		double[][] lagIndexedGaps = new double[_N][_R];
//		for (int lag = 0; lag < _R; lag++) {
//			int n = 0;
//			for (Id<Person> personId : this.allPersonIds()) {
//				lagIndexedGaps[n][lag] += personId2newScoreOverReplications.get((_R - 1) - lag).get(personId)
//						- personId2oldScoreOverReplications.get((_R - 1) - lag).get(personId);
//				n++;
//			}
//		}
//		final List<Double> weights = Arrays.stream(this.updateMuAndCalculateLagIndexedWeightsForGaps(lagIndexedGaps))
//				.boxed().collect(Collectors.toList());
//		Collections.reverse(weights);
//		return weights;
//	}

	List<Double> updateMuAndCalculateWeights(final List<Map<Id<Person>, Double>> personId2newScoreOverReplications,
			final List<Map<Id<Person>, Double>> personId2oldScoreOverReplications) {
		final int _R = personId2newScoreOverReplications.size();
		final int _N = personId2newScoreOverReplications.get(0).size();
		double[][] lagIndexedGaps = new double[_N][_R];
		for (int r = 0; r < _R; r++) {
			int n = 0;
			for (Id<Person> personId : this.allPersonIds()) {
				lagIndexedGaps[n][r] += personId2newScoreOverReplications.get(r).get(personId)
						- personId2oldScoreOverReplications.get(r).get(personId);
				n++;
			}
		}
		return Arrays.stream(this.updateMuAndCalculateLagIndexedWeightsForGaps(lagIndexedGaps)).boxed()
				.collect(Collectors.toList());
	}

	double[] updateMuAndCalculateLagIndexedWeightsForGaps(final double[][] lagIndexedGaps) {
		final int dim = Math.min(lagIndexedGaps[0].length, this.lagIndexedDistance.length);
		if (dim <= 1) {
			final double[] lagIndexedWeights = new double[lagIndexedGaps[0].length];
			lagIndexedWeights[0] = 1.0;
			this.optimalMu = null;
			this.minMu = null;
			this.maxMu = null;
			return lagIndexedWeights;
		} else {
			final KernelSmoother ks = new KernelSmoother(this.newReducedToDims(lagIndexedGaps, null, dim),
					this.newReducedToDims(this.lagIndexedDistance, dim, dim));
			if (this.getOptimalMu() == null) {
				this.optimalMu = ks.optimalMu;
			} else {
				this.optimalMu = this.muInertia * this.getOptimalMu() + (1.0 - this.muInertia) * ks.optimalMu;
			}
			this.minMu = ks.minMu;
			this.maxMu = ks.maxMu;
			return Arrays.copyOf(ks.computeWeights(0, this.optimalMu, true), lagIndexedGaps[0].length);
		}
	}

	void registerReplanners(final Set<Id<Person>> replanners, final int maxLag) {

		this.iteration++;

		for (Id<Person> replannerId : replanners) {
			final LinkedList<Integer> replanIterations = this.personId2lastReplan.get(replannerId);
			replanIterations.addFirst(this.iteration);
			while (replanIterations.getLast() < this.iteration - maxLag) {
				replanIterations.removeLast();
			}
		}

		final int dim = Math.min(maxLag, this.iteration) + 1;
		this.lagIndexedDistance = new double[dim][dim];
		for (LinkedList<Integer> replanIterationList : this.personId2lastReplan.values()) {
			int[][] different = new int[dim][dim];
			for (int replanIteration : replanIterationList) {
				int replanLag = this.iteration - replanIteration;
				for (int lagUntil = 0; lagUntil <= replanLag; lagUntil++) {
					for (int lagAfter = replanLag + 1; lagAfter < dim; lagAfter++) {
						different[lagUntil][lagAfter] = 1;
						different[lagAfter][lagUntil] = 1;
					}
				}
			}
			for (int r = 0; r < dim; r++) {
				for (int s = 0; s < dim; s++) {
					this.lagIndexedDistance[r][s] += different[r][s];
				}
			}
		}

		if (this.verbose) {
			this.toFile("iteration = " + this.iteration + "\n");
			this.toFile("\n");
			for (int r = 0; r < dim; r++) {
				for (int s = 0; s < dim; s++) {
					this.toFile(this.lagIndexedDistance[r][s] + " ");
				}
				this.toFile("\n");
			}
			this.toFile("\n");
		}
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		int maxLag = 5;

		Id<Person> id1 = Id.createPersonId("id1");
		Id<Person> id2 = Id.createPersonId("id2");
		Set<Id<Person>> idSet0 = new LinkedHashSet<>(0);
		Set<Id<Person>> idSet1 = new LinkedHashSet<>(1);
		idSet1.add(id1);
		Set<Id<Person>> idSet2 = new LinkedHashSet<>(1);
		idSet2.add(id2);
		Set<Id<Person>> idSet12 = new LinkedHashSet<>(2);
		idSet12.add(id1);
		idSet12.add(id2);

		PlanChangeAnalyzer analyzer = new PlanChangeAnalyzer(idSet12, 0, "./");
		analyzer.verbose = true;

		analyzer.registerReplanners(idSet0, maxLag);
		analyzer.registerReplanners(idSet0, maxLag);
		analyzer.registerReplanners(idSet0, maxLag);
		analyzer.registerReplanners(idSet0, maxLag);
		analyzer.registerReplanners(idSet0, maxLag);

		analyzer.registerReplanners(idSet12, maxLag);

		analyzer.registerReplanners(idSet0, maxLag);
		analyzer.registerReplanners(idSet0, maxLag);
		analyzer.registerReplanners(idSet0, maxLag);
		analyzer.registerReplanners(idSet0, maxLag);
		analyzer.registerReplanners(idSet0, maxLag);

		System.out.println("... DONE");
	}
}
