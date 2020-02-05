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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Geometry;
import org.matsim.core.gbl.MatsimRandom;

import floetteroed.utilities.math.MathHelpers;
import stockholm.patryk.Building;

/**
 * 
 * @author Gunnar Flötteröd, pased on Patryk Larek
 *
 */
public class Zone {

	// -------------------- CONSTANTS --------------------

	private static final String VILLA = "villa";

	private static final String APARTMENT = "apartment";

	// -------------------- MEMBERS --------------------

	private final Random rnd = MatsimRandom.getLocalInstance(); // TODO

	private final String zoneId;

	private Geometry geometry;

	// All buildings.

	private final List<Building> allBuildings = new ArrayList<>();

	// Home buildings.

	private final List<Building> villas = new ArrayList<>();

	private final Map<Building, Double> apartmentBuilding2size = new LinkedHashMap<>();

	private double apartmentBuildingSizeSum = 0;

	// Work buildings.

	private final Map<Building, Double> workBuilding2size = new LinkedHashMap<>();

	private double workBuildingSizeSum = 0;

	// -------------------- CONSTRUCTION --------------------

	public Zone(final String zoneId) {
		this.zoneId = zoneId;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void addBuilding(final Building building) {
		this.allBuildings.add(building);
		if (building.isVilla()) {
			this.villas.add(building);
		} else if (building.isApartmentBuilding()) {
			this.apartmentBuilding2size.put(building,
					(double) building.getBuildingSize());
			this.apartmentBuildingSizeSum += building.getBuildingSize();
		} else if (building.isWorkBuilding()) {
			this.workBuilding2size.put(building,
					(double) building.getBuildingSize());
			this.workBuildingSizeSum += building.getBuildingSize();
		} else {
			Logger.getLogger(this.getClass().getName()).warning(
					"Undefined building type. Building not added to zone.");
		}
	}

	public void setGeometry(final Geometry geometry) {
		this.geometry = geometry;
	}

	public String getId() {
		return zoneId;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public Geometry drawHomeGeometry(final String buildingType) {
		if (VILLA.equals(buildingType) && !this.villas.isEmpty()) {
			return this.villas.get(this.rnd.nextInt(this.villas.size()))
					.getGeometry();
		} else if (APARTMENT.equals(buildingType)
				&& !this.apartmentBuilding2size.isEmpty()) {
			return MathHelpers.draw(this.apartmentBuilding2size,
					this.apartmentBuildingSizeSum, this.rnd).getGeometry();
		} else {
//			Logger.getLogger(this.getClass().getName()).warning(
//					"Could not fine home building of type " + buildingType
//							+ " in zone " + this.zoneId
//							+ "; falling back to zone geometry.");
			return this.getGeometry();
		}
	}

	public Geometry drawWorkGeometry() {
		if (!this.workBuilding2size.isEmpty()) {
			return MathHelpers.draw(this.workBuilding2size,
					this.workBuildingSizeSum, this.rnd).getGeometry();
		} else {
//			Logger.getLogger(this.getClass().getName()).warning(
//					"Could not fine work building  in zone " + this.zoneId
//							+ "; falling back to zone geometry.");
			return this.getGeometry();
		}
	}

	public Geometry drawOtherGeometry() {
		if (!this.allBuildings.isEmpty()) {
			return this.allBuildings.get(
					this.rnd.nextInt(this.allBuildings.size())).getGeometry();
		} else {
//			Logger.getLogger(this.getClass().getName()).warning(
//					"Could not fine any building in zone " + this.zoneId
//							+ "; falling back to zone geometry.");
			return this.getGeometry();
		}
	}
}
