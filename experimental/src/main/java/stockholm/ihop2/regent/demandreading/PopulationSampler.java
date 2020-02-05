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

import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PopulationSampler {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String prefix = "./test/regentmatsim/exchange/trips";
		final String fromFile = prefix + ".xml";
		final double fraction = 0.01;
		final String toFile = prefix + "_" + +fraction + ".xml";

		final ObjectAttributes all = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				all);
		reader.readFile(fromFile);

		final ObjectAttributes subset = ObjectAttributeUtils2
				.newFractionalSubset(all, fraction);
		System.out.println("size of all is "
				+ ObjectAttributeUtils2.allObjectKeys(all).size());
		System.out.println("size of subset is "
				+ ObjectAttributeUtils2.allObjectKeys(subset).size());

		final ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(
				subset);
		writer.writeFile(toFile);

		System.out.println("... DONE");
	}
}
