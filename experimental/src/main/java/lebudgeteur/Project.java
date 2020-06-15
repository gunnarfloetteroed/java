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

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class Project {

	private final double[] targetFundings;

	private final double[] maxDeferrals;

	private final double[] minConsumptions;

	private final int index;

	Project(int duration, int index) {
		this.targetFundings = new double[duration];
		this.maxDeferrals = new double[duration];
		this.minConsumptions = new double[duration];
		this.index = index;
	}
	
	int getIndex() {
		return this.index;
	}

	double getTargetFunding(int timeBin) {
		return this.targetFundings[timeBin];
	}

	double getMaxDeferral(int timeBin) {
		return this.maxDeferrals[timeBin];
	}

	double getMinConsumption(int timeBin) {
		return this.minConsumptions[timeBin];
	}

	void setTargetFundings(double[] targetFundings) {
		arraycopy(targetFundings, 0, this.targetFundings, 0, this.targetFundings.length);
	}

	void setMaxDeferrals(double[] maxDeferrals) {
		arraycopy(maxDeferrals, 0, this.maxDeferrals, 0, this.maxDeferrals.length);
	}

	void setMinConsumptions(double[] minConsumptions) {
		arraycopy(minConsumptions, 0, this.minConsumptions, 0, this.minConsumptions.length);
	}
}
