package floetteroed.misc.simulation.kwmqueueing;

import java.util.Random;

import floetteroed.misc.simulation.eventbased.DistributionRealizer;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class ExponentialDistribution implements UnivariateDistribution {

	// -------------------- CONSTANTS --------------------

	private final Random rnd;

	private final double lambda;

	// -------------------- CONSTRUCTION --------------------

	public ExponentialDistribution(final double lambda, final Random rnd) {
		this.rnd = rnd;
		this.lambda = lambda;
	}

	public double getLambda() {
		return this.lambda;
	}

	// --------------- IMPLEMENTATION of UnivariateDistribution ---------------

	@Override
	public double next() {
		return DistributionRealizer.drawExponential(this.lambda, this.rnd);
	}

}
