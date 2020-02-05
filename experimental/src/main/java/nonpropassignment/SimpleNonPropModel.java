/*
 * Copyright 2020 Gunnar Flötteröd
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
package nonpropassignment;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer.Formula;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SimpleNonPropModel {

	// -------------------- CONSTANTS --------------------

	private final double eps = 1e-8;

	private final int maxEval = Integer.MAX_VALUE;

	private final int maxIt = Integer.MAX_VALUE;

	private final double[] minC;

	// -------------------- VARIABLES --------------------

	private List<double[]> v = new ArrayList<>();

	private List<double[]> w = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public SimpleNonPropModel(final double[] minCost) {
		this.minC = minCost;
	}

	// -------------------- GETTERS --------------------

	public int getLinkCnt() {
		return this.minC.length;
	}

	public int getBasisSize() {
		return this.v.size();
	}

	// -------------------- EVALUATE --------------------

	public double[] sumIntoX(final List<int[]> allXn) {
		final double[] x = new double[this.getLinkCnt()];
		for (int[] xn : allXn) {
			for (int index : xn) {
				x[index]++;
			}
		}
		return x;
	}

	public List<Double> predictFreeFlowCost(final List<int[]> allXn) {
		final List<Double> freeFlowCost = new ArrayList<>(allXn.size());
		for (int[] xn : allXn) {
			freeFlowCost.add(Utils.innerProductWithIndicatorIndices(this.minC, xn));
		}
		return freeFlowCost;
	}

	public double[] predictG(final double[] x) {
		final double[] g = new double[this.getBasisSize()];
		for (int r = 0; r < this.getBasisSize(); r++) {
			g[r] = Utils.innerProduct(x, this.v.get(r));
		}
		return g;
	}

	public List<Double> predictCongestionCost(final List<int[]> allXn, final double[] g, final double gSum) {
		final List<Double> congestionCost = new ArrayList<>(allXn.size());
		for (int n = 0; n < allXn.size(); n++) {
			double val = 0.0;
			for (int r = 0; r < this.getBasisSize(); r++) {
				val += g[r] / gSum * Utils.innerProductWithIndicatorIndices(this.w.get(r), allXn.get(n));
			}
			congestionCost.add(val);
		}
		return congestionCost;
	}

	public double evaluate(final List<int[]> allXn, final List<Double> realizedDn, final List<Double> predictedFreeCost,
			final List<Double> predictedCongestionCost) {
		double e2sum = 0.0;
		for (int n = 0; n < allXn.size(); n++) {
			final double e = predictedFreeCost.get(n) + predictedCongestionCost.get(n) - realizedDn.get(n);
			e2sum += e * e;
		}
		return (e2sum / 2.0 / 3600.0 / allXn.size());
	}

	// -------------------- UPDATE --------------------

	public void update(final List<int[]> allXn, final List<Double> realizedDn, final List<Double> predictedFreeCost,
			final List<Double> predictedCongestionCost, final double[] x, final double[] g, final double gSum) {

		final double newG = 1.0; // Since v=x/<x,x> and g=<v,x>.

		final ObjectiveFunction objectiveFunction = new ObjectiveFunction(new MultivariateFunction() {
			@Override
			public double value(double[] trialW) {
				double val = 0;
				for (int n = 0; n < allXn.size(); n++) {
					final double en = Utils.innerProductWithIndicatorIndices(trialW, allXn.get(n)) * newG
							/ (gSum + newG)
							- (realizedDn.get(n) - predictedCongestionCost.get(n) * gSum / (gSum + newG)
									- predictedFreeCost.get(n));
					val += en * en;
				}
				return (0.5 * val);
			}
		});
		final ObjectiveFunctionGradient objectiveFunctionGradient = new ObjectiveFunctionGradient(
				new MultivariateVectorFunction() {
					@Override
					public double[] value(double[] trialW) throws IllegalArgumentException {
						final double[] grad = new double[getLinkCnt()];
						for (int n = 0; n < allXn.size(); n++) {
							final double en = Utils.innerProductWithIndicatorIndices(trialW, allXn.get(n)) * newG
									/ (gSum + newG)
									- (realizedDn.get(n) - predictedCongestionCost.get(n) * gSum / (gSum + newG)
											- predictedFreeCost.get(n));
							Utils.addIndicatorIndices(grad, allXn.get(n), en);
						}
						Utils.mult(grad, newG / (gSum + newG));
						return grad;
					}
				});
		final InitialGuess initialGuess = new InitialGuess(new double[this.getLinkCnt()]);

		final ConvergenceChecker<PointValuePair> checker = new SimpleValueChecker(this.eps, this.eps);
		final ConvergenceChecker<PointValuePair> verboseChecker = new ConvergenceChecker<PointValuePair>() {
			@Override
			public boolean converged(int arg0, PointValuePair arg1, PointValuePair arg2) {
				// System.out.println("checking: " + arg1.getValue());
				return checker.converged(arg0, arg1, arg2);
			}
		};

		final Formula formula = Formula.FLETCHER_REEVES;
		final NonLinearConjugateGradientOptimizer cg = new NonLinearConjugateGradientOptimizer(formula, verboseChecker);
		this.w.add(cg.optimize(objectiveFunction, objectiveFunctionGradient, initialGuess, new MaxEval(this.maxEval),
				new MaxIter(this.maxIt), GoalType.MINIMIZE).getPoint());

		final double[] vNew = new double[this.getLinkCnt()];
		Utils.add(vNew, x, 1.0 / Utils.innerProduct(x, x));
		this.v.add(vNew);
	}
}
