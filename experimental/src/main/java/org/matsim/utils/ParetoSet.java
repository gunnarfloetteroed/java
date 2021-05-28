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
package org.matsim.utils;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import floetteroed.utilities.math.MathHelpers;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ParetoSet<R extends Object> {

	// TODO
	public Set<Point<R>> paretoSet = new LinkedHashSet<>();

	public static class Point<R> {
		final double x;
		final double y;
		// TODO
		public final R ref;

		Point(final double x, final double y, final R ref) {
			this.x = x;
			this.y = y;
			this.ref = ref;
		}

		boolean dominates(Point<R> other) {
			return ((this.x < other.x && this.y <= other.y) || (this.x <= other.x && this.y < other.y));
		}

		@Override
		public boolean equals(Object otherObject) {
			if (otherObject instanceof Point) {
				return this.ref.equals(((Point<?>) otherObject).ref);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return ref.hashCode();
		}
		
		@Override
		public String toString() {
			return this.ref + "(" + this.x + "," + this.y + ")";
		}
	}

	void addPoint(Point<R> candidatePoint) {
		final Set<Point<R>> dominatedPoints = new LinkedHashSet<>();
		for (Point<R> existingPoint : this.paretoSet) {
			if (existingPoint.dominates(candidatePoint)) {
				return;
			} else if (candidatePoint.dominates(existingPoint)) {
				dominatedPoints.add(existingPoint);
			}
		}
		this.paretoSet.removeAll(dominatedPoints);
		this.paretoSet.add(candidatePoint);		
	}
	
	public void addPoint(final double x, final double y, final R reference) {
		this.addPoint(new Point<>(x, y, reference));
	}

	public static void main(String[] args) {
		Random rnd = new Random();
		ParetoSet<Integer> pareto = new ParetoSet<>();
		for (int r = 0; r < 20; r++) {
			double x = MathHelpers.round(rnd.nextGaussian(), 2);
			double y = MathHelpers.round(rnd.nextGaussian(), 2);
			Point<Integer> cand = new Point<Integer>(x, y, r);
			System.out.println("testing: " + cand);
			pareto.addPoint(cand);
			System.out.println("resulting: " + pareto.paretoSet);
			System.out.println();
		}
	}
	
}
