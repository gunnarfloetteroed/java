package floetteroed.misc.simulation.kwmqueueing.examples;

import java.util.List;
import java.util.Random;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;
import floetteroed.misc.simulation.kwmqueueing.jobs.RoutedJob;

class Intersection2x2DemandGenerator extends DemandGenerator<RoutedJob> {

	Intersection2x2DemandGenerator(final Random rnd) {
		this.setRandom(rnd);
	}

	@Override
	protected RoutedJob newJob(final String id, final KWMQueueingSimLink originLink,
			final KWMQueueingSimLink destinationLink, final List<KWMQueueingSimLink> route,
			double departureTime_s) {
		return new RoutedJob(id, departureTime_s, route);
	}

}
