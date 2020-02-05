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
package stockholm.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FirstNLines {

	public FirstNLines() {
	}

	public static void main(String[] args) throws IOException {

		final String file = "./test/regentmatsim/exchange/trips2.xml";
//		final String to = "./test/regentmatsim/exchange/trips2.xml";
		final int maxLines = 100; // Integer.MAX_VALUE;
		int lines = 0;

		final BufferedReader reader = new BufferedReader(new FileReader(file));
//		final PrintWriter writer = new PrintWriter(to);
		
		String line;
		int objects = 0;
		while ((line = reader.readLine()) != null && (lines++) < maxLines) {
			System.out.println(line);
//			if (objects % 1000 == 0) {
//				System.out.println(objects);
//			}
//			if (line.startsWith("  <object id=")) {
//				writer.println("  <object id=\"" + (objects++) + "\">");
//			} else {
//				writer.println(line);
//			}
		}

//		writer.flush();
//		writer.close();
		
		reader.close();
	}
}
