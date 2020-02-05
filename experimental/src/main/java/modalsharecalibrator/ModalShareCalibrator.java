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
package modalsharecalibrator;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import floetteroed.utilities.math.MathHelpers;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ModalShareCalibrator {

	// -------------------- INNER CLASS --------------------

	public static class Mode {

		public final String name;

		public final Double realShare; // may be null for an unobserved mode

		Mode(final String name, final Double realShare) {
			this.name = name;
			this.realShare = realShare;
		}
	}

	// -------------------- CONSTANTS --------------------

	// private final double initialTrustRegion;

	// private final double iterationExponent;

	// private final boolean useSolutionDerivatives;

	// -------------------- MEMBERS --------------------

	private final Map<String, Mode> name2mode = new LinkedHashMap<>();

	// private final Map<Id<Person>, Map<Mode, Double>> personId2mode2proba = new
	// LinkedHashMap<>();

	private final Map<Id<Person>, Map<Mode, Integer>> personId2mode2cnt = new LinkedHashMap<>();

	// private Map<Id<Person>, Map<Mode, Integer>> prevPersonId2mode2cnt = null;

	private Map<Mode, Integer> prevSimCounts = null;
	private Map<Mode, Double> prevDeltaASCs = null;

	// -------------------- CONSTRUCTION --------------------

	public ModalShareCalibrator() {
	}

	// public ModalShareCalibrator(final double initialTrustRegion, final double
	// iterationExponent,
	// final boolean useSolutionDerivatives) {
	// this.initialTrustRegion = initialTrustRegion;
	// this.iterationExponent = iterationExponent;
	// this.useSolutionDerivatives = useSolutionDerivatives;
	// }

	// -------------------- INTERNALS --------------------

	private Set<Mode> allObservedModes() {
		final LinkedHashSet<Mode> result = new LinkedHashSet<>();
		this.name2mode.values().stream().filter(m -> (m.realShare != null)).forEach(m -> result.add(m));
		return result;
	}

	/* package (for testing) */ Mode getMode(final String name) {
		return this.name2mode.get(name);
	}

	/* package (for testing) */ double getObjectiveFunctionValue(final Map<Mode, Double> simulatedCounts) {
		final double sum = simulatedCounts.values().stream().mapToDouble(c -> c).sum();
		double result = 0.0;
		for (Mode mode : this.allObservedModes()) {
			result += Math.pow(simulatedCounts.get(mode) - mode.realShare * sum, 2.0) / (mode.realShare * sum);
		}
		return (0.5 * result);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addMode(final String modeName, final Double realShare) {
		final Mode mode = new Mode(modeName, realShare);
		this.name2mode.put(modeName, mode);
	}

	public void clearSimulatedData() {
		// this.prevPersonId2mode2cnt = new LinkedHashMap<>(this.personId2mode2cnt);
		this.personId2mode2cnt.clear();
		// this.personId2mode2proba.clear();
	}

	public void setSimulatedData(final Id<Person> personId, final Map<String, Integer> mode2simCnt,
			final int iteration) {
		final int tripCnt = mode2simCnt.entrySet().stream().filter(e -> this.name2mode.containsKey(e.getKey()))
				.mapToInt(e -> e.getValue()).sum();
		if (tripCnt > 0) {
			final Map<Mode, Integer> mode2cnt = new LinkedHashMap<>();
			final Map<Mode, Double> mode2proba = new LinkedHashMap<>();
			this.personId2mode2cnt.put(personId, mode2cnt);
			// this.personId2mode2proba.put(personId, mode2proba);
			mode2simCnt.entrySet().forEach(e -> {
				final Mode mode = this.name2mode.get(e.getKey());
				if (mode != null) {
					mode2cnt.put(mode, e.getValue());
					mode2proba.put(mode, e.getValue().doubleValue() / tripCnt);
				}
			});
		}
	}

	public Map<Mode, Integer> newMode2simulatedCounts() {
		final Map<Mode, Integer> result = new LinkedHashMap<>();
		this.personId2mode2cnt.values().stream().forEach(mode2cnt -> mode2cnt.entrySet().stream().forEach(
				entry -> result.put(entry.getKey(), result.getOrDefault(entry.getKey(), 0) + entry.getValue())));
		return result;
	}

	public Map<Mode, Integer> newMode2simulatedCounts(final Map<Id<Person>, Map<Mode, Integer>> personId2mode2cnt) {
		final Map<Mode, Integer> result = new LinkedHashMap<>();
		if (personId2mode2cnt != null) {
			personId2mode2cnt.values().stream().forEach(mode2cnt -> mode2cnt.entrySet().stream().forEach(
					entry -> result.put(entry.getKey(), result.getOrDefault(entry.getKey(), 0) + entry.getValue())));
		}
		return result;
	}

	// public Map<Tuple<Mode, Mode>, Double> get_dSimulatedCounts_dASCs(final
	// Map<Mode, Integer> simulatedCounts) {
	// final Map<Tuple<Mode, Mode>, Double> result = new LinkedHashMap<>();
	// for (Mode countMode : this.allObservedModes()) {
	// for (Mode ascMode : this.name2mode.values()) {
	// result.put(new Tuple<>(countMode, ascMode),
	// (countMode.equals(ascMode) ? simulatedCounts.getOrDefault(countMode, 0) :
	// 0.0));
	// }
	// }
	// for (Id<Person> personId : this.personId2mode2proba.keySet()) {
	// for (Map.Entry<Mode, Integer> mode2cnt :
	// this.personId2mode2cnt.get(personId).entrySet()) {
	// for (Map.Entry<Mode, Double> mode2proba :
	// this.personId2mode2proba.get(personId).entrySet()) {
	// final Tuple<Mode, Mode> key = new Tuple<>(mode2cnt.getKey(),
	// mode2proba.getKey());
	// if (result.containsKey(key)) {
	// result.put(key, result.get(key) - mode2cnt.getValue() *
	// mode2proba.getValue());
	// }
	// }
	// }
	// }
	// return result;
	// }

	// public Map<Mode, Double> get_dQ_dASCs(final Map<Mode, Integer>
	// simulatedCounts,
	// final Map<Tuple<Mode, Mode>, Double> dSimulatedCounts_dASCs) {
	// final double sum = simulatedCounts.values().stream().mapToDouble(c ->
	// c).sum();
	//
	// final Map<Mode, Double> dQ_dASC = new LinkedHashMap<>();
	// this.name2mode.values().stream().forEach(mode -> dQ_dASC.put(mode, 0.0));
	//
	// for (Mode mode : this.allObservedModes()) {
	// final double realCount = mode.realShare * sum;
	// final double fact = (simulatedCounts.getOrDefault(mode, 0) - realCount) /
	// realCount;
	// dQ_dASC.entrySet().stream().forEach(
	// e -> e.setValue(e.getValue() + fact * dSimulatedCounts_dASCs.get(new
	// Tuple<>(mode, e.getKey()))));
	// }
	// return dQ_dASC;
	// }

	// public Map<Mode, Double> getDeltaASC(final Map<Mode, Double> dQ_dASC, final
	// int iteration) {
	//
	// final double unconstrStepLength =
	// Math.sqrt(dQ_dASC.values().stream().mapToDouble(v -> v * v).sum());
	// final double maxStepLength = this.initialTrustRegion
	// * Math.pow(1.0 / (iteration + 1.0), this.iterationExponent);
	// final double eta = Math.min(1.0, maxStepLength / unconstrStepLength);
	//
	// final Map<Mode, Double> result = new LinkedHashMap<>();
	// dQ_dASC.entrySet().stream().forEach(e -> result.put(e.getKey(), -eta *
	// e.getValue()));
	// return result;
	// }

	private final Logger log = Logger.getLogger(this.getClass());

	private double truncLn(final double cnt) {
		return Math.log(Math.max(cnt, 1));
	}

	public Map<Mode, Double> getDeltaASC() {

		final Map<Mode, Double> deltaASCs = new LinkedHashMap<>();
		final Map<Mode, Integer> simCounts = this.newMode2simulatedCounts(this.personId2mode2cnt);
		final int tripSum = simCounts.values().stream().mapToInt(c -> c).sum();

		final LinkedList<Mode> observedModes = new LinkedList<>(this.allObservedModes());
		final Mode refMode = observedModes.removeFirst();
		deltaASCs.put(refMode, 0.0);
		final int refRealCnt = MathHelpers.round(tripSum * refMode.realShare);
		final double refSimCnt = simCounts.getOrDefault(refMode, 0);

		// if ((this.prevDeltaASCs == null) || (this.prevSimCounts == null)) {

		for (Mode observedMode : observedModes) {
			final double realCnt = tripSum * observedMode.realShare;
			final double simCnt = simCounts.getOrDefault(observedMode, 0);
			deltaASCs.put(observedMode, (this.truncLn(realCnt) - this.truncLn(refRealCnt))
					- (this.truncLn(simCnt) - this.truncLn(refSimCnt)));
		}

		// } else {
		//
		// final double prevRefSimCnt = this.prevSimCounts.get(refMode);
		// final double refSimCntDeduct = (refRealCnt - refSimCnt) / (refRealCnt -
		// prevRefSimCnt) * prevRefSimCnt;
		//
		// // final BoundedRatio br = new BoundedRatio(1e-6, 0.5);
		// // final double prevReferenceSimCnt = this.prevSimCounts.get(referenceMode);
		// //
		// for (Mode observedMode : observedModes) {
		//
		// final double realCnt = tripSum * observedMode.realShare;
		// final double simCnt = simCounts.getOrDefault(observedMode, 0);
		// final double prevSimCnt = this.prevSimCounts.get(observedMode);
		// final double simCntDeduct = (realCnt - simCnt) / (realCnt - prevSimCnt) *
		// prevSimCnt;
		//
		// final double deltaASC = (this.truncLn(realCnt) - this.truncLn(refRealCnt))
		// - (this.truncLn(simCnt - simCntDeduct) - this.truncLn(refSimCnt -
		// refSimCntDeduct));
		// deltaASCs.put(observedMode, deltaASC);
		//
		// // final double num = (this.truncLn(realCnt) -
		// this.truncLn(referenceRealCnt))
		// // - (this.truncLn(simCnt) - this.truncLn(referenceSimCnt));
		// // final double den = (this.truncLn(simCnt) - this.truncLn(prevSimCnt))
		// // - (this.truncLn(referenceSimCnt) - this.truncLn(prevReferenceSimCnt));
		//
		// // final double num = (this.truncLn(realCnt) -
		// this.truncLn(referenceRealCnt))
		// // - (this.truncLn(simCnt) - this.truncLn(referenceSimCnt));
		// // final double den = (this.truncLn(simCnt) - this.truncLn(prevSimCnt))
		// // - (this.truncLn(referenceSimCnt) - this.truncLn(prevReferenceSimCnt));
		// //
		// // deltaASCs.put(observedMode, br.ratio(num, den) *
		// // this.prevDeltaASCs.get(observedMode));
		// }
		// }

		{
			final Map<Mode, Double> simulatedShares = new LinkedHashMap<>();
			simCounts.entrySet().forEach(e -> simulatedShares.put(e.getKey(), e.getValue().doubleValue() / tripSum));
			log.info("============================================================");
			log.info("mode\trealShare\tsimulatedShare");
			for (Mode mode : this.name2mode.values()) {
				log.info(mode.name + "\t" + mode.realShare + "\t" + simulatedShares.getOrDefault(mode, 0.0));
			}
			log.info("============================================================");
		}

		this.prevSimCounts = new LinkedHashMap<>(simCounts);
		this.prevDeltaASCs = new LinkedHashMap<>(deltaASCs);

		return deltaASCs;

		// 2019-10-10 version below.
		//
		// final Map<Mode, Integer> simulatedCounts = this.newMode2simulatedCounts();
		// final Map<Mode, Double> simulatedShares;
		// {
		// final double sum = simulatedCounts.values().stream().mapToDouble(c ->
		// c).sum();
		// simulatedShares = new LinkedHashMap<>();
		// simulatedCounts.entrySet().forEach(e -> simulatedShares.put(e.getKey(),
		// e.getValue() / sum));
		// }
		// final Map<Tuple<Mode, Mode>, Double> dSimulatedCounts_dASCs =
		// this.get_dSimulatedCounts_dASCs(simulatedCounts);
		// final Map<Mode, Double> dQ_dASC = this.get_dQ_dASCs(simulatedCounts,
		// dSimulatedCounts_dASCs);
		// final Map<Mode, Double> result = this.getDeltaASC(dQ_dASC, iteration);
		//
		// log.info("============================================================");
		// log.info("mode\trealShare\tsimulatedShare");
		// for (Mode mode : this.name2mode.values()) {
		// log.info(mode.name + "\t" + mode.realShare + "\t" +
		// simulatedShares.getOrDefault(mode, 0.0));
		// }
		// log.info("------------------------------------------------------------");
		// log.info("observedMode\tascMode\tdSimCnt/dASC");
		// for (Map.Entry<Tuple<Mode, Mode>, Double> entry :
		// dSimulatedCounts_dASCs.entrySet()) {
		// log.info(entry.getKey().getFirst().name + "\t" +
		// entry.getKey().getSecond().name + "\t" + entry.getValue());
		// }
		// log.info("------------------------------------------------------------");
		// log.info("mode\tdeltaASC");
		// for (Map.Entry<Mode, Double> entry : result.entrySet()) {
		// log.info(entry.getKey().name + "\t" + entry.getValue());
		// }
		// log.info("============================================================");
		//
		// return result;
	}
}
