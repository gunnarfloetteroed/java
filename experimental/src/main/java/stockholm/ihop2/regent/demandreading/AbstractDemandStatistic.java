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
package stockholm.ihop2.regent.demandreading;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AbstractDemandStatistic {

	public final Set<String> allPersonIds = new TreeSet<>();

	public final Map<String, Set<Object>> attribute2values = new LinkedHashMap<>();

	public final Map<Object, Integer> attributeValue2cnt = new LinkedHashMap<>();
	
	public final Map<String, Integer> mode2cnt = new LinkedHashMap<>();

	protected void addAttribute(final String key, final Object value) {
		
		Set<Object> values = this.attribute2values.get(key);
		if (values == null) {
			values = new TreeSet<>();
			this.attribute2values.put(key, values);
		}
		values.add(value);
		
		Integer cnt = this.attributeValue2cnt.get(value);
		if (cnt == null) {
			this.attributeValue2cnt.put(value, 1);
		} else {
			this.attributeValue2cnt.put(value, cnt + 1);			
		}
		
	}

	protected void addTripMode(final String mode) {
		Integer cnt = this.mode2cnt.get(mode);
		if (cnt == null) {
			this.mode2cnt.put(mode, 1);
		} else {
			this.mode2cnt.put(mode, cnt + 1);
		}
	}

	public void printSummaryStatistic() {
		System.out.println();
		System.out.println("NUMBER OF DISTINCT PERSONS:");
		System.out.println();
		System.out.println(this.allPersonIds.size());
		System.out.println();
		System.out.println("ALL DISTINCT ATTRIBUTES:");
		System.out.println();
		System.out.println(this.attribute2values.keySet());
		System.out.println();
		System.out.println("ALL DISTINCT ATTRIBUTE VALUES:");
		System.out.println();
		for (Map.Entry<String, Set<Object>> entry : this.attribute2values.entrySet()) {
			System.out.print(entry.getKey() + ": ");
			if (entry.getValue().size() <= 100) {
				System.out.println(entry.getValue());
			} else {
				System.out.println("[" + entry.getValue().size() + " entries.]");
			}
		}
		System.out.println();
		System.out.println("ALL DISTINCT ATTRIBUTE VALUE FREQUENCIES:");
		System.out.println();
		for (Map.Entry<Object, Integer> entry : this.attributeValue2cnt.entrySet()) {
			System.out.println(entry);
		}
		System.out.println();
		System.out.println("ALL DISTINCT MODES:");
		System.out.println();
		for (Map.Entry<?, ?> entry : this.mode2cnt.entrySet()) {
			System.out.println(entry);
		}

	}
}
