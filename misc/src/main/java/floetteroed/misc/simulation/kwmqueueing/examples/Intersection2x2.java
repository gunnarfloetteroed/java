package floetteroed.misc.simulation.kwmqueueing.examples;

import java.util.Arrays;
import java.util.Random;

import floetteroed.misc.simulation.eventbased.PrintEventHandler;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimEvent;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNetwork;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNode;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimulation;
import floetteroed.misc.simulation.kwmqueueing.LinkStateHandler;
import floetteroed.utilities.Units;


public class Intersection2x2 {

	// -------------------- CONSTANTS --------------------

	private final double rhoMax_veh_m = 140.0 / 1000;
	private final double vehLength_m = 1.0 / this.rhoMax_veh_m;
	private final double bwdWaveSpeed_m_s = Units.M_S_PER_KM_H * 18.0;

	private final double fwdLag1_s;
	private final double fwdLag2_s;
	private final double fwdLag3_s;
	private final double fwdLag4_s;

	private final double bwdLag1_s;
	private final double bwdLag2_s;
	private final double bwdLag3_s;
	private final double bwdLag4_s;

	private final int spaceCap1_veh;
	private final int spaceCap2_veh;
	private final int spaceCap3_veh;
	private final int spaceCap4_veh;

	private final double flowCap1_veh_s;
	private final double flowCap2_veh_s;
	private final double flowCap3_veh_s;
	private final double flowCap4_veh_s;

	private final double arrivalsDuration_s;
	private final double arrivals1_veh_s;
	private final double arrivals2_veh_s;

	private final double beta13;
	private final double beta23;

	// -------------------- MEMBERSR --------------------

	private Random rnd = new Random();

	private LinkStateHandler linkStateHandler = null;

	// -------------------- CONSTRUCTION --------------------

