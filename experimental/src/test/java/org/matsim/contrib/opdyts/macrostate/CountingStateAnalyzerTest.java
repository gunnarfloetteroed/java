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

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CountingStateAnalyzerTest {

	static double eps = 1e-8;

	@Test(expected = RuntimeException.class)
	public void testLocked() {
		CountingStateAnalyzer<String> analyzer = new CountingStateAnalyzer<>(0, 3600, 24);
		analyzer.finalizeAndLock();
		analyzer.registerIncrease("1", 1000);
	}

	@Test(expected = RuntimeException.class)
	public void testCannotDecrease1() {
		CountingStateAnalyzer<String> analyzer = new CountingStateAnalyzer<>(0, 3600, 24);
		analyzer.registerDecrease("1", 1000);
	}

	@Test(expected = RuntimeException.class)
	public void testCannotDecrease2() {
		CountingStateAnalyzer<String> analyzer = new CountingStateAnalyzer<>(0, 3600, 24);
		analyzer.registerIncrease("1", 1000);
		analyzer.registerDecrease("1", 5000);
		analyzer.registerDecrease("1", 9000);
	}

	@Test(expected = RuntimeException.class)
	public void testBinIsLocked() {
		CountingStateAnalyzer<String> analyzer = new CountingStateAnalyzer<>(0, 3600, 24);
		analyzer.registerIncrease("1", 1000);
		analyzer.registerIncrease("1", 5000);
		analyzer.registerDecrease("1", 1000);
	}

	@Test
	public void test() {
		CountingStateAnalyzer<String> analyzer = new CountingStateAnalyzer<>(0, 3600, 24);		

		analyzer.registerIncrease("1", 7 * 3600);

		analyzer.registerIncrease("2", 8 * 3600);
		analyzer.registerIncrease("2", 8 * 3600 + 1800);

		analyzer.registerIncrease("3", 9 * 3600);
		analyzer.registerIncrease("3", 9 * 3600);
		analyzer.registerDecrease("3", 9 * 3600);

		analyzer.registerDecrease("1", 10 * 3600 + 1800);
		
		analyzer.finalizeAndLock();

		assertEquals(0, analyzer.getCount("1", 6), eps);
		assertEquals(1, analyzer.getCount("1", 7), eps);
		assertEquals(1, analyzer.getCount("1", 8), eps);
		assertEquals(1, analyzer.getCount("1", 9), eps);
		assertEquals(0.5, analyzer.getCount("1", 10), eps);

		assertEquals(0, analyzer.getCount("2", 7), eps);
		assertEquals(1.5, analyzer.getCount("2", 8), eps);
		assertEquals(2.0, analyzer.getCount("2", 9), eps);
		
		assertEquals(0, analyzer.getCount("3", 8), eps);
		assertEquals(1, analyzer.getCount("3", 9), eps);
		assertEquals(1, analyzer.getCount("3", 10), eps);
		
		assertEquals(new HashSet<>(Arrays.asList("1", "2", "3")), analyzer.observedLinkSetView());
		
		analyzer.reset();
		assertEquals(new HashSet<>(), analyzer.observedLinkSetView());
	}
}
