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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import floetteroed.cadyts.calibrators.Calibrator;
import utils.MyConfigUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ModalShareCalibrationConfigGroup extends ReflectiveConfigGroup {

	// -------------------- CONSTANTS --------------------

	public static final String GROUP_NAME = "modalShareCalibration";

	// -------------------- CONSTRUCTION --------------------

	public ModalShareCalibrationConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- mode --------------------

	public static enum ModeType {
		on, off
	};

	private ModeType mode = ModeType.on;

	@StringGetter("mode")
	public ModeType getMode() {
		return this.mode;
	}

	@StringSetter("mode")
	public void setMode(final ModeType mode) {
		this.mode = mode;
	}

	public boolean isOn() {
		return ModeType.on.equals(this.mode);
	}

	// -------------------- inertia --------------------

	private double inertia = Calibrator.DEFAULT_REGRESSION_INERTIA;

	@StringGetter("inertia")
	public Double getInertia() {
		return this.inertia;
	}

	@StringSetter("inertia")
	public void setInertia(final double inertia) {
		this.inertia = inertia;
	}

	// -------------------- reproductionWeight --------------------

	private double reproductionWeight = 1.0;

	@StringGetter("reproductionWeight")
	public Double getReproductionWeight() {
		return this.reproductionWeight;
	}

	@StringSetter("reproductionWeight")
	public void setReproductionWeight(final double reproductionWeight) {
		this.reproductionWeight = reproductionWeight;
	}

	// -------------------- iterationExponent --------------------
	//
	// private Double iterationExponent = null;
	//
	// @StringGetter("iterationExponent")
	// public Double getIterationExponent() {
	// return this.iterationExponent;
	// }
	//
	// @StringSetter("iterationExponent")
	// public void setIterationExponent(final double iterationExponent) {
	// this.iterationExponent = iterationExponent;
	// }

	// -------------------- OVERRIDING OF ConfigGroup --------------------

	@Override
	public ConfigGroup createParameterSet(final String type) {
		if (TransportModeDataSet.TYPE.equals(type)) {
			return new TransportModeDataSet();
		} else {
			throw new RuntimeException("Unknown parameter set type: " + type);
		}
	}

	// -------------------- INNER CLASS --------------------

	public static class TransportModeDataSet extends ReflectiveConfigGroup {

		// CONSTANT

		public static final String TYPE = "modalShareData";

		// CONSTRUCTION

		TransportModeDataSet() {
			super(TYPE);
		}

		// mode

		private String mode = null;

		@StringGetter("mode")
		public String getMode() {
			return this.mode;
		}

		@StringSetter("mode")
		public void setMode(final String mode) {
			this.mode = mode;
		}

		// subModes

		private Set<String> subModes = new LinkedHashSet<>();

		@StringGetter("subModes")
		public String getSubModes() {
			return MyConfigUtils.listToString(new ArrayList<>(this.subModes));
		}

		@StringSetter("subModes")
		public void setSubModes(final String subModes) {
			this.subModes = new LinkedHashSet<>(MyConfigUtils.stringToList(subModes));
		}

		public Set<String> getSubModeSet() {
			return this.subModes;
		}

		// share

		private Double share = null; // may be null for unobserved mode

		@StringGetter("share")
		public Double getShare() {
			return this.share;
		}

		@StringSetter("share")
		public void setShare(final Double share) {
			this.share = share;
		}
		
		// initialASC

		private double initialASC = 0.0;

		@StringGetter("initialASC")
		public double getInitialASC() {
			return this.initialASC;
		}

		@StringSetter("initialASC")
		public void setInitialASC(final double initialASC) {
			this.initialASC = initialASC;
		}
	}
}
