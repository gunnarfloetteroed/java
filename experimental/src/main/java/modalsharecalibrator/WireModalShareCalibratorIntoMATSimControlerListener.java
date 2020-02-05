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

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import com.google.inject.Inject;

import modalsharecalibrator.ModalShareCalibrationConfigGroup.TransportModeDataSet;
import modalsharecalibrator.ModalShareCalibrator.Mode;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WireModalShareCalibratorIntoMATSimControlerListener implements AfterMobsimListener {

	// -------------------- CONSTANTS --------------------

	private final ModalShareCalibrator calibrator;

	private final ModeASCContainer modeASCContainer;

	private final CalibrationModeExtractor modeExtractor;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public WireModalShareCalibratorIntoMATSimControlerListener(final Config config,
			final ModeASCContainer modeASCContainer, final CalibrationModeExtractor modeExtractor) {
		final ModalShareCalibrationConfigGroup calibrConf = ConfigUtils.addOrGetModule(config,
				ModalShareCalibrationConfigGroup.class);
		this.calibrator = new ModalShareCalibrator();
		// calibrConf.getInitialTrustRegion(),
		// calibrConf.getIterationExponent(), calibrConf.getUseSolutionDerivatives());
		for (ConfigGroup paramSet : calibrConf.getParameterSets(TransportModeDataSet.TYPE)) {
			final TransportModeDataSet modeDataSet = (TransportModeDataSet) paramSet;
			this.calibrator.addMode(modeDataSet.getMode(), modeDataSet.getShare());
			modeASCContainer.setASC(modeDataSet.getMode(), modeDataSet.getInitialASC());
		}
		this.modeASCContainer = modeASCContainer;
		this.modeExtractor = modeExtractor;
	}

	// --------------- IMPLEMENTATION OF AfterMobsimListener ---------------

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		this.calibrator.clearSimulatedData();
		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			this.calibrator.setSimulatedData(person.getId(),
					this.modeExtractor.extractTripModes(person.getSelectedPlan()), event.getIteration());
		}
		// Logger.getLogger(this.getClass()).warn("Using hardwired sqrtMSA step size
		// guess.");

		for (Map.Entry<Mode, Double> ascEntry : this.calibrator.getDeltaASC().entrySet()) {
			this.modeASCContainer.setASC(ascEntry.getKey().name,
					this.modeASCContainer.getASC(ascEntry.getKey().name) + ascEntry.getValue());
			Logger.getLogger(this.getClass()).info("Set ASC for mode " + ascEntry.getKey().name + " to "
					+ this.modeASCContainer.getASC(ascEntry.getKey().name) + ".");
		}

	}
}
