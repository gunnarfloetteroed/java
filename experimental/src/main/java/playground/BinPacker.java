/*
 * Copyright 2021 Gunnar Flötteröd
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
package playground;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class BinPacker<B> {

	public class Entry {
		final double value;
		final Iterable<B> bins;

		public Entry(double value, Iterable<B> bins) {
			this.value = value;
			this.bins = bins;
		}
	}

	private List<Entry> entries = new ArrayList<>();

	public void add(double value, Iterable<B> bins) {
		this.entries.add(new Entry(value, bins));
	}

	public void run(int capacities) {

		Collections.sort(this.entries, new Comparator<Entry>() {
			@Override
			public int compare(Entry o1, Entry o2) {
				return Double.compare(o2.value, o1.value);
			}
		});

		final Map<B, Integer> binToRemainingCapacity = new LinkedHashMap<>();
		for (Entry entry : this.entries) {
			entry.bins.forEach(b -> binToRemainingCapacity.put(b, capacities));
		}

		double sum = 0;
		
		for (Entry entry : this.entries) {
			boolean accept = true;
			for (B bin : entry.bins) {
				accept &= (binToRemainingCapacity.get(bin) >= 1);
				if (!accept) {
					break;
				}
			}

			if (accept) {
				entry.bins.forEach(b -> binToRemainingCapacity.put(b, binToRemainingCapacity.get(b) - 1));
				sum += entry.value;
			}
		}
		System.out.println(sum);
	}

	public static void main(String[] args) {
		BinPacker<Integer> gp = new BinPacker<>();
		gp.add(1.0, Arrays.asList(0, 1, 0));
		gp.add(2.0, Arrays.asList(1, 0, 0));
		gp.add(3.0, Arrays.asList(1, 0, 0));
		gp.run(1);
	}

}
