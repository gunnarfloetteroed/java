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

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationAnalyzer {

	static void save(final JFreeChart chart, final String fileName) {
		try {
			ChartUtilities.saveChartAsPNG(new File(fileName + ".png"), chart, 1000, 600);

			// SVGGraphics2D g2 = new SVGGraphics2D(500, 300);
			// Rectangle r = new Rectangle(0, 0, 500, 300);
			// chart.draw(g2, r);
			// SVGUtils.writeToSVG(new File(fileName + ".svg"), g2.getSVGElement());

			// Transcoder transcoder = new PDFTranscoder();
			// TranscoderInput transcoderInput = new TranscoderInput(new FileInputStream(new
			// File(fileName + ".svg")));
			// TranscoderOutput transcoderOutput = new TranscoderOutput(new
			// FileOutputStream(new File(fileName + ".pdf")));
			// transcoder.transcode(transcoderInput, transcoderOutput);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static void configureUtilityPlot(final AccelerationAnalysisIntervalPlot plot) {
		plot.setLog(true);
		plot.setRange(0, 1000, -5, -2);
		plot.setGrid(200, 0.5);
		plot.addRelativeLegend(0.5, 0.95, 0.1, 0.1);
		plot.setXTick(200);
		plot.setYTick(0.5);
	}

	static void configureUtilityGapPlot(final AccelerationAnalysisIntervalPlot plot) {
		plot.setLog(true);
		plot.setRange(0, 1000, 1, 5);
		plot.setGrid(200, 0.5);
		plot.addRelativeLegend(0.5, 0.95, 0.1, 0.1);
		plot.setXTick(200);
		plot.setYTick(0.5);
	}

	static void configureAgePlot(final AccelerationAnalysisIntervalPlot plot, final double maxAge,
			final double ageTick) {
		plot.setLog(false);
		plot.setRange(0, 1000, 0, maxAge);
		plot.setGrid(200, ageTick);
		plot.addRelativeLegend(0.05, 0.95, 0.1, 0.1);
		plot.setXTick(200);
		plot.setYTick(ageTick);
	}

	static void configureAgeCorrelationPlot(final AccelerationAnalysisIntervalPlot plot) {
		plot.setLog(false);
		plot.setRange(0, 1000, 0, 1.0);
		plot.setGrid(200, 0.2);
		plot.addRelativeLegend(0.05, 0.95, 0.1, 0.1);
		plot.setXTick(200);
		plot.setYTick(0.2);
	}

	static void configureRealizedLambdaSeries(final AccelerationAnalysisIntervalPlot plot) {
		plot.setLog(false);
		plot.setRange(0, 1000, 0, 0.5);
		plot.setGrid(200, 0.1);
		plot.addRelativeLegend(0.5, 0.95, 0.1, 0.1);
		plot.setXTick(200);
		plot.setYTick(0.1);
	}

	public static void main(String[] args) {
		System.out.println("STARTED...");

		// final AccelerationExperimentData acceptNegativeDisappointment = new
		// AccelerationExperimentData(
		// "/Users/GunnarF/NoBackup/data-workspace/searchacceleration/greedo_acceptNegativeDissapointment",
		// 0, 10,
		// 1000);

		final int scenarioCnt = 10;

		// final double minUtlGap = 1;
		// final double maxUtlGap = 5;

		// final double minUtl = -5;
		// final double maxUtl = -2;

		final int sampleX = 10;

		final double deltaIt = 200;
		// final double deltaUtl = 0.5;
		// final double deltaUtlGap = 0.5;

		// final boolean log = true;
		// final boolean legend = true;

		final AccelerationExperimentData adaptiveMSA = new AccelerationExperimentData("./adaptive_MSA", 0, scenarioCnt,
				1000);
		final AccelerationExperimentData adaptiveMSA_congested = new AccelerationExperimentData(
				"./adaptive_MSA_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData greedo_noAgeing_msa_enforceLambda = new AccelerationExperimentData(
				"./greedo_no-ageing_msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_msa_enforceLambda_congested = new AccelerationExperimentData(
				"./greedo_no-ageing_msa_enforce-lambda_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData greedo_noAgeing_msa_freeLambda = new AccelerationExperimentData(
				"./greedo_no-ageing_msa_free-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_msa_freeLambda_congested = new AccelerationExperimentData(
				"./greedo_no-ageing_msa_free-lambda_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_enforceLambda = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_enforceLambda_congested = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_enforce-lambda_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_freeLambda = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_free-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_noAgeing_sqrtMsa_freeLambda_congested = new AccelerationExperimentData(
				"./greedo_no-ageing_sqrt-msa_free-lambda_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData greedo_withAgeing_msa_enforceLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_msa_enforceLambda_congested = new AccelerationExperimentData(
				"./greedo_with-ageing_msa_enforce-lambda_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData greedo_withAgeing_msa_freeLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_msa_free-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_msa_freeLambda_congested = new AccelerationExperimentData(
				"./greedo_with-ageing_msa_free-lambda_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_enforceLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_enforce-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_enforceLambda_congested = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_enforce-lambda_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_freeLambda = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_free-lambda", 0, scenarioCnt, 1000);
		final AccelerationExperimentData greedo_withAgeing_sqrtMsa_freeLambda_congested = new AccelerationExperimentData(
				"./greedo_with-ageing_sqrt-msa_free-lambda_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData sbayti2007MSA = new AccelerationExperimentData("./Sbayti2007_MSA", 0,
				scenarioCnt, 1000);
		final AccelerationExperimentData sbayti2007MSA_congested = new AccelerationExperimentData(
				"./Sbayti2007_MSA_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData sbayti2007SqrtMSA = new AccelerationExperimentData("./Sbayti2007_sqrt_MSA", 0,
				scenarioCnt, 1000);
		final AccelerationExperimentData sbayti2007SqrtMSA_congested = new AccelerationExperimentData(
				"./Sbayti2007_sqrt_MSA_congested", 0, scenarioCnt, 1000);

		final AccelerationExperimentData sqrtMSA = new AccelerationExperimentData("./sqrt_MSA", 0, scenarioCnt, 1000);
		final AccelerationExperimentData sqrtMSA_congested = new AccelerationExperimentData("./sqrt_MSA_congested", 0,
				scenarioCnt, 1000);

		final AccelerationExperimentData vanillaMSA = new AccelerationExperimentData("./vanilla_MSA", 0, scenarioCnt,
				1000);
		final AccelerationExperimentData vanillaMSA_congested = new AccelerationExperimentData(
				"./vanilla_MSA_congested", 0, scenarioCnt, 1000);

		// {
		// final PairwiseTTest tTests = new PairwiseTTest();
		// tTests.addData(greedo_noAgeing_sqrtMsa_enforceLambda.newExpectedUtilityChangesFinalPoints(),
		// "proposed, no aging, sqrtMSA");
		// tTests.addData(greedo_withAgeing_sqrtMsa_enforceLambda.newExpectedUtilityChangesFinalPoints(),
		// "proposed, with aging, sqrtMSA");
		//
		// tTests.addData(sbayti2007MSA.newExpectedUtilityChangesFinalPoints(), "Sbayti
		// (2007), MSA");
		// tTests.addData(sbayti2007SqrtMSA.newExpectedUtilityChangesFinalPoints(),
		// "Sbayti (2007), sqrt-MSA");
		//
		// tTests.addData(adaptiveMSA.newExpectedUtilityChangesFinalPoints(), "adaptive
		// MSA");
		// tTests.addData(sqrtMSA.newExpectedUtilityChangesFinalPoints(), "sqrt-MSA");
		// tTests.addData(vanillaMSA.newExpectedUtilityChangesFinalPoints(), "vanilla
		// MSA");
		//
		// try {
		// PrintWriter writer = new PrintWriter("tTests.txt");
		// writer.print(tTests.toString());
		// writer.flush();
		// writer.close();
		// } catch (FileNotFoundException e) {
		// throw new RuntimeException(e);
		// }
		// }

		// {
		// final PairwiseTTest tTestsCongested = new PairwiseTTest();
		// tTestsCongested.addData(
		// greedo_noAgeing_sqrtMsa_enforceLambda_congested.newExpectedUtilityChangesFinalPoints(),
		// "proposed, no aging, sqrtMSA, congested");
		// tTestsCongested.addData(
		// greedo_withAgeing_sqrtMsa_enforceLambda_congested.newExpectedUtilityChangesFinalPoints(),
		// "proposed, with aging, sqrt, congested");
		// tTestsCongested.addData(sbayti2007SqrtMSA_congested.newExpectedUtilityChangesFinalPoints(),
		// "Sbayti (2007), MSA, congested");
		//
		// try {
		// PrintWriter writer = new PrintWriter("tTests_congested.txt");
		// writer.print(tTestsCongested.toString());
		// writer.flush();
		// writer.close();
		// } catch (FileNotFoundException e) {
		// throw new RuntimeException(e);
		// }
		// }

		// UTILITIES STARTING HERE ==========================================

		{ // REALIZED UTILITY, MSA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(vanillaMSA.newRealizedUtilitiesSeries("basic MSA"));
			plot.addSeries(sqrtMSA.newRealizedUtilitiesSeries("sqrt-MSA"));
			plot.addSeries(adaptiveMSA.newRealizedUtilitiesSeries("adaptive MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_msa.tex", sampleX);
		}

		{ // REALIZED UTILITY, MSA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(vanillaMSA_congested.newRealizedUtilitiesSeries("basic MSA"));
			plot.addSeries(sqrtMSA_congested.newRealizedUtilitiesSeries("sqrt-MSA"));
			plot.addSeries(adaptiveMSA_congested.newRealizedUtilitiesSeries("adaptive MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_msa_congested.tex", sampleX);
		}

		{ // REALIZED UTILITY, SBAYTI
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(sbayti2007MSA.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(sbayti2007SqrtMSA.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_sbayti.tex", sampleX);
		}

		{ // REALIZED UTILITY, SBAYTI, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(sbayti2007MSA_congested.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(sbayti2007SqrtMSA_congested.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_sbayti_congested.tex", sampleX);
		}

		{ // REALIZED UTILITY, PROPOSED, WITH aging, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_freeLambda.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_greedo_withAgeing_freeLambda.tex", sampleX);
		}

		{ // REALIZED UTILITY, PROPOSED, WITH aging, FREE LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_freeLambda_congested.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda_congested.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_greedo_withAgeing_freeLambda_congested.tex", sampleX);
		}

		{ // REALIZED UTILITY, PROPOSED, WITH aging, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_enforceLambda.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_greedo_withAgeing_enforcedLambda.tex", sampleX);
		}

		{ // REALIZED UTILITY, PROPOSED, WITH aging, ENFORCED LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_enforceLambda_congested.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(
					greedo_withAgeing_sqrtMsa_enforceLambda_congested.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_greedo_withAgeing_enforcedLambda_congested.tex", sampleX);
		}

		{ // REALIZED UTILITY, PROPOSED, NO aging, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_freeLambda.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_greedo_noAgeing_freeLambda.tex", sampleX);
		}

		{ // REALIZED UTILITY, PROPOSED, NO aging, FREE LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_freeLambda_congested.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda_congested.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_greedo_noAgeing_freeLambda_congested.tex", sampleX);
		}

		{ // REALIZED UTILITY, PROPOSED, NO aging, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_enforceLambda.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_greedo_noAgeing_enforcedLambda.tex", sampleX);
		}

		{ // REALIZED UTILITY, PROPOSED, NO aging, ENFORCED LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_enforceLambda_congested.newRealizedUtilitiesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_congested.newRealizedUtilitiesSeries("with sqrt-MSA"));
			configureUtilityPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtl, maxUtl);
			// plot.setGrid(deltaIt, deltaUtl);
			// if (legend) {
			// plot.addLegend(500, -2.1, 100, 0.3);
			// }
			// } else {
			// }
			plot.render("realized_utilities_greedo_noAgeing_enforcedLambda_congested.tex", sampleX);
		}

		// UTILITY GAPS STARTING HERE ==========================================

		{ // MSA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(vanillaMSA.newExpectedUtilityChangesSeries("basic MSA"));
			plot.addSeries(sqrtMSA.newExpectedUtilityChangesSeries("sqrt-MSA"));
			plot.addSeries(adaptiveMSA.newExpectedUtilityChangesSeries("adaptive MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_msa.tex", sampleX);
		}

		{ // MSA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(vanillaMSA_congested.newExpectedUtilityChangesSeries("basic MSA"));
			plot.addSeries(sqrtMSA_congested.newExpectedUtilityChangesSeries("sqrt-MSA"));
			plot.addSeries(adaptiveMSA_congested.newExpectedUtilityChangesSeries("adaptive MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_msa_congested.tex", sampleX);
		}

		{ // SBAYTI
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(sbayti2007MSA.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(sbayti2007SqrtMSA.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_sbayti.tex", sampleX);
		}

		{ // SBAYTI, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(sbayti2007MSA_congested.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(sbayti2007SqrtMSA_congested.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_sbayti_congested.tex", sampleX);
		}

		{ // PROPOSED, WITH aging, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_freeLambda.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_greedo_withAgeing_freeLambda.tex", sampleX);
		}

		{ // PROPOSED, WITH aging, FREE LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_freeLambda_congested.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(
					greedo_withAgeing_sqrtMsa_freeLambda_congested.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_greedo_withAgeing_freeLambda_congested.tex", sampleX);
		}

		{ // PROPOSED, WITH aging, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_enforceLambda.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_greedo_withAgeing_enforcedLambda.tex", sampleX);
		}

		{ // PROPOSED, WITH aging, ENFORCED LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_withAgeing_msa_enforceLambda_congested.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(
					greedo_withAgeing_sqrtMsa_enforceLambda_congested.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_greedo_withAgeing_enforcedLambda_congested.tex", sampleX);
		}

		{ // PROPOSED, NO aging, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_freeLambda.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_greedo_noAgeing_freeLambda.tex", sampleX);
		}

		{ // PROPOSED, NO aging, FREE LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_freeLambda_congested.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(
					greedo_noAgeing_sqrtMsa_freeLambda_congested.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_greedo_noAgeing_freeLambda_congested.tex", sampleX);
		}

		{ // PROPOSED, NO aging, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_enforceLambda.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_greedo_noAgeing_enforcedLambda.tex", sampleX);
		}

		{ // PROPOSED, NO aging, ENFORCED LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(log);
			plot.addSeries(greedo_noAgeing_msa_enforceLambda_congested.newExpectedUtilityChangesSeries("with MSA"));
			plot.addSeries(
					greedo_noAgeing_sqrtMsa_enforceLambda_congested.newExpectedUtilityChangesSeries("with sqrt-MSA"));
			configureUtilityGapPlot(plot);
			// if (log) {
			// plot.setRange(0, 1000, minUtlGap, maxUtlGap);
			// plot.setGrid(deltaIt, deltaUtlGap);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			// } else {
			// }
			plot.render("utility_gaps_greedo_noAgeing_enforcedLambda_congested.tex", sampleX);
		}

		// AGE PERCENTILES ==================================================

		{ // AGE PERCENTILES, SBAYTI, SQRTMSA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(sbayti2007SqrtMSA.newAgePercentile90Series("90 percentile"));
			plot.addSeries(sbayti2007SqrtMSA.newAgePercentile60Series("60 percentile"));
			plot.addSeries(sbayti2007SqrtMSA.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 1000, 200);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_sbayti_sqrtMSA.tex", sampleX);
		}

		{ // AGE PERCENTILES, SBAYTI, SQRTMSA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(sbayti2007SqrtMSA_congested.newAgePercentile90Series("90 percentile"));
			plot.addSeries(sbayti2007SqrtMSA_congested.newAgePercentile60Series("60 percentile"));
			plot.addSeries(sbayti2007SqrtMSA_congested.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 1000, 200);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_sbayti_sqrtMSA_congested.tex", sampleX);
		}

		{ // AGE PERCENTILES, GREEDO, NO aging, sqrtMSA, freeLambda
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda.newAgePercentile90Series("90 percentile"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda.newAgePercentile60Series("60 percentile"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 1000, 200);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_greedo_noAgeing_sqrtMSA_freeLambda.tex", sampleX);
		}

		{ // AGE PERCENTILES, GREEDO, NO aging, sqrtMSA, enforcedLambda, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda_congested.newAgePercentile90Series("90 percentile"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda_congested.newAgePercentile60Series("60 percentile"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda_congested.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 1000, 200);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_greedo_noAgeing_sqrtMSA_freeLambda_congested.tex", sampleX);
		}

		{ // AGE PERCENTILES, GREEDO, WITH aging, sqrtMSA, freeLambda
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda.newAgePercentile90Series("90 percentile"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda.newAgePercentile60Series("60 percentile"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 20, 4);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_greedo_withAgeing_sqrtMSA_freeLambda.tex", sampleX);
		}

		{ // AGE PERCENTILES, GREEDO, WITH aging, sqrtMSA, enforcedLambda, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda_congested.newAgePercentile90Series("90 percentile"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda_congested.newAgePercentile60Series("60 percentile"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda_congested.newAgePercentile30Series("30 percentile"));
			configureAgePlot(plot, 20, 4);
			// plot.addLegend(100, 1000 - 100.0 / 3, 100, 100);
			// plot.setRange(0, 1000, 0, 1000);
			// plot.setGrid(deltaIt, deltaIt);
			plot.render("agePercentiles_greedo_withAgeing_sqrtMSA_freeLambda_congested.tex", sampleX);
		}

		// AGE CORRELATIONS STARTING HERE ==========================================

		{ // PROPOSED, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda.newAgeCorrelationSeries("with aging"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda.newAgeCorrelationSeries("no aging"));
			configureAgeCorrelationPlot(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("ageCorrelation_greedo_sqrtMsa_freeLambda.tex", sampleX);
		}

		{ // PROPOSED, FREE LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda_congested.newAgeCorrelationSeries("with aging"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda_congested.newAgeCorrelationSeries("no aging"));
			configureAgeCorrelationPlot(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("ageCorrelation_greedo_sqrtMsa_freeLambda_congested.tex", sampleX);
		}

		{ // PROPOSED, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda.newAgeCorrelationSeries("with aging"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda.newAgeCorrelationSeries("no aging"));
			configureAgeCorrelationPlot(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("ageCorrelation_greedo_sqrtMsa_enforcedLambda.tex", sampleX);
		}

		{ // PROPOSED, ENFORCED LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_congested.newAgeCorrelationSeries("with aging"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_congested.newAgeCorrelationSeries("no aging"));
			configureAgeCorrelationPlot(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("ageCorrelation_greedo_sqrtMsa_enforcedLambda_congested.tex", sampleX);
		}

		// (EFFECTIVE) REPLANNING RATES ==========================================

		{ // PROPOSED, FREE LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda.newRealizedLambdaSeries("with aging"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda.newRealizedLambdaSeries("no aging"));
			configureRealizedLambdaSeries(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("effectiveReplanning_greedo_sqrtMsa_freeLambda.tex", sampleX);
		}

		{ // PROPOSED, FREE LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_freeLambda_congested.newRealizedLambdaSeries("with aging"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_freeLambda_congested.newRealizedLambdaSeries("no aging"));
			configureRealizedLambdaSeries(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("effectiveReplanning_greedo_sqrtMsa_freeLambda_congested.tex", sampleX);
		}

		{ // PROPOSED, ENFORCED LAMBDA
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda.newRealizedLambdaSeries("with aging"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda.newRealizedLambdaSeries("no aging"));
			configureRealizedLambdaSeries(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("effectiveReplanning_greedo_sqrtMsa_enforcedLambda.tex", sampleX);
		}

		{ // PROPOSED, ENFORCED LAMBDA, CONGESTED
			final AccelerationAnalysisIntervalPlot plot = new AccelerationAnalysisIntervalPlot();
			// plot.setLog(false);
			// plot.addSeries(greedo_withAgeing_msa_freeLambda.newAgeCorrelationSeries("with
			// MSA"));
			plot.addSeries(greedo_withAgeing_sqrtMsa_enforceLambda_congested.newRealizedLambdaSeries("with aging"));
			plot.addSeries(greedo_noAgeing_sqrtMsa_enforceLambda_congested.newRealizedLambdaSeries("no aging"));
			configureRealizedLambdaSeries(plot);
			// plot.setRange(0, 1000, 0, 1.0);
			// plot.setGrid(deltaIt, 0.2);
			// if (legend) {
			// plot.addLegend(500, 5 - 0.1 * 4.0 / 3.0, 100, 0.4);
			// }
			plot.render("effectiveReplanning_greedo_sqrtMsa_enforcedLambda_congested.tex", sampleX);
		}

		System.out.println("...DONE");
	}

}
