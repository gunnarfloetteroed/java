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
package lebudgeteur;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import floetteroed.utilities.math.MathHelpers;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LeBudgeteur {

	private final int duration;

	private boolean verbose = false;

	private final Map<String, Project> label2proj = new LinkedHashMap<>();

	public LeBudgeteur(int maxYears) {
		this.duration = maxYears;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public Project createProject(String label) {
		if (this.label2proj.containsKey(label)) {
			throw new RuntimeException("Project " + label + " exists already.");
		}
		this.label2proj.put(label, new Project(label, this.duration, this.label2proj.size()));
		return this.label2proj.get(label);
	}

	public void setTargetFundings(String label, double... targetFundings) {
		this.label2proj.get(label).setTargetFundings(targetFundings);
	}

	public void setMaxDeferrals(String label, double... maxDeferrals) {
		this.label2proj.get(label).setMaxDeferrals(maxDeferrals);
	}

	public void setMinTotalConsumptions(String label, double... minConsumptions) {
		this.label2proj.get(label).setMinTotalConsumptions(minConsumptions);
	}

	public void putOtherConsumptions(String projectLabel, String consumptionLabel, double... otherConsumptions) {
		this.label2proj.get(projectLabel).putOtherConsumptions(consumptionLabel, otherConsumptions);
	}

	private int _P() {
		return this.label2proj.size();
	}

	private int _Y() {
		return this.duration;
	}

	private int x_index(Project proj, int y) {
		return proj.getIndex() * _Y() + y;
	}

	private int z_index(Project proj, int y) {
		return _P() * _Y() + proj.getIndex() * _Y() + y;
	}

	private int d_index(int y) {
		return 2 * _P() * _Y() + y;
	}

	private void prnLabels() {
		if (!this.verbose) {
			return;
		}
		final String[] indices = new String[(2 * _P() + 1) * _Y()];
		for (int y = 0; y < this.duration; y++) {
			for (Project proj : this.label2proj.values()) {
				int p = proj.getIndex();
				indices[x_index(proj, y)] = "x_" + p + "" + y;
				indices[z_index(proj, y)] = "z_" + p + "" + y;
			}
			indices[d_index(y)] = "d_" + y;
		}
		for (String val : indices) {
			System.out.print(val + "\t");
		}
		System.out.println();
	}

	private void prn(String label, double[] vals) {
		if (!this.verbose) {
			return;
		}
		System.out.println(label);
		for (double val : vals) {
			System.out.print(val + "\t");
		}
		System.out.println();
	}

	private void roundPrintTab(double val) {
		System.out.print(MathHelpers.round(val) + "\t");
	}

	public double[] newSeries(double baseValue, double growthFactor) {
		double[] result = new double[this.duration];
		for (int i = 0; i < result.length; i++) {
			result[i] = baseValue * Math.pow(growthFactor, i);
		}
		return result;
	}

	public void solve(double[] salaries, int firstYear) {

		final int dim = (2 * _P() + 1) * _Y();
		final double _M = 1000;

		prnLabels();

		final LinearObjectiveFunction objFct;
		{
			final double[] coeffs = new double[dim];
			for (int y = 0; y < this.duration; y++) {
				double timeWeight = Math.exp(-y);
				for (Project proj : this.label2proj.values()) {
					coeffs[x_index(proj, y)] = 0;
					coeffs[z_index(proj, y)] = timeWeight;
				}
				coeffs[d_index(y)] = _M;
			}
			prn("objFctCoeffs", coeffs);
			objFct = new LinearObjectiveFunction(coeffs, 0);
		}

		final List<LinearConstraint> constraints;
		{
			constraints = new ArrayList<LinearConstraint>();
			for (int y = 0; y < this.duration; y++) {
				for (Project proj : this.label2proj.values()) {
					{
						// keep track of deferred funding
						double[] coeffs = new double[dim];
						coeffs[x_index(proj, y)] = 1.0;
						coeffs[z_index(proj, y)] = 1.0;
						if (y > 0) {
							coeffs[z_index(proj, y - 1)] = -1.0;
						}
						prn("defer(p=" + proj.getIndex() + ",y=" + y + ")", coeffs);
						constraints.add(new LinearConstraint(coeffs, Relationship.EQ, proj.getAvailableFunding(y)));
						// constraints.add(new LinearConstraint(coeffs, Relationship.EQ,
						// proj.getTargetFunding(y)));
					}
					{
						// min-consumptions
						double[] coeffs = new double[dim];
						coeffs[x_index(proj, y)] = 1.0;
						prn("min-cons(p=" + proj.getIndex() + ",y=" + y + ")", coeffs);
						// constraints.add(new LinearConstraint(coeffs, Relationship.GEQ,
						// proj.getMinConsumption(y)));
						constraints.add(new LinearConstraint(coeffs, Relationship.GEQ, proj.getMinOwnConsumption(y)));
					}
					{
						// max-deferral
						double[] coeffs = new double[dim];
						coeffs[z_index(proj, y)] = 1.0;
						if (this.verbose) {
							prn("max-defer(p=" + proj.getIndex() + ",y=" + y + ")", coeffs);
						}
						constraints.add(new LinearConstraint(coeffs, Relationship.LEQ, proj.getMaxDeferral(y)));
					}
				}
				{
					// cover salary
					double[] coeffs = new double[dim];
					for (Project proj : this.label2proj.values()) {
						coeffs[x_index(proj, y)] = 1.0;
					}
					coeffs[d_index(y)] = 1.0;
					prn("salary(y=" + y + ")", coeffs);
					constraints.add(new LinearConstraint(coeffs, Relationship.EQ, salaries[y]));
				}
			}
		}

		final double[] result = (new SimplexSolver()).optimize(objFct, new LinearConstraintSet(constraints),
				new NonNegativeConstraint(true), GoalType.MINIMIZE).getPoint();
		prnLabels();
		prn("solution", result);

		// REPORT

		System.out.println();
		System.out.print("Year\t");
		for (String label : this.label2proj.keySet()) {
			System.out.print(label + "(target)\t");
			System.out.print(label + "(used, other)\t");
			System.out.print(label + "(used, self)\t");
			System.out.print(label + "(deferred)\t");
		}
		System.out.println("salary\tdeficit");

		for (int y = 0; y < this.duration; y++) {
			System.out.print((firstYear + y) + "\t");
			for (Project proj : this.label2proj.values()) {
				roundPrintTab(proj.getTargetFunding(y));
				roundPrintTab(proj.getOtherConsumptions(y));
				roundPrintTab(result[x_index(proj, y)]);
				roundPrintTab(result[z_index(proj, y)]);
			}
			roundPrintTab(salaries[y]);
			roundPrintTab(result[d_index(y)]);
			System.out.println();
		}
	}
}
