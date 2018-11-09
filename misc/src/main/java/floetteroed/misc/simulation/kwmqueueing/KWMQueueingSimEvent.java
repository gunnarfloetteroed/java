package floetteroed.misc.simulation.kwmqueueing;

import floetteroed.misc.simulation.eventbased.AbstractEvent;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class KWMQueueingSimEvent extends AbstractEvent<KWMQueueingSimEvent> {

	// -------------------- TYPES --------------------

	public static enum TYPE {
		UQ_JOB_ARR, UQ_SPACE_ARR, DQ_JOB_ARR, DQ_JOB_SERVICE, NULL;
	};

	// -------------------- CONSTANTS --------------------

	private final TYPE type;

	private final KWMQueueingSimLink link;

	private final KWMQueueingSimJob job;

	// --------------------CONSTRUCTION --------------------

	public KWMQueueingSimEvent(final double time_s, final TYPE type,
			final KWMQueueingSimLink link, final KWMQueueingSimJob job) {
		super(time_s);
		this.type = type;
		this.link = link;
		this.job = job;
	}

	// --------------------GETTERS --------------------

	public double getTime_s() {
		return this.getTime();
	}

	public TYPE getType() {
		return this.type;
	}

	public KWMQueueingSimLink getLink() {
		return this.link;
	}

	public KWMQueueingSimJob getJob() {
		return this.job;
	}

	// -------------------- OVERRIDING OF OBJECT --------------------

	@Override
	public String toString() {
		return this.type + " of job "
				+ (this.job == null ? "null" : this.job.getId()) + " at time "
				+ this.getTime_s() + "s on link "
				+ (this.link == null ? "null" : this.link.getId());
	}
}
