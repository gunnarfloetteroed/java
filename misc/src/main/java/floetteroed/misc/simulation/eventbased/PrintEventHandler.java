package floetteroed.misc.simulation.eventbased;

import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <E>
 *            the event type
 */
public class PrintEventHandler<E extends AbstractEvent<E>> extends
		AbstractEventHandler<E> {

	// --------------- OVERRIDING OF AbstractEventHandler ---------------

	@Override
	public boolean isResponsible(final E event) {
		return true;
	}

	@Override
	public List<E> process(final E event) {
		System.out.println(event);
		return null;
	}

}
