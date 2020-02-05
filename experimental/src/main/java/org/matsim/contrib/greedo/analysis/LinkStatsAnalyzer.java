/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.greedo.analysis;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.jfree.data.xy.YIntervalSeries;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinkStatsAnalyzer {

	private int[] new24hrArray(final int val) {
		final int[] result = new int[24];
		java.util.Arrays.fill(result, val);
		return result;
	}

	private Map<String, List<double[]>> link2relTTsList = new LinkedHashMap<>();

	public LinkStatsAnalyzer() {
	}

	public void addStats(final String linkStatsFileName) {

		Map<String, double[]> link2relTTs = new LinkedHashMap<>();

		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setOmitEmptyColumns(false);
		try {
			parser.parse(linkStatsFileName, new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startCurrentDataRow() {

					final double length_m = this.getDoubleValue("LENGTH");

					if (length_m >= 10.0) { // to avoid tiny connector links

						final String linkId = this.getStringValue("LINK");
						final double freeSpeed_m_s = this.getDoubleValue("FREESPEED");
						final double freeFlowTT_s = length_m / freeSpeed_m_s;

						final double[] relTTs = link2relTTs.computeIfAbsent(linkId, key -> new double[24]);

						for (int h = 0; h < 24; h++) {
							final double tt_s = this.getDoubleValue("TRAVELTIME" + h + "-" + (h + 1) + "avg");
							relTTs[h] = (tt_s / freeFlowTT_s);
						}
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for (String linkId : link2relTTs.keySet()) {
			this.link2relTTsList.computeIfAbsent(linkId, id -> new ArrayList<>()).add(link2relTTs.get(linkId));
		}
	}

	public void run(final int firstScenarioIndex, final int scenarioCnt, final int iterationCnt) {

		for (int scenarioIndex = firstScenarioIndex; scenarioIndex < firstScenarioIndex
				+ scenarioCnt; scenarioIndex++) {
			final String path = "./run" + scenarioIndex + "/output/ITERS/it." + iterationCnt + "/";

			try {
				final String compressedFileName = path + iterationCnt + ".linkstats.txt.gz";
				System.out.println("processing file: " + compressedFileName);

				final GZIPInputStream in = new GZIPInputStream(new FileInputStream(compressedFileName));
				final BufferedReader br = new BufferedReader(new InputStreamReader(in));
				final String uncompressedFileName = path + iterationCnt + ".linkstats.txt";
				final PrintWriter writer = new PrintWriter(uncompressedFileName);
				String line;
				while ((line = br.readLine()) != null) {
					writer.println(line);
				}
				writer.flush();
				writer.close();
				br.close();

				this.addStats(uncompressedFileName);

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void printReport(final String reportFileName, final int noOfExp) {

		final int[] minMoreThan2Times = this.new24hrArray(Integer.MAX_VALUE);
		final int[] maxMoreThan2Times = this.new24hrArray(Integer.MIN_VALUE);

		final int[] minMoreThan4Times = this.new24hrArray(Integer.MAX_VALUE);
		final int[] maxMoreThan4Times = this.new24hrArray(Integer.MIN_VALUE);

		for (int exp = 0; exp < noOfExp; exp++) {

			/*
			 * Check for a given experiment (exp) how many links exceeded per time bin (h)
			 * two respectively four times their free-flow travel time.
			 */

			final int[] moreThan2Times = new int[24];
			final int[] moreThan4Times = new int[24];
			for (String linkId : this.link2relTTsList.keySet()) {
				final double[] relTTs = this.link2relTTsList.get(linkId).get(exp);
				for (int h = 0; h < 24; h++) {
					if (relTTs[h] >= 2.0) {
						moreThan2Times[h]++;
						if (relTTs[h] >= 4.0) {
							moreThan4Times[h]++;
						}
					}
				}
			}

			/*
			 * Update min/max statistics over all experiments.
			 */
			
			for (int h = 0; h < 24; h++) {
				minMoreThan2Times[h] = Math.min(minMoreThan2Times[h], moreThan2Times[h]);
				maxMoreThan2Times[h] = Math.max(maxMoreThan2Times[h], moreThan2Times[h]);
				minMoreThan4Times[h] = Math.min(minMoreThan4Times[h], moreThan4Times[h]);
				maxMoreThan4Times[h] = Math.max(maxMoreThan4Times[h], moreThan4Times[h]);
			}
		}

		// here starts the plotting

		final double linkCnt = this.link2relTTsList.size();
		final YIntervalSeries factor2orMore = new YIntervalSeries("$\\geq$2x free flow tt");
		final YIntervalSeries factor4orMore = new YIntervalSeries("$\\geq$4x free flow tt");
		for (int h = 0; h < 24; h++) {
			factor2orMore.add(h + 0.5, 0, minMoreThan2Times[h] / linkCnt, maxMoreThan2Times[h] / linkCnt);
			factor4orMore.add(h + 0.5, 0, minMoreThan4Times[h] / linkCnt, maxMoreThan4Times[h] / linkCnt);
		}

		final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
		plot.setLog(false);
		plot.addSeries(factor2orMore);
		plot.addSeries(factor4orMore);
		plot.setRange(0, 24, 0.0, 0.1);
		plot.setGrid(4, 0.02);
		plot.addRelativeLegend(0.05, 0.95, 0.1, 0.1);
		plot.setXTick(4);
		plot.setYTick(0.02);
		plot.render(reportFileName, 1);

	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		final LinkStatsAnalyzer analyzer = new LinkStatsAnalyzer();
//		 analyzer.addStats("C:\\Users\\GunnarF\\NoBackup\\data-workspace\\1000.linkstats.txt");
//		analyzer.printReport("congestion.tex", 1);
		analyzer.run(0, 10, 1000);
		analyzer.printReport("congestion.tex", 10);
		System.out.println("... DONE.");
	}

}
