package floetteroed.misc.simulation.kwmqueueing;

import static floetteroed.utilities.Discretizer.interpolateOrder0;
import static floetteroed.utilities.math.MathHelpers.round;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SingleLinkStatisticSummarizer {

	// -------------------- MEMBERS --------------------

	private final int spaceCap_jobs;

	private final int binSize_s;

	private final int binCnt;

	private final int[][][][] data; // [time][uq][dq][rq]

	private long samples;

	// -------------------- CONSTRUCTION --------------------

	public SingleLinkStatisticSummarizer(final int spaceCap_jobs, final int binSize_s,
			final int binCnt) {
		this.spaceCap_jobs = spaceCap_jobs;
		this.binSize_s = binSize_s;
		this.binCnt = binCnt;
		this.data = new int[binCnt][spaceCap_jobs + 1][spaceCap_jobs + 1][spaceCap_jobs + 1];
		this.samples = 0;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void addHandlerContent(final LinkStateHandler handler,
			final KWMQueueingSimLink link) {
		final List<Double> uq = interpolateOrder0(handler.getTimes_s(),
				handler.getUpstreamQueueStates(link), 0.0, this.binSize_s,
				this.binCnt);
		final List<Double> dq = interpolateOrder0(handler.getTimes_s(),
				handler.getDownstreamQueueStates(link), 0.0, this.binSize_s,
				this.binCnt);
		final List<Double> rq = interpolateOrder0(handler.getTimes_s(),
				handler.getRunningQueueStates(link), 0.0, this.binSize_s,
				this.binCnt);
		for (int k = 0; k < this.binCnt; k++) {
			this.data[k][round(uq.get(k))][round(dq.get(k))][round(rq.get(k))]++;
		}
		this.samples++;
	}

	public void toFile(final String fileName) throws FileNotFoundException {
		final PrintWriter writer = new PrintWriter(fileName);
		writer.println("% Start time: 0 seconds");
		writer.println("% End time: " + this.binSize_s * (this.binCnt - 1)
				+ " seconds");
		writer.println("% Time step size: " + this.binSize_s);
		writer.println("% Number of replications: " + this.samples);
		writer.println("%");
		writer.println("% time[s]\tUQ\tDQ\tLI\tfrequency\t(events with zero frequency are omitted)");
		writer.println("%");
		for (int bin = 0; bin < this.binCnt; bin++) {
			for (int uq = 0; uq <= this.spaceCap_jobs; uq++) {
				for (int dq = 0; dq <= this.spaceCap_jobs; dq++) {
					for (int li = 0; li <= this.spaceCap_jobs; li++) {
						final int cnt = this.data[bin * this.binSize_s][uq][dq][li];
						if (cnt > 0) {
							writer.println(bin * this.binSize_s + "\t" + uq
									+ "\t" + dq + "\t" + li + "\t" + cnt);
						}
					}
				}
			}
		}
		writer.flush();
		writer.close();
	}

	// TODO for testing
	public double[] avgOccups_veh() {
		final double[] result = new double[this.data.length];
		for (int bin = 0; bin < this.binCnt; bin++) {
			for (int uq = 0; uq <= this.spaceCap_jobs; uq++) {
				for (int dq = 0; dq <= this.spaceCap_jobs; dq++) {
					for (int li = 0; li <= this.spaceCap_jobs; li++) {
						result[bin] += this.data[bin * this.binSize_s][uq][dq][li]
								* (li + dq);
					}
				}
			}
			result[bin] /= (double) this.samples;
		}
		return result;
	}

}
