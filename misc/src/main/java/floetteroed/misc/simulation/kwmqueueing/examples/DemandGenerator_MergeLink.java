package floetteroed.misc.simulation.kwmqueueing.examples;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimEvent;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNetwork;
import floetteroed.misc.simulation.kwmqueueing.jobs.RoutedJob;

public class DemandGenerator_MergeLink extends DemandGenerator<RoutedJob> {
	DemandGenerator_MergeLink() {
	}

	List<KWMQueueingSimEvent> newDemand(final KWMQueueingSimNetwork net,
			final double lambdaA1_veh_s, final double lambdaA2_veh_s, final double lambdaA3_veh_s,
			final double lambdaB_veh_s, final double straightTurningProba, final double duration_s) {
		List<KWMQueueingSimEvent> result = new LinkedList<KWMQueueingSimEvent>();

		final KWMQueueingSimLink l1 = net.getLink("1");
		final KWMQueueingSimLink l2 = net.getLink("2");
		final KWMQueueingSimLink l3 = net.getLink("3");


		// from A to C
		result.addAll(this.newArrivalEvents(l1, l3, Arrays.asList(l1, l3),
				lambdaA1_veh_s * straightTurningProba, 0.0, duration_s/3));
		result.addAll(this.newArrivalEvents(l1, l3, Arrays.asList(l1, l3),
				lambdaA2_veh_s * straightTurningProba, duration_s/3, 2*duration_s/3));
		result.addAll(this.newArrivalEvents(l1, l3, Arrays.asList(l1, l3),
				lambdaA3_veh_s * straightTurningProba, 2*duration_s/3, duration_s));

		// from D to C
		result.addAll(this.newArrivalEvents(l2, l3, Arrays.asList(l2, l3),
				lambdaB_veh_s * straightTurningProba, 0.0, duration_s));

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
