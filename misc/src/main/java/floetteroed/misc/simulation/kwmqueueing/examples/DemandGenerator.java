package floetteroed.misc.simulation.kwmqueueing.examples;

import static floetteroed.misc.simulation.eventbased.DistributionRealizer.drawExponential;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimEvent;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimJob;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <J>
 *            the QueueingJob type
 */
public abstract class DemandGenerator<J extends KWMQueueingSimJob> {

	// -------------------- STATIC MEMBERS --------------------

	private static long cnt = 0;

	private Random rnd = new Random();

	// -------------------- CONSTRUCTION --------------------

	public DemandGenerator() {
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setRandom(final Random rnd) {
		this.rnd = rnd;
	}

	public Random getRandom() {
		return this.rnd;
	}

	// -------------------- IMPLEMENTATION --------------------

	public List<KWMQueueingSimEvent> newArrivalEvents(final KWMQueueingSimLink originLink,
			final KWMQueueingSimLink destinationLink, final List<KWMQueueingSimLink> route,
			final double arrivals_veh_s, final double start_s,
			final double end_s) {
		final List<KWMQueueingSimEvent> arrivals = new LinkedList<KWMQueueingSimEvent>();
		if (arrivals_veh_s > 0) {
			double time_s = start_s + drawExponential(arrivals_veh_s, this.rnd);
			while (time_s < end_s) {
				final KWMQueueingSimJob job = this.newJob(Long.toString(++cnt),
						originLink, destinationLink, route, time_s);
				arrivals.add(new KWMQueueingSimEvent(time_s,
						KWMQueueingSimEvent.TYPE.UQ_JOB_ARR, originLink, job));
				time_s += drawExponential(arrivals_veh_s, this.rnd);
			}
		}
		return arrivals;
	}

	// -------------------- INTERFACE DEFINITION --------------------

	protected abstract J newJob(final String id, final KWMQueueingSimLink originLink,
			final KWMQueueingSimLink destinationLink, final List<KWMQueueingSimLink> route,
			final double departureTime_s);

}
