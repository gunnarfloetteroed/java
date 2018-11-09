package floetteroed.misc.simulation.kwmqueueing;

import floetteroed.utilities.networks.construction.AbstractNetwork;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class KWMQueueingSimNetwork extends
		AbstractNetwork<KWMQueueingSimNode, KWMQueueingSimLink> {

	// -------------------- CONSTRUCTION --------------------

	public KWMQueueingSimNetwork(final String id, final String type) {
		super(id, type);
	}

}
