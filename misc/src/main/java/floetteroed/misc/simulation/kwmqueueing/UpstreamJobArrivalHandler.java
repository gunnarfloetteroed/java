package floetteroed.misc.simulation.kwmqueueing;

import java.util.LinkedList;
import java.util.List;

import floetteroed.misc.simulation.eventbased.AbstractEventHandler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class UpstreamJobArrivalHandler extends
		AbstractEventHandler<KWMQueueingSimEvent> {

	// -------------------- CONSTRUCTION --------------------

	public UpstreamJobArrivalHandler() {
		super();
	}

	// --------------- IMPLEMENTATION OF AbstractEventHandler ---------------

	@Override
	public boolean isResponsible(final KWMQueueingSimEvent event) {
		return KWMQueueingSimEvent.TYPE.UQ_JOB_ARR.equals(event.getType());
	}

	@Override
	public List<KWMQueueingSimEvent> process(final KWMQueueingSimEvent event) {

		List<KWMQueueingSimEvent> newEvents = null;
		final KWMQueueingSimLink link = event.getLink();
		final KWMQueueingSimJob job = event.getJob();

		if (!link.spillsBack()) {
			link.incrUQ();
			link.addJobToRQ(job);
			job.advanceToNextLink();
			newEvents = new LinkedList<KWMQueueingSimEvent>();
			newEvents.add(new KWMQueueingSimEvent(event.getTime_s()
					+ link.getFwdLag_s(), KWMQueueingSimEvent.TYPE.DQ_JOB_ARR,
					link, job));
		}

		return newEvents;
	}

}
