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
package stockholm.ihop2.integration;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.matsim.core.config.ConfigUtils;

class SummaryCreator {

	public SummaryCreator() {
	}

	static void run(final int maxIterations) {
		final String lastMATSimIteration = ConfigUtils
				.loadConfig("./input/matsim-config.xml").getModule("controler")
				.getValue("lastIteration");

		for (int iteration = 1; iteration <= maxIterations; iteration++) {
			final String fromPath = "./matsim-output." + iteration + "/";
			final String toPath = "./summary/iteration-" + iteration + "/";
			try {
				FileUtils.copyDirectory(new File(fromPath + "ITERS/it."
						+ lastMATSimIteration), new File(toPath + "it."
						+ lastMATSimIteration));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(
						"./departure-time-histograms." + iteration + ".txt"),
						new File("./summary/"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(
						"./travel-cost-statistics." + iteration + ".txt"),
						new File("./summary/"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(
						new File(fromPath + "logfile.log"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "logfileWarningsErrors.log"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "stopwatch.png"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "scorestats.png"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "stopwatch.txt"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				FileUtils.copyFileToDirectory(new File(fromPath
						+ "scorestats.txt"), new File(toPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			FileUtils.copyFileToDirectory(new File("./log.txt"), new File(
					"./summary/"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			FileUtils.copyFileToDirectory(new File("./config.xml"), new File(
					"./summary/"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			FileUtils.copyFileToDirectory(
					new File("./input/matsim-config.xml"), new File(
							"./summary/"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		run(1);
	}

}
