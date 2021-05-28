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

import java.util.Random;

import floetteroed.utilities.math.BasicStatistics;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TestExpNorms {

	static Random rnd = new Random();
	static int repl = 1000;
	static int dim = 1000;
	static double proba = 100.0 / dim;
	static boolean doExp = true;

	static double val() {
		if (rnd.nextDouble() < proba) {
			if (rnd.nextBoolean()) {
				return 1.0;
			} else {
				return -1.0;
			}
		} else {
			return 0.0;
		}
	}

	public static void main(String[] args) {
		System.out.println("muLambdaX\tmuLambdaY\tmean");
		for (double muLambdaX = 0.0; muLambdaX < 10.001; muLambdaX += 0.1) {
//			for (double muLambdaY = 0.0; muLambdaY < 10.001; muLambdaY += 0.5) 
			double muLambdaY = muLambdaX;
			{
				BasicStatistics stats = new BasicStatistics();
				for (int r = 0; r < repl; r++) {
					double x2Sum = 0.0;
					double y2Sum = 0.0;
					double xySum = 0.0;
					for (int i = 0; i < dim; i++) {
						double x = muLambdaX * val();
						double y = muLambdaY * val();
						if (doExp) {
							x = Math.exp(x);
							y = Math.exp(y);
						} else {
							x = Math.max(0.0, x);
							y = Math.max(0.0, y);
						}
						x2Sum += x * x;
						y2Sum += y * y;
						xySum += x * y;
					}
					stats.add(xySum / Math.sqrt(x2Sum) / Math.sqrt(y2Sum));
				}
				System.out.println((muLambdaX + "\t" + muLambdaY + "\t" + stats.getAvg()).replace('.', ','));
			}
		}
	}
}
