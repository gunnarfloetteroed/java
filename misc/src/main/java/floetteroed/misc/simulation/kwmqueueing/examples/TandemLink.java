package floetteroed.misc.simulation.kwmqueueing.examples;

import java.io.FileNotFoundException;
import java.util.Random;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNetwork;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNode;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimulation;
import floetteroed.misc.simulation.kwmqueueing.LinkStateHandler;
import floetteroed.misc.simulation.kwmqueueing.SingleLinkStatisticSummarizer;


public class TandemLink {

	
	// SCENARIO-SPECIFIC PARAMETERS

		static final long replications = 1000 * 1000;
		static final double straightTurningProba = 1.0;
		static final double tMax = 600.0; // simulation duration

		static final int fwdLag_s = 5;
		static final int bwdLag_s = 10;
		static final int spaceCap_veh = 10;

		static final double gamma_veh_s = 0.6;
		static final double gammaB_veh_s = 0.01;
		static final double gammaC_veh_s = 0.1;
		static final double innerFlowCap_veh_s = 0.4;
		static final double exitFlowCap_veh_s = 0.2;
		
		public static void main(String[] args) throws FileNotFoundException {
			
				System.out.println("STARTED");
				

				// CONFIGURE
				String filePrefix = new StringBuilder("./TandemLink_mu")
			    .append(exitFlowCap_veh_s)
			    .append("_inn")
			    .append(innerFlowCap_veh_s)
			    .append("_lambda")
			    .append(gamma_veh_s)
			    .append("_cap")
			    .append(spaceCap_veh)
			    .append("_fwd")
			    .append(fwdLag_s)
			    .append("_testOutput")
			    .toString();


				final int binSize_s = 1;
				final int binCnt = 1 + (int) tMax;

				// CREATE STATISTICS COLLECTORS

				final SingleLinkStatisticSummarizer l1stats = new SingleLinkStatisticSummarizer(
						spaceCap_veh, binSize_s, binCnt);
				final SingleLinkStatisticSummarizer l2stats = new SingleLinkStatisticSummarizer(
						spaceCap_veh, binSize_s, binCnt);
				//final NodeStatisticSummarizer nodestats = new NodeStatisticSummarizer(
						//spaceCap_veh, spaceCap_veh, binSize_s, binCnt);
				final TurnCountSummarizer out12stats = new TurnCountSummarizer(
						spaceCap_veh, binSize_s, binCnt);
				
				// RUN REPLICATIONS

				final long tic = System.currentTimeMillis();

				for (long run = 0; run < replications; run++) {

					if (run % 1000 == 0) {
						System.out.println("RUN " + (run + 1));
					}

					// CREATE THE NETWORK

					final KWMQueueingSimNetwork net = new KWMQueueingSimNetwork(
							"\"networks-paper\" network", "KWM-Queueing-Simulation");

					final KWMQueueingSimNode nA = new KWMQueueingSimNode("A");
					net.addNode(nA);
					final KWMQueueingSimNode nB = new KWMQueueingSimNode("B");
					net.addNode(nB);
					final KWMQueueingSimNode nC = new KWMQueueingSimNode("C");
					net.addNode(nC);

					final KWMQueueingSimLink l1 = new KWMQueueingSimLink("1");
					l1.setFwdLag_s(fwdLag_s);
					l1.setBwdLag_s(bwdLag_s);
					l1.setSpaceCapacity_jobs(spaceCap_veh);
					l1.setServiceCapacity_jobs_s(innerFlowCap_veh_s);
					KWMQueueingSimNetwork.connect(nA, nB, l1);
					net.addLink(l1);

					final KWMQueueingSimLink l2 = new KWMQueueingSimLink("2");
					l2.setFwdLag_s(fwdLag_s);
					l2.setBwdLag_s(bwdLag_s);
					l2.setSpaceCapacity_jobs(spaceCap_veh);
					l2.setServiceCapacity_jobs_s(exitFlowCap_veh_s);
					KWMQueueingSimNetwork.connect(nB, nC, l2);
					net.addLink(l2);

					

					// CREATE THE SIMULATION

					final KWMQueueingSimulation sim = new KWMQueueingSimulation(
							new Random(), net);
					sim.setInstantaneousUnblocking(false); //change it to true for comparison
					final LinkStateHandler linkStateHandler = new LinkStateHandler(net);
					sim.addHandler(linkStateHandler);
					final TurnCounter outflowCounter = new TurnCounter();
					sim.addHandler(outflowCounter);
					// sim.addHandler(new PrintEventHandler<KWMQueueingEvent>());

					// ADD THE DEMAND

					sim.addEvents(new DemandGenerator_TandemLink().newDemand(net,
							gamma_veh_s, gammaB_veh_s, gammaC_veh_s, straightTurningProba, tMax));

					// RUN THE SIMULATION

					sim.run();

					// EXTRACT THE RESULTS

					l1stats.addHandlerContent(linkStateHandler, l1);
					l2stats.addHandlerContent(linkStateHandler, l2);
					//nodestats.addHandlerContent(linkStateHandler,l1,linkStateHandler,l6);
					out12stats.addHandlerContent(outflowCounter, l1, l2);
					//System.out.println(outflowCounter.toString());
					
				}

				// WRITE RESULTS TO FILE

				l1stats.toFile(filePrefix + "_1.data");
				l2stats.toFile(filePrefix + "_2.data");
				//nodestats.toFile(filePrefix + "_node.data");
				out12stats.toFile(filePrefix + "_outflow12.data");
				

				// WRITE OUT SOME TEST DATA

				double[] avg1 = l1stats.avgOccups_veh();
				double[] avg2 = l2stats.avgOccups_veh();
				

				System.out
						.println("time\tavg1\tavg2");
				for (int i = 0; i < avg1.length; i++) {
					System.out.print(i + "\t");
					System.out.print(avg1[i] + "\t");
					System.out.println(avg2[i]);
				}

				System.out.println("DONE after " + (System.currentTimeMillis() - tic)
						+ "ms");
			}
}
		
		

