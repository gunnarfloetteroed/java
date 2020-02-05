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
package stockholm.ihop2.regent;

import java.io.IOException;

import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;

import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class EmmeTravelTimes implements TabularFileHandler {

	private final Matrices travelTimes = new Matrices();

	private Matrix currentlyReadMatrix = null;
	
	public EmmeTravelTimes() {
	}
	
	public void read(final String fileName) {
		this.currentlyReadMatrix = this.travelTimes.createMatrix(fileName, "");
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setCommentTags(new String[] { "c", "t", "a" });
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void write(final String fileName) {
		final MatricesWriter writer = new MatricesWriter(this.travelTimes);
		writer.write(fileName);
	}

	@Override
	public String preprocess(String line) {
		return line;
	}

	@Override
	public void startDocument() {
	}

	@Override
	public void startRow(String[] row) {
		final String fromZoneId = row[0];
		for (int i = 1; i < row.length; i++) {
			final String[] destData = row[i].split("\\Q" + ":" + "\\E");
			final String toZoneId = destData[0];
			final double value = Double.parseDouble(destData[1]);
			this.currentlyReadMatrix.createAndAddEntry(fromZoneId, toZoneId, value);
		}
	}

	@Override
	public void endDocument() {

	}

	public static void main(String[] args) {

		System.out.println("STARTED");
		
		final String work = "./test/referencedata/EMME_traveltimes_OTHER_mf9.txt";
		final EmmeTravelTimes emmeTT = new EmmeTravelTimes();
		emmeTT.read(work);
		emmeTT.write("./test/referencedata/EMME_traveltimes_OTHER_mf9.xml");

		System.out.println("DONE");
	}

}
