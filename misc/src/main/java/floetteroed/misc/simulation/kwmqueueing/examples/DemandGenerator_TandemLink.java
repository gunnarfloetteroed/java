package floetteroed.misc.simulation.kwmqueueing.examples;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimEvent;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNetwork;
import floetteroed.misc.simulation.kwmqueueing.jobs.RoutedJob;


public class DemandGenerator_TandemLink extends DemandGenerator<RoutedJob> {
	DemandGenerator_TandemLink() {
	}

	List<KWMQueueingSimEvent> newDemand(final KWMQueueingSimNetwork net,
			final double gammaA_veh_s, final double gammaB_veh_s, final double gammaC_veh_s,
			final double straightTurningProba, final double duration_s) {
		List<KWMQueueingSimEvent> result = new LinkedList<KWMQueueingSimEvent>();

		final KWMQueueingSimLink l1 = net.getLink("1");
		final KWMQueueingSimLink l2 = net.getLink("2");
		

		// from A to C
		result.addAll(this.newArrivalEvents(l1, l2, Arrays.asList(l1, l2),
				gammaA_veh_s * straightTurningProba, 0.0, duration_s/3));
		result.addAll(this.newArrivalEvents(l1, l2, Arrays.asList(l1, l2),
				gammaB_veh_s * straightTurningProba, duration_s/3, 2*duration_s/3));
		result.addAll(this.newArrivalEvents(l1, l2, Arrays.asList(l1, l2),
				gammaC_veh_s * straightTurningProba, 2*duration_s/3, duration_s));
		return result;
	}

	@Override
	protected RoutedJob newJob(final String id,
			final KWMQueueingSimLink originLink,
			final KWMQueueingSimLink destinationLink,
			final List<KWMQueueingSimLink> route, final double departureTime_s) {
		return new RoutedJob(id, departureTime_s, route);
	}

}
