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
package stockholm.ihop2.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LexicographicallyOrderedPositiveNumberStrings {

	private int maxValue;

	private final String zeros;

	public LexicographicallyOrderedPositiveNumberStrings(final int maxValue) {
		this.maxValue = maxValue;
		final int digits;
		if (maxValue == 0) {
			digits = 1;
		} else {
			digits = ((int) Math.log10(maxValue)) + 1;
		}
		final StringBuffer zeroBuffer = new StringBuffer("0");
		while (zeroBuffer.length() < digits) {
			zeroBuffer.append("0");
		}
		this.zeros = zeroBuffer.toString();
	}

	public String toString(final int number) {
		if ((number < 0) || (number > this.maxValue)) {
			throw new IllegalArgumentException(number + " is not in {0,...,"
					+ maxValue + "}");
		}
		final String secondPart = Integer.toString(number);
		return this.zeros.substring(0,
				this.zeros.length() - secondPart.length())
				+ secondPart;
	}
	
	public static final void main(String[] args) {
		
		LexicographicallyOrderedPositiveNumberStrings test = new LexicographicallyOrderedPositiveNumberStrings(100);
		final List<String> list = new ArrayList<>();
		for (int i = 0; i <= 100; i++) {
			list.add(test.toString(i));
		}
		Collections.shuffle(list);
		Collections.sort(list);
		for (String entry : list) {
			System.out.println(entry);
		}
	}

}
