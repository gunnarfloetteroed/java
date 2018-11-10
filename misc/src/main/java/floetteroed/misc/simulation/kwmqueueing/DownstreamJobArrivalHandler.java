package floetteroed.misc.simulation.kwmqueueing;

import java.util.LinkedList;
import java.util.List;

import floetteroed.misc.simulation.eventbased.AbstractEventHandler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class DownstreamJobArrivalHandler extends AbstractEventHandler<KWMQueueingSimEvent> {

	// -------------------- CONSTANTS --------------------

	// private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	public DownstreamJobArrivalHandler() {
		super();
		// this.rnd = rnd;
	}

	// --------------- IMPLEMENTATION OF AbstractEventHandler ---------------

	@Override
	public boolean isResponsible(final KWMQueueingSimEvent event) {
		return KWMQueueingSimEvent.TYPE.DQ_JOB_ARR.equals(event.getType());
	}

	@Override
	public List<KWMQueueingSimEvent> process(final KWMQueueingSimEvent event) throws RuntimeException {

		List<KWMQueueingSimEvent> newEvents = null;
		final KWMQueueingSimLink link = event.getLink();
		final KWMQueueingSimJob job = link.removeFirstJobFromRQ();
		if (!job.equals(event.getJob())) {
			throw new RuntimeException(
					"Jobs do not match. Running queue: " + job.getId() + "; event: " + event.getJob().getId() + ".");
		}

		link.addJobToDQ(job);
		if (link.getDQJobCnt() == 1) {
			final KWMQueueingSimLink nextLink = job.getNextLink();
			newEvents = new LinkedList<KWMQueueingSimEvent>();
			newEvents.add(new KWMQueueingSimEvent(event.getTime_s() + link.getServiceDistribution(nextLink).next(),
					KWMQueueingSimEvent.TYPE.DQ_JOB_SERVICE, link, job));
			if (nextLink != null) {
				nextLink.addPriorityOfApproachingVehicle(link.getPriority());
			}
		}

		return newEvents;
	}

}
