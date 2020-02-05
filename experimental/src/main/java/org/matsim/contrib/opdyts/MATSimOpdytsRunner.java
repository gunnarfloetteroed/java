package org.matsim.contrib.opdyts;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.macrostate.DifferentiatedLinkOccupancyAnalyzer;
import org.matsim.contrib.opdyts.macrostate.SimulationMacroStateAnalyzer;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.microstate.MATSimStateFactory;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunction;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.gbl.MatsimRandom;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.RandomSearchBuilder;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import floetteroed.utilities.TimeDiscretization;

/**
 * The main class for running a MATSim/Opdyts optimization.
 * <p>
 * Implementations of the following interfaces receive injections during every
 * optimization stage (comprising one MATSim run):
 * <ul>
 * <li>StateFactory
 * <li>ObjectiveFunction
 * <li>DecisionVariableRandomizer
 * <li>ConvergenceCriterion
 * </ul>
 * 
 * @author Gunnar Flötteröd
 *
 */
public class MATSimOpdytsRunner<U extends DecisionVariable, X extends MATSimState> {

	// -------------------- CONSTANTS --------------------

	private final String outputDirectory;

	private final OpdytsConfigGroup opdytsConfig;

	private final TimeDiscretization timeDiscretization;

	private final MATSimSimulationWrapper<U, X> matsimSimulationWrapper;

	// -------------------- MEMBERS --------------------

	private ConvergenceCriterion convergenceCriterion;

	private SelfTuner selfTuner;

	// -------------------- CONSTRUCTION --------------------

	public MATSimOpdytsRunner(final Scenario scenario, final MATSimStateFactory<U, X> stateFactory) {

		this.outputDirectory = scenario.getConfig().controler().getOutputDirectory();
		this.opdytsConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), OpdytsConfigGroup.class);
		this.timeDiscretization = new TimeDiscretization(this.opdytsConfig.getStartTime_s(),
				this.opdytsConfig.getBinSize_s(), this.opdytsConfig.getBinCount());

		this.matsimSimulationWrapper = new MATSimSimulationWrapper<>(scenario, stateFactory,
				this.opdytsConfig.getEnBlockSimulationIterations(),
				this.opdytsConfig.getStateExtractionOffset());
		final Set<String> networkModes = new HashSet<>(scenario.getConfig().qsim().getMainModes());
		if (networkModes.size() > 0) {
			this.matsimSimulationWrapper
					.addSimulationStateAnalyzer(new DifferentiatedLinkOccupancyAnalyzer(this.timeDiscretization,
							networkModes, new LinkedHashSet<>(scenario.getNetwork().getLinks().keySet())));
		}
		this.matsimSimulationWrapper.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.binder().requestInjection(stateFactory);
			}
		});

		this.convergenceCriterion = this.opdytsConfig.newConvergenceCriterion();
		// this.convergenceCriterion = new FixedIterationNumberConvergenceCriterion(
		// this.opdytsConfig.getNumberOfIterationsForConvergence(),
		// this.opdytsConfig.getNumberOfIterationsForAveraging());

		this.selfTuner = new SelfTuner(this.opdytsConfig.getInitialEquilibriumGapWeight(),
				this.opdytsConfig.getInitialUniformityGapWeight());
		this.selfTuner.setInertia(this.opdytsConfig.getInertia());
		this.selfTuner.setNoisySystem(this.opdytsConfig.isNoisySystem());
		this.selfTuner.setWeightScale(this.opdytsConfig.getSelfTuningWeightScale());
	}

	// --------------- SETTERS TO OVERRIDE OPDYTS DEFAULT CLASSES ---------------

	public void setConvergenceCriterion(final ConvergenceCriterion convergenceCriterion) {
		this.convergenceCriterion = convergenceCriterion;
	}

	public void setSelfTuner(final SelfTuner selfTuner) {
		this.selfTuner = selfTuner;
	}

	public void setDirectlyAdjustedDecisionVariable(final DecisionVariable directlyAdjustedDecisionVariable) {
		this.matsimSimulationWrapper.setDirectlyAdjustedDecisionVariable(directlyAdjustedDecisionVariable);
	}

	// ----- CONFIGURATION OF (NOT YET STARTED/CREATED) SIMULATIONS/CONTROLERS -----

	public void addSimulationStateAnalyzer(SimulationMacroStateAnalyzer analyzer) {
		this.matsimSimulationWrapper.addSimulationStateAnalyzer(analyzer);
	}

	public void setReplacingModules(AbstractModule... replacingModules) {
		this.matsimSimulationWrapper.setReplacingModules(replacingModules);
	}

	public void addOverridingModule(AbstractModule abstractModule) {
		this.matsimSimulationWrapper.addOverridingModule(abstractModule);
	}

	public void setFreezeRandomSeed(boolean freezeRandomSeed) {
		this.matsimSimulationWrapper.setFreezeRandomSeed(freezeRandomSeed);
	}

	public void setOpdytsProgressListener(OpdytsProgressListener opdytsProgressListener) {
		this.matsimSimulationWrapper.setOpdytsProgressListener(opdytsProgressListener);
	}

	// -------------------- RUN --------------------

	public void run(final DecisionVariableRandomizer<U> randomizer, final U initialDecisionVariable,
			final MATSimObjectiveFunction<X> objectiveFunction) {

		final RandomSearchBuilder<U, X> builder = new RandomSearchBuilder<>();
		builder.setConvergenceCriterion(this.convergenceCriterion).setDecisionVariableRandomizer(randomizer)
				.setInitialDecisionVariable(initialDecisionVariable)
				.setMaxOptimizationStages(this.opdytsConfig.getMaxIteration())
				.setMaxSimulationTransitions(this.opdytsConfig.getMaxTransition())
				.setObjectiveFunction(objectiveFunction).setRandom(MatsimRandom.getRandom()).setSelfTuner(selfTuner)
				.setSimulator(this.matsimSimulationWrapper);
		final RandomSearch<U, X> randomSearch = builder.build();

		// TODO NEW
		this.matsimSimulationWrapper.addOverridingModule(objectiveFunction.newAbstractModule());

		this.matsimSimulationWrapper.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.binder().requestInjection(randomizer);
				this.binder().requestInjection(objectiveFunction); // TODO rather let the objective function modules
																	// take care of this?
				this.binder().requestInjection(convergenceCriterion);
			}
		});

		randomSearch.setLogPath(this.outputDirectory);
		randomSearch.setMaxTotalMemory(this.opdytsConfig.getMaxTotalMemory());
		randomSearch.setMaxMemoryPerTrajectory(this.opdytsConfig.getMaxMemoryPerTrajectory());
		randomSearch.setWarmupIterations(this.opdytsConfig.getWarmUpIterations());
		randomSearch.setUseAllWarmupIterations(this.opdytsConfig.getUseAllWarmUpIterations());
		randomSearch.setSmallestAcceptedImprovement(this.opdytsConfig.getSmallestAcceptedImprovement());
		
		randomSearch.run();
	}
}
