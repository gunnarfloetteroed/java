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
package org.matsim.contrib.opdyts.macrostate;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RecursiveCountAverageTest {

	final static double eps = 1e-8;

	@Test
	public void test() {
		final RecursiveCountAverage avg = new RecursiveCountAverage(0.0);
		assertEquals(0.0, avg.getAverage(), eps);
		assertEquals(0.0, avg.getInitialTime(), eps);
		assertEquals(0.0, avg.getFinalTime(), eps);

		avg.inc(1.0);
		assertEquals(0.0, avg.getAverage(), eps);
		assertEquals(0.0, avg.getInitialTime(), eps);
		assertEquals(1.0, avg.getFinalTime(), eps);

		avg.inc(2.0);		
		assertEquals(0.5, avg.getAverage(), eps);
		assertEquals(0.0, avg.getInitialTime(), eps);
		assertEquals(2.0, avg.getFinalTime(), eps);

		avg.inc(2.0);		
		assertEquals(0.5, avg.getAverage(), eps);
		assertEquals(0.0, avg.getInitialTime(), eps);
		assertEquals(2.0, avg.getFinalTime(), eps);

		avg.advanceTo(4.0);
		assertEquals(7.0 / 4.0, avg.getAverage(), eps);
		assertEquals(0.0, avg.getInitialTime(), eps);
		assertEquals(4.0, avg.getFinalTime(), eps);
	}
}
