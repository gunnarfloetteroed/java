package floetteroed.misc.simulation.kwmqueueing;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SingletonDistribution implements UnivariateDistribution {

	// -------------------- CONSTANTS --------------------

	public final double value;

	// -------------------- CONSTRUCTION --------------------

	public SingletonDistribution(final double value) {
		this.value = value;
	}

	// --------------- IMPLEMENTATION of UnivariateDistribution ---------------

	@Override
	public double next() {
		return value;
	}

}
