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

import java.util.List;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class Plans {

	final List<int[]> allXn;

	final List<Double> realizedCosts;

	final List<Double> freeFlowCosts;

	// Array index: links. Values in allXn are indices of deltaX.
	final double[] x;

	Plans(final List<int[]> allXn, final List<Double> allRealizedCosts, final List<Double> allFreeFlowCosts,
			final int linkCnt) {
		this.allXn = allXn;
		this.realizedCosts = allRealizedCosts;
		this.freeFlowCosts = allFreeFlowCosts;

		this.x = new double[linkCnt];
		for (int[] xn : allXn) {
			for (int index : xn) {
				x[index]++;
			}
		}
	}
}
