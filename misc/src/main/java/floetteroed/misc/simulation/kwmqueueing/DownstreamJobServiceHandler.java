package floetteroed.misc.simulation.kwmqueueing;

import java.util.LinkedList;
import java.util.List;

import floetteroed.misc.simulation.eventbased.AbstractEventHandler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class DownstreamJobServiceHandler extends AbstractEventHandler<KWMQueueingSimEvent> {

	// -------------------- MEMBERS --------------------

	private final KWMQueueingSimulation queueingSimulation;

	// -------------------- CONSTRUCTION --------------------

	public DownstreamJobServiceHandler(final KWMQueueingSimulation queueingSimulation) {
		this.queueingSimulation = queueingSimulation;
	}

	// --------------- IMPLEMENTATION OF AbstractEventHandler ---------------

	@Override
	public boolean isResponsible(final KWMQueueingSimEvent event) {
		return KWMQueueingSimEvent.TYPE.DQ_JOB_SERVICE.equals(event.getType());
	}

	@Override
	public List<KWMQueueingSimEvent> process(final KWMQueueingSimEvent event) throws RuntimeException {

		final List<KWMQueueingSimEvent> newEvents = new LinkedList<KWMQueueingSimEvent>();
		final double time_s = event.getTime_s();
		final KWMQueueingSimLink link = event.getLink();
		final KWMQueueingSimJob job = event.getJob();
		final KWMQueueingSimLink nextLink = job.getNextLink();

		if (nextLink == null) { // arrival at destination
			job.advanceToNextLink();
			link.removeFirstJobFromLink(time_s, newEvents);
		} else { // attempt to enter non-null downstream link
			if (nextLink.spillsBack()) {
				if (this.queueingSimulation.getInstantaneousUnblocking()) {
					link.setBlockingLinkAndTime_s(nextLink, time_s);
				} else {
					newEvents
							.add(new KWMQueueingSimEvent(time_s + link.getServiceDistribution(job.getNextLink()).next(),
									KWMQueueingSimEvent.TYPE.DQ_JOB_SERVICE, link, job));
				}
			} else {
				if (nextLink.isHighestApproachingPriority(link.getPriority())) {
					link.removeFirstJobFromLink(time_s, newEvents);
					nextLink.removePriorityOfApproachingVehicle(link.getPriority());
					//newEvents.add(new KWMQueueingSimEvent(time_s, KWMQueueingSimEvent.TYPE.UQ_JOB_ARR, nextLink, job));	
					newEvents.add(new KWMQueueingSimEvent(time_s, KWMQueueingSimEvent.TYPE.DQ_JOB_FLOW, link, job));
				} else {
					newEvents
					.add(new KWMQueueingSimEvent(time_s + link.getServiceDistribution(job.getNextLink()).next(),
							KWMQueueingSimEvent.TYPE.DQ_JOB_SERVICE, link, job));					
				}
			}
		}

		return newEvents;
	}
}
