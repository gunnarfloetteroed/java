package org.matsim.contrib.opdyts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.macrostate.SimulationMacroStateAnalyzer;
import org.matsim.contrib.opdyts.microstate.MATSimStateFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.TerminationCriterion;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;

/**
 * @author michaelzilske created this on 08/10/15.
 * @author Gunnar modified this since 2015.
 */
class MATSimSimulationWrapper<U extends DecisionVariable, X extends SimulatorState> implements Simulator<U, X> {

	// -------------------- MEMBERS --------------------

	private final MATSimStateFactory<U, X> stateFactory;

	private final Scenario scenario;

	// A list because the order matters in the state space vector.
	private final List<SimulationMacroStateAnalyzer> simulationStateAnalyzers = new ArrayList<>();

	private final int numberOfEnBlockMatsimIterations;

	private final int stateExtractionOffset;
	
	private DecisionVariable directlyAdjustedDecisionVariable = null;

	private AbstractModule[] replacingModules = null;

	private AbstractModule overrides = AbstractModule.emptyModule();

	private boolean freezeRandomSeed = false;

	private int numberOfCompletedSimulationRuns = 0;

	private OpdytsProgressListener opdytsProgressListener = new OpdytsProgressListener() {
	};

	// -------------------- CONSTRUCTION --------------------

	MATSimSimulationWrapper(final Scenario scenario, final MATSimStateFactory<U, X> stateFactory,
			final int numberOfEnBlockMATSimIterations, final int stateExtractionOffset) {
		this.stateFactory = stateFactory;
		this.scenario = scenario;
		this.numberOfEnBlockMatsimIterations = numberOfEnBlockMATSimIterations;
		this.stateExtractionOffset = stateExtractionOffset;
		
		// Because the simulation is run multiple times.
		final String outputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
		this.scenario.getConfig().controler().setOutputDirectory(outputDirectory + "_0");

		// Because Opdyts assumes no systematic changes in the simulation dynamics.
		this.scenario.getConfig().strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);
		this.scenario.getConfig().planCalcScore().setFractionOfIterationsToStartScoreMSA(Double.POSITIVE_INFINITY);
	}

	// -------------------- CONFIGURATION --------------------

	void addSimulationStateAnalyzer(final SimulationMacroStateAnalyzer analyzer) {
		if (this.simulationStateAnalyzers.contains(analyzer)) {
			throw new RuntimeException("Analyzer " + analyzer + " has already been added.");
		}
		this.simulationStateAnalyzers.add(analyzer);
	}

	void setReplacingModules(final AbstractModule... replacingModules) {
		this.replacingModules = replacingModules;
	}

	void addOverridingModule(AbstractModule abstractModule) {
		this.overrides = AbstractModule.override(Arrays.asList(this.overrides), abstractModule);
	}

	public void setFreezeRandomSeed(final boolean freezeRandomSeed) {
		this.freezeRandomSeed = freezeRandomSeed;
	}

	public void setOpdytsProgressListener(final OpdytsProgressListener opdytsProgressListener) {
		this.opdytsProgressListener = opdytsProgressListener;
	}

	void setDirectlyAdjustedDecisionVariable(final DecisionVariable directlyAdjustedDecisionVariable) {
		this.directlyAdjustedDecisionVariable = directlyAdjustedDecisionVariable;
	}

	// --------------- IMPLEMENTATION OF Simulator INTERFACE ---------------

	@Override
	public SimulatorState run(final TrajectorySampler<U, X> trajectorySampler) {

		/*
		 * (1) This function is called in many iterations. Each time, it executes a
		 * complete MATSim run. To avoid that the MATSim output files are overwritten
		 * each time, set iteration-specific output directory names.
		 */

		String outputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
		outputDirectory = outputDirectory.substring(0, outputDirectory.lastIndexOf("_")) + "_"
				+ this.numberOfCompletedSimulationRuns;
		this.scenario.getConfig().controler().setOutputDirectory(outputDirectory);
		if (!this.freezeRandomSeed) {
			this.scenario.getConfig().global().setRandomSeed((new Random()).nextLong());
		}

		/*
		 * (2) Create the MATSimDecisionVariableSetEvaluator that is supposed to
		 * "optimize along" the MATSim run of this iteration.
		 */

		final WireOpdytsIntoMATSimControlerListener<U, X> wireOpdytsIntoMATSimControlerListener = new WireOpdytsIntoMATSimControlerListener<>(
				trajectorySampler, this.stateFactory, this.simulationStateAnalyzers,
				this.numberOfEnBlockMatsimIterations, 
				this.stateExtractionOffset,
				this.directlyAdjustedDecisionVariable);

		/*
		 * (3) Create, configure, and run a new MATSim Controler.
		 */

		final Controler controler = new Controler(this.scenario);
		if ((this.replacingModules != null) && (this.replacingModules.length > 0)) {
			controler.setModules(this.replacingModules);
		}
		controler.addOverridingModule(this.overrides);
		controler.addControlerListener(wireOpdytsIntoMATSimControlerListener);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				binder().requestInjection(wireOpdytsIntoMATSimControlerListener);
				bind(OpdytsProgressListener.class).toInstance(opdytsProgressListener);
			}
		});
		controler.setTerminationCriterion(new TerminationCriterion() {
			@Override
			public boolean continueIterations(int iteration) {
				return (!wireOpdytsIntoMATSimControlerListener.foundSolution());
			}
		});

		controler.run();
		this.numberOfCompletedSimulationRuns++;

		return wireOpdytsIntoMATSimControlerListener.getFinalState();
	}

	@Override
	public SimulatorState run(final TrajectorySampler<U, X> evaluator, final SimulatorState initialState) {
		if (initialState != null) {
			initialState.implementInSimulation();
		}
		return this.run(evaluator);
	}
}
