package floetteroed.misc.simulation.kwmqueueing.examples;
import java.io.FileNotFoundException;
import java.util.Random;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNetwork;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimNode;
import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimulation;
import floetteroed.misc.simulation.kwmqueueing.LinkStateHandler;
import floetteroed.misc.simulation.kwmqueueing.SingleLinkStatisticSummarizer;

public class MergeLink {
	// SCENARIO-SPECIFIC PARAMETERS
	        static final double tMax = 600.0;
			static final long replications = 1000 * 1000;
			static final double straightTurningProba = 1.0;
			static final int fwdLag_s = 5;
			static final int bwdLag_s = 10;
			static final int spaceCap_veh = 10;
			static final double gammaA1_veh_s = 0.2;
			static final double gammaA2_veh_s = 0.6;
			static final double gammaA3_veh_s = 0.02;
			static final double gammaB_veh_s = 0.1;	
			static final double innerFlowCap_veh_s = 0.4;
			static final double innerFlowCap_veh_s_B = 0.4;
			static final double exitFlowCap_veh_s = 0.01;
			
			public static void main(String[] args) throws FileNotFoundException {

				System.out.println("STARTED");
				
				// CONFIGURE
				String filePrefix = new StringBuilder("./MergeLink_lambda")
						.append(gammaA1_veh_s)
						.append("_inn")
					    .append(innerFlowCap_veh_s)
					    .append("_lambda")
					    .append(gammaB_veh_s)
					    .append("_inn")
					    .append(innerFlowCap_veh_s_B)
					    .append("_mu")
			            .append(exitFlowCap_veh_s)  
			            .append("_testOutput")
			            .toString();

				final int binSize_s = 1;
				final int binCnt = 1 + (int) tMax;	
			
				// CREATE STATISTICS COLLECTORS

				final SingleLinkStatisticSummarizer l1stats = new SingleLinkStatisticSummarizer(
						spaceCap_veh, binSize_s, binCnt);
				final SingleLinkStatisticSummarizer l2stats = new SingleLinkStatisticSummarizer(
						spaceCap_veh, binSize_s, binCnt);
				final SingleLinkStatisticSummarizer l3stats = new SingleLinkStatisticSummarizer(
						spaceCap_veh, binSize_s, binCnt);
				final TurnCountSummarizer out13stats = new TurnCountSummarizer(
						spaceCap_veh, binSize_s, binCnt);
				final TurnCountSummarizer out23stats = new TurnCountSummarizer(
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
					final KWMQueueingSimNode nD = new KWMQueueingSimNode("D");
					net.addNode(nD);

					final KWMQueueingSimLink l1 = new KWMQueueingSimLink("1");
					l1.setFwdLag_s(fwdLag_s);
					l1.setBwdLag_s(bwdLag_s);
					l1.setSpaceCapacity_jobs(spaceCap_veh);
					l1.setServiceCapacity_jobs_s(innerFlowCap_veh_s);
					l1.setPriority(2);
					KWMQueueingSimNetwork.connect(nA, nB, l1);
					net.addLink(l1);
					
					final KWMQueueingSimLink l2 = new KWMQueueingSimLink("2");
					l2.setFwdLag_s(fwdLag_s);
					l2.setBwdLag_s(bwdLag_s);
					l2.setSpaceCapacity_jobs(spaceCap_veh);
					l2.setServiceCapacity_jobs_s(innerFlowCap_veh_s_B);
					l2.setPriority(1);
					KWMQueueingSimNetwork.connect(nD, nB, l2);
					net.addLink(l2);

					final KWMQueueingSimLink l3 = new KWMQueueingSimLink("3");
					l3.setFwdLag_s(fwdLag_s);
					l3.setBwdLag_s(bwdLag_s);
					l3.setSpaceCapacity_jobs(spaceCap_veh);
					l3.setServiceCapacity_jobs_s(exitFlowCap_veh_s);
					KWMQueueingSimNetwork.connect(nB, nC, l3);
					net.addLink(l3);

					

					// CREATE THE SIMULATION

					final KWMQueueingSimulation sim = new KWMQueueingSimulation(
							new Random(), net);
					sim.setInstantaneousUnblocking(false);// with Instantaneous Unblocking is not modified yet
					final LinkStateHandler linkStateHandler = new LinkStateHandler(net);
					sim.addHandler(linkStateHandler);
					final TurnCounter outflowCounter = new TurnCounter();
					sim.addHandler(outflowCounter);
					//sim.addHandler(new PrintEventHandler<KWMQueueingEvent>());

					// ADD THE DEMAND

					sim.addEvents(new DemandGenerator_MergeLink().newDemand(net,
							gammaA1_veh_s,gammaA2_veh_s, gammaA3_veh_s,gammaB_veh_s, straightTurningProba, tMax));

					// RUN THE SIMULATION

					sim.run();

					// EXTRACT THE RESULTS

					l1stats.addHandlerContent(linkStateHandler, l1);
					l2stats.addHandlerContent(linkStateHandler, l2);
					l3stats.addHandlerContent(linkStateHandler, l3);
					out13stats.addHandlerContent(outflowCounter, l1, l3);
					out23stats.addHandlerContent(outflowCounter, l2, l3);
					//nodestats13.addHandlerContent(linkStateHandler,l1,linkStateHandler,l3);
					//nodestats23.addHandlerContent(linkStateHandler,l2,linkStateHandler,l3);

				}

				// WRITE RESULTS TO FILE

				l1stats.toFile(filePrefix + "_1.data");
				l2stats.toFile(filePrefix + "_2.data");
				l3stats.toFile(filePrefix + "_3.data");
				//nodestats13.toFile(filePrefix + "_node13.data");
				//nodestats23.toFile(filePrefix + "_node23.data");
				out13stats.toFile(filePrefix + "_outflow13.data");
				out23stats.toFile(filePrefix + "_outflow23.data");

				// WRITE OUT SOME TEST DATA

				double[] avg1 = l1stats.avgOccups_veh();
				double[] avg2 = l2stats.avgOccups_veh();
				double[] avg3 = l3stats.avgOccups_veh();
				

				System.out
						.println("time\tavg1\tavg2\tavg3");
				for (int i = 0; i < avg1.length; i++) {
					System.out.print(i + "\t");
					System.out.print(avg1[i] + "\t");
					System.out.print(avg2[i] + "\t");
					System.out.print(avg3[i]);
				}

				System.out.println("DONE after " + (System.currentTimeMillis() - tic)
						+ "ms");
			}
}
