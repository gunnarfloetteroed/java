package floetteroed.misc.simulation.kwmqueueing;

import static floetteroed.utilities.Discretizer.interpolateOrder1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import floetteroed.misc.simulation.eventbased.AbstractEventHandler;
import floetteroed.utilities.DynamicData;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class LinkStateHandler extends AbstractEventHandler<KWMQueueingSimEvent> {

	// -------------------- MEMBERS --------------------

	private final List<Double> times_s = new ArrayList<Double>();

	private final Map<KWMQueueingSimLink, List<Double>> link2upstreamQueue = new LinkedHashMap<KWMQueueingSimLink, List<Double>>();

	private final Map<KWMQueueingSimLink, List<Double>> link2downstreamQueue = new LinkedHashMap<KWMQueueingSimLink, List<Double>>();

	private final Map<KWMQueueingSimLink, List<Double>> link2runningQueue = new LinkedHashMap<KWMQueueingSimLink, List<Double>>();

	// -------------------- CONSTRUCTION --------------------

	public LinkStateHandler(final KWMQueueingSimNetwork net) {
		for (KWMQueueingSimLink link : net.getLinks()) {
			this.link2upstreamQueue.put(link, new ArrayList<Double>());
			this.link2downstreamQueue.put(link, new ArrayList<Double>());
			this.link2runningQueue.put(link, new ArrayList<Double>());
		}
	}

	// -------------------- SIMPLE GETTERS --------------------

	public int size() {
		return this.times_s.size();
	}

	public List<Double> getTimes_s() {
		return this.times_s;
	}

	public List<Double> getUpstreamQueueStates(final KWMQueueingSimLink link) {
		return this.link2upstreamQueue.get(link);
	}

	public List<Double> getDownstreamQueueStates(final KWMQueueingSimLink link) {
		return this.link2downstreamQueue.get(link);
	}

	public List<Double> getRunningQueueStates(final KWMQueueingSimLink link) {
		return this.link2runningQueue.get(link);
	}

	// -------------------- COMPLEX GETTERS --------------------

	public double lastTime_s() {
		if (this.size() == 0) {
			return Double.NEGATIVE_INFINITY;
		} else {
			return this.times_s.get(this.size() - 1);
		}
	}

	public List<Double> getAbsoluteOccupancies(final KWMQueueingSimLink link) {
		final Vector result = new Vector(this.getRunningQueueStates(link));
		result.add(new Vector(this.getDownstreamQueueStates(link)));
		return result.asList();
	}

	public List<Double> getRelativeOccupancies(final KWMQueueingSimLink link) {
		final List<Double> result = new ArrayList<Double>(this.size());
		final double spaceCap_jobs = link.getSpaceCapacity_jobs();
		for (Double x : this.getAbsoluteOccupancies(link)) {
			result.add(x / spaceCap_jobs);
		}
		return result;
	}

	public DynamicData<KWMQueueingSimLink> getRelativeOccupanciesAsDynamicData(
			final int startTime_s, final int binSize_s, final int binCnt) {
		final DynamicData<KWMQueueingSimLink> result = new DynamicData<KWMQueueingSimLink>(
				startTime_s, binSize_s, binCnt);
		for (KWMQueueingSimLink link : this.link2upstreamQueue.keySet()) {
			final List<Double> values = interpolateOrder1(this.times_s,
					this.getRelativeOccupancies(link), startTime_s, binSize_s,
					binCnt);
			for (int bin = 0; bin < binCnt; bin++) {
				result.add(link, bin, values.get(bin));
			}
		}
		return result;
	}

	// --------------- IMPLEMENTATION OF AbstractEventHandler ---------------

	@Override
	public boolean isResponsible(final KWMQueueingSimEvent event) {
		if (event.getTime_s() < this.lastTime_s()) {
			throw new RuntimeException("current event time "
					+ event.getTime_s() + "s is before last event time "
					+ this.lastTime_s() + "s");
		}
		return true;
	}

	@Override
	public List<KWMQueueingSimEvent> process(final KWMQueueingSimEvent event) {
		if (event.getTime_s() == this.lastTime_s()) {
			final int lastIndex = this.size() - 1;
			for (KWMQueueingSimLink link : this.link2upstreamQueue.keySet()) {
				this.link2upstreamQueue.get(link).set(lastIndex,
						new Double(link.getUQJobCnt()));
				this.link2downstreamQueue.get(link).set(lastIndex,
						new Double(link.getDQJobCnt()));
				this.link2runningQueue.get(link).set(lastIndex,
						new Double(link.getRQJobCnt()));
			}
		} else if (event.getTime_s() > this.lastTime_s()) {
			this.times_s.add(event.getTime());
			for (KWMQueueingSimLink link : this.link2upstreamQueue.keySet()) {
				this.link2upstreamQueue.get(link).add(
						new Double(link.getUQJobCnt()));
				this.link2downstreamQueue.get(link).add(
						new Double(link.getDQJobCnt()));
				this.link2runningQueue.get(link).add(
						new Double(link.getRQJobCnt()));
			}
		} else {
			throw new RuntimeException("current event time "
					+ event.getTime_s() + "s is before last event time "
					+ this.lastTime_s() + "s");
		}
		return null;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();

		result.append("time[s]\t");
		for (KWMQueueingSimLink link : this.link2upstreamQueue.keySet()) {
			result.append(link.getId());
			result.append("[veh]\t");
		}
		result.append("\n");

		for (int i = 0; i < this.times_s.size(); i++) {
			result.append(this.times_s.get(i));
			result.append("\t");
			for (KWMQueueingSimLink link : this.link2upstreamQueue.keySet()) {
				result.append((int) (this.getRunningQueueStates(link).get(i) + this
						.getDownstreamQueueStates(link).get(i)));
				result.append("\t");
			}
			result.append("\n");
		}

		return result.toString();
	}

}
