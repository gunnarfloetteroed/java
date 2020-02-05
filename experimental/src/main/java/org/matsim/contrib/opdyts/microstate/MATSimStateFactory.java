package org.matsim.contrib.opdyts.microstate;

import org.matsim.api.core.v01.population.Population;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * A factory for MATSim simulation states.
 * 
 * @author Gunnar Flötteröd
 *
 * @see MATSimState
 * @see DecisionVariable
 */
public interface MATSimStateFactory<U extends DecisionVariable, X extends SimulatorState> {

	/**
	 * Creates a new object representation of the current MATSim simulation state.
	 * 
	 * IMPORTANT: Do not take over a controler reference into the state object and
	 * attempt to compute state properties (such as objective function values) on
	 * the fly. Instead, compute all relevant state attributes explicitly when
	 * creating the state object.
	 * 
	 * @see MATSimState
	 * 
	 * @param population
	 *            the current MATSim population
	 * @param stateVector
	 *            a vector representation of the state to be created
	 * @param decisionVariable
	 *            the decision variable that has led to the state to be created
	 * @return the current MATSim simulation state
	 */
	public X newState(Population population, Vector stateVector, U decisionVariable);

}
