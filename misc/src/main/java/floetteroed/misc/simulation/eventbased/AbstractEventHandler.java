package floetteroed.misc.simulation.eventbased;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <E>
 *            the event type
 */
public abstract class AbstractEventHandler<E extends AbstractEvent<E>> {

	// -------------------- MEMBERS --------------------

	private AbstractEventHandler<E> nextHandler = null;

	// -------------------- CONSTRUCTION --------------------

	public AbstractEventHandler() {
	}

	// -------------------- IMPLEMENTATION --------------------

	void addHandler(final AbstractEventHandler<E> nextHandler) {
		if (this.nextHandler == null) {
			this.nextHandler = nextHandler;
		} else {
			this.nextHandler.addHandler(nextHandler);
		}
	}

	List<E> handle(final E event) {
		List<E> newEvents = null;
		if (this.isResponsible(event)) {
			final List<E> myEvents = this.process(event);
			if (myEvents != null) {
				newEvents = new LinkedList<E>();
				newEvents.addAll(myEvents);
			}
		}
		if (this.nextHandler != null) {
			final List<E> nextEvents = this.nextHandler.handle(event);
			if (nextEvents != null) {
				if (newEvents == null) {
					newEvents = new LinkedList<E>();
				}
				newEvents.addAll(nextEvents);
			}
		}
		return newEvents;
	}

	void beforeSimulation() {
		this.myBeforeSimulation();
		if (this.nextHandler != null) {
			this.nextHandler.beforeSimulation();
		}
	}

	void afterSimulation() {
		if (this.nextHandler != null) {
			this.nextHandler.afterSimulation();
		}
		this.myAfterSimulation();
	}

	// -------------------- INTERFACE DEFINITION --------------------

	public void myBeforeSimulation() {
	}

	public abstract boolean isResponsible(E event);

	public abstract List<E> process(E event);

	public void myAfterSimulation() {
	}

}
