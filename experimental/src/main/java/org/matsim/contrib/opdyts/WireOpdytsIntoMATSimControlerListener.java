package org.matsim.contrib.opdyts;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.opdyts.macrostate.SimulationMacroStateAnalyzer;
import org.matsim.contrib.opdyts.microstate.MATSimStateFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class WireOpdytsIntoMATSimControlerListener<U extends DecisionVariable, X extends SimulatorState>
		implements StartupListener, BeforeMobsimListener, AfterMobsimListener, ShutdownListener, IterationEndsListener {

	// -------------------- CONSTANTS --------------------

	private final boolean averageMemory = false;

	private final int memory = 1;

	private final int numberOfEnBlockMatsimIterations;

	private final int stateExtractionOffset;

	// -------------------- MEMBERS --------------------

	private final TrajectorySampler<U, X> trajectorySampler;

	private final MATSimStateFactory<U, X> stateFactory;

	// A list because the order matters in the state space vector.
	private final List<SimulationMacroStateAnalyzer> simulationStateAnalyzers;

	// TODO NEW 2018-11-20. Decision variables that are adjusted without search.
	private final DecisionVariable directlyAdjustedDecisionVariable;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private Population population;

	@Inject
	private OpdytsProgressListener opdytsProgressListener;

	private LinkedList<Vector> stateList = null;

	private X finalState = null;

	private boolean justStarted = true;

	// -------------------- CONSTRUCTION --------------------

	WireOpdytsIntoMATSimControlerListener(final TrajectorySampler<U, X> trajectorySampler,
			final MATSimStateFactory<U, X> stateFactory,
			final List<SimulationMacroStateAnalyzer> simulationStateAnalyzers,
			final int numberOfEnBlockMatsimIterations, final int stateExtractionOffset,
			final DecisionVariable directlyAdjustedDecisionVariable) {
		this.trajectorySampler = trajectorySampler;
		this.stateFactory = stateFactory;
		this.simulationStateAnalyzers = simulationStateAnalyzers;
		this.numberOfEnBlockMatsimIterations = numberOfEnBlockMatsimIterations;
		this.stateExtractionOffset = stateExtractionOffset;
		this.directlyAdjustedDecisionVariable = directlyAdjustedDecisionVariable;
	}

	// -------------------- INTERNALS --------------------

	private X newState() {
		final Vector newSummaryStateVector;
		if (this.averageMemory) {
			// average state vectors
			newSummaryStateVector = this.stateList.getFirst().copy();
			for (int i = 1; i < this.memory; i++) {
				newSummaryStateVector.add(this.stateList.get(i));
			}
			newSummaryStateVector.mult(1.0 / this.memory);
		} else {
			// concatenate state vectors
			newSummaryStateVector = Vector.concat(this.stateList);
		}
		return this.stateFactory.newState(this.population, newSummaryStateVector,
				this.trajectorySampler.getCurrentDecisionVariable());
	}

	// -------------------- RESULT ACCESS --------------------

	boolean foundSolution() {
		return this.trajectorySampler.foundSolution();
	}

	X getFinalState() {
		return finalState;
	}

	// --------------- CONTROLLER LISTENER IMPLEMENTATIONS ---------------

	@Override
	public void notifyStartup(final StartupEvent event) {

		this.opdytsProgressListener.callToNotifyStartup_opdyts(event);

		this.stateList = new LinkedList<Vector>();

		if (this.simulationStateAnalyzers.isEmpty()) {
			throw new RuntimeException("No simulation state analyzers have been added.");
		}
		// for (SimulationMacroStateAnalyzer analyzer : this.simulationStateAnalyzers) {
		// analyzer.clear();
		// this.eventsManager.addHandler(analyzer);
		// }

		// >>> TESTING >>>

		FileUtils.deleteQuietly(new File(event.getServices().getControlerIO().getOutputFilename("planstats.log")));
		FileUtils.deleteQuietly(new File(event.getServices().getControlerIO().getOutputFilename("statestats.log")));

		// <<< TESTING <<<

		this.justStarted = true;
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {

		this.opdytsProgressListener.callToNotifyBeforeMobsim_opdyts(event);

		if (event.getIteration() % this.numberOfEnBlockMatsimIterations == 0) {

			this.opdytsProgressListener.expectToBeBeforePhysicalMobsimRun(event.getIteration());

			/*
			 * (1) The mobsim must have been run at least once to allow for the extraction
			 * of a vector-valued system state. The "just started" MATSim iteration is hence
			 * run through without Opdyts in the loop.
			 */
			if (this.justStarted) {

				this.opdytsProgressListener.beforeVeryFirstPhysicalMobsimRun(event.getIteration());
				this.justStarted = false;

			} else {

				this.opdytsProgressListener.beforeOtherThanVeryFirstPhysicalMobsimRun(event.getIteration());

				/*
				 * (2) Extract the instantaneous state vector.
				 */
				Vector newInstantaneousStateVector = null;
				for (SimulationMacroStateAnalyzer analyzer : this.simulationStateAnalyzers) {
					if (newInstantaneousStateVector == null) {
						newInstantaneousStateVector = analyzer.newStateVectorRepresentation();
					} else {
						newInstantaneousStateVector = Vector.concat(newInstantaneousStateVector,
								analyzer.newStateVectorRepresentation());
					}
				}

				/*
				 * (3) Add instantaneous state vector to the list of past state vectors and
				 * ensure that the size of this list is equal to what the memory parameter
				 * prescribes.
				 */
				this.stateList.addFirst(newInstantaneousStateVector);
				while (this.stateList.size() < this.memory) {
					this.stateList.addFirst(newInstantaneousStateVector);
				}
				while (this.stateList.size() > this.memory) {
					this.stateList.removeLast();
				}

				/*
				 * (4) Inform the TrajectorySampler that one iteration has been completed and
				 * provide the resulting state.
				 */
				this.trajectorySampler.afterIteration(this.newState());

				// TODO NEW 2018-11-20
				if (this.directlyAdjustedDecisionVariable != null) {
					this.directlyAdjustedDecisionVariable.implementInSimulation();
				}

				this.opdytsProgressListener.extractedStateAndCalledTrajectorySampler(event.getIteration());
			}

		}

		// TESTING

		try {
			int planCnt = 0;
			int selectedPlanElementCnt = 0;
			double scoreSum = 0;
			for (Person person : this.population.getPersons().values()) {
				planCnt += person.getPlans().size();
				selectedPlanElementCnt += person.getSelectedPlan().getPlanElements().size();
				scoreSum += person.getSelectedPlan().getScore();
			}
			FileUtils.writeStringToFile(
					new File(event.getServices().getControlerIO().getOutputFilename("planstats.log")),
					"it=" + event.getIteration() + "  planCnt=" + planCnt + "  selectedPlanElementCnt="
							+ selectedPlanElementCnt + "  scoreSum=" + scoreSum + "\n",
					true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// TESTING

		if (event.getIteration() % this.numberOfEnBlockMatsimIterations == this.stateExtractionOffset) {

			/*
			 * (5) This is before a relevant mobsim iteration. Reset and register the macro
			 * state analyzers.
			 */
			for (SimulationMacroStateAnalyzer analyzer : this.simulationStateAnalyzers) {
				analyzer.clear();
				this.eventsManager.addHandler(analyzer);
			}
			this.opdytsProgressListener.clearedAndAddedMacroStateAnalyzers(event.getIteration());

		}
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {

		this.opdytsProgressListener.callToNotifyAfterMobsim_opdyts(event);

		if (event.getIteration() % this.numberOfEnBlockMatsimIterations == this.stateExtractionOffset) {
			/*
			 * This is after a relevant mobsim. Remove the macro state analyzers.
			 */
			this.opdytsProgressListener.expectToBeAfterAPhysicalMobsimRun(event.getIteration());
			for (SimulationMacroStateAnalyzer analyzer : this.simulationStateAnalyzers) {
				this.eventsManager.removeHandler(analyzer);
			}
			this.opdytsProgressListener.removedButDidNotClearMacroStateAnalyzers(event.getIteration());
		}
	}

	/*
	 * TODO Given that an iteration is assumed to end before the "mobsim execution"
	 * step, the final state is only approximately correctly computed because it
	 * leaves out the last iteration's "replanning" step.
	 * 
	 */
	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		this.finalState = this.newState();
	}

	@Inject
	private TravelTime travelTime;

	@Inject
	private Network net;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// >>> TESTING >>>
		try {
			double ttSum = 0;
			for (Link link : this.net.getLinks().values()) {
				ttSum += this.travelTime.getLinkTravelTime(link, 8 * 3600, null, null);
			}
			FileUtils.writeStringToFile(
					new File(event.getServices().getControlerIO().getOutputFilename("statestats.log")), ttSum + "\n",
					true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		// <<< TESTING <<<
	}
}