	public Intersection2x2(final int spaceCap1_veh, final int spaceCap2_veh,
			final int spaceCap3_veh, final int spaceCap4_veh,
			double flowCap1_veh_s, double flowCap2_veh_s,
			double flowCap3_veh_s, double flowCap4_veh_s,
			final double vel1_m_s, final double vel2_m_s,
			final double vel3_m_s, final double vel4_m_s,
			final double arrivalsDuration_s, double arrivals1_veh_s,
			double arrivals2_veh_s, final double beta13, final double beta23) {

		/*
		 * Ensure that bottleneck capacities do not exceed fundamental diagram
		 * capacities.
		 */

		flowCap1_veh_s = Math.min(flowCap1_veh_s, vel1_m_s
				* this.bwdWaveSpeed_m_s / (vel1_m_s + this.bwdWaveSpeed_m_s)
				* this.rhoMax_veh_m);
		flowCap2_veh_s = Math.min(flowCap2_veh_s, vel2_m_s
				* this.bwdWaveSpeed_m_s / (vel2_m_s + this.bwdWaveSpeed_m_s)
				* this.rhoMax_veh_m);
		flowCap3_veh_s = Math.min(flowCap3_veh_s, vel3_m_s
				* this.bwdWaveSpeed_m_s / (vel3_m_s + this.bwdWaveSpeed_m_s)
				* this.rhoMax_veh_m);
		flowCap4_veh_s = Math.min(flowCap4_veh_s, vel4_m_s
				* this.bwdWaveSpeed_m_s / (vel4_m_s + this.bwdWaveSpeed_m_s)
				* this.rhoMax_veh_m);

		// -------------------- DERIVED --------------------

		final double length1_m = spaceCap1_veh * this.vehLength_m;
		final double length2_m = spaceCap2_veh * this.vehLength_m;
		final double length3_m = spaceCap3_veh * this.vehLength_m;
		final double length4_m = spaceCap4_veh * this.vehLength_m;

		this.fwdLag1_s = length1_m / vel1_m_s;
		this.fwdLag2_s = length2_m / vel2_m_s;
		this.fwdLag3_s = length3_m / vel3_m_s;
		this.fwdLag4_s = length4_m / vel4_m_s;

		this.bwdLag1_s = length1_m / this.bwdWaveSpeed_m_s;
		this.bwdLag2_s = length2_m / this.bwdWaveSpeed_m_s;
		this.bwdLag3_s = length3_m / this.bwdWaveSpeed_m_s;
		this.bwdLag4_s = length4_m / this.bwdWaveSpeed_m_s;

		this.spaceCap1_veh = spaceCap1_veh;
		this.spaceCap2_veh = spaceCap2_veh;
		this.spaceCap3_veh = spaceCap3_veh;
		this.spaceCap4_veh = spaceCap4_veh;

		this.flowCap1_veh_s = flowCap1_veh_s;
		this.flowCap2_veh_s = flowCap2_veh_s;
		this.flowCap3_veh_s = flowCap3_veh_s;
		this.flowCap4_veh_s = flowCap4_veh_s;

		this.arrivalsDuration_s = arrivalsDuration_s;
		this.arrivals1_veh_s = arrivals1_veh_s;
		this.arrivals2_veh_s = arrivals2_veh_s;

		this.beta13 = beta13;
		this.beta23 = beta23;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run() {

		// -------------------- CONSTRUCT NETWORK --------------------

		final KWMQueueingSimNetwork net = new KWMQueueingSimNetwork("2x2 intersection",
				"KWM-Queueing");

		final KWMQueueingSimNode n1 = new KWMQueueingSimNode("n1");
		net.addNode(n1);

		final KWMQueueingSimNode n2 = new KWMQueueingSimNode("n2");
		net.addNode(n2);

		final KWMQueueingSimNode c = new KWMQueueingSimNode("c");
		net.addNode(c);

		final KWMQueueingSimNode n3 = new KWMQueueingSimNode("n3");
		net.addNode(n3);

		final KWMQueueingSimNode n4 = new KWMQueueingSimNode("n4");
		net.addNode(n4);

		final KWMQueueingSimLink l1 = new KWMQueueingSimLink("l1");
		l1.setFwdLag_s(this.fwdLag1_s);
		l1.setBwdLag_s(this.bwdLag1_s);
		l1.setSpaceCapacity_jobs(this.spaceCap1_veh);
		l1.setServiceCapacity_jobs_s(this.flowCap1_veh_s);
		l1.setPriority(1);
		KWMQueueingSimNetwork.connect(n1, c, l1);
		net.addLink(l1);

		final KWMQueueingSimLink l2 = new KWMQueueingSimLink("l2");
		l2.setFwdLag_s(this.fwdLag2_s);
		l2.setBwdLag_s(this.bwdLag2_s);
		l2.setSpaceCapacity_jobs(this.spaceCap2_veh);
		l2.setServiceCapacity_jobs_s(this.flowCap2_veh_s);
		l2.setPriority(2);
		KWMQueueingSimNetwork.connect(n2, c, l2);
		net.addLink(l2);

		final KWMQueueingSimLink l3 = new KWMQueueingSimLink("l3");
		l3.setFwdLag_s(this.fwdLag3_s);
		l3.setBwdLag_s(this.bwdLag3_s);
		l3.setSpaceCapacity_jobs(this.spaceCap3_veh);
		l3.setServiceCapacity_jobs_s(this.flowCap3_veh_s);
		KWMQueueingSimNetwork.connect(c, n3, l3);
		net.addLink(l3);

		final KWMQueueingSimLink l4 = new KWMQueueingSimLink("l4");
		l4.setFwdLag_s(this.fwdLag4_s);
		l4.setBwdLag_s(this.bwdLag4_s);
		l4.setSpaceCapacity_jobs(this.spaceCap4_veh);
		l4.setServiceCapacity_jobs_s(this.flowCap4_veh_s);
		KWMQueueingSimNetwork.connect(c, n4, l4);
		net.addLink(l4);

		// -------------------- RUN THE SIMULATION --------------------

		final KWMQueueingSimulation sim = new KWMQueueingSimulation(rnd, net);
		sim.setInstantaneousUnblocking(false);

		final Intersection2x2DemandGenerator demand = new Intersection2x2DemandGenerator(
				rnd);
		sim.addEvents(demand.newArrivalEvents(l1, l3, Arrays.asList(l1, l3),
				arrivals1_veh_s * beta13, 0.0, arrivalsDuration_s));
//		sim.addEvents(demand.newArrivalEvents(l1, l4, Arrays.asList(l1, l4),
//				arrivals1_veh_s * (1.0 - beta13), 0.0, arrivalsDuration_s));
		sim.addEvents(demand.newArrivalEvents(l2, l3, Arrays.asList(l2, l3),
				10 * arrivals2_veh_s * beta23, 0.0, arrivalsDuration_s));
//		sim.addEvents(demand.newArrivalEvents(l2, l4, Arrays.asList(l2, l4),
//				arrivals2_veh_s * (1.0 - beta23), 0.0, arrivalsDuration_s));

		this.linkStateHandler = new LinkStateHandler(net);
		sim.addHandler(this.linkStateHandler);
		sim.addHandler(new PrintEventHandler<KWMQueueingSimEvent>());
		sim.run();
		
		System.out.println(this.linkStateHandler);
	}

	public void run(final int replications) {
		for (int r = 0; r < replications; r++) {
			this.run();
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		Intersection2x2 intersection = new Intersection2x2(10, 10, 10, 10,
				Units.VEH_S_PER_VEH_H * 2000, Units.VEH_S_PER_VEH_H * 2000,
				Units.VEH_S_PER_VEH_H * 2000, Units.VEH_S_PER_VEH_H * 2000,
				Units.M_S_PER_KM_H * 36, Units.M_S_PER_KM_H * 36,
				Units.M_S_PER_KM_H * 36, Units.M_S_PER_KM_H * 36, 120.0,
				Units.VEH_S_PER_VEH_H * 1500, Units.VEH_S_PER_VEH_H * 1500,
				0.5, 0.5);

		intersection.run();

		System.out.println("... DONE");
	}

}
