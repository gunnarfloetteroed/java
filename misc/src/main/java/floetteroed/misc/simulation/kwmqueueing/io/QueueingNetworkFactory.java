package floetteroed.misc.simulation.kwmqueueing.io;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNetwork;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNode;
import floetteroed.utilities.networks.construction.AbstractNetworkFactory;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class QueueingNetworkFactory extends
		AbstractNetworkFactory<KWMQueueingSimNode, KWMQueueingSimLink, KWMQueueingSimNetwork> {

	// --------------- IMPLEMENTATION OF AbstractNetworkFactory ---------------

	@Override
	protected KWMQueueingSimNetwork newNetwork(final String id, final String type) {
		return new KWMQueueingSimNetwork(id, type);
	}

	@Override
	protected KWMQueueingSimNode newNode(final String id) {
		return new KWMQueueingSimNode(id);
	}

	@Override
	protected KWMQueueingSimLink newLink(final String id) {
		return new KWMQueueingSimLink(id);
	}

}
