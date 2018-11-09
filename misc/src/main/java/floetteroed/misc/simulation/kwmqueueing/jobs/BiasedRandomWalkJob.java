package floetteroed.misc.simulation.kwmqueueing.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimJob;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;

/**
 * 
 * A job that performs a random walk with outflow-capacity proportional turning
 * probabilities, possibly towards a destination.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class BiasedRandomWalkJob implements KWMQueueingSimJob {

	// -------------------- CONSTANTS --------------------

	private final String id;

	private final double startTime_s;

	private final KWMQueueingSimLink origin;

	private final KWMQueueingSimLink destination;

	private final Random rnd;

	// -------------------- MEMBERS --------------------

	private KWMQueueingSimLink currentLink;

	private KWMQueueingSimLink nextLink;

	// -------------------- CONSTRUCTION --------------------

	public BiasedRandomWalkJob(final String id, final double startTime_s,
			final KWMQueueingSimLink origin, final KWMQueueingSimLink destination,
			final Random rnd) {

		this.id = id;
		this.startTime_s = startTime_s;
		this.origin = origin;
		this.destination = destination;
		this.rnd = rnd;

		this.currentLink = null;
		this.nextLink = origin;
	}

	// -------------------- IMPLEMENTATION OF QueueingJob --------------------

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
		return this.origin;
	}

	@Override
	public KWMQueueingSimLink getDestinationLink() {
		return this.destination;
	}

	@Override
	public KWMQueueingSimLink getNextLink() {
		return this.nextLink;
	}

	@Override
	public KWMQueueingSimLink getCurrentLink() {
		return this.currentLink;
	}

	@Override
	public void advanceToNextLink() {
		this.currentLink = this.nextLink;
		if (this.currentLink != null) {
			if (this.currentLink.equals(this.destination)
					|| this.currentLink.getOutLinks().size() == 0) {
				this.nextLink = null;
			} else {
				final List<KWMQueueingSimLink> candidates = new ArrayList<KWMQueueingSimLink>(
						this.currentLink.getOutLinks());
				this.nextLink = candidates.get(this.rnd.nextInt(candidates
						.size()));
			}
		}
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		return this.getId();
	}
}
