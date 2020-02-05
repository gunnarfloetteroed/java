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
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import modalsharecalibrator.ModalShareCalibrator.Mode;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ModalShareCalibratorTest {

	final Random rnd = new Random(4711);

	private int indexOfMax(final double[] utils, final Double[] ascs) {
		double[] randomUtils = new double[utils.length];
		for (int i = 0; i < randomUtils.length; i++) {
			randomUtils[i] = utils[i] + ascs[i] + this.rnd.nextGaussian();
		}

		int result = 0;
		for (int i = 1; i < utils.length; i++) {
			if (randomUtils[i] > randomUtils[result]) {
				result = i;
			}
		}
		return result;
	}

	@Test
	public void test() {

		System.out.println("STARTED ...");

		final String[] modes = new String[] { "car", "pt", "walk", "bike" };
		final int _N = 1000;
		final double[][] utils = new double[_N][];
		for (int n = 0; n < _N; n++) {
			utils[n] = new double[] { rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble() };
		}
		final Double[] ascs = new Double[] { 0.0, 0.0, 0.0, 0.0 };

 		final ModalShareCalibrator calibrator = new ModalShareCalibrator();

		calibrator.addMode("car", 0.5);
		calibrator.addMode("pt", 0.3);
		calibrator.addMode("walk", 0.1);
		calibrator.addMode("bike", 0.1);

		final Random rnd = new Random();
		final int _R = 100;
		for (int r = 0; r < _R; r++) {
			for (int n = 0; n < _N; n++) {
				final String choice = modes[this.indexOfMax(utils[n], ascs)];
				final Map<String, Integer> mode2count = new LinkedHashMap<>();
				mode2count.put(choice, 1);

				final String randomChoice = modes[rnd.nextInt(modes.length)];
				mode2count.put(randomChoice, mode2count.getOrDefault(randomChoice, 0) + 1);

				calibrator.setSimulatedData(Id.createPersonId(n), mode2count, r);
			}

			final Map<Mode, Integer> simulatedCounts = calibrator.newMode2simulatedCounts();
			final Map<Mode, Double> deltaASC = calibrator.getDeltaASC();
			ascs[0] += deltaASC.get(calibrator.getMode("car"));
			ascs[1] += deltaASC.get(calibrator.getMode("pt"));
			ascs[2] += deltaASC.get(calibrator.getMode("walk"));
			ascs[3] += deltaASC.get(calibrator.getMode("bike"));

			System.out.println("\tcar\tsim=" + simulatedCounts.get(calibrator.getMode("car")));
			System.out.println("\tpt\tsim=" + simulatedCounts.get(calibrator.getMode("pt")));
			System.out.println("\twalk\tsim=" + simulatedCounts.get(calibrator.getMode("walk")));
			System.out.println("\tbikt\tsim=" + simulatedCounts.get(calibrator.getMode("bike")));
		}

		System.out.println("... DONE");

	}

}
