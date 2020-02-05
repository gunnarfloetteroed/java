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
package nonpropassignment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.Tuple;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TupleIndexer<T> {

	// -------------------- MEMBERS --------------------

	private final Map<Tuple<T, T>, Integer> tuple2index = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TupleIndexer() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void add(final Tuple<T, T> key) {
		if (!this.tuple2index.containsKey(key)) {
			this.tuple2index.put(key, this.tuple2index.size());
		}
	}

	public Map<Tuple<T, T>, Integer> getTuple2IndexView() {
		return Collections.unmodifiableMap(this.tuple2index);
	}

	public Integer getIndex(final Tuple<T, T> key) {
		return this.tuple2index.get(key);
	}

	public int size() {
		return this.tuple2index.size();
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<Tuple<T, T>, Integer> entry : this.tuple2index.entrySet()) {
			result.append(entry.getKey() + " <-> " + entry.getValue() + "\n");
		}
		return result.toString();
	}
}
