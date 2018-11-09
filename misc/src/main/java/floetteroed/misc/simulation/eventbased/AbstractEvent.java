package floetteroed.misc.simulation.eventbased;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <E>
 *            the event type
 * 
 */
public abstract class AbstractEvent<E extends AbstractEvent<E>> implements
		Comparable<E> {

	// -------------------- CONSTANTS --------------------

	private final double time;

	// -------------------- CONSTRUCTION --------------------

	public AbstractEvent(final double time) {
		this.time = time;
	}

	// --------------------GETTERS --------------------

	public double getTime() {
		return this.time;
	}

	// --------------------IMPLEMENTATION OF Comparable --------------------

	@Override
	public int compareTo(final E other) {
		return (int) Math.signum(this.time - other.getTime());
	}

}
