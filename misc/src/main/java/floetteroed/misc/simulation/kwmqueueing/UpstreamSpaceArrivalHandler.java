package floetteroed.misc.simulation.kwmqueueing;

import java.util.LinkedList;
import java.util.List;

import floetteroed.misc.simulation.eventbased.AbstractEventHandler;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class UpstreamSpaceArrivalHandler extends AbstractEventHandler<KWMQueueingSimEvent> {

	// -------------------- MEMBERS --------------------

	private final KWMQueueingSimulation queueingSimulation;

	// -------------------- CONSTRUCTION --------------------

	public UpstreamSpaceArrivalHandler(final KWMQueueingSimulation queueingSimulation) {
		super();
		this.queueingSimulation = queueingSimulation;
	}

	// --------------- IMPLEMENTATION OF AbstractEventHandler ---------------

	@Override
	public boolean isResponsible(final KWMQueueingSimEvent event) {
		return KWMQueueingSimEvent.TYPE.UQ_SPACE_ARR.equals(event.getType());
	}

	@Override
	public List<KWMQueueingSimEvent> process(final KWMQueueingSimEvent event) {

		List<KWMQueueingSimEvent> newEvents = null;
		final KWMQueueingSimLink link = event.getLink();
		link.decrUQ();

		if (this.queueingSimulation.getInstantaneousUnblocking()) {
			KWMQueueingSimLink unblockedLink = null;
			for (KWMQueueingSimLink cand : link.getInLinks()) {
				if (link.equals(cand.getBlockingLink()) && ((unblockedLink == null)
						|| (cand.getBlockingTime_s() < unblockedLink.getBlockingTime_s()))) {
					unblockedLink = cand;
				}
			}
			if (unblockedLink != null) {
				newEvents = new LinkedList<KWMQueueingSimEvent>();
				final KWMQueueingSimJob unblockedJob = unblockedLink.removeFirstJobFromLink(event.getTime_s(),
						newEvents);
				link.removePriorityOfApproachingVehicle(unblockedLink.getPriority());
				newEvents.add(new KWMQueueingSimEvent(event.getTime_s(), KWMQueueingSimEvent.TYPE.UQ_JOB_ARR, link,
						unblockedJob));
			}
		}

		return newEvents;
	}
}
