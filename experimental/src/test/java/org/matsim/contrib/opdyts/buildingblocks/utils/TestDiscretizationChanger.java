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
package org.matsim.contrib.opdyts.buildingblocks.utils;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TestDiscretizationChanger {

	@Test
	public void testTotals() {

		final int fromStartTime_s = 1000;
		final int fromBinSize_s = 100;
		double[] fromData = new double[] { 0.0, 1.0 };

		final int fromBinCnt = fromData.length;
		TimeDiscretization fromDiscr = new TimeDiscretization(fromStartTime_s, fromBinSize_s, fromBinCnt);
		DiscretizationChanger.DataType dataType = DiscretizationChanger.DataType.TOTALS;
		DiscretizationChanger dc = new DiscretizationChanger(fromDiscr, fromData, dataType);

		// identical copy
		dc.run(new TimeDiscretization(fromStartTime_s, fromBinSize_s, fromBinCnt));
		Assert.assertEquals(2, dc.getToTotalsCopy().length);
		Assert.assertEquals(0.0, dc.getToTotalsCopy()[0], 1e-8);
		Assert.assertEquals(1.0, dc.getToTotalsCopy()[1], 1e-8);

		// split one in two
		dc.run(new TimeDiscretization(fromStartTime_s, fromBinSize_s / 2, fromBinCnt * 2));
		Assert.assertEquals(4, dc.getToTotalsCopy().length);
		Assert.assertEquals(0.0, dc.getToTotalsCopy()[0], 1e-8);
		Assert.assertEquals(0.0, dc.getToTotalsCopy()[1], 1e-8);
		Assert.assertEquals(0.5, dc.getToTotalsCopy()[2], 1e-8);
		Assert.assertEquals(0.5, dc.getToTotalsCopy()[3], 1e-8);

		// merge two in one
		dc.run(new TimeDiscretization(fromStartTime_s, fromBinSize_s * 2, fromBinCnt / 2));
		Assert.assertEquals(1, dc.getToTotalsCopy().length);
		Assert.assertEquals(1.0, dc.getToTotalsCopy()[0], 1e-8);

		// contained in one larger outer bin
		dc.run(new TimeDiscretization(fromStartTime_s - 100, fromBinSize_s * fromBinCnt + 200, 1));
		Assert.assertEquals(1, dc.getToTotalsCopy().length);
		Assert.assertEquals(1.0, dc.getToTotalsCopy()[0], 1e-8);

		// shifted smaller bins covering all from bins
		dc.run(new TimeDiscretization(fromStartTime_s - fromBinSize_s / 2, fromBinSize_s, fromBinCnt + 1));
		Assert.assertEquals(3, dc.getToTotalsCopy().length);
		Assert.assertEquals(0.0, dc.getToTotalsCopy()[0], 1e-8);
		Assert.assertEquals(0.5, dc.getToTotalsCopy()[1], 1e-8);
		Assert.assertEquals(0.5, dc.getToTotalsCopy()[2], 1e-8);

		// shifted smaller bins not covering all from bins
		dc.run(new TimeDiscretization(fromStartTime_s - fromBinSize_s / 2, fromBinSize_s, fromBinCnt));
		Assert.assertEquals(2, dc.getToTotalsCopy().length);
		Assert.assertEquals(0.0, dc.getToTotalsCopy()[0], 1e-8);
		Assert.assertEquals(0.5, dc.getToTotalsCopy()[1], 1e-8);

		// shifted smaller bins not covering all from bins
		dc.run(new TimeDiscretization(fromStartTime_s + fromBinSize_s / 2, fromBinSize_s, fromBinCnt));
		Assert.assertEquals(2, dc.getToTotalsCopy().length);
		Assert.assertEquals(0.5, dc.getToTotalsCopy()[0], 1e-8);
		Assert.assertEquals(0.5, dc.getToTotalsCopy()[1], 1e-8);
	}

	@Test
	public void selfReproductionTest() {

		Random rnd = new Random(4711);
		TimeDiscretization fromDiscr = new TimeDiscretization(0, 3600, 24);

		double[] fromData = new double[24];
		for (int k = 0; k < 24; k++) {
			fromData[k] = rnd.nextDouble();
		}

		for (DiscretizationChanger.DataType dataType : new DiscretizationChanger.DataType[] {
				DiscretizationChanger.DataType.TOTALS, DiscretizationChanger.DataType.RATES }) {

			DiscretizationChanger fromDC = new DiscretizationChanger(fromDiscr, fromData, dataType);

			for (int i : new int[] { 1, 2, 4 }) {

				TimeDiscretization toDiscr = new TimeDiscretization(0, 3600 / i, 24 * i);
				fromDC.run(toDiscr);

				double[] backInData = null;
				if (DiscretizationChanger.DataType.TOTALS.equals(dataType)) {
					backInData = fromDC.getToTotalsCopy();
				} else if (DiscretizationChanger.DataType.RATES.equals(dataType)) {
					backInData = fromDC.getToRatesCopy();
				}
				DiscretizationChanger backDC = new DiscretizationChanger(toDiscr, backInData, dataType);
				backDC.run(fromDiscr);

				Assert.assertArrayEquals(fromDC.getFromTotalsCopy(), backDC.getToTotalsCopy(), 1e-8);
				Assert.assertArrayEquals(fromDC.getFromRatesCopy(), backDC.getToRatesCopy(), 1e-8);
			}
		}
	}
}
