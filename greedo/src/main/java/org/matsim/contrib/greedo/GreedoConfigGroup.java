/*
 * Greedo -- Equilibrium approximation for general-purpose multi-agent simulations.
 *
 * Copyright 2022 Gunnar Flötteröd
 * 
 *
 * This file is part of Greedo.
 *
 * Greedo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Greedo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Greedo.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@gmail.com
 *
 */
package org.matsim.contrib.greedo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.utils.misc.StringUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class GreedoConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "greedo";

	public GreedoConfigGroup() {
		super(GROUP_NAME);
	}

	//

	private int maxMemory = 10;

	@StringGetter("maxMemory")
	public int getMaxMemory() {
		return this.maxMemory;
	}

	@StringSetter("maxMemory")
	public void setMaxMemory(final int maxMemory) {
		this.maxMemory = maxMemory;
	}

	//

	private double smoothingInertia = 0.0;

	@StringGetter("smoothingInertia")
	public double getSmoothingInertia() {
		return this.smoothingInertia;
	}

	@StringSetter("smoothingInertia")
	public void setSmoothingInertia(final double smoothingInertia) {
		this.smoothingInertia = smoothingInertia;
	}

	// -------------------- MATSIM-SPECIFIC: STRATEGY TREATMENT --------------------

	private static String listToString(final List<String> list) {
		final StringBuilder builder = new StringBuilder();
		if (list.size() > 0) {
			builder.append(list.get(0));
		}
		for (int i = 1; i < list.size(); i++) {
			builder.append(',');
			builder.append(list.get(i));
		}
		return builder.toString();
	}

	private static List<String> stringToList(final String string) {
		final ArrayList<String> result = new ArrayList<>();
		for (String part : StringUtils.explode(string, ',')) {
			result.add(part.trim().intern());
		}
		result.trimToSize();
		return result;
	}

	//

	private Set<String> cheapStrategies = new LinkedHashSet<String>(Arrays.asList(DefaultStrategy.TimeAllocationMutator));	

	@StringGetter("cheapStrategies")
	public String getCheapStrategies() {
		return listToString(new ArrayList<>(this.cheapStrategies));
	}

	@StringSetter("cheapStrategies")
	public void setCheapStrategies(final String cheapStrategies) {
		this.cheapStrategies = new LinkedHashSet<>(stringToList(cheapStrategies));
	}

	public void addCheapStrategy(final String cheapStrategy) {
		this.cheapStrategies.add(cheapStrategy);
	}
	
	public Set<String> getCheapStrategySet() {
		return this.cheapStrategies;
	}
	
	//
	
	private Set<String> expensiveStrategies = new LinkedHashSet<String>(Arrays.asList(DefaultStrategy.ReRoute,
			DefaultStrategy.TimeAllocationMutator_ReRoute, DefaultStrategy.ChangeSingleTripMode,
			DefaultStrategy.SubtourModeChoice, DefaultStrategy.ChangeTripMode, DefaultStrategy.ChangeLegMode,
			DefaultStrategy.ChangeSingleLegMode, DefaultStrategy.TripSubtourModeChoice));

	@StringGetter("expensiveStrategies")
	public String getExpensiveStrategies() {
		return listToString(new ArrayList<>(this.expensiveStrategies));
	}

	@StringSetter("expensiveStrategies")
	public void setExpensiveStrategies(final String expensiveStrategies) {
		this.expensiveStrategies = new LinkedHashSet<>(stringToList(expensiveStrategies));
	}

	public Set<String> getExpensiveStrategySet() {
		return this.expensiveStrategies;
	}

	public void addExpensiveStrategy(final String expensiveStrategy) {
		this.expensiveStrategies.add(expensiveStrategy);
	}
	
	// -------------------- ONLY FOR TESTING --------------------

	public static enum StepControlType {
		MSA, EXP
	}

	private StepControlType stepControl = StepControlType.EXP;

	@StringGetter("stepControl")
	public StepControlType getStepControl() {
		return this.stepControl;
	}

	@StringSetter("stepControl")
	public void setStepControl(final StepControlType stepControl) {
		this.stepControl = stepControl;
	}

	//
	
	public static enum ReplannerIdentifierType {
		MSA, SBAYTI2007, GAPPROP
	}

	private ReplannerIdentifierType replannerIdentifier = ReplannerIdentifierType.SBAYTI2007;

	@StringGetter("replannerIdentifier")
	public ReplannerIdentifierType getReplannerIdentifier() {
		return this.replannerIdentifier;
	}

	@StringSetter("replannerIdentifier")
	public void setReplannerIdentifier(final ReplannerIdentifierType replannerIdentifier) {
		this.replannerIdentifier = replannerIdentifier;
	}

	//

	private double initialStepSizeFactor = 1.0;

	@StringGetter("initialStepSizeFactor")
	public double getInitialStepSizeFactor() {
		return this.initialStepSizeFactor;
	}

	@StringSetter("initialStepSizeFactor")
	public void setInitialStepSizeFactor(double initialStepSizeFactor) {
		this.initialStepSizeFactor = initialStepSizeFactor;
	}

	//

	private double replanningRateIterationExponent = -1.0;

	@StringGetter("replanningRateIterationExponent")
	public double getReplanningRateIterationExponent() {
		return this.replanningRateIterationExponent;
	}

	@StringSetter("replanningRateIterationExponent")
	public void setReplanningRateIterationExponent(double replanningRateIterationExponent) {
		this.replanningRateIterationExponent = replanningRateIterationExponent;
	}

	//

	private boolean isUsingThreshold = false;

	@StringGetter("isUsingThreshold")
	public boolean isUsingThreshold() {
		return this.isUsingThreshold;
	}

	@StringSetter("isUsingThreshold")
	public void setIsUsingThreshold(boolean isUsingThreshold) {
		this.isUsingThreshold = isUsingThreshold;
	}

	//

	private boolean isGapWeighting = false;

	@StringGetter("isGapWeighting")
	public boolean isGapWeighting() {
		return this.isGapWeighting;
	}

	@StringSetter("isGapWeighting")
	public void setIsGapWeighting(boolean isGapWeighting) {
		this.isGapWeighting = isGapWeighting;
	}
}
