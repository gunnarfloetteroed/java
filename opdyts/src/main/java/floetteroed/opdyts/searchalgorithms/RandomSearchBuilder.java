package floetteroed.opdyts.searchalgorithms;

import java.util.Random;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;

/**
 * 
 * @author Kai Nagel
 * @author Gunnar
 *
 * @param <U>
 *            the decision variable type
 */
public class RandomSearchBuilder<U extends DecisionVariable, X extends SimulatorState> {

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

	private Simulator<U, X> simulator = null;

	/**
	 * See {@link Simulator}.
	 * 
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U, X> setSimulator(final Simulator<U, X> simulator) {
		this.simulator = simulator;
		return this;
	}

	// ------------------------------------------------------------

	private ConvergenceCriterion convergenceCriterion = null;

	/**
	 * Defines the convergence criterion.
	 *
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U, X> setConvergenceCriterion(final ConvergenceCriterion convergenceCriterion) {
		this.convergenceCriterion = convergenceCriterion;
		return this;
	}

	// ------------------------------------------------------------

	private ObjectiveFunction<X> objectiveFunction = null;

	/**
	 * The objective function: a quantitative measure of what one wants to achieve.
	 * To be minimized.
	 * 
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U, X> setObjectiveFunction(final ObjectiveFunction<X> objectiveFunction) {
		this.objectiveFunction = objectiveFunction;
		return this;
	}

	// ------------------------------------------------------------

	private SelfTuner selfTuner = null;

	public final RandomSearchBuilder<U, X> setSelfTuner(final SelfTuner selfTuner) {
		this.selfTuner = selfTuner;
		return this;
	}

	// ------------------------------------------------------------

	private Random random = null;

	public final RandomSearchBuilder<U, X> setRandom(final Random rnd) {
		this.random = rnd;
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
	public final RandomSearchBuilder<U, X> setDecisionVariableRandomizer(
			final DecisionVariableRandomizer<U> decisionVariableRandomizer) {
		this.decisionVariableRandomizer = decisionVariableRandomizer;
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
	public final RandomSearchBuilder<U, X> setInitialDecisionVariable(final U initialDecisionVariable) {
		this.initialDecisionVariable = initialDecisionVariable;
		return this;
	}

	// ------------------------------------------------------------

	private Integer maxOptimizationStages = null;

	/**
	 * Maximum number of random search stages.
	 * 
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U, X> setMaxOptimizationStages(int maxOptimizationStages) {
		this.maxOptimizationStages = maxOptimizationStages;
		return this;
	}

	// ------------------------------------------------------------

	private Integer maxSimulationTransitions = null;

	/**
	 * Maximum total number of evaluated simulator transitions.
	 * 
	 * For default value see code of {@link RandomSearchBuilder}.
	 */
	public final RandomSearchBuilder<U, X> setMaxSimulationTransitions(int maxSimulationTransitions) {
		this.maxSimulationTransitions = maxSimulationTransitions;
		return this;
	}

	// ============================================================

	public final RandomSearch<U, X> build() {

		assertImplemented(this.simulator, Simulator.class);
		assertImplemented(this.convergenceCriterion, ConvergenceCriterion.class);
		assertImplemented(this.objectiveFunction, ObjectiveFunction.class);
		assertImplemented(this.selfTuner, SelfTuner.class);
		assertImplemented(this.random, Random.class);
		assertImplemented(this.decisionVariableRandomizer, DecisionVariableRandomizer.class);
		assertNotNull(this.initialDecisionVariable, "The initial decision variable is null.");

		assertTrue((this.maxOptimizationStages != null) || (this.maxSimulationTransitions != null),
				"Neither the maximum number of optimization stages nor "
						+ "the maximum number of simulation transitions is specified.");
		if (this.maxOptimizationStages == null) {
			this.maxOptimizationStages = Integer.MAX_VALUE;
		} else {
			assertTrue(this.maxOptimizationStages > 0,
					"The maximum number of optimization stages is not strictly positive.");
		}
		if (this.maxSimulationTransitions == null) {
			this.maxSimulationTransitions = Integer.MAX_VALUE;
		} else {
			assertTrue(this.maxSimulationTransitions > 0,
					"The maximum number of simulation transitions is not strictly positive.");
		}

		return new RandomSearch<>(this.simulator, this.convergenceCriterion, this.objectiveFunction, this.selfTuner,
				this.random, this.decisionVariableRandomizer, this.initialDecisionVariable, this.maxOptimizationStages,
				this.maxSimulationTransitions);
	}
}
