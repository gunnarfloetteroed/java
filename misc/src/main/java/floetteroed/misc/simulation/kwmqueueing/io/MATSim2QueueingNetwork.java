package floetteroed.misc.simulation.kwmqueueing.io;

import static floetteroed.utilities.math.MathHelpers.round;
import static floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader.LINKS_CAPPERIOD;
import static floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader.LINK_CAPACITY;
import static floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader.LINK_FREESPEED;
import static floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader.LINK_LENGTH;
import static floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader.LINK_PERMLANES;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNetwork;
import floetteroed.utilities.Time;
import floetteroed.utilities.Units;
import floetteroed.utilities.networks.construction.NetworkPostprocessor;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MATSim2QueueingNetwork implements
		NetworkPostprocessor<KWMQueueingSimNetwork> {

	// -------------------- CONSTANTS --------------------

	public static final double CAR_LENGTH_M = 1000.0 / 140.0;

	public static final double BWD_WAVESPEED_M_S = Units.M_S_PER_KM_H * 18.0;

	// --------------- IMPLEMENTATION OF NetworkPostprocessor ---------------

	@Override
	public void run(final KWMQueueingSimNetwork net) {
		final double capperiod_s = Time.secFromStr(
				net.getLinksAttr(LINKS_CAPPERIOD), ':');
		for (KWMQueueingSimLink link : net.getLinks()) {
			link.setServiceCapacity_jobs_s(parseDouble(link
					.getAttr(LINK_CAPACITY)) / capperiod_s);
			final double length_m = parseDouble(link.getAttr(LINK_LENGTH));
			link.setSpaceCapacity_jobs(parseInt(link.getAttr(LINK_PERMLANES))
					* max(1, round(length_m / CAR_LENGTH_M)));
			link.setFwdLag_s(length_m
					/ parseDouble(link.getAttr(LINK_FREESPEED)));
			link.setBwdLag_s(length_m / BWD_WAVESPEED_M_S);
		}
	}
}
