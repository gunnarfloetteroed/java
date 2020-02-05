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
package utils;

import java.util.LinkedList;

/**
 *
 * @author Gunnar Flötteröd
 *
 *         TODO Move to utilities package.
 */
public class MovingWindowAverage {

	// -------------------- CONSTANTS --------------------

	private final int minLength;

	private final int maxLength;

	private final double maxRelativeLength;

	// -------------------- MEMBERS --------------------

	private final LinkedList<Double> data = new LinkedList<>();

	private double sum = 0.0;

	private int totalCount = 0;

	// -------------------- CONSTRUCTION --------------------

	public MovingWindowAverage(final int minLength, final int maxLength, final double maxRelativeLength) {
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.maxRelativeLength = maxRelativeLength;
	}

	public MovingWindowAverage(final int minLength, final int maxLength) {
		this(minLength, maxLength, 1.0);
	}

	public MovingWindowAverage(final int minLength, final double maxShare) {
		this(minLength, Integer.MAX_VALUE, maxShare);
	}

	public MovingWindowAverage() {
		this(1, Integer.MAX_VALUE, 1.0);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void add(final double value) {

		// Add the new value to the beginning of the list.
		this.data.addFirst(value);
		this.sum += value;
		this.totalCount++;

		// Possibly remove old values from the end of the list.
		while ((this.data.size() > this.minLength) && ((this.data.size() > this.maxLength)
				|| (this.data.size() - 1 >= this.maxRelativeLength * this.totalCount))) {
			this.sum -= this.data.removeLast();
			// Do not reduce this.count, which counts all values ever added.
		}
	}

	public Double[] getDataAsDoubleArray() {
		return this.data.toArray(new Double[this.data.size()]);
	}

	public int getMinLength() {
		return this.minLength;
	}

	public int getMaxLength() {
		return this.maxLength;
	}

	public double getMaxRelativeLength() {
		return this.maxRelativeLength;
	}

	public int getTotalCount() {
		return this.totalCount;
	}

	public int size() {
		return this.data.size();
	}

	public double sum() {
		return this.sum;
	}

	public double average() {
		return this.sum / this.data.size();
	}

	public double mostRecentValue() {
		return this.data.getFirst();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	static void test1() {
		MovingWindowAverage avg = new MovingWindowAverage(2, 4, 1.0);
		for (int i = 1; i < 10; i++) {
			avg.add(i);
			System.out.println(avg.data);
		}
		System.out.println();
	}

	static void test2() {
		MovingWindowAverage avg = new MovingWindowAverage(2, Integer.MAX_VALUE, 0.5);
		for (int i = 1; i < 10; i++) {
			avg.add(i);
			System.out.println(avg.size() + " of " + avg.getTotalCount() + ": " + avg.data);
		}
		System.out.println();
	}

	public static void main(String[] args) {
		test1();
		test2();
	}
}
