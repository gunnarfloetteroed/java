package floetteroed.misc.simulation.kwmqueueing.examples;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import floetteroed.misc.simulation.kwmqueueing.KWMQueueingSimLink;

public class TurnCountSummarizer {
	
	// -------------------- MEMBERS --------------------

			private final int spaceCap_jobs;

			private final int binSize_s;

			private final int binCnt;

			private final int[][] data; // [time][outflow]

			private long samples;
	// -------------------- CONSTRUCTION --------------------

			TurnCountSummarizer(final int spaceCap_jobs, final int binSize_s,
					final int binCnt) {
				this.spaceCap_jobs = spaceCap_jobs;//space capacity of the next link
				this.binSize_s = binSize_s;
				this.binCnt = binCnt;
				this.data = new int[binCnt][spaceCap_jobs + 1];
				this.samples = 0;
			}
	// -------------------- IMPLEMENTATION --------------------

			void addHandlerContent(final TurnCounter handler,
					final KWMQueueingSimLink link1, final KWMQueueingSimLink link2) {
				for (int k = 0; k < this.binCnt; k++) { 
					final long result = handler.getCount(link1.getId(),link2.getId(),k);
					int outflow = (int) result;
					this.data[k][outflow]++;
				}
				this.samples++;
				
			}

			void toFile(final String fileName) throws FileNotFoundException {
				final PrintWriter writer = new PrintWriter(fileName);
				writer.println("% Start time: 0 seconds");
				writer.println("% End time: " + this.binSize_s * (this.binCnt - 1)
						+ " seconds");
				writer.println("% Time step size: " + this.binSize_s);
				writer.println("% Number of replications: " + this.samples);
				writer.println("%");
				writer.println("% time[s]\tOutflow\tfrequency\t(events with zero frequency are omitted)");
				writer.println("%");
				for (int bin = 0; bin < this.binCnt; bin++) {
					for (int uq = 0; uq <= this.spaceCap_jobs; uq++) {
					
								final int cnt = this.data[bin * this.binSize_s][uq];
								if (cnt > 0) {
									writer.println(bin * this.binSize_s + "\t" + uq
											+ "\t"  + cnt);
								}
						
					}
				}
				writer.flush();
				writer.close();
			}
}
