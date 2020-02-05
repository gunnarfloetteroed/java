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

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class NonPropModel {

	// -------------------- CONSTANTS --------------------

	private final int linkCnt;

	private final int basisSize;

	private final double regularizationWeight;

	// -------------------- VARIABLES --------------------

	private final List<Plans> plans = new ArrayList<>();

	private double _Q;

	private double fitWithoutRegularization;

	private double[][] dQdv = null;

	private double[][] dQdw = null;

	// -------------------- CONSTRUCTION --------------------

	public NonPropModel(final int linkCnt, final int basisSize, final double regularizationWeight) {
		this.linkCnt = linkCnt;
		this.basisSize = basisSize;
		this.regularizationWeight = regularizationWeight;
	}

	public NonPropModel(final int basisSize, final double regularizationWeight, final NonPropModel parent) {
		this(parent.getLinkCnt(), basisSize, regularizationWeight);
		this.plans.addAll(parent.plans);
	}

	// -------------------- GETTERS --------------------

	public double getQ() {
		return this._Q;
	}

	public double getFitWithoutRegularization() {
		return this.fitWithoutRegularization;
	}

	public int getLinkCnt() {
		return this.linkCnt;
	}

	public int getBasisSize() {
		return this.basisSize;
	}

	public double getRegularizationWeight() {
		return this.regularizationWeight;
	}

	// -------------------- PLANS UPDATE --------------------

	public void addPlans(final Plans plans) {
		this.plans.add(plans);
	}

	// -------------------- MODEL COEFFICIENT UPDATE --------------------

	public void setCoefficients(final double[][] v, final double[][] w) {

		this._Q = 0.0;
		this.fitWithoutRegularization = 0.0;
		this.dQdv = new double[this.basisSize][this.linkCnt];
		this.dQdw = new double[this.basisSize][this.linkCnt];

		if (this.plans.size() == 0) {
			throw new RuntimeException("No plans registered!");
		}

		final double fact;
		{
			long totalPlanCnt = 0;
			for (Plans plans : this.plans) {
				totalPlanCnt += plans.allXn.size();
			}
			fact = 1.0 / 3600.0 / totalPlanCnt;
		}

		for (Plans plans : this.plans) {

			final double[] g = new double[this.basisSize];
			for (int s = 0; s < this.basisSize; s++) {
				g[s] = Utils.innerProduct(v[s], plans.x);

			}

			final double[] e = new double[plans.allXn.size()];
			for (int n = 0; n < plans.allXn.size(); n++) {
				e[n] = plans.freeFlowCosts.get(n) - plans.realizedCosts.get(n);
				for (int s = 0; s < this.basisSize; s++) {
					e[n] += g[s] * Utils.innerProductWithIndicatorIndices(w[s], plans.allXn.get(n));
				}
			}

			for (int n = 0; n < plans.allXn.size(); n++) {
				this._Q += 0.5 * fact * e[n] * e[n];
				for (int s = 0; s < this.basisSize; s++) {
					Utils.add(this.dQdv[s], plans.x,
							fact * e[n] * Utils.innerProductWithIndicatorIndices(w[s], plans.allXn.get(n)));
					Utils.addIndicatorIndices(this.dQdw[s], plans.allXn.get(n), fact * e[n] * g[s]);
				}
			}
		}

		this.fitWithoutRegularization = this._Q;

		for (int r = 0; r < this.basisSize; r++) {
			for (int i = 0; i < this.linkCnt; i++) {
				this._Q += 0.5 * this.regularizationWeight * (v[r][i] * v[r][i] + w[r][i] * w[r][i]);
				this.dQdv[r][i] += this.regularizationWeight * v[r][i];
				this.dQdw[r][i] += this.regularizationWeight * w[r][i];
			}
		}
	}

	// -------------------- RESULT ACCESS --------------------

	public double[][] extract(final double[] trialPoint, final int startIndex) {
		final double[][] result = new double[this.basisSize][this.linkCnt];
		for (int r = 0; r < this.basisSize; r++) {
			System.arraycopy(trialPoint, startIndex + r * this.linkCnt, result[r], 0, this.linkCnt);
		}
		return result;
	}

	public double[][] extractV(final double[] trialPoint) {
		return this.extract(trialPoint, 0);
	}

	public double[][] extractW(final double[] trialPoint) {
		return this.extract(trialPoint, this.basisSize * this.linkCnt);
	}

	public void flatten(final double target[], final double[][] matrix, final int startIndex) {
		for (int r = 0; r < this.basisSize; r++) {
			System.arraycopy(matrix[r], 0, target, startIndex + r * this.linkCnt, this.linkCnt);
		}
	}

	private double[] getFlattenedGradient() {
		final double[] result = new double[2 * this.basisSize * this.linkCnt];
		this.flatten(result, this.dQdv, 0);
		this.flatten(result, this.dQdw, this.basisSize * this.linkCnt);
		return result;
	}

	public MultivariateFunction getObjectiveFunction() {
		return new MultivariateFunction() {
			@Override
			public double value(double[] trialPoint) {
				// if (trialPoint != lastTrialPoint) {
				setCoefficients(extractV(trialPoint), extractW(trialPoint));
				// }
				// lastTrialPoint = trialPoint;
				final double value = _Q;
				// System.out.println(" testing.. " + value);
				return value; // getObjectiveFunctionValue();
			}
		};
	}

	public MultivariateVectorFunction getObjectiveFunctionGradient() {
		return new MultivariateVectorFunction() {
			@Override
			public double[] value(double[] trialPoint) throws IllegalArgumentException {
				// if (trialPoint != lastTrialPoint) {
				setCoefficients(extractV(trialPoint), extractW(trialPoint));
				// }
				// lastTrialPoint = trialPoint;
				return getFlattenedGradient();
			}
		};
	}

	// >>> NEW >>>

	public MultivariateFunction getObjectiveFunctionVW(final double[][] v, final double[][] w) {
		return new MultivariateFunction() {
			@Override
			public double value(double[] trialPoint) {
				// if (trialPoint != lastTrialPoint) {
				setCoefficients(v != null ? v : extract(trialPoint, 0), w != null ? w : extract(trialPoint, 0));
				// }
				// lastTrialPoint = trialPoint;
				final double value = _Q;
				// System.out.println(" testing.. " + value);
				return value; // getObjectiveFunctionValue();
			}
		};
	}

	public MultivariateVectorFunction getObjectiveFunctionGradientVW(final double[][] v, final double[][] w) {
		return new MultivariateVectorFunction() {
			@Override
			public double[] value(double[] trialPoint) throws IllegalArgumentException {
				// if (trialPoint != lastTrialPoint) {
				setCoefficients(v != null ? v : extract(trialPoint, 0), w != null ? w : extract(trialPoint, 0));
				// }
				// lastTrialPoint = trialPoint;

				final double[] grad = new double[basisSize * linkCnt];
				if (v == null && w != null) {
					flatten(grad, dQdv, 0);
				} else if (v != null && w == null) {
					flatten(grad, dQdw, 0);
				} else {
					throw new RuntimeException("v=" + v + ", w=" + w);
				}
				return grad;
			}
		};
	}

	public double getMeanFit(final double[] costs) {
		double e2sum = 0;
		double cnt = 0;
		for (Plans plans : this.plans) {
			for (int n = 0; n < plans.allXn.size(); n++) {
				final double e = Utils.innerProductWithIndicatorIndices(costs, plans.allXn.get(n))
						- plans.realizedCosts.get(n);
				e2sum += e * e;
				cnt++;
			}
		}

		return (0.5 / cnt / 3600.0) * e2sum;
	}
}
