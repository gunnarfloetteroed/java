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
package stockholm.ihop2.regent.demandreading.experimental;

import static stockholm.utils.ShapeUtils.drawPointFromGeometry;

import java.util.logging.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.utils.objectattributes.ObjectAttributes;

import stockholm.ihop2.regent.RegentDictionary;
import stockholm.ihop2.regent.demandreading.ZonalSystem;
import stockholm.ihop2.regent.demandreading.Zone;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ActivityLocationSampler {

	// -------------------- MEMBERS --------------------

	private final ObjectAttributes personAttributes;

	private final ZonalSystem zonalSystem;

	private final CoordinateTransformation zonesToMATSimTrafo;

	private boolean zonesMustContainNodes; // initialized in the constructor

	// -------------------- CONSTRUCTION AND CONFIGURATION --------------------

	public ActivityLocationSampler(final ObjectAttributes personAttributes,
			final ZonalSystem zonalSystem,
			final CoordinateTransformation zonesToMATSimTrafo) {
		this.personAttributes = personAttributes;
		this.zonalSystem = zonalSystem;
		this.zonesToMATSimTrafo = zonesToMATSimTrafo;
		this.setZonesMustContainNodes(true);
	}

	public void setZonesMustContainNodes(final boolean zonesMustContainNodes) {
		this.zonesMustContainNodes = zonesMustContainNodes;
		if (this.zonesMustContainNodes) {
			Logger.getLogger(this.getClass().getName())
					.info("Zones that do not contain at least one network node are ignored.");
		} else {
			Logger.getLogger(this.getClass().getName())
					.info("All zones, even those that do not contain any network nodes, are considered.");
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public Coord drawHomeCoordinate(final String personId) {
		final Zone homeZone = this.zonalSystem
				.getZone((String) this.personAttributes.getAttribute(personId,
						RegentDictionary.REGENT_HOMEZONE_ATTRIBUTE));
		if (this.zonalSystem.getNodes(homeZone).isEmpty()) {
			Logger.getLogger(this.getClass().getName()).warning(
					"Person " + personId + "'s home-zone " + homeZone
							+ " contains no nodes.");
			if (this.zonesMustContainNodes) {
				return null;
			}
		}
		final String homeBuildingType = (String) this.personAttributes
				.getAttribute(personId, RegentDictionary.REGENT_HOUSINGTYPE_ATTRIBUTE);
		return this.zonesToMATSimTrafo.transform(drawPointFromGeometry(homeZone
				.drawHomeGeometry(homeBuildingType)));
	}

	public Coord drawWorkCoordinate(final String personId) {
		final Zone workZone = this.zonalSystem
				.getZone((String) this.personAttributes.getAttribute(personId,
						RegentDictionary.REGENT_WORKZONE_ATTRIBUTE));
		if (this.zonalSystem.getNodes(workZone).isEmpty()) {
			Logger.getLogger(this.getClass().getName()).warning(
					"Person " + personId + "'s work-zone " + workZone
							+ " contains no nodes.");
			if (this.zonesMustContainNodes) {
				return null;
			}
		}
		return this.zonesToMATSimTrafo.transform(drawPointFromGeometry(workZone
				.drawWorkGeometry()));
	}

	public Coord drawOtherCoordinate(final String personId) {
		final Zone otherZone = this.zonalSystem
				.getZone((String) this.personAttributes.getAttribute(personId,
						RegentDictionary.REGENT_OTHERZONE_ATTRIBUTE));
		if (this.zonalSystem.getNodes(otherZone).isEmpty()) {
			Logger.getLogger(this.getClass().getName()).warning(
					"Person " + personId + "'s other-zone " + otherZone
							+ " contains no nodes.");
			if (this.zonesMustContainNodes) {
				return null;
			}
		}
		return this.zonesToMATSimTrafo
				.transform(drawPointFromGeometry(otherZone.drawOtherGeometry()));
	}

}
