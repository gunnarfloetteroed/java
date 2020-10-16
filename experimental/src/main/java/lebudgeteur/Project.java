/*
 * Copyright 2020 Gunnar Flötteröd
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
package lebudgeteur;

import static java.lang.System.arraycopy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class Project {

	private final double[] targetFundings;

	private final double[] maxDeferrals;

	private final double[] minConsumptions;

	private final Map<String, double[]> otherConsumption = new LinkedHashMap<>();

	private final int index;

	private final String label;

	Project(String label, int duration, int index) {
		this.targetFundings = new double[duration];
		this.maxDeferrals = new double[duration];
		this.minConsumptions = new double[duration];
		this.index = index;
		this.label = label;
	}

	int getIndex() {
		return this.index;
	}

	double getTargetFunding(int timeBin) {
		return this.targetFundings[timeBin];
	}

	double getOtherConsumptions(int timeBin) {
		double result = 0;
		for (double[] otherConsumptions : this.otherConsumption.values()) {
			result += otherConsumptions[timeBin];
		}
		return result;
	}

	double getAvailableFunding(int timeBin) {
		double result = this.targetFundings[timeBin] - this.getOtherConsumptions(timeBin);
		if (result < 0) {
			throw new RuntimeException(label + ", time bin " + timeBin
					+ ": Other consumption exceeds target funding, result is " + result + ".");
		}
		return result;
	}

	double getMaxDeferral(int timeBin) {
		return this.maxDeferrals[timeBin];
	}

	double getMinConsumption(int timeBin) {
		return this.minConsumptions[timeBin];
	}

	double getMinOwnConsumption(int timeBin) {
		return Math.max(0.0, this.minConsumptions[timeBin] - this.getOtherConsumptions(timeBin));
	}

	void setTargetFundings(double[] targetFundings) {
		arraycopy(targetFundings, 0, this.targetFundings, 0, this.targetFundings.length);
	}

	void setMaxDeferrals(double[] maxDeferrals) {
		arraycopy(maxDeferrals, 0, this.maxDeferrals, 0, this.maxDeferrals.length);
	}

	void setMinTotalConsumptions(double[] minConsumptions) {
		arraycopy(minConsumptions, 0, this.minConsumptions, 0, this.minConsumptions.length);
	}

	void putOtherConsumptions(String label, double[] otherConsumptions) {
		this.otherConsumption.put(label, otherConsumptions);
	}

}
