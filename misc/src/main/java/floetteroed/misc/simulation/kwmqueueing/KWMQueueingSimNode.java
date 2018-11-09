package floetteroed.misc.simulation.kwmqueueing;

import floetteroed.utilities.networks.construction.AbstractNode;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class KWMQueueingSimNode extends
		AbstractNode<KWMQueueingSimNode, KWMQueueingSimLink> {

	// -------------------- CONSTRUCTION --------------------

	public KWMQueueingSimNode(final String id) {
		super(id);
	}

}
