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
package stockholm.ihop2.utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;

import floetteroed.utilities.FractionalIterable;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CompareTTMatrices {

	public CompareTTMatrices() {
	}

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final String type = "OTHER";
		final Matrix xMatrix;
		{
			final Matrices m1 = new Matrices();
			final MatsimMatricesReader r1 = new MatsimMatricesReader(m1, null);
			r1.readFile("./test/matsim-testrun/tourtts.xml");
			xMatrix = m1.getMatrix(type);
			// r1.readFile("./test/matsim-testrun/traveltimes.xml");
			// xMatrix = m1.getMatrix("TT_07:30:00");
			// MatrixUtils.mult(xMatrix, 2.0); // we want tours, not trips
		}

		final Matrix yMatrix;
		{
			final Matrices m1 = new Matrices();
			final MatsimMatricesReader r1 = new MatsimMatricesReader(m1, null);
			r1.readFile("./test/matsim-testrun/WRONG/tourtts.xml");
			yMatrix = m1.getMatrix(type);			
//			final Matrices m1 = new Matrices();
//			final MatsimMatricesReader r1 = new MatsimMatricesReader(m1, null);
//			r1.readFile("./test/referencedata/EMME_traveltimes_" + type
//					+ "_mf8.xml");
//			// r1.readFile("./test/referencedata/EMME_traveltimes_" + type +
//			// "_mf9.xml");
//			// r1.readFile("./test/matsim-testrun/freeflow-traveltimes.xml");
//			yMatrix = m1.getMatrices().values().iterator().next();
//			// MatrixUtils.mult(yMatrix, 2.0); // we want tours, not trips
		}

		final double frac = 0.1;
		// final PrintWriter writer = new
		// PrintWriter("./test/matsim-testrun/matsim-vs-emme_WORK_0-01.txt");
		final PrintWriter writer = new PrintWriter(
				"./test/matsim-testrun/right-vs-wrong_" + type + ".txt");

		for (List<Entry> row1 : new FractionalIterable<ArrayList<Entry>>(
				xMatrix.getFromLocations().values(), Math.sqrt(frac))) {
			for (Entry entry1 : new FractionalIterable<Entry>(row1,
					Math.sqrt(frac))) {
				final Entry entry2 = yMatrix.getEntry(entry1.getFromLocation(),
						entry1.getToLocation());
				if (entry2 != null) {
					writer.println(entry1.getValue() + "," + entry2.getValue());
				}
			}
		}
		writer.flush();
		writer.close();

		System.out.println("... DONE");
	}

}
