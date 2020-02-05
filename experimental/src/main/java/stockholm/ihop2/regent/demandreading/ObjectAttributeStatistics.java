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

import java.util.List;

import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class ObjectAttributeStatistics extends AbstractDemandStatistic {

	public ObjectAttributeStatistics(final ObjectAttributes objectAttributes) {
		this.parse(objectAttributes);
	}

	public ObjectAttributeStatistics(final String demandFile) {
		final ObjectAttributes objectAttributes = new ObjectAttributes();
		(new ObjectAttributesXmlReader(objectAttributes)).readFile(demandFile);
		this.parse(objectAttributes);
	}

	private void parse(final ObjectAttributes personAttributes) {
		final List<String> allAttributes = ObjectAttributeUtils2.allAttributeKeys(personAttributes);
		for (String personId : ObjectAttributeUtils2.allObjectKeys(personAttributes)) {
			this.allPersonIds.add(personId);
			for (String attribute : allAttributes) {
				this.addAttribute(attribute, personAttributes.getAttribute(personId, attribute));
			}
			this.addTripMode(personAttributes.getAttribute(personId, "worktourmode").toString());
			this.addTripMode(personAttributes.getAttribute(personId, "worktourmode").toString());
			this.addTripMode(personAttributes.getAttribute(personId, "othertourmode").toString());
			this.addTripMode(personAttributes.getAttribute(personId, "othertourmode").toString());
		}
	}

	public static void main(String[] args) {
		ObjectAttributeStatistics stats = new ObjectAttributeStatistics(
				"/Users/GunnarF/NoBackup/data-workspace/ihop2/ihop2-data/demand-input/trips.xml");
		stats.printSummaryStatistic();
	}

}
