package org.matsim.contrib.ier;

import org.matsim.contrib.ier.emulator.AgentEmulator;
import org.matsim.contrib.ier.emulator.SimulationEmulatorImpl;
import org.matsim.contrib.ier.emulator.SimulationEmulator;
import org.matsim.contrib.ier.replannerselection.AllReplannersSelector;
import org.matsim.contrib.ier.replannerselection.ReplannerSelector;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.corelisteners.PlansReplanning;

/**
 * This module overrides the default replanning.
 * 
 * @author shoerl
 */
public final class IERModule extends AbstractModule {

	private final Class<? extends ReplannerSelector> replannerSelectorClass;

	public IERModule(Class<? extends ReplannerSelector> replannerSelector) {
		this.replannerSelectorClass = replannerSelector;
	}

	public IERModule() {
		this(AllReplannersSelector.class);
	}

	@Override
	public void install() {
		bind(PlansReplanning.class).to(IERReplanning.class);
		bind(AgentEmulator.class);

		// We choose the simple emulator for now.
		bind(SimulationEmulator.class).to(SimulationEmulatorImpl.class);

		bind(ReplannerSelector.class).to(this.replannerSelectorClass);
	}
}
