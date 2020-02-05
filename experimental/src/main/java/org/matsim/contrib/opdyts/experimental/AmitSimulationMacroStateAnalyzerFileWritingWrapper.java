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
package org.matsim.contrib.opdyts.experimental;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.contrib.opdyts.macrostate.SimulationMacroStateAnalyzer;
import org.matsim.core.utils.io.IOUtils;

import floetteroed.utilities.math.Vector;

/**
 * TODO NOT TESTED!
 *
 * @author amit
 * @author Gunnar Flötteröd
 *
 */
public class AmitSimulationMacroStateAnalyzerFileWritingWrapper implements SimulationMacroStateAnalyzer {

	private final SimulationMacroStateAnalyzer analyzer;

	final String filePrefix;
	
	private final int fileWritingIteration;
	
	private boolean fileHasBeenWritten = false;
	
	private int iteration = 0;

	public AmitSimulationMacroStateAnalyzerFileWritingWrapper(final SimulationMacroStateAnalyzer analyzer,
			final String filePrefix, final int fileWritingIteration) {
		this.analyzer = analyzer;
		this.filePrefix = filePrefix;
		this.fileWritingIteration = fileWritingIteration;
	}

	@Override
	public void clear() {
		this.analyzer.clear();
		this.fileHasBeenWritten = false;
	}

	@Override
	public Vector newStateVectorRepresentation() {
		final Vector result = this.analyzer.newStateVectorRepresentation();
		if (!this.fileHasBeenWritten) {
			if (this.iteration % this.fileWritingIteration == 0) {
				String outFile = filePrefix + this.analyzer.getClass().getSimpleName() + ".txt";
				writeData(result, outFile);
			}
			this.iteration++;
			this.fileHasBeenWritten = true;
		}
		return result;
	}

	void writeData(final Vector vector, final String outFile) {
		List<Double> vectorElements = new ArrayList<>(vector.asList());
		Collections.sort(vectorElements, Collections.reverseOrder());

		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			for (Double d : vectorElements) {
				writer.write(d + "\n");
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written/read. Reason : " + e);
		}
	}
}
