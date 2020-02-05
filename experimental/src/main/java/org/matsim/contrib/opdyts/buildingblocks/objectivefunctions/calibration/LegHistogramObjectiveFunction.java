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
package org.matsim.contrib.opdyts.buildingblocks.objectivefunctions.calibration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.analysis.LegHistogram;
import org.matsim.contrib.opdyts.buildingblocks.objectivefunctions.utils.NonnegativeTimeSeriesObjectiveFunction;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
@Deprecated
public class LegHistogramObjectiveFunction extends NonnegativeTimeSeriesObjectiveFunction<MATSimState> {

	public static class StateComponent {
		public final Map<String, int[]> mode2departureData = new LinkedHashMap<>();
		public Map<String, int[]> mode2arrivalData = new LinkedHashMap<>();
	}

	private static enum DepartureArrivalType {
		DEPARTURE, ARRIVAL
	};

	private final DepartureArrivalType departureArrival;

	private final Set<String> legModes;

	private LegHistogramObjectiveFunction(final DepartureArrivalType departureArrival, final Set<String> modes,
			final double[] realData) {
		super(realData);
		this.legModes = modes;
		this.departureArrival = departureArrival;
	}

	public static LegHistogramObjectiveFunction newDepartures(final Set<String> mode, final double[] realData) {
		return new LegHistogramObjectiveFunction(DepartureArrivalType.DEPARTURE, mode, realData);
	}

	public static LegHistogramObjectiveFunction newArrivals(final Set<String> mode, final double[] realData) {
		return new LegHistogramObjectiveFunction(DepartureArrivalType.ARRIVAL, mode, realData);
	}

	// >>>>> TODO NEW, UNTESTED >>>>>

	private LegHistogramObjectiveFunction.StateComponent legHistogramData;

	private AfterMobsimListener myListener = new AfterMobsimListener() {

		@Inject
		private LegHistogram legHist;

		@Override
		public void notifyAfterMobsim(final AfterMobsimEvent event) {
			legHistogramData = new LegHistogramObjectiveFunction.StateComponent();
			for (String mode : this.legHist.getLegModes()) {
				legHistogramData.mode2departureData.put(mode, this.legHist.getDepartures(mode));
				legHistogramData.mode2arrivalData.put(mode, this.legHist.getArrivals(mode));
			}
		}
	};

	@Override
	public AbstractModule newAbstractModule() {
		return new AbstractModule() {
			@Override
			public void install() {
				this.addControlerListenerBinding().toInstance(myListener);
			}			
		};
	}

	// <<<<< TODO NEW, UNTESTED <<<<<

	@Override
	public double[] simData(final MATSimState state) {
		// final StateComponent histogramData =
		// state.getComponent(StateComponent.class);
		final StateComponent histogramData = this.legHistogramData;

		final double[] simData = new double[histogramData.mode2departureData.values().iterator().next().length];
		for (String legMode : this.legModes) {
			final int[] simDataPerMode;
			if (DepartureArrivalType.DEPARTURE.equals(this.departureArrival)) {
				simDataPerMode = histogramData.mode2departureData.get(legMode);
			} else if (DepartureArrivalType.ARRIVAL.equals(this.departureArrival)) {
				simDataPerMode = histogramData.mode2arrivalData.get(legMode);
			} else {
				throw new RuntimeException("Unknown: " + this.departureArrival);
			}
			for (int i = 0; i < simDataPerMode.length; i++) {
				simData[i] += simDataPerMode[i];
			}
		}
		return simData;
	}
}
