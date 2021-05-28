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
package playground;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NonNegativeConstraint;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TestApacheLP {

	public static void main(String[] args) {
		Random rnd = new Random();
		int decVarCnt = 10 * 1000;
		int constrCnt = 1000;
		double constrDensity = 0.01;

		System.out.println("STARTED ...");
		
		for (int _M = 1000; _M >= 1; _M--) {

			double[] objFctCoeffs = new double[decVarCnt];
			double[][] constrCoeffs = new double[constrCnt][decVarCnt];
			for (int i = 0; i < decVarCnt; i++) {
				objFctCoeffs[i] = - Math.log(Math.max(1e-8,  rnd.nextDouble()));
			}

				for (int j = 0; j < constrCnt; j++) {
					// if (j % 1000 == 0) {System.out.println(j);};
					double[] array = constrCoeffs[j];
					for (int i = 0; i < decVarCnt; i++) {
					if (rnd.nextDouble() < constrDensity) {
						array[i] = 1.0;
					}
				}
			}

			final LinearObjectiveFunction objFct = new LinearObjectiveFunction(objFctCoeffs, 0.0);

			final List<LinearConstraint> constraints = new ArrayList<LinearConstraint>(decVarCnt + constrCnt);
			for (int i = 0; i < decVarCnt; i++) {
				double[] lhs = new double[decVarCnt];
				lhs[i] = 1.0;
				constraints.add(new LinearConstraint(lhs, Relationship.LEQ, 1.0));
			}
			for (int j = 0; j < constrCnt; j++) {
				final LinearConstraint constr = new LinearConstraint(constrCoeffs[j], Relationship.LEQ, _M);
				constraints.add(constr);
			}

			final double[] result = (new SimplexSolver()).optimize(objFct, new LinearConstraintSet(constraints),
					new NonNegativeConstraint(true), GoalType.MAXIMIZE).getPoint();
			
			double lambdaRealized = Arrays.stream(result).average().getAsDouble();
			System.out.println(_M + "\t" + lambdaRealized);
		}

		System.out.println("... DONE");
	}

}
