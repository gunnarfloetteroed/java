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

import java.io.File;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationAnalyzer2 {

	static void configureUtilityGapPlot(final AccelerationAnalysisIntervalPlot plot) {
		plot.setLog(true);
		plot.setRange(0, 2000, -2.5, 2.5);
		plot.setGrid(400, 1.0);
		plot.addRelativeLegend(0.5, 0.4, 0.1, 0.1);
		plot.setXTick(400);
		plot.setYTick(1.0);
	}

	static void configureUtilityPlot(final AccelerationAnalysisIntervalPlot plot) {
		plot.setLog(true);
		plot.setRange(0, 2000, -5, -1.5);
		plot.setGrid(400, 0.5);
		plot.addRelativeLegend(0.5, 0.4, 0.1, 0.1);
		plot.setXTick(400);
		plot.setYTick(0.5);
	}

	static void configureAgePlot(final AccelerationAnalysisIntervalPlot plot, final double maxAge,
			final double ageTick) {
		plot.setLog(false);
		plot.setRange(0, 2000, 0, maxAge);
		plot.setGrid(400, ageTick);
		plot.addRelativeLegend(0.05, 0.95, 0.1, 0.1);
		plot.setXTick(400);
		plot.setYTick(ageTick);
	}

	static void configureRealizedLambdaSeries(final AccelerationAnalysisIntervalPlot plot) {
		plot.setLog(false);
		plot.setRange(0, 2000, 0, 1.0);
		plot.setGrid(400, 0.2);
		plot.addRelativeLegend(0.5, 0.4, 0.1, 0.1);
		plot.setXTick(400);
		plot.setYTick(0.2);
	}

	public static void main(String[] args) {
		System.out.println("STARTED...");

		final int scenarioCnt = 10;

		final int sampleX = 10;
		final double deltaIt = 200;

		final AccelerationExperimentData baseMSA = new AccelerationExperimentData("./base_MSA", 0, scenarioCnt,
				2000);
		final AccelerationExperimentData baseSqrtMSA = new AccelerationExperimentData("./base_sqrt-MSA", 0, scenarioCnt,
				2000);
		final AccelerationExperimentData nnBSqrtMSA = new AccelerationExperimentData("./nnB_sqrt-MSA", 0, scenarioCnt,
				2000);
		final AccelerationExperimentData nullSqrtMSA = new AccelerationExperimentData("./null_sqrt-MSA", 0, scenarioCnt,
				2000);
		final AccelerationExperimentData selfSqrtMSA = new AccelerationExperimentData("./self_sqrt-MSA", 0, scenarioCnt,
				2000);


		// UTILITIES STARTING HERE ==========================================

		{
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(baseMSA.newRealizedUtilitiesSeries("base MSA"));
			plot.addSeries(baseSqrtMSA.newRealizedUtilitiesSeries("base sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_msa-vs-sqrtMSA.tex", sampleX);
		}
		
		{
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(nnBSqrtMSA.newRealizedUtilitiesSeries("nnB sqrt-MSA"));
			plot.addSeries(nullSqrtMSA.newRealizedUtilitiesSeries("null sqrt-MSA"));
			plot.addSeries(selfSqrtMSA.newRealizedUtilitiesSeries("self sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_sqrtMSA-variations.tex", sampleX);
		}


		// UTILITY GAPS STARTING HERE ==========================================

		{ 
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(baseMSA.newExpectedUtilityChangesSeries("base MSA"));
			plot.addSeries(baseSqrtMSA.newExpectedUtilityChangesSeries("base sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_msa-vs-sqrtMSA.tex", sampleX);
		}

		{ 
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(nnBSqrtMSA.newExpectedUtilityChangesSeries("nnB sqrt-MSA"));
			plot.addSeries(nullSqrtMSA.newExpectedUtilityChangesSeries("null sqrt-MSA"));
			plot.addSeries(selfSqrtMSA.newExpectedUtilityChangesSeries("self sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_sqrtMSA-variations.tex", sampleX);
		}

		// AGE PERCENTILES ==================================================

		{
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(baseMSA.newAgePercentile90Series("90 percentile"));
			plot.addSeries(baseMSA.newAgePercentile60Series("60 percentile"));
			plot.addSeries(baseMSA.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 100, 10);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_baseMSA.tex", sampleX);
		}

		{
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(baseSqrtMSA.newAgePercentile90Series("90 percentile"));
			plot.addSeries(baseSqrtMSA.newAgePercentile60Series("60 percentile"));
			plot.addSeries(baseSqrtMSA.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 100, 10);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_baseSqrtMSA.tex", sampleX);
		}

		{
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(nnBSqrtMSA.newAgePercentile90Series("90 percentile"));
			plot.addSeries(nnBSqrtMSA.newAgePercentile60Series("60 percentile"));
			plot.addSeries(nnBSqrtMSA.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 100, 10);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_nnBSqrtMSA.tex", sampleX);
		}

		{
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(nullSqrtMSA.newAgePercentile90Series("90 percentile"));
			plot.addSeries(nullSqrtMSA.newAgePercentile60Series("60 percentile"));
			plot.addSeries(nullSqrtMSA.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 100, 10);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_nullSqrtMSA.tex", sampleX);
		}

		{
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(selfSqrtMSA.newAgePercentile90Series("90 percentile"));
			plot.addSeries(selfSqrtMSA.newAgePercentile60Series("60 percentile"));
			plot.addSeries(selfSqrtMSA.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 100, 10);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_selfSqrtMSA.tex", sampleX);
		}

		// (EFFECTIVE) REPLANNING RATES ==========================================

		{
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(baseMSA.newRealizedLambdaSeries("base MSA"));
			plot.addSeries(baseSqrtMSA.newRealizedLambdaSeries("base sqrt-MSA"));
			configureRealizedLambdaSeries(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("effectiveReplanning_msa-vs-sqrtMSA.tex", sampleX);
		}

		{
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(nnBSqrtMSA.newRealizedLambdaSeries("nnB sqrt-MSA"));
			plot.addSeries(nullSqrtMSA.newRealizedLambdaSeries("null sqrt-MSA"));
			plot.addSeries(selfSqrtMSA.newRealizedLambdaSeries("self sqrt-MSA"));
			configureRealizedLambdaSeries(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("effectiveReplanning_sqrtMSA-variations.tex", sampleX);
		}

		System.out.println("...DONE");
	}

}
