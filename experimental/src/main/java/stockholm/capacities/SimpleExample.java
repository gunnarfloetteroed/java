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
package stockholm.capacities;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import floetteroed.utilities.math.MathHelpers;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SimpleExample {

	static double sigma2e = 1.0; // prior variance
	static double sigma2f = 1.0; // measurement variance
	
	static final RealMatrix _I = new Array2DRowRealMatrix(new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } });

	// static final double c1 = 1.0;
	// static final double f = 2.0;
	// static final double c2 = c1 * (1.0 + 1.0 / f);
	// static final double c3 = f * c2;

	static double c1 = 1.0;
	static double c2 = 2.0;
	static double c3 = 3.0;

	static final double dTdc1 = -1.0 / (c1 * c1);
	static final double dTdc2 = -1.0 / (c2 * c2);
	static final double dTdc3 = -1.0 / (c3 * c3);

	static final RealVector g = new ArrayRealVector(new double[] { dTdc1, dTdc2, dTdc3 });

	private static RealMatrix calibrMatrix(final double a11, final double a21, final double a31) {
		final RealMatrix _A = new Array2DRowRealMatrix(
				new double[][] { { a11, 1.0 - a11 }, { a21, 1.0 - a21 }, { a31, 1.0 - a31 } });
		try {
			RealMatrix tmp = _A.transpose().multiply(_A);
			tmp = new LUDecomposition(tmp).getSolver().getInverse();
			return tmp.multiply(_A.transpose());
		} catch (Exception e) {
			return null;
		}
	}

	private static RealMatrix errorCov(final double a11, final double a21, final double a31) {
		final RealMatrix _A = new Array2DRowRealMatrix(
				new double[][] { { a11, 1.0 - a11 }, { a21, 1.0 - a21 }, { a31, 1.0 - a31 } });
		try {
			RealMatrix tmp = _A.transpose().multiply(_A);
			DecompositionSolver solver = new LUDecomposition(tmp).getSolver();
			if (!solver.isNonSingular()) {
				return null;
			}
			tmp = solver.getInverse();
			tmp = _A.multiply(tmp).multiply(_A.transpose());
			return _I.add(tmp.scalarMultiply(-1.0));
		} catch (Exception e) {
			return null;
		}
	}

	private static Double var(RealMatrix errorCov) {
		if (errorCov != null) {
			return (errorCov.preMultiply(g)).dotProduct(g);
		} else {
			return null;
		}
	}

	private static String toString(RealMatrix matrix) {
		StringBuffer result = new StringBuffer();
		for (int row = 0; row < matrix.getRowDimension(); row++) {
			result.append("[ ");
			for (int col = 0; col < matrix.getColumnDimension(); col++) {
				result.append(MathHelpers.round(matrix.getEntry(row, col), 2));
				result.append("\t");
			}
			result.append("]");
			if (row < matrix.getRowDimension() - 1) {
				result.append("\n");
			}
		}
		return result.toString();
	}

	private static void prnResult(String msg, double a11, double a21, double a31) {
		System.out.println(msg);
		System.out.println("Calibr. matrix is ");
		System.out.println(toString(calibrMatrix(a11, a21, a31)));
		RealMatrix resCov = errorCov(a11, a21, a31);
		System.out.println("Residual cov. matrix is ");
		System.out.println(toString(resCov));
		System.out.println("var(t1 + t2 + t3) is approx. " + MathHelpers.round(var(resCov), 2));
		System.out.println();
	}

	public static void main(String[] args) {

		System.out.println("                         ");
		System.out.println("          link 1         ");
		System.out.println("      -------------      ");
		System.out.println("     /             \\    ");
		System.out.println("    /               \\   ");
		System.out.println("-> o                 o ->");
		System.out.println("    \\               /   ");
		System.out.println("     \\             /    ");
		System.out.println("      ----- o -----      ");
		System.out.println("    link 2     link 3    ");
		System.out.println();

		System.out.println("capacity on link 1 is c1 = " + c1);
		System.out.println("capacity on link 2 is c2 = " + c2);
		System.out.println("capacity on link 3 is c3 = " + c3);
		System.out.println();

		System.out.println("g = " + g);
		System.out.println();

		System.out.println("travel time t1 on path (1) is 1/c1 = " + 1 / c1);
		System.out.println("travel time t2 + t3 on path (2,3) is 1/c2 + 1/c3 = " + (1 / c2 + 1 / c3));
		System.out.println();

		prnResult("Link 1 is one group.", 1, 0, 0);
		prnResult("Link 2 is one group.", 0, 1, 0);
		prnResult("Link 3 is one group.", 0, 0, 1);

		// Double bestA11 = null;
		// Double bestA21 = null;
		// Double bestA31 = null;
		// Double bestVar = Double.POSITIVE_INFINITY;
		// final double inc = 0.01;
		// for (double a11 = 0.0; a11 <= 1.0; a11 += inc) {
		// for (double a21 = 0.0; a21 <= 1.0; a21 += inc) {
		// if (Math.abs(a21 - a11) > inc) {
		// for (double a31 = 0.0; a31 <= 1.0; a31 += inc) {
		// if (Math.abs(a31 - a11) > inc && Math.abs(a31 - a21) > inc) {
		// final Double var = var(errorCov(a11, a21, a31));
		// if (var != null && var < bestVar) {
		// bestVar = var;
		// bestA11 = a11;
		// bestA21 = a21;
		// bestA31 = a31;
		// System.out.println(bestA11 + ", " + bestA21 + ", " + bestA31 + " -> " +
		// bestVar);
		// }
		// }
		// }
		// }
		// }
		// }

	}

}
