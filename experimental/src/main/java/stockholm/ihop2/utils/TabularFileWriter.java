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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TODO Clean this up and move it to utilities.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TabularFileWriter {

	// -------------------- CONFIGURATION --------------------

	private String separator = ",";

	private String noDataValue = "";

	// -------------------- RUNTIME PARAMETERS --------------------

	private final Map<String, String> key2value = new LinkedHashMap<String, String>();

	private PrintWriter writer = null;

	// -------------------- CONSTRUCTION AND CONFIGURATION --------------------

	public TabularFileWriter() {
	}

	public void setSeparator(final String separator) {
		this.separator = separator;
	}

	public void setNoDataValue(final String noDataValue) {
		this.noDataValue = noDataValue;
	}

	public void addKey(final String key) {
		if (this.writer != null) {
			throw new RuntimeException(
					"Cannot add keys when writing has already started.");
		}
		this.key2value.put(key, null);
	}

	public void addKeys(final String... keys) {
		for (String key : keys) {
			this.addKey(key);
		}
	}

	// -------------------- WRITING --------------------

	public void open(final String fileName) throws FileNotFoundException {
		if (this.writer != null) {
			throw new RuntimeException("Writer is already open.");
		}
		this.writer = new PrintWriter(fileName);
		for (Iterator<String> it = this.key2value.keySet().iterator(); it
				.hasNext();) {
			this.writer.print(it.next());
			if (it.hasNext()) {
				this.writer.print(this.separator);
			}
		}
		this.writer.println();
	}

	public void setValue(final String key, final String value) {
		this.key2value.put(key, value);
	}

	public void setValue(final String key, final double value) {
		this.setValue(key, Double.toString(value));
	}

	public void setValue(final String key, final int value) {
		this.setValue(key, Integer.toString(value));
	}

	public void setValue(final String key, final Object value) {
		this.setValue(key, value.toString());
	}

	private String formattedValue(final String key) {
		final String candidateResult = this.key2value.get(key);
		return candidateResult != null ? candidateResult : this.noDataValue;
	}

	private void clearValues() {
		for (Map.Entry<String, String> key2valueEntry : this.key2value
				.entrySet()) {
			key2valueEntry.setValue(null);
		}
	}

	public void writeValues() {
		for (Iterator<String> it = this.key2value.values().iterator(); it
				.hasNext();) {
			final String val = it.next();
			this.writer.print(val);
			// System.out.print(val);
			if (it.hasNext()) {
				this.writer.print(this.separator);
				// System.out.print(this.separator);
			}
		}
		this.writer.println();
		// System.out.println();
		this.clearValues();
	}

	public void writeLine(final String line) {
		this.writer.println(line);
	}

	public void close() {
		this.writer.flush();
		this.writer.close();
		this.writer = null;
		this.clearValues();
	}
}
