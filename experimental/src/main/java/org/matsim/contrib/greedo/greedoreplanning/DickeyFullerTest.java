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
package org.matsim.contrib.greedo.greedoreplanning;

import static java.lang.Math.pow;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class DickeyFullerTest {

	public static final double DEFAULT_CRITICAL_VALUE = -3.0;

	public final double criticalValue;

	public final double offsetCoeff;
	public final double delta;
	public final double trendCoeff;

	public final double arCoeffTStatistic;

	public final double trendCoeffTStatistic;

	public final double mean;

	public final double varianceOfMean;

	public DickeyFullerTest(final List<Double> data) {
		this(data.stream().mapToDouble(i -> i).toArray());
	}

	public DickeyFullerTest(final List<Double> data, final double criticalValue) {
		this(data.stream().mapToDouble(i -> i).toArray(), criticalValue);
	}

	public DickeyFullerTest(final double[] data) {
		this(data, DEFAULT_CRITICAL_VALUE);
	}

	public DickeyFullerTest(final double data[], final double criticalValue) {
		this.criticalValue = criticalValue;

		/*-
		 * x(k + 1)        =       a x(k) + b + e(k)
		 * x(k + 1) - x(k) = (a - 1) x(k) + b + e(k)
		 * y(k)            =   delta x(k) + b + e(k)
		 */
		final double[][] x = new double[data.length - 1][2];
		final double[] y = new double[data.length - 1];
		for (int k = 0; k < data.length - 1; k++) {
			x[k][0] = data[k];
			x[k][1] = k;
			y[k] = data[k + 1] - data[k];
		}

		final OLSMultipleLinearRegression regr = new OLSMultipleLinearRegression();
		regr.setNoIntercept(false);
		regr.newSampleData(y, x);

		this.offsetCoeff = regr.estimateRegressionParameters()[0];
		this.delta = regr.estimateRegressionParameters()[1];
		this.trendCoeff = regr.estimateRegressionParameters()[2];

		this.arCoeffTStatistic = delta / regr.estimateRegressionParametersStandardErrors()[1];
		this.trendCoeffTStatistic = regr.estimateRegressionParameters()[2]
				/ regr.estimateRegressionParametersStandardErrors()[2];

		this.mean = Arrays.stream(data).average().getAsDouble();
		final int _K = data.length;
		final double a = delta + 1;
		this.varianceOfMean = regr.estimateErrorVariance() / pow(_K, 2)
				* (_K + 2 * a * (_K * (1 - a) - (1 - pow(a, _K))) / pow(1 - a, 2));
	}

	public boolean getStationary() {
		return (this.arCoeffTStatistic < this.criticalValue)
				&& (Math.abs(this.trendCoeffTStatistic) < Math.abs(this.criticalValue));
	}

	// -------------------- BELOW ONLY FOR TESTING --------------------

	private static double critVal = -3.0;

	private static int size = 100;

	private static void stationaryTest() {
		Random rnd = new Random();
		double data[] = new double[size];
		data[0] = rnd.nextGaussian();
		for (int k = 1; k < data.length; k++) {
			data[k] = rnd.nextGaussian();
		}
		System.out.println();
		final DickeyFullerTest test = new DickeyFullerTest(data, critVal);
		System.out.println("stationary: tAR = " + test.arCoeffTStatistic + ", tTrend = " + test.trendCoeffTStatistic
				+ ", stationary = " + test.getStationary());
	}

	private static void randomWalkTest() {
		Random rnd = new Random();
		double data[] = new double[size];
		data[0] = rnd.nextGaussian();
		for (int k = 1; k < data.length; k++) {
			data[k] = data[k - 1] + rnd.nextGaussian();
		}
		System.out.println();
		final DickeyFullerTest test = new DickeyFullerTest(data, critVal);
		System.out.println("randomWalk: tAR = " + test.arCoeffTStatistic + ", tTrend = " + test.trendCoeffTStatistic
				+ ", stationary = " + test.getStationary());
	}

	private static void linearTrendTest(double slope) {
		Random rnd = new Random();
		double data[] = new double[size];
		data[0] = rnd.nextGaussian();
		for (int k = 1; k < data.length; k++) {
			data[k] = slope * k + rnd.nextGaussian();
		}
		System.out.println();
		final DickeyFullerTest test = new DickeyFullerTest(data, critVal);
		System.out.println("lin.trend(slope=" + slope + "): tAR = " + test.arCoeffTStatistic + ", tTrend = "
				+ test.trendCoeffTStatistic + ", stationary = " + test.getStationary());
		System.out.println("offset / ar / trend = " + test.offsetCoeff + " / " + test.delta + " / " + test.trendCoeff);
	}

	private static void quadraticTrendTest(double slope) {
		Random rnd = new Random();
		double data[] = new double[size];
		data[0] = rnd.nextGaussian();
		for (int k = 1; k < data.length; k++) {
			data[k] = slope * k * k + rnd.nextGaussian();
		}
		System.out.println();
		final DickeyFullerTest test = new DickeyFullerTest(data, critVal);
		System.out.println("quad.trend(coeff=" + slope + "): tAR = " + test.arCoeffTStatistic + ", tTrend = "
				+ test.trendCoeffTStatistic + ", stationary = " + test.getStationary());
		System.out.println("offset / ar / trend = " + test.offsetCoeff + " / " + test.delta + " / " + test.trendCoeff);
	}

	public static void main(String[] args) {
		stationaryTest();
		randomWalkTest();
		linearTrendTest(0);
		linearTrendTest(0.01);
		linearTrendTest(0.1);
		linearTrendTest(1.0);
		quadraticTrendTest(0);
		quadraticTrendTest(0.01);
		quadraticTrendTest(0.1);
		quadraticTrendTest(1.0);
	}
}
