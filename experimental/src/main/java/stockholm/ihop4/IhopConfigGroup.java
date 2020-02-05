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
package stockholm.ihop4;

import java.util.function.Function;

import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.CompositeDecisionVariable;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.OneAtATimeRandomizer;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite.RandomCombinationRandomizer;
import org.matsim.core.config.ReflectiveConfigGroup;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class IhopConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "ihop";

	public IhopConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- tollZoneCountsFolder --------------------

	private String tollZoneCountsFolder = null;

	@StringSetter("tollZoneCountsFolder")
	public void setTollZoneCountsFolder(final String tollZoneCountsFolder) {
		this.tollZoneCountsFolder = tollZoneCountsFolder;
	}

	@StringGetter("tollZoneCountsFolder")
	public String getTollZoneCountsFolder() {
		return this.tollZoneCountsFolder;
	}

	// -------------------- tollZoneTimeIntervall --------------------

	public enum TollZoneTimeIntervallType {morningPeak, eveningPeak, bothPeaks, allDay};

	private TollZoneTimeIntervallType tollZoneTimeIntervall = null;
	
	@StringSetter("tollZoneTimeIntervall")
	public void setTollZoneTimeIntervall(final TollZoneTimeIntervallType tollZoneTimeIntervall) {
		this.tollZoneTimeIntervall = tollZoneTimeIntervall;
	}

	@StringGetter("tollZoneTimeIntervall")
	public TollZoneTimeIntervallType getTollZoneTimeIntervall() {
		return this.tollZoneTimeIntervall;
	}

	// -------------------- simulatedPopulationShare --------------------

	private Double simulatedPopulationShare = null;

	@StringSetter("simulatedPopulationShare")
	public void setSimulatedPopulationShare(final Double simulatedPopulationShare) {
		this.simulatedPopulationShare = simulatedPopulationShare;
	}

	@StringGetter("simulatedPopulationShare")
	public Double getSimulatedPopulationShare() {
		return this.simulatedPopulationShare;
	}

	// -------------------- countResidualMagnitude --------------------

	public static enum CountResidualMagnitudeType {
		absolute, square
	};

	private CountResidualMagnitudeType countResidualMagnitude = null;

	@StringGetter("countResidualMagnitude")
	public CountResidualMagnitudeType getCountResidualMagnitude() {
		return this.countResidualMagnitude;
	}

	@StringSetter("countResidualMagnitude")
	public void setCountResidualMagnitude(final CountResidualMagnitudeType countResidualMagnitude) {
		this.countResidualMagnitude = countResidualMagnitude;
	}

	public Function<Double, Double> newCountResidualMagnitudeFunction() {
		if (CountResidualMagnitudeType.absolute.equals(this.countResidualMagnitude)) {
			return (Double res) -> Math.abs(res);
		} else if (CountResidualMagnitudeType.square.equals(this.countResidualMagnitude)) {
			return (Double res) -> res * res;
		} else {
			throw new RuntimeException("Unknown countResidualMagnitude: " + this.countResidualMagnitude);
		}
	}

	// -------------------- DECISION VARIABLE RANDOMIZATION --------------------

	// TODO one could move this into OpdytsIntegration?

	// randomizer

	public static enum DecisionVariableRandomizerType {
		coordByCoord, randomRecombination
	};

	private DecisionVariableRandomizerType decisionVariableRandomizer = null;

	@StringGetter("decisionVariableRandomizer")
	public DecisionVariableRandomizerType getDecisionVariableRandomizer() {
		return this.decisionVariableRandomizer;
	}

	@StringSetter("decisionVariableRandomizer")
	public void setRandomizer(final DecisionVariableRandomizerType randomizer) {
		this.decisionVariableRandomizer = randomizer;
	}

	// numberOfVariations

	private Integer numberOfDecisionVariableVariations = null;

	@StringGetter("numberOfDecisionVariableVariations")
	public Integer getNumberOfDecisionVariableVariations() {
		return this.numberOfDecisionVariableVariations;
	}

	@StringSetter("numberOfDecisionVariableVariations")
	public void setNumberOfDecisionVariableVariations(final Integer numberOfVariations) {
		this.numberOfDecisionVariableVariations = numberOfVariations;
	}

	// innovationProba

	private Double decisionVariableInnovationProba = null;

	@StringGetter("decisionVariableInnovationProba")
	public Double getDecisionVariableInnovationProba() {
		return this.decisionVariableInnovationProba;
	}

	@StringSetter("decisionVariableInnovationProba")
	public void setDecisionVariableInnovationProba(final Double decisionVariableInnovationProba) {
		this.decisionVariableInnovationProba = decisionVariableInnovationProba;
	}

	// convenience factory method

	public DecisionVariableRandomizer<CompositeDecisionVariable> newDecisionVariableRandomizer() {
		if (DecisionVariableRandomizerType.coordByCoord.equals(this.decisionVariableRandomizer)) {
			return new OneAtATimeRandomizer();
		} else if (DecisionVariableRandomizerType.randomRecombination.equals(this.decisionVariableRandomizer)) {
			return new RandomCombinationRandomizer(this.numberOfDecisionVariableVariations,
					this.decisionVariableInnovationProba);
		} else {
			throw new RuntimeException("Unknown decision variable randomizer: " + this.decisionVariableRandomizer);
		}
	}

	// timeStepSize_s

	private Double activityTimeStepSize_s = null;

	@StringGetter("activityTimeStepSize_s")
	public Double getActivityTimeStepSize_s() {
		return this.activityTimeStepSize_s;
	}

	@StringSetter("activityTimeStepSize_s")
	public void setActivityTimeStepSize_(final Double activityTimeStepSize_s) {
		this.activityTimeStepSize_s = activityTimeStepSize_s;
	}

	// timeStepSize_s

	private Double performingStepSize_utils_hr = null;

	@StringGetter("performingStepSize_utils_hr")
	public Double getPerformingStepSize_utils_hr() {
		return this.performingStepSize_utils_hr;
	}

	@StringSetter("performingStepSize_utils_hr")
	public void setPerformingStepSize_utils_hr(final Double performingStepSize_utils_hr) {
		this.performingStepSize_utils_hr = performingStepSize_utils_hr;
	}

	// simulatedPopulationShareStepSize

	private Double simulatedPopulationShareStepSize = null;

	@StringGetter("simulatedPopulationShareStepSize")
	public Double getSimulatedPopulationShareStepSize() {
		return this.simulatedPopulationShareStepSize;
	}

	@StringSetter("simulatedPopulationShareStepSize")
	public void setSimulatedPopulationShareStepSize(final Double simulatedPopulationShareStepSize) {
		this.simulatedPopulationShareStepSize = simulatedPopulationShareStepSize;
	}

}
