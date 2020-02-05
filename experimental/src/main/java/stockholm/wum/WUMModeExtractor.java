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
package stockholm.wum;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigGroup;

import modalsharecalibrator.CalibrationModeExtractor;
import modalsharecalibrator.ModalShareCalibrationConfigGroup;
import modalsharecalibrator.ModalShareCalibrationConfigGroup.TransportModeDataSet;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WUMModeExtractor implements CalibrationModeExtractor {

	// -------------------- MEMBERS --------------------

	private final Map<String, String> subMode2mode;

	private final Set<String> tripTerminatingActivities;

	// -------------------- CONSTRUCTION --------------------

	public WUMModeExtractor(final ModalShareCalibrationConfigGroup calibrConfig,
			final String... tripTerminatingActivities) {
		this.subMode2mode = new LinkedHashMap<>();
		for (ConfigGroup paramSet : calibrConfig.getParameterSets(TransportModeDataSet.TYPE)) {
			final TransportModeDataSet modeDataSet = (TransportModeDataSet) paramSet;
			for (String subMode : modeDataSet.getSubModeSet()) {
				this.subMode2mode.put(subMode, modeDataSet.getMode());
				Logger.getLogger(this.getClass())
						.info("associating submode " + subMode + " with measurement mode " + modeDataSet.getMode());
			}
		}
		this.tripTerminatingActivities = new LinkedHashSet<>(Arrays.asList(tripTerminatingActivities));
	}

	// --------------- IMPLEMENTATION OF CalibrationModeExtractor ---------------

	@Override
	public Map<String, Integer> extractTripModes(final Plan plan) {
		final Map<String, Integer> mode2cnt = new LinkedHashMap<>();
		String currentMode = null;
		for (PlanElement pE : plan.getPlanElements()) {
			if (pE instanceof Activity) {
				final Activity act = (Activity) pE;
				if (this.tripTerminatingActivities.contains(act.getType())) {
					if (currentMode != null) {
						mode2cnt.put(currentMode, mode2cnt.getOrDefault(currentMode, 0) + 1);
					}
					currentMode = null;
				}
			} else if (pE instanceof Leg) {
				final Leg leg = (Leg) pE;
				final String mode = this.subMode2mode.get(leg.getMode());
				if (mode != null) {
					if (currentMode == null) {
						currentMode = mode;
					} else if (!mode.equals(currentMode)) {
						throw new RuntimeException(
								"incompatible modes between two real activities: " + mode + "," + currentMode);
					}
				}
			}
		}
		return mode2cnt;
	}
}
