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
package org.matsim.contrib.greedo;

import java.util.Random;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ThreePointProjector {

	// TODO encapsulate
	public double[] z1 = null;
	public double[] z2 = null;
	public double[] z3 = null;

	public void update(final double[] x1, final double[] x2, final double[] x3) {
		double d12 = 0;
		double d23 = 0;
		double d31 = 0;
		for (int i = 0; i < x1.length; i++) {
			d12 += Math.pow(x1[i] - x2[i], 2.0);
			d23 += Math.pow(x2[i] - x3[i], 2.0);
			d31 += Math.pow(x3[i] - x1[i], 2.0);
		}
		d12 = Math.sqrt(d12);
		d23 = Math.sqrt(d23);
		d31 = Math.sqrt(d31);
		this.update(d12, d23, d31);
	}

	public void update(final double d12, final double d23, final double d31) {
		double[] avg = new double[2];
		
		this.z1 = new double[] { 0.0, 0.0 };
		
		this.z2 = new double[] { d12, 0.0 };
		avg[0] += this.z2[0];
		
		final double z31 = (d31 * d31 - d23 * d23 + d12 * d12) / 2.0 / d12;
		this.z3 = new double[] { z31, Math.sqrt(d31 * d31 - z31 * z31) };
		avg[0] += this.z3[0];
		avg[1] += this.z3[1];
		
		avg[0] /= 3.0;
		avg[1] /= 3.0;
		
		this.z1[0] -= avg[0];
		this.z2[0] -= avg[0];
		this.z3[0] -= avg[0];
		
		this.z1[1] -= avg[1];
		this.z2[1] -= avg[1];
		this.z3[1] -= avg[1];
	}

	public static void main(String[] args) {
		final ThreePointProjector tpp = new ThreePointProjector();
		final Random rnd = new Random();
		final int dim = 100;
		for (int r = 0; r < 50; r++) {
			double[] x1 = new double[dim];
			double[] x2 = new double[dim];
			double[] x3 = new double[dim];
			for (int i = 0; i < dim; i++) {
				x1[i] = Math.pow(rnd.nextGaussian(), 2.0);
				x2[i] = 1 + rnd.nextGaussian();
				x3[i] = rnd.nextGaussian();
			}
			tpp.update(x1, x2, x3);
			System.out.println(tpp.z1[0] + "\t" + tpp.z1[1] + "\t" + tpp.z2[0] + "\t" + tpp.z2[1] + "\t" + tpp.z3[0]
					+ "\t" + tpp.z3[1]);
		}
	}

}
