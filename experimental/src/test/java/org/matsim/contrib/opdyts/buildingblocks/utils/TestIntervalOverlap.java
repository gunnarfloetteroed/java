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

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TestIntervalOverlap {

	@Test
	public void testIntervalOverlap1() {
		/*-
		 * |-----|
		 *          |-----|
		 */
		double start1 = 0.0;
		double end1 = 1.0;
		double start2 = 2.0;
		double end2 = 3.0;
		double overlap = DiscretizationChanger.overlap(start1, end1, start2, end2);
		Assert.assertEquals(0, overlap, 1e-8);
	}

	@Test
	public void testIntervalOverlap2() {
		/*-
		 *    |-----|
		 *       |-----|
		 */
		double start1 = 0.0;
		double end1 = 2.0;
		double start2 = 1.0;
		double end2 = 3.0;
		double overlap = DiscretizationChanger.overlap(start1, end1, start2, end2);
		Assert.assertEquals(1.0, overlap, 1e-8);
	}

	@Test
	public void testIntervalOverlap3() {
		/*-
		 *    |--------|
		 *       |-----|
		 */
		double start1 = 0.0;
		double end1 = 3.0;
		double start2 = 1.0;
		double end2 = 3.0;
		double overlap = DiscretizationChanger.overlap(start1, end1, start2, end2);
		Assert.assertEquals(2.0, overlap, 1e-8);
	}

	@Test
	public void testIntervalOverlap4() {
		/*-
		 *       |-----|
		 *       |-----|
		 */
		double start1 = 1.0;
		double end1 = 3.0;
		double start2 = 1.0;
		double end2 = 3.0;
		double overlap = DiscretizationChanger.overlap(start1, end1, start2, end2);
		Assert.assertEquals(2.0, overlap, 1e-8);
	}

	@Test
	public void testIntervalOverlap5() {
		/*-
		 *       |--------|
		 *       |-----|
		 */
		double start1 = 1.0;
		double end1 = 4.0;
		double start2 = 1.0;
		double end2 = 3.0;
		double overlap = DiscretizationChanger.overlap(start1, end1, start2, end2);
		Assert.assertEquals(2.0, overlap, 1e-8);
	}

	@Test
	public void testIntervalOverlap6() {
		/*-
		 *          |-----|
		 *       |-----|
		 */
		double start1 = 2.0;
		double end1 = 4.0;
		double start2 = 1.0;
		double end2 = 3.0;
		double overlap = DiscretizationChanger.overlap(start1, end1, start2, end2);
		Assert.assertEquals(1.0, overlap, 1e-8);
	}

	@Test
	public void testIntervalOverlap7() {
		/*-
		 *             |-----|
		 *       |-----|
		 */
		double start1 = 3.0;
		double end1 = 5.0;
		double start2 = 1.0;
		double end2 = 3.0;
		double overlap = DiscretizationChanger.overlap(start1, end1, start2, end2);
		Assert.assertEquals(0.0, overlap, 1e-8);
	}

	@Test
	public void testIntervalOverlap8() {
		/*-
		 *       |-----|
		 *    |-----------| 
		 */
		double start1 = 1.0;
		double end1 = 3.0;
		double start2 = 0.0;
		double end2 = 4.0;
		double overlap = DiscretizationChanger.overlap(start1, end1, start2, end2);
		Assert.assertEquals(2.0, overlap, 1e-8);
	}

}
