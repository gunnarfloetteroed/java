package org.matsim.contrib.opdyts.microstate;

import org.matsim.api.core.v01.population.Population;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param <U>
 *            decision variable type
 */
public class MATSimStateFactoryImpl<U extends DecisionVariable, X extends SimulatorState>
		implements MATSimStateFactory<U, MATSimState> {

	public MATSimStateFactoryImpl() {
	}

	@Override
	public final MATSimState newState(final Population population, final Vector stateVector, final U decisionVariable) {
		final MATSimState result = new MATSimState(population, stateVector);
//		this.addComponents(result);
		return result;
	}

//	/**
//	 * An inheritance-based attempt to support a modular objective function.
//	 * Reasoning behind this:
//	 * <p>
//	 * The MATSimOpdytsRunner ensures dependency injections into MATSimStateFactory
//	 * and hence into subclasses of this. Subclasses are hence able to access the
//	 * entire MATSim simulation machinery and to extract and insert (in a type-safe
//	 * manner) components (e.g. their injected members) into the MATSimState.
//	 * <p>
//	 * Composition may be better, but type-safety may be more complicated to ensure.
//	 */
//	protected void addComponents(final MATSimState state) {
//	}

}
