package floetteroed.opdyts.searchalgorithms;

import java.util.Random;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;

/**
 * 
 * @author Kai Nagel
 * @author Gunnar
 *
 * @param <U>
 *            the decision variable type
 */
public class RandomSearchBuilder<U extends DecisionVariable> {

	// -------------------- HELPERS --------------------

	private static void assertTrue(final boolean condition, final String errorMsg) {
		if (!condition) {
			throw new RuntimeException(errorMsg);
		}
	}

	private static void assertNotNull(final Object object, final String errorMsg) {
		assertTrue(object != null, errorMsg);
	}

	private static <C> void assertImplemented(final C object, final Class<C> clazz) {
		assertNotNull(object, clazz.getSimpleName() + " is not implemented.");
	}

	// ==================== PARAMETER DEFINITIONS ====================

	private Simulator<U> simulator = null;

	/**
	 * See {@link Simulator}.
	 * 
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U> setSimulator(final Simulator<U> simulator) {
		this.simulator = simulator;
		return this;
	}

	// ------------------------------------------------------------

	private DecisionVariableRandomizer<U> decisionVariableRandomizer = null;

	/**
	 * Very problem-specific: given a decision variable, create trial variations
	 * thereof. These variations should be large enough to yield a measurable change
	 * in objective function value but they should still be relatively small (in the
	 * sense of a local search).
	 * 
	 * From the experiments performed so far, it appears as if the number of trial
	 * decision variables should be as large as memory allows.
	 *
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U> setDecisionVariableRandomizer(
			final DecisionVariableRandomizer<U> decisionVariableRandomizer) {
		this.decisionVariableRandomizer = decisionVariableRandomizer;
		return this;
	}

	// ------------------------------------------------------------

	private ConvergenceCriterion convergenceCriterion = null;

	/**
	 * Defines the convergence criterion.
	 *
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U> setConvergenceCriterion(final ConvergenceCriterion convergenceCriterion) {
		this.convergenceCriterion = convergenceCriterion;
		return this;
	}

	// ------------------------------------------------------------

	private ObjectiveFunction objectiveFunction = null;

	/**
	 * The objective function: a quantitative measure of what one wants to achieve.
	 * To be minimized.
	 * 
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U> setObjectiveFunction(final ObjectiveFunction objectiveFunction) {
		this.objectiveFunction = objectiveFunction;
		return this;
	}

	// ------------------------------------------------------------

	private SelfTuner selfTuner = null;

	public final RandomSearchBuilder<U> setSelfTuner(final SelfTuner selfTuner) {
		this.selfTuner = selfTuner;
		return this;
	}
	
	// ------------------------------------------------------------

	private Random random = null;
	
	public final RandomSearchBuilder<U> setRandom(final Random rnd) {
		this.random = rnd;
		return this;
	}

	// ------------------------------------------------------------

	private U initialDecisionVariable = null;

	/**
	 * The starting point of the search. The initial simulation configuration should
	 * be such the simulation is converged given this decision variable.
	 * 
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U> setInitialDecisionVariable(final U initialDecisionVariable) {
		this.initialDecisionVariable = initialDecisionVariable;
		return this;
	}

	// ------------------------------------------------------------

	public static final int DEFAULT_MAXOPTIMIZATIONSTAGES = Integer.MAX_VALUE;

	private int maxOptimizationStages = DEFAULT_MAXOPTIMIZATIONSTAGES;

	/**
	 * Maximum number of random search stages.
	 * 
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U> setMaxOptimizationStages(int maxOptimizationStages) {
		this.maxOptimizationStages = maxOptimizationStages;
		return this;
	}

	// ------------------------------------------------------------

	public static final int DEFAULT_MAXSIMULATIONTRANSITIONS = Integer.MAX_VALUE;

	private int maxSimulationTransitions = DEFAULT_MAXSIMULATIONTRANSITIONS;

	/**
	 * Maximum total number of evaluated simulator transitions.
	 * 
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U> setMaxSimulationTransitions(int maxSimulationTransitions) {
		this.maxSimulationTransitions = maxSimulationTransitions;
		return this;
	}

	// /**
	// * How many candidate decision variables are to be created. Based on empirical
	// * experience, the more the better. Available memory defines an upper limit.
	// *
	// * For default value see code of {@link RandomSearch.Builder}.
	// */
	// public final RandomSearchBuilder<U> setPopulationSize(int populationSize) {
	// this.populationSize = populationSize;
	// return this;
	// }

	// /**
	// * For default value see code of {@link RandomSearch.Builder}.
	// */
	// public final RandomSearchBuilder<U> setRnd(Random rnd) {
	// this.rnd = rnd;
	// return this;
	// }

	// /**
	// * For all practical purposes, keep this "true". "false" is only for
	// * debugging.
	// *
	// * For default value see code of {@link RandomSearch.Builder}.
	// */
	// public final RandomSearchBuilder<U> setInterpolate(boolean interpolate) {
	// this.interpolate = interpolate;
	// return this;
	// }

	// /**
	// * If the currently best decision variable is to be included in the set of
	// * new candidate decision variables. More an experimental feature, better
	// * keep it "false".
	// *
	// * For default value see code of {@link RandomSearch.Builder}.
	// */
	// public final RandomSearchBuilder<U> setIncludeCurrentBest(boolean
	// includeCurrentBest) {
	// this.includeCurrentBest = includeCurrentBest;
	// return this;
	// }

	// public final RandomSearchBuilder<U> setWarmupIterations(int
	// warmupIterations) {
	// this.warmupIterations = warmupIterations;
	// return this;
	// }

	// public final RandomSearchBuilder<U> setUseAllWarmupIterations(boolean
	// useAllWarmupIterations) {
	// this.useAllWarmupIterations = useAllWarmupIterations;
	// return this;
	// }

	public final RandomSearch<U> build() {
		assertImplemented(this.simulator, Simulator.class);
		assertImplemented(this.convergenceCriterion, ConvergenceCriterion.class);
		assertImplemented(this.objectiveFunction, ObjectiveFunction.class);
		assertImplemented(this.selfTuner, SelfTuner.class);
		assertImplemented(this.random, Random.class);
		assertImplemented(this.decisionVariableRandomizer, DecisionVariableRandomizer.class);
		assertNotNull(this.initialDecisionVariable, "the initial decision variable is null");

		assertTrue(this.maxOptimizationStages > 0,
				"the maximum number of optimization stages is not strictly positive");
		assertTrue(this.maxSimulationTransitions > 0,
				"the maximum number of simulation transitions is not strictly positive");

		return new RandomSearch<>(simulator, convergenceCriterion, objectiveFunction, 
				selfTuner, random,
				decisionVariableRandomizer,
				initialDecisionVariable, maxOptimizationStages, maxSimulationTransitions);
	}
}
