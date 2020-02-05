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
package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MakeHeaders {

	private static final boolean test = false;

	private static List<String> readFile(final String dir, final String startKey) throws IOException {
		final List<String> result = new ArrayList<String>();
		final BufferedReader reader = new BufferedReader(new FileReader(dir));
		boolean foundStart = (startKey == null);
		String line;
		while ((line = reader.readLine()) != null) {
			foundStart = (foundStart || startKey == null || line.trim().startsWith(startKey));
			if (foundStart) {
				result.add(line);
			}
		}
		reader.close();
		return result;
	}

	private static void processDir(final String dirName, List<String> header) throws IOException {
		final File dir = new File(dirName);
		if (!dir.isDirectory()) {
			if (dir.getName().endsWith(".java")) {
				final List<String> croppedContent = readFile(dirName, "package");
				if (croppedContent.size() == 0) {
					throw new RuntimeException("skipped " + dir + " because of missing package keyword");
				} else {
					final PrintWriter writer;
					if (test) {
						writer = null;
					} else {
						writer = new PrintWriter(new File(dirName));
					}
					for (String line : header) {
						if (test) {
							System.out.println(line);
						} else {
							writer.println(line);
						}
					}
					for (String line : croppedContent) {
						if (test) {
							System.out.println(line);
						} else {
							writer.println(line);
						}
					}
					if (!test) {
						writer.flush();
						writer.close();
					}
				}
			}
		}
	}

	private static void traverse(final String dirName, List<String> header) throws IOException {
		processDir(dirName, header);
		final File dir = new File(dirName);
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				traverse(new File(dir, children[i]).getAbsolutePath(), header);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("STARTED");

		System.out.println("stockholm.saleem ...");
		traverse("/Nobackup/Profilen/git-2018/vsp-playgrounds/stockholm/src/main/java/saleem",
				readFile("./docs/class-header_saleem.txt", null));

		System.out.println("stockholm.gunnar ...");
		traverse("/Nobackup/Profilen/git-2018/vsp-playgrounds/stockholm/src/main/java/gunnar",
				readFile("./docs/class-header_gunnar.txt", null));

		System.out.println("gunnar (vsp-playground)...");
		traverse("/Nobackup/Profilen/git-2018/vsp-playgrounds/gunnar/src/main/java",
				readFile("./docs/class-header_gunnar.txt", null));

		// System.out.println("bioroute...");
		// traverse("/Nobackup/Profilen/git/public-java/src/floetteroed/bioroute",
		// readFile("./docs/class-header_bioroute.txt", null));
		//
		// System.out.println("cadyts...");
		// traverse("/Nobackup/Profilen/git/public-java/src/floetteroed/cadyts",
		// readFile("./docs/class-header_cadyts.txt", null));
		//
		// System.out.println("opdyts...");
		// traverse("/Nobackup/Profilen/git/public-java/src/floetteroed/opdyts",
		// readFile("./docs/class-header_opdyts.txt", null));
		//
		// System.out.println("utilities...");
		// traverse("/Nobackup/Profilen/git/public-java/src/floetteroed/utilities",
		// readFile("./docs/class-header_utilities.txt", null));

		System.out.println("DONE");
	}
}
