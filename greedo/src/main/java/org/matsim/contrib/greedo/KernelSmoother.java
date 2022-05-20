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

import java.util.Arrays;
import java.util.Random;

import org.apache.log4j.Logger;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class KernelSmoother {

	private static Logger log = Logger.getLogger(KernelSmoother.class);

	// -------------------- MEMBERS --------------------

	// data[n][r] is realization n in distance bin r
	private final double data[][];

	// dist[r][s] is distance between bin r and bin s
	private final double dist[][];

	private final int sampleCnt;
	private final int binCnt;

	// minDist[r] is the smallest distance of bin r to any other bin
	private final double minDist[];

	final Double optimalMu;
	final Double minMu;
	final Double maxMu;

	// -------------------- CONSTRUCTION --------------------

	KernelSmoother(double[][] data, double[][] distances) {
		this.data = data;
		this.dist = distances;

		this.sampleCnt = data.length;
		this.binCnt = data[0].length;

		if (!this.dimensionsOK(data, this.sampleCnt, this.binCnt)) {
			throw new RuntimeException(
					"data matrix does not have dimensions " + this.sampleCnt + " x " + this.binCnt + " throughout");
		}
		if (!this.dimensionsOK(distances, this.binCnt, this.binCnt)) {
			throw new RuntimeException(
					"distance matrix does not have dimensions " + this.binCnt + " x " + this.binCnt + " throughout");
		}

		if ((this.sampleCnt == 0) || (this.binCnt == 0)) {

			throw new RuntimeException("No data.");

		} else if (this.binCnt == 1) {

			log.info("Just one data bin. Setting all estimation parameters to null.");
			this.minDist = null;
			this.optimalMu = null;
			this.minMu = null;
			this.maxMu = null;

		} else {

			double maxMinDist = Double.NEGATIVE_INFINITY;
			this.minDist = new double[this.binCnt];
			for (int i = 0; i < this.binCnt; i++) {
				this.minDist[i] = Double.POSITIVE_INFINITY;
				for (int j = 0; j < this.binCnt; j++) {
					if (j != i) {
						this.minDist[i] = Math.min(this.minDist[i], distances[i][j]);
					}
				}
				maxMinDist = Math.max(maxMinDist, this.minDist[i]);
			}
			this.minMu = 0.0;
			this.maxMu = Math.min(1e6, 10.0 / maxMinDist); // The unnormalized weight of any bin is at least exp(-10).
			log.info("min mu = " + this.minMu + " => E2 = " + this.err2(this.minMu));
			log.info("max mu = " + this.maxMu + " => E2 = " + this.err2(this.maxMu));

			final double gr = (Math.sqrt(5.0) + 1.0) / 2.0;
			double a = this.minMu;
			double b = this.maxMu;
			double c = b - (b - a) / gr;
			double d = a + (b - a) / gr;
			while (Math.abs(b - a) > 1e-6) {
				if (this.err2(c) < this.err2(d)) {
					b = d;
				} else {
					a = c;
				}
				c = b - (b - a) / gr;
				d = a + (b - a) / gr;
			}
			this.optimalMu = (b + a) / 2;
			log.info("opt mu = " + this.optimalMu + " => E2 = " + this.err2(this.optimalMu));
		}
	}

	// -------------------- INTERNALS --------------------

	private boolean dimensionsOK(double[][] matrix, int dim1, int dim2) {
		if (matrix.length != dim1) {
			return false;
		}
		for (double[] row : matrix) {
			if (row.length != dim2) {
				return false;
			}
		}
		return true;
	}

	private double err2(double mu) {
		double err2 = 0;
		for (int i = 0; i < this.binCnt; i++) {
			double[] weights = this.computeWeights(i, mu, false);
			for (int n = 0; n < this.sampleCnt; n++) {
				double pred = 0.0;
				for (int j = 0; j < this.binCnt; j++) {
					pred += weights[j] * this.data[n][j];
				}
				err2 += Math.pow(this.data[n][i] - pred, 2.0);
			}
		}
		return err2;
	}

	// -------------------- IMPLEMENTATION --------------------

	Double getOptimalMu() {
		return this.optimalMu;
	}

	double[] computeWeights(int i, double mu, boolean includeSelf) {
		if ((this.binCnt == 1) && !includeSelf) {
			throw new RuntimeException("not including self but only one data bin.");
		}
		double[] result = new double[this.binCnt];
		for (int j = 0; j < this.binCnt; j++) {
			if (includeSelf || (j != i)) {
				result[j] = Math.exp(-mu * (this.dist[i][j] - (includeSelf ? 0.0 : this.minDist[i])));
			}
		}
		final double weightSum = Arrays.stream(result).sum();
		for (int j = 0; j < this.binCnt; j++) {
			result[j] /= weightSum;
		}
		return result;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	private double predictFORTESTINGONLY(int n, int i, double mu, boolean includeSelf) {
		final double[] weights = this.computeWeights(i, mu, includeSelf);
		double result = 0.0;
		for (int j = 0; j < this.data[n].length; j++) {
			result += weights[j] * this.data[n][j];
		}
		return result;
	}

	public static void main(String[] args) {

		int cnt = 100;
		double stddev = 10;
		Random rnd = new Random();
		double y[][] = new double[1][cnt];
		double dist[][] = new double[cnt][cnt];
		for (int i = 0; i < cnt; i++) {
			y[0][i] = 1.0 * i + stddev * rnd.nextGaussian();
			for (int j = 0; j < cnt; j++) {
				dist[i][j] = Math.abs(i - j);
			}
		}

		double bandwidth = 3;
		KernelSmoother ks = new KernelSmoother(y, dist);
		for (int i = 0; i < cnt; i++) {
			System.out.println(i + "\t" + y[0][i] + "\t" + ks.predictFORTESTINGONLY(0, i, bandwidth, false) + "\t"
					+ ks.predictFORTESTINGONLY(0, i, bandwidth, true) + "\t"
					+ ks.predictFORTESTINGONLY(0, i, ks.getOptimalMu(), false) + "\t"
					+ ks.predictFORTESTINGONLY(0, i, ks.getOptimalMu(), true));
		}

	}

}
