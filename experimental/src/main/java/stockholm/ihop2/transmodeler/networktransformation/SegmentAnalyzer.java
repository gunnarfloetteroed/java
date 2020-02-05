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
package stockholm.ihop2.transmodeler.networktransformation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SegmentAnalyzer {

	private final Map<String, Set<String>> relevantSegment2descriptions = new LinkedHashMap<>();

	private final Map<String, Set<String>> segment2directedSegments = new LinkedHashMap<>();

	private final Map<String, String> directedSegment2directedLink = new LinkedHashMap<>();

	public SegmentAnalyzer() {
	}

	private void addToValueSet(final String key, final String value, final Map<String, Set<String>> map) {
		Set<String> values = map.get(key);
		if (values == null) {
			values = new LinkedHashSet<>();
			map.put(key, values);
		}
		values.add(value);
	}

	public void addRelevantSegment(final String relevantSegment, final String description) {
		this.addToValueSet(relevantSegment, description, this.relevantSegment2descriptions);
	}

	public void add(final String segment, final String directedSegment, final String directedLink) {
		if (this.relevantSegment2descriptions.containsKey(segment)) {
			this.addToValueSet(segment, directedSegment, this.segment2directedSegments);
			this.directedSegment2directedLink.put(directedSegment, directedLink);
		}
	}

	public String getSummary(final Network network) {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<String, Set<String>> entry : this.relevantSegment2descriptions.entrySet()) {
			final String segment = entry.getKey();
			result.append(segment + "\n");
			for (String description : entry.getValue()) {
				result.append("  " + description + "\n");
			}
			if (this.segment2directedSegments.containsKey(segment)) {
				for (String directedSegment : this.segment2directedSegments.get(segment)) {
					result.append("    " + directedSegment + "  -->  ");
					if (!network.getLinks()
							.containsKey(Id.createLinkId(this.directedSegment2directedLink.get(directedSegment)))) {
						result.append("REMOVED: ");
					}
					result.append(this.directedSegment2directedLink.get(directedSegment) + "\n");

				}
			}
			result.append("\n");
		}
		return result.toString();
	}

}
