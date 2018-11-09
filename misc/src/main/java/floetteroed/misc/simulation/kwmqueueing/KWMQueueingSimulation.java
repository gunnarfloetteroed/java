package floetteroed.misc.simulation.kwmqueueing;

import java.util.Collection;
import java.util.Random;

import floetteroed.misc.simulation.eventbased.AbstractEventHandler;
import floetteroed.misc.simulation.eventbased.EventBasedSimulation;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class KWMQueueingSimulation {

	// -------------------- MEMBERS --------------------

	private final EventBasedSimulation<KWMQueueingSimEvent> simEngine;

	private boolean instantaneousUnblocking = true;

	// -------------------- CONSTRUCTION --------------------

	public KWMQueueingSimulation(final Random rnd, final KWMQueueingSimNetwork net) {
		this.simEngine = new EventBasedSimulation<KWMQueueingSimEvent>();
		this.simEngine.addHandler(new DownstreamJobArrivalHandler());
		this.simEngine.addHandler(new DownstreamJobServiceHandler(this));
		this.simEngine.addHandler(new UpstreamJobArrivalHandler());
		this.simEngine.addHandler(new UpstreamSpaceArrivalHandler(this));
		this.addEvent(new KWMQueueingSimEvent(0.0, KWMQueueingSimEvent.TYPE.NULL,
				null, null));
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setTerminationTime(final double terminationTime) {
		this.simEngine.setTerminationTime(terminationTime);
	}

	public void setInstantaneousUnblocking(final boolean instantaneousUnblocking) {
		this.instantaneousUnblocking = instantaneousUnblocking;
	}

	public boolean getInstantaneousUnblocking() {
		return this.instantaneousUnblocking;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addHandler(final AbstractEventHandler<KWMQueueingSimEvent> handler) {
		this.simEngine.addHandler(handler);
	}

	public void addEvent(final KWMQueueingSimEvent e) {
		this.simEngine.addEvent(e);
	}

	public void addEvents(final Collection<KWMQueueingSimEvent> events) {
		this.simEngine.addEvents(events);
	}

	public void run() {
		this.simEngine.run();
	}

}
