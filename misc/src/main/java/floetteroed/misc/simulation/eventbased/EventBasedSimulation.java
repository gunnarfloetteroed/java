package floetteroed.misc.simulation.eventbased;

import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <E>
 *            the event type
 * 
 */
public class EventBasedSimulation<E extends AbstractEvent<E>> {

	// -------------------- MEMBERS --------------------

	private final PriorityQueue<E> events = new PriorityQueue<E>();

	private AbstractEventHandler<E> firstHandler = null;

	private double terminationTime = Double.POSITIVE_INFINITY;

	// -------------------- CONSTRUCTION --------------------

	public EventBasedSimulation() {
	}

	// -------------------- SETTERS AND GETTERS --------------------

	// TODO NEW
	public void setTerminationTime(final double terminationTime) {
		this.terminationTime = terminationTime;
	}

	public double getTerminationTime() {
		return this.terminationTime;
	}

	public void addHandler(final AbstractEventHandler<E> newHandler) {
		if (this.firstHandler == null) {
			this.firstHandler = newHandler;
		} else {
			this.firstHandler.addHandler(newHandler);
		}
	}

	public void addEvent(final E e) {
		this.events.add(e);
	}

	public void addEvents(final Collection<E> events) {
		this.events.addAll(events);
	}

	// -------------------- SIMULATION --------------------

	public void run() {
		this.firstHandler.beforeSimulation();
		// TODO handling of termination time is new
		while (this.events.size() > 0) {
			final E event = this.events.peek();
			if (event.getTime() <= this.terminationTime) {
				this.events.remove();
				final List<E> newEvents = this.firstHandler.handle(event);
				if (newEvents != null) {
					this.events.addAll(newEvents);
				}
			} else {
				break;
			}
		}
		this.firstHandler.afterSimulation();
	}
}
