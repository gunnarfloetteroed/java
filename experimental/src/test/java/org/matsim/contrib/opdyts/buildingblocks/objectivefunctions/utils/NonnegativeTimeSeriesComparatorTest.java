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
package org.matsim.contrib.opdyts.buildingblocks.objectivefunctions.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class NonnegativeTimeSeriesComparatorTest {

	@Test
	public void test() {

		NonnegativeTimeSeriesComparator comp = new NonnegativeTimeSeriesComparator();
		
		double[] x = new double[] { 0.0, 0.0, 0.0, 0.0 };
		double[] y = new double[] { 0.0, 0.0, 0.0, 0.0 };
		comp.compute(x, y);
		Assert.assertEquals(0.0, comp.getAbsoluteDifference(), 1e-8);
		Assert.assertEquals(0.0, comp.getEarthMoverDistance(), 1e-8);

		x = new double[] { 0.0, 1.0, 0.0, 0.0 };
		y = new double[] { 0.0, 0.0, 0.0, 0.0 };
		comp.compute(x, y);
		Assert.assertEquals(1.0, comp.getAbsoluteDifference(), 1e-8);
		Assert.assertEquals(0.0, comp.getEarthMoverDistance(), 1e-8);

		x = new double[] { 0.0, 0.0, 0.0, 0.0 };
		y = new double[] { 0.0, 1.0, 0.0, 0.0 };
		comp.compute(x, y);
		Assert.assertEquals(1.0, comp.getAbsoluteDifference(), 1e-8);
		Assert.assertEquals(0.0, comp.getEarthMoverDistance(), 1e-8);

		x = new double[] { 0.0, 0.0, 1.0, 0.0 };
		y = new double[] { 0.0, 1.0, 0.0, 0.0 };
		comp.compute(x, y);
		Assert.assertEquals(0.0, comp.getAbsoluteDifference(), 1e-8);
		Assert.assertEquals(1.0, comp.getEarthMoverDistance(), 1e-8);

		x = new double[] { 2.0, 0.0, 0.0, 0.0 };
		y = new double[] { 0.0, 0.0, 1.0, 1.0 };
		comp.compute(x, y);
		Assert.assertEquals(0.0, comp.getAbsoluteDifference(), 1e-8);
		Assert.assertEquals(5.0, comp.getEarthMoverDistance(), 1e-8);

		x = new double[] { 2.0, 0.0, 0.0, 0.0 };
		y = new double[] { 0.0, 0.0, 2.0, 2.0 };
		comp.compute(x, y);
		Assert.assertEquals(2.0, comp.getAbsoluteDifference(), 1e-8);
		Assert.assertEquals(5.0, comp.getEarthMoverDistance(), 1e-8);

		x = new double[] { 2.0, 2.0, 0.0, 0.0 };
		y = new double[] { 0.0, 1.0, 1.0, 0.0 };
		comp.compute(x, y);
		Assert.assertEquals(2.0, comp.getAbsoluteDifference(), 1e-8);
		Assert.assertEquals(2.0, comp.getEarthMoverDistance(), 1e-8);
		
		x = new double[] { 0.0, 0.0, 2.0, 2.0 };
		y = new double[] { 0.0, 1.0, 1.0, 0.0 };
		comp.compute(x, y);
		Assert.assertEquals(2.0, comp.getAbsoluteDifference(), 1e-8);
		Assert.assertEquals(2.0, comp.getEarthMoverDistance(), 1e-8);
	}
}
