package floetteroed.misc.simulation.kwmqueueing.jobs;

import java.util.Arrays;
import java.util.List;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimJob;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;

/**
 * A job that follows a fixed route.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class RoutedJob implements KWMQueueingSimJob {

	// -------------------- CONSTANTS --------------------

	private final String id;

	private final double startTime_s;

	private final List<KWMQueueingSimLink> route;

	// -------------------- MEMBERS --------------------

	private int linkIndex = -1;

	// -------------------- CONSTRUCTION --------------------

	public RoutedJob(final String id, final double startTime_s,
			final List<KWMQueueingSimLink> route) {
		this.id = id;
		this.startTime_s = startTime_s;
		this.route = route;
	}

	public RoutedJob(final String id, final double startTime_s,
			final KWMQueueingSimLink... route) {
		this(id, startTime_s, Arrays.asList(route));
	}

	// -------------------- IMPLEMENTATION OF Job INTERFACE --------------------

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public double getStartTime_s() {
		return this.startTime_s;
	}

	@Override
	public KWMQueueingSimLink getOriginLink() {
		return this.route.get(0);

	}

	@Override
	public KWMQueueingSimLink getDestinationLink() {
		return this.route.get(this.route.size() - 1);
	}

	@Override
	public KWMQueueingSimLink getNextLink() {
		if (this.linkIndex < this.route.size() - 1) {
			return this.route.get(this.linkIndex + 1);
		} else {
			return null;
		}
	}

	@Override
	public void advanceToNextLink() throws RuntimeException {
		if (this.linkIndex < this.route.size()) {
			this.linkIndex++;
		} else {
			throw new RuntimeException("Job " + this.getId()
					+ " is moved beyond its destination.");
		}
	}

	@Override
	public KWMQueueingSimLink getCurrentLink() {
		if (this.linkIndex >= 0 && this.linkIndex <= this.route.size() - 1) {
			return this.route.get(this.linkIndex);
		} else {
			return null;
		}
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		return this.getId();
	}
}
