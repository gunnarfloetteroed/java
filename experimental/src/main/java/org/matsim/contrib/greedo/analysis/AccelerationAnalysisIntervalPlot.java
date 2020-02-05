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

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;

import umontreal.ssj.charts.XYLineChart;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationAnalysisIntervalPlot {

	private Double legendLeftX = null;
	private Double legendTopY = null;
	private Double legendLineLength = null;
	private Double legendRowDistance = null;

	private Double relLegendLeftX = null;
	private Double relLegendTopY = null;
	private Double relLegendLineLength = null;
	private Double relLegendRowDistance = null;

	private Double xTick = null;
	private Double yTick = null;

	private boolean log = false;

	public void addLegend(final double legendLeftX, final double legendTopY, final double legendLineLength,
			final double legendRowDistance) {
		this.legendLeftX = legendLeftX;
		this.legendTopY = legendTopY;
		this.legendLineLength = legendLineLength;
		this.legendRowDistance = legendRowDistance;
	}

	public void addRelativeLegend(final double relLegendLeftX, final double relLegendTopY,
			final double relLegendLineLength, final double relLegendRowDistance) {
		this.relLegendLeftX = relLegendLeftX;
		this.relLegendTopY = relLegendTopY;
		this.relLegendLineLength = relLegendLineLength;
		this.relLegendRowDistance = relLegendRowDistance;

	}

	public void setXTick(final double xTick) {
		this.xTick = xTick;
	}

	public void setYTick(final double yTick) {
		this.yTick = yTick;
	}

	public void setLog(final boolean log) {
		this.log = log;
	}

	private double logOrNot(final double val) {
		if (this.log) {
			return Math.signum(val) * Math.log(Math.abs(val));
		} else {
			return val;
		}
	}

	private double[] range = null;

	private Double xGrid = null;
	private Double yGrid = null;

	public void setRange(final double xmin, final double xmax, final double ymin, final double ymax) {
		this.range = new double[] { xmin, xmax, ymin, ymax };
	}

	public void setGrid(double xGrid, double yGrid) {
		this.xGrid = xGrid;
		this.yGrid = yGrid;
	}

	// private final List<Color> colors = Arrays.asList(Color.RED, Color.BLUE,
	// Color.GREEN, Color.YELLOW, Color.MAGENTA,
	// Color.CYAN, Color.CYAN, Color.CYAN, Color.CYAN, Color.CYAN, Color.CYAN,
	// Color.CYAN);

	private final List<String> dashPatterns = Arrays.asList("solid", "dashed", "dotted");
	// "densely dotted", "loosely dotted",
	// "densely dashed", "loosely dashed", "loosely dashed", "loosely dashed",
	// "loosely dashed",
	// "loosely dashed", "loosely dashed", "loosely dashed");

	private final List<YIntervalSeries> allSeries = new ArrayList<>();

	public AccelerationAnalysisIntervalPlot() {
	}

	public void addSeries(final YIntervalSeries series) {
		if (this.allSeries.size() == 3) {
			System.err.println("Cannot plot more than 3 series in one figure.");
		} else {
			this.allSeries.add(series);
		}
	}

	private String newDummy(List<String> allDummies) {
		String result = "DUMMY" + allDummies.size();
		allDummies.add(result);
		return result;
	}

	public void render(String fileName, final int deltaX) {

		List<String> dummyLabels = new LinkedList<>();

		final XYSeriesCollection data = new XYSeriesCollection();
		for (int seriesIndex = 0; seriesIndex < this.allSeries.size(); seriesIndex++) {
			YIntervalSeries intervalSeries = this.allSeries.get(seriesIndex);
			final XYSeries lowerLine = new XYSeries(newDummy(dummyLabels));
			// final XYSeries meanLine = new XYSeries("mean" + seriesIndex);
			final XYSeries upperLine = new XYSeries(newDummy(dummyLabels));
			for (int itemIndex = 0; itemIndex < intervalSeries.getItemCount(); itemIndex++) {
				if (intervalSeries.getX(itemIndex).intValue() % deltaX == 0) {
					final double yLow = this.logOrNot(intervalSeries.getYLowValue(itemIndex));
					final double y = this.logOrNot(intervalSeries.getYValue(itemIndex));
					final double yHigh = this.logOrNot(intervalSeries.getYHighValue(itemIndex));
					if (!Double.isNaN(yLow) && !Double.isNaN(y) && !Double.isNaN(yHigh)) {
						lowerLine.add(intervalSeries.getX(itemIndex).doubleValue(), yLow);
						// meanLine.add(intervalSeries.getX(itemIndex).doubleValue(), y);
						upperLine.add(intervalSeries.getX(itemIndex).doubleValue(), yHigh);
					}
				}
			}
			data.addSeries(lowerLine);
			// data.addSeries(meanLine);
			data.addSeries(upperLine);

		}

		XYLineChart chart = new XYLineChart(null, null, null, data);

		for (int seriesIndex = 0; seriesIndex < this.allSeries.size(); seriesIndex++) {

			chart.getSeriesCollection().setColor(seriesIndex * 2 + 0, Color.BLACK);
			chart.getSeriesCollection().setColor(seriesIndex * 2 + 1, Color.BLACK);

			chart.getSeriesCollection().setDashPattern(seriesIndex * 2 + 0, this.dashPatterns.get(seriesIndex));
			chart.getSeriesCollection().setDashPattern(seriesIndex * 2 + 1, this.dashPatterns.get(seriesIndex));

			chart.getSeriesCollection().setPlotStyle(seriesIndex * 2 + 0, "sharp plot");
			chart.getSeriesCollection().setPlotStyle(seriesIndex * 2 + 1, "sharp plot");
		}

		// override absolute by relative legend if available

		if ((this.range != null) && (this.relLegendLeftX != null)) {
			this.addLegend(this.range[0] + this.relLegendLeftX * (this.range[1] - this.range[0]),
					this.range[2] + this.relLegendTopY * (this.range[3] - this.range[2]),
					this.relLegendLineLength * (this.range[1] - this.range[0]),
					this.relLegendRowDistance * (this.range[3] - this.range[2]));
		}

		// override absolute by relative legend if available

		if (this.legendLeftX != null) {
			for (int seriesIndex = 0; seriesIndex < this.allSeries.size(); seriesIndex++) {
				final int lineIndex = chart.add(
						new double[] { this.legendLeftX, this.legendLeftX + this.legendLineLength },
						new double[] { this.legendTopY - seriesIndex * this.legendRowDistance,
								this.legendTopY - seriesIndex * this.legendRowDistance });
				chart.getSeriesCollection().setColor(lineIndex, Color.BLACK);
				chart.getSeriesCollection().setDashPattern(lineIndex, this.dashPatterns.get(seriesIndex));
				chart.getSeriesCollection().setName(lineIndex, this.allSeries.get(seriesIndex).getKey().toString());
			}
		}

		if (this.range != null) {
			chart.setManualRange(this.range);
		} else {
			chart.setAutoRange00(true, true);
		}

		if ((this.xGrid != null) && (this.yGrid != null)) {
			chart.enableGrid(this.xGrid, this.yGrid);
		}

		if (this.xTick != null) {
			chart.getXAxis().setLabels(this.xTick);
		}
		if (this.yTick != null) {
			chart.getYAxis().setLabels(this.yTick);
		}

		chart.setLatexDocFlag(false);

		String result = chart.toLatex(6, 4);
		for (String dummy : dummyLabels) {
			result = result.replace(dummy, "");
		}

		try {
			PrintWriter writer = new PrintWriter(fileName);
			writer.print(result);
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		// chart.toLatexFile(fileName, 6, 4);
		// chart.setLatexDocFlag(true);
		// System.out.println(chart.toLatex(12, 8));
	}

	public static void main(String[] args) {
		System.out.println("STARTED");

		AccelerationAnalysisIntervalPlot aap = new AccelerationAnalysisIntervalPlot();

		{
			YIntervalSeries series1 = new YIntervalSeries("series1");
			series1.add(0, 1, 1, 1);
			series1.add(1, 2, 1.5, 2.5);
			series1.add(2, 3, 2, 4);
			aap.addSeries(series1);
		}

		{
			YIntervalSeries series2 = new YIntervalSeries("series2");
			series2.add(0, -1, -1, -1);
			series2.add(1, -2, -2.5, -1.5);
			series2.add(2, -3, -4, -2);
			aap.addSeries(series2);
		}

		{
			YIntervalSeries series3 = new YIntervalSeries("series3");
			series3.add(0, 0, 0, 0);
			series3.add(1, 0, -0.5, 0.5);
			series3.add(2, 0, -1, 1);
			aap.addSeries(series3);
		}

		{
			YIntervalSeries series4 = new YIntervalSeries("series4");
			series4.add(0, -3, -3, -3);
			series4.add(1, 0, -0.5, 0.5);
			series4.add(2, 3, 2, 4);
			aap.addSeries(series4);
		}

		aap.render("NormalChart.tex", 1);

		System.out.println("DONE");
	}

}
