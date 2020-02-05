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
package org.matsim.contrib.opdyts.buildingblocks.calibration.counting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.matsim.core.controler.AbstractModule;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CountMeasurements {

	// -------------------- CONSTANTS --------------------

	private Map<CountMeasurementSpecification, double[]> measSpec2data = new LinkedHashMap<>();

	private final Supplier<Double> simulatedPopulationShare;

	final Function<Double, Double> residualEvaluator;

	// -------------------- MEMBERS --------------------

	private int simulatedSensorDataExtractionInterval = 1;

	private List<AbstractModule> modules = null;

	// 2018-12-06 New, for event file analysis.
	private List<LinkEntryCounter> handlers = null;

	private List<LinkEntryCountDeviationObjectiveFunction> objectiveFunctions = null;

	private Double sumOfEvaluatedResidualsAtZeroSimulation = null;

	// -------------------- CONSTRUCTION --------------------

	public CountMeasurements(final Supplier<Double> simulatedPopulationShare,
			final Function<Double, Double> residualEvaluator) {
		this.simulatedPopulationShare = simulatedPopulationShare;
		this.residualEvaluator = residualEvaluator;
	}

	// -------------------- SETTERS --------------------

	public void setSimulatedSensorDataExtractionInterval(final int extractionInterval) {
		this.simulatedSensorDataExtractionInterval = extractionInterval;
	}

	// -------------------- COMPOSITION --------------------

	public void addMeasurement(final CountMeasurementSpecification spec, double[] data) {
		this.measSpec2data.put(spec, data);
	}

	// -------------------- BUILDING --------------------

	// public void build() {
	//
	// this.sumOfEvaluatedResidualsAtZeroSimulation = 0.0;
	//
	// this.modules = new ArrayList<>(this.measSpec2data.size());
	// this.handlers = new ArrayList<>(this.measSpec2data.size());
	// this.objectiveFunctions = new ArrayList<>(this.measSpec2data.size());
	// final Map<CountMeasurementSpecification, LinkEntryCounter>
	// measSpec2linkEntryCounter = new LinkedHashMap<>();
	//
	// for (Map.Entry<CountMeasurementSpecification, double[]> entry :
	// this.measSpec2data.entrySet()) {
	// final CountMeasurementSpecification spec = entry.getKey();
	//
	// final LinkEntryCounter simCounter;
	// if (measSpec2linkEntryCounter.containsKey(spec)) {
	// simCounter = measSpec2linkEntryCounter.get(spec);
	// } else {
	// simCounter = new LinkEntryCounter(spec);
	// measSpec2linkEntryCounter.put(spec, simCounter);
	// this.modules.add(new AbstractModule() {
	// @Override
	// public void install() {
	// this.addEventHandlerBinding().toInstance(simCounter);
	// this.addControlerListenerBinding().toInstance(simCounter);
	// }
	// });
	// this.handlers.add(simCounter);
	// }
	//
	// final double[] data = entry.getValue();
	// this.objectiveFunctions.add(new
	// LinkEntryCountDeviationObjectiveFunction(data, simCounter,
	// this.residualEvaluator, this.simulatedPopulationShare));
	// for (Double count : data) {
	// this.sumOfEvaluatedResidualsAtZeroSimulation +=
	// this.residualEvaluator.apply(count);
	// }
	// }
	// }

	public void build(final int startTime_s, final int endTime_s) {

		this.sumOfEvaluatedResidualsAtZeroSimulation = 0.0;

		this.modules = new ArrayList<>(this.measSpec2data.size());
		this.handlers = new ArrayList<>(this.measSpec2data.size());
		this.objectiveFunctions = new ArrayList<>(this.measSpec2data.size());
		final Map<CountMeasurementSpecification, LinkEntryCounter> measSpec2linkEntryCounter = new LinkedHashMap<>();

		for (Map.Entry<CountMeasurementSpecification, double[]> entry : this.measSpec2data.entrySet()) {
			final CountMeasurementSpecification spec = entry.getKey();

			final LinkEntryCounter simCounter;
			if (measSpec2linkEntryCounter.containsKey(spec)) {
				simCounter = measSpec2linkEntryCounter.get(spec);
			} else {
				
				simCounter = new LinkEntryCounter(spec);
				simCounter.setSimulatedDataExtractionInterval(this.simulatedSensorDataExtractionInterval);
				
				measSpec2linkEntryCounter.put(spec, simCounter);
				this.modules.add(new AbstractModule() {
					@Override
					public void install() {
						this.addEventHandlerBinding().toInstance(simCounter);
						this.addControlerListenerBinding().toInstance(simCounter);
					}
				});
				this.handlers.add(simCounter);
			}

			final TimeDiscretization timeDiscr = spec.getTimeDiscretization();
			final int startBin_inclusive = Math.max(0, timeDiscr.getBin(startTime_s));
			final int endBin_exclusive = Math.min(timeDiscr.getBin(endTime_s), timeDiscr.getBinCnt() - 1) + 1;

			final double[] data = entry.getValue();
			final LinkEntryCountDeviationObjectiveFunction objFct = new LinkEntryCountDeviationObjectiveFunction(data,
					simCounter, this.residualEvaluator, this.simulatedPopulationShare);
			objFct.setStartEndBin(startBin_inclusive, endBin_exclusive);
			this.objectiveFunctions.add(objFct);
			// for (Double count : data) {
			// this.sumOfEvaluatedResidualsAtZeroSimulation +=
			// this.residualEvaluator.apply(count);
			// }
			for (int bin = startBin_inclusive; bin < endBin_exclusive; bin++) {
				this.sumOfEvaluatedResidualsAtZeroSimulation += this.residualEvaluator.apply(data[bin]);
			}
		}
	}

	// -------------------- GETTERS --------------------

	public List<LinkEntryCounter> getHandlers() {
		return this.handlers;
	}

	public List<AbstractModule> getModules() {
		return this.modules;
	}

	public List<LinkEntryCountDeviationObjectiveFunction> getObjectiveFunctions() {
		return this.objectiveFunctions;
	}

	public Double getSumOfEvaluatdResidualsAtZeroSimulation() {
		return this.sumOfEvaluatedResidualsAtZeroSimulation;
	}

}
