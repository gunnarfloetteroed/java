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
package org.matsim.contrib.opdyts.buildingblocks.convergencecriteria;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterionResult;
import floetteroed.opdyts.trajectorysampling.Transition;
import floetteroed.utilities.math.Vector;

/**
 * Estimates an AR(1) model of the objective function time series
 * <p>
 * Q(k + 1) = a * Q(k) + b
 * <p>
 * Varies the time (i.e. iteration) bin size of the model as well as the number
 * of iterations over which convergence is tested and looks for a configuration
 * where both model parameters a and b are statistically insignificant.
 * <p>
 * <ul>
 * <li>a being insignificant indicates the absence of serial correlation across
 * bins, meaning that objective function averages per time bin may reasonably be
 * treated as statistically independent.
 * <li>b being insignificant indicates the absence of a trend.
 * </ul>
 * Once a and b are insignificant, the variance of the average objective
 * function can be estimated over the (presumably independent) time bins; once
 * it falls below a desired threshold, convergence is declared.
 * <p>
 * <em>This does not claim to be rigorous statistics. The AR(1) model structure
 * is simplistic, the selection of stationary time intervals and of time bin
 * sizes jointly with the statistical testing is probably also problematic.<em>
 *
 * @author Gunnar Flötteröd
 *
 */
public class AR1ConvergenceCriterion implements ConvergenceCriterion {

	// -------------------- CONSTANTS --------------------

	private final double maxConvergedMeanStddev;

	// -------------------- PARAMETER MEMBERS --------------------

	private int minSampleCntPerParameter = 5;

	// -------------------- SOLUTION MEMBERS --------------------

	private boolean converged = false;

	private Double convergedMean = null;

	private Double convergedMeanStddev = null;

	private Integer convergedBinSize = null;

	private Integer convergedSinceIteration = null;

	// -------------------- CONSTRUCTION --------------------

	public AR1ConvergenceCriterion(final double maxConvergedMeanStddev) {
		this.maxConvergedMeanStddev = maxConvergedMeanStddev;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setMinSampleCntPerParameter(final int minSampleCntPerParameter) {
		this.minSampleCntPerParameter = minSampleCntPerParameter;
	}

	public boolean getConverged() {
		return this.converged;
	}

	public Double getConvergedMean() {
		return this.convergedMean;
	}

	public Double getConvergedMeanStddev() {
		return this.convergedMeanStddev;
	}

	public Integer getConvergedBinSize() {
		return this.convergedBinSize;
	}

	public Integer getConvergedSinceIteration() {
		return this.convergedSinceIteration;
	}

	// -------------------- INTERNALS --------------------

	private double binAvg(double[] data, int bin, int binSize) {
		double sum = 0;
		for (int i = bin * binSize; i < (bin + 1) * binSize; i++) {
			sum += data[i];
		}
		return (sum / binSize);
	}

	private boolean statisticallyZero(double val, double sttdev) {
		return (val - 2.0 * sttdev) * (val + 2.0 * sttdev) <= 0;
	}

	// -------------------- IMPLEMENTATION --------------------

	/**
	 * 
	 * @param data
	 *            A data list, sorted in increasing time order: [x(0), x(1), ...].
	 */
	public void process(final List<Double> dataList) {
		double[] dataArray = new double[dataList.size()];
		int i = dataList.size();
		for (Double val : dataList) {
			dataArray[--i] = val;
		}
		this.process(dataArray);
	}

	/**
	 * 
	 * @param data
	 *            A data array, sorted in <b>decreasing</b> time order: [x(k),
	 *            x(k-1), ..., x(0)].
	 */
	private void process(final double[] data) {

		this.converged = false;
		this.convergedMean = null;
		this.convergedMeanStddev = null;
		this.convergedBinSize = null;
		this.convergedSinceIteration = null;

		final int minBinCnt = 2 * this.minSampleCntPerParameter + 1;
		final int maxBinSize = data.length / minBinCnt;
		final int minBinSize = Math.max(1, (int) Math.round(0.1 * maxBinSize));

		// The smaller the bins, the better.
		for (int binSize = minBinSize; binSize <= maxBinSize; binSize++) {

			final int maxBinCnt = data.length / binSize;
			final double[] allBinData = new double[maxBinCnt];
			for (int bin = 0; bin < maxBinCnt; bin++) {
				allBinData[bin] = this.binAvg(data, bin, binSize);
			}

			// The more bins, the better.
			for (int usedBinCnt = maxBinCnt; usedBinCnt >= minBinCnt; usedBinCnt--) {

				final double[] usedBinData = new double[usedBinCnt];
				System.arraycopy(allBinData, 0, usedBinData, 0, usedBinCnt);
				final DescriptiveStatistics stats = new DescriptiveStatistics(usedBinData);

				double[] y = new double[usedBinCnt];
				double[][] x = new double[usedBinCnt][1];
				for (int bin = 0; bin < usedBinCnt - 1; bin++) {
					y[bin] = allBinData[bin] - stats.getMean();
					x[bin][0] = allBinData[bin + 1] - stats.getMean();
				}
				y[usedBinCnt - 1] = 0.0;
				x[usedBinCnt - 1][0] = 1e-8;

				final OLSMultipleLinearRegression regr = new OLSMultipleLinearRegression();
				regr.setNoIntercept(false);
				regr.newSampleData(y, x);
				final double[] beta = regr.estimateRegressionParameters();
				final double[] betaSigma = regr.estimateRegressionParametersStandardErrors();

				final double meanStddev = stats.getStandardDeviation() / Math.sqrt(usedBinCnt - 1);
				if ((meanStddev <= this.maxConvergedMeanStddev) && this.statisticallyZero(beta[0], betaSigma[0])
						&& this.statisticallyZero(beta[1], betaSigma[1])) {
					this.converged = true;
					this.convergedMean = stats.getMean();
					this.convergedMeanStddev = meanStddev;
					this.convergedBinSize = binSize;
					this.convergedSinceIteration = data.length - usedBinCnt * binSize;
				}
			}
		}
	}

	// --------------- IMPLEMENTATION OF Opdyts ConvergenceCriterion ---------------

	private <U extends DecisionVariable> List<Double> objectiveFunctionValues(final List<Transition<U>> transitions) {
		final List<Double> result = new ArrayList<Double>(transitions.size());
		for (Transition<?> transition : transitions) {
			result.add(transition.getToStateObjectiveFunctionValue());
		}
		return result;
	}

	@Override
	public <U extends DecisionVariable> ConvergenceCriterionResult evaluate(
			List<Transition<U>> mostRecentTransitionSequence, int totalTransitionSequenceLength) {

		final List<Double> data = this.objectiveFunctionValues(mostRecentTransitionSequence);
		this.process(data);

		if (this.getConverged()) {

			// gap statistics
			final Vector totalDelta = mostRecentTransitionSequence.get(this.getConvergedSinceIteration()).getDelta()
					.copy();
			for (int i = this.getConvergedSinceIteration() + 1; i < mostRecentTransitionSequence.size(); i++) {
				totalDelta.add(mostRecentTransitionSequence.get(i).getDelta());
			}
			final int averagingIterations = data.size() - this.convergedSinceIteration;

			// package the results
			return new ConvergenceCriterionResult(true, this.getConvergedMean(), this.getConvergedMeanStddev(),
					totalDelta.euclNorm() / averagingIterations, 1.0 / averagingIterations,
					mostRecentTransitionSequence.get(0).getDecisionVariable(), mostRecentTransitionSequence.size());

		} else {
			return new ConvergenceCriterionResult(false, null, null, null, null, null, null);
		}
	}
}
