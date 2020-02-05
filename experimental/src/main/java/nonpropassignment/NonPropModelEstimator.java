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

import java.util.Random;

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
public class NonPropModelEstimator {

	private final Random rnd = new Random();

	private final NonPropModel model;

	private final double eps;

	private final int maxEval;

	private final int maxIt;

	public NonPropModelEstimator(final NonPropModel model, final double eps, final int maxEval, final int maxIt) {
		this.model = model;
		this.eps = eps;
		this.maxEval = maxEval;
		this.maxIt = maxIt;
	}

	public NonPropModelEstimator(final NonPropModel model) {
		this(model, 1e-8, 1000, 100);
	}

	public double[] run(double[] startPoint) {

		// System.out.print("initial: ");
		if (startPoint == null) {
			startPoint = new double[2 * model.getBasisSize() * model.getLinkCnt()];
			for (int i = 0; i < startPoint.length; i++) {
				startPoint[i] = 2.0 * (this.rnd.nextDouble() - 0.5);
				// System.out.print(startPoint[i] + " ");
			}
		}
		// System.out.println();

		System.out.print("INITIAL: " + this.model.getObjectiveFunction().value(startPoint));
		final ObjectiveFunction objectiveFunction = new ObjectiveFunction(this.model.getObjectiveFunction());
		final ObjectiveFunctionGradient objectiveFunctionGradient = new ObjectiveFunctionGradient(
				this.model.getObjectiveFunctionGradient());
		final InitialGuess initialGuess = new InitialGuess(startPoint);

		// System.out.print("gradient:");
		// final MultivariateVectorFunction gradFct =
		// this.model.getObjectiveFunctionGradient();
		// final double[] gradVal = gradFct.value(startPoint);
		// for (int i = 0; i < gradVal.length; i++) {
		// System.out.print(gradVal[i] + " ");
		// }
		// System.out.println();

		final ConvergenceChecker<PointValuePair> checker = new SimpleValueChecker(this.eps, this.eps);
		final ConvergenceChecker<PointValuePair> verboseChecker = new ConvergenceChecker<PointValuePair>() {
			@Override
			public boolean converged(int arg0, PointValuePair arg1, PointValuePair arg2) {
				System.out.println("checking: " + arg1.getValue());
				return checker.converged(arg0, arg1, arg2);
			}
		};
		// final ConvergenceChecker<PointValuePair> iterationCntChecker = new
		// ConvergenceChecker<PointValuePair>() {
		// @Override
		// public boolean converged(int arg0, PointValuePair arg1, PointValuePair arg2)
		// {
		// System.out.println("it " + arg0 + ", checking: " + arg1.getValue());
		// return (arg0 > 10);
		// }
		// };

		final Formula formula = Formula.FLETCHER_REEVES;
		final NonLinearConjugateGradientOptimizer cg = new NonLinearConjugateGradientOptimizer(formula, verboseChecker);

		final PointValuePair resultPointValue = cg.optimize(objectiveFunction, objectiveFunctionGradient, initialGuess,
				new MaxEval(this.maxEval), new MaxIter(this.maxIt), GoalType.MINIMIZE);
		// System.out.print("final: ");
		// for (int i = 0; i < resultPointValue.getPoint().length; i++) {
		// System.out.print(resultPointValue.getPoint()[i] + " ");
		// }
		// System.out.println();
		System.out.println("\tFINAL: " + resultPointValue.getValue());

		return resultPointValue.getPoint();
	}

	public double[] runAlternating(double[] startPoint, final double outerEps) {

		if (startPoint == null) {
			startPoint = new double[2 * model.getBasisSize() * model.getLinkCnt()];
			for (int i = 0; i < startPoint.length; i++) {
				startPoint[i] = 2.0 * (this.rnd.nextDouble() - 0.5);
			}
		}
		double[] point = new double[startPoint.length];
		System.arraycopy(startPoint, 0, point, 0, startPoint.length);

		double[][] v = this.model.extractV(point);
		double[][] w = this.model.extractW(point);

		double vQ;
		double wQ;

		do {

			// ----- V STEP -----
			
			{
				final double[] vPoint = new double[this.model.getBasisSize() * this.model.getLinkCnt()];
				this.model.flatten(vPoint, v, 0);

				final ObjectiveFunction objectiveFunction = new ObjectiveFunction(
						this.model.getObjectiveFunctionVW(null, w));
				final ObjectiveFunctionGradient objectiveFunctionGradient = new ObjectiveFunctionGradient(
						this.model.getObjectiveFunctionGradientVW(null, w));
				final InitialGuess initialGuess = new InitialGuess(vPoint);

				final ConvergenceChecker<PointValuePair> checker = new SimpleValueChecker(0, this.eps);
				final ConvergenceChecker<PointValuePair> verboseChecker = new ConvergenceChecker<PointValuePair>() {
					@Override
					public boolean converged(int arg0, PointValuePair arg1, PointValuePair arg2) {
						System.out.println("(V) checking: " + arg1.getValue());
						return checker.converged(arg0, arg1, arg2);
					}
				};
				final NonLinearConjugateGradientOptimizer cg = new NonLinearConjugateGradientOptimizer(
						Formula.FLETCHER_REEVES, verboseChecker);
				final PointValuePair resultPointValue = cg.optimize(objectiveFunction, objectiveFunctionGradient,
						initialGuess, new MaxEval(this.maxEval), new MaxIter(this.maxIt), GoalType.MINIMIZE);

				v = this.model.extract(resultPointValue.getPoint(), 0);
				vQ = resultPointValue.getValue();
			}

			// ----- W STEP -----
			{
				final double[] wPoint = new double[this.model.getBasisSize() * this.model.getLinkCnt()];
				this.model.flatten(wPoint, w, 0);
				
				final ObjectiveFunction objectiveFunction = new ObjectiveFunction(
						this.model.getObjectiveFunctionVW(v, null));
				final ObjectiveFunctionGradient objectiveFunctionGradient = new ObjectiveFunctionGradient(
						this.model.getObjectiveFunctionGradientVW(v, null));
				final InitialGuess initialGuess = new InitialGuess(wPoint);

				final ConvergenceChecker<PointValuePair> checker = new SimpleValueChecker(this.eps, this.eps);
				final ConvergenceChecker<PointValuePair> verboseChecker = new ConvergenceChecker<PointValuePair>() {
					@Override
					public boolean converged(int arg0, PointValuePair arg1, PointValuePair arg2) {
						System.out.println("(W) checking: " + arg1.getValue());
						return checker.converged(arg0, arg1, arg2);
					}
				};
				final NonLinearConjugateGradientOptimizer cg = new NonLinearConjugateGradientOptimizer(
						Formula.FLETCHER_REEVES, verboseChecker);
				final PointValuePair resultPointValue = cg.optimize(objectiveFunction, objectiveFunctionGradient,
						initialGuess, new MaxEval(this.maxEval), new MaxIter(this.maxIt), GoalType.MINIMIZE);

				w = this.model.extract(resultPointValue.getPoint(), 0);
				wQ = resultPointValue.getValue();
			}
			
		} while (Math.abs(vQ - wQ) > outerEps);

		this.model.flatten(point, v, 0);
		this.model.flatten(point, w, point.length / 2);
		
		return point;
	}

}
