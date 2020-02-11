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
public class PropModel {

	// -------------------- CONSTANTS --------------------

	private final int linkCnt;

	private final double regularizationWeight;

	// -------------------- VARIABLES --------------------

	private final List<Plans> plans = new ArrayList<>();

	private double _Q;

	private double fitWithoutRegularization;

	private double[] dQdc = null;

	// -------------------- CONSTRUCTION --------------------

	public PropModel(final int linkCnt, final double regularizationWeight) {
		this.linkCnt = linkCnt;
		this.regularizationWeight = regularizationWeight;
	}

	public PropModel(final double regularizationWeight, final PropModel parent) {
		this(parent.getLinkCnt(), regularizationWeight);
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

	public double getRegularizationWeight() {
		return this.regularizationWeight;
	}

	// -------------------- PLANS UPDATE --------------------

	public void addPlans(final Plans plans) {
		this.plans.add(plans);
	}

	// -------------------- MODEL COEFFICIENT UPDATE --------------------

	public void setCoefficients(final double[] c) {

		this._Q = 0.0;
		this.fitWithoutRegularization = 0.0;
		this.dQdc = new double[this.linkCnt];

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
			for (int n = 0; n < plans.allXn.size(); n++) {
				final double e = Utils.innerProductWithIndicatorIndices(c, plans.allXn.get(n))
						- plans.realizedCosts.get(n);
				this._Q += 0.5 * fact * e * e;
				Utils.addIndicatorIndices(this.dQdc, plans.allXn.get(n), fact * e);
			}
		}

		this.fitWithoutRegularization = this._Q;
		for (int i = 0; i < this.linkCnt; i++) {
			this._Q += 0.5 * this.regularizationWeight * (c[i] * c[i]);
			this.dQdc[i] += this.regularizationWeight * c[i];
		}
	}

	// -------------------- RESULT ACCESS --------------------

	public MultivariateFunction getObjectiveFunction() {
		return new MultivariateFunction() {
			@Override
			public double value(double[] trialPoint) {
				// if (trialPoint != lastTrialPoint) {
				setCoefficients(trialPoint);
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
				setCoefficients(trialPoint);
				// }
				// lastTrialPoint = trialPoint;
				return dQdc;
			}
		};
	}
}
