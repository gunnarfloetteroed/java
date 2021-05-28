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
package org.matsim.contrib.greedo.variabilityanalysis;

import static java.lang.Math.max;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SlotStatistics<P> {

	private Set<P> newVisitors = new LinkedHashSet<>();

	private Set<P> oldVisitors = new LinkedHashSet<>();

	private final LinkedList<Set<P>> pastArrivals;

	final double[] arrivalCoeffs;

	final double[] departureCoeffs;

	private double unexplainedTrend;

	private Double lastArrivalError = null;
	private Double lastDepartureError = null;
	private Double lastVisitorError = null;

	public SlotStatistics(final int memory) {
		this.pastArrivals = new LinkedList<>();
		for (int l = 0; l < memory; l++) {
			this.pastArrivals.add(new LinkedHashSet<>(0));
		}
		this.arrivalCoeffs = new double[memory];
		this.departureCoeffs = new double[memory];
	}

	public int memory() {
		return this.arrivalCoeffs.length;
	}

	public void addNewVisitor(final P particle) {
		this.newVisitors.add(particle);
	}

	private Integer recurrence(final P particle) {
		int recurrence = 0;
		for (Set<P> previous : this.pastArrivals) {
			if (previous.contains(particle)) {
				return recurrence;
			}
			recurrence++;
		}
		return null;
	}

	public double predictedArrivals() {
		double result = 0;
		for (int l = 0; l < this.memory(); l++) {
			result += this.arrivalCoeffs[l] * this.pastArrivals.get(l).size();
		}
		return result;
	}

	public double predictedDepartures() {
		double result = 0;
		for (int l = 0; l < this.memory(); l++) {
			result += this.departureCoeffs[l] * this.pastArrivals.get(l).size();
		}
		return result;
	}

	public double predictedVisits() {
		return (this.oldVisitors.size() + this.predictedArrivals() - this.predictedDepartures()
				+ this.unexplainedTrend);
	}

	public void finalizeNewVisitors(final double innoWeight) {

		final Set<P> arrivals = new LinkedHashSet<>(this.newVisitors);
		arrivals.removeAll(this.oldVisitors);
		final double[] recurrentArrivals = new double[this.memory()];
		for (P particle : arrivals) {
			final Integer l = this.recurrence(particle);
			if (l != null) {
				recurrentArrivals[l]++;
			}
		}

		final Set<P> departures = new LinkedHashSet<>(this.oldVisitors);
		departures.removeAll(this.newVisitors);
		final double[] recurrentDepartures = new double[this.memory()];
		for (P particle : departures) {
			final Integer l = this.recurrence(particle);
			if (l != null) {
				recurrentDepartures[l]++;
			}
		}

		this.lastArrivalError = this.predictedArrivals() - arrivals.size();
		this.lastDepartureError = this.predictedDepartures() - departures.size();
		this.lastVisitorError = this.predictedVisits() - this.newVisitors.size();

		double predictedNewVisitors = this.oldVisitors.size() + this.unexplainedTrend;
		for (int l = 0; l < this.memory(); l++) {
			final double pastArrivalCnt = this.pastArrivals.get(l).size();
			this.arrivalCoeffs[l] = (1.0 - innoWeight) * this.arrivalCoeffs[l]
					+ innoWeight * (recurrentArrivals[l] / max(1e-8, pastArrivalCnt));
			this.departureCoeffs[l] = (1.0 - innoWeight) * this.departureCoeffs[l]
					+ innoWeight * (recurrentDepartures[l] / max(1e-8, pastArrivalCnt));
			predictedNewVisitors += (this.arrivalCoeffs[l] - this.departureCoeffs[l]) * pastArrivalCnt;
		}
		this.unexplainedTrend = (1.0 - innoWeight) * this.unexplainedTrend
				+ innoWeight * (this.newVisitors.size() - predictedNewVisitors);

		this.pastArrivals.addFirst(arrivals);
		this.pastArrivals.removeLast();

		this.oldVisitors = this.newVisitors;
		this.newVisitors = new LinkedHashSet<>();
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("E(visits)=" + this.lastVisitorError + "\tE(arrivals)=" + this.lastArrivalError
				+ "\tE(departures)=" + this.lastDepartureError + "\t");
		for (int l = 0; l < this.memory(); l++) {
			result.append("a(" + l + ")=" + this.arrivalCoeffs[l] + "\t");
		}
		for (int l = 0; l < this.memory(); l++) {
			result.append("d(" + l + ")=" + this.departureCoeffs[l] + "\t");
		}
		result.append("unexplainedTrend=" + this.unexplainedTrend);
		return result.toString();
	}

	public Double getLastArrivalError() {
		return lastArrivalError;
	}

	public Double getLastDepartureError() {
		return lastDepartureError;
	}

	public Double getLastVisitorError() {
		return lastVisitorError;
	}
}
