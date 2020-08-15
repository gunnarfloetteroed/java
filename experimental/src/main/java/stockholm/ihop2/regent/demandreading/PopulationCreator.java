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

import static org.matsim.utils.objectattributes.ObjectAttributeUtils2.allObjectKeys;
import static stockholm.saleem.StockholmTransformationFactory.WGS84_EPSG3857;
import static stockholm.saleem.StockholmTransformationFactory.WGS84_SWEREF99;
import static stockholm.saleem.StockholmTransformationFactory.getCoordinateTransformation;
import static stockholm.utils.ShapeUtils.drawPointFromGeometry;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.utils.objectattributes.ObjectAttributeUtils2;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import floetteroed.utilities.FractionalIterable;
import stockholm.ihop2.regent.RegentDictionary;
import stockholm.saleem.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd, based on Patryk Larek
 *
 */
public class PopulationCreator {

	// -------------------- CONSTANTS --------------------

	public static final String HOME = "home";
	public static final String WORK = "work";
	public static final String OTHER = "other";

	// TODO NEW 2018-12-06
	public static final String INTERMEDIATE_HOME = "intermediate_home";

	public static final String income_SEK_yr = "income_SEK_yr";

	public static final int REFERENCE_YEAR = 2015;

	// anything not completely nonsensical, just to get started
	final double workDuration_s = 8.0 * 3600.0;
	final double intermediateHomeDuration_s = 1.0 * 3600.0;
	final double otherDuration_s = 1.0 * 3600.0;
	final double tripDuration_s = 0.5 * 3600;

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final ZonalSystem zonalSystem;

	private double populationSampleFactor = 1.0;

	// 2020-08-14: changed while moving to MATSim 12
	private final ObjectAttributes personAttributes = new ObjectAttributes();

	// -------------------- CONSTRUCTION --------------------

	/*
	 * TODO When adding the link attributes, the zonal system is not affected,
	 * meaning that zones may keep pointers at nodes that have been removed. The
	 * difference is probably not so large but still this is inconsistent.
	 * 
	 * Better pass on the link attributes (file name) into this constructor and
	 * first reduce the network and only then add it to the zonal system.
	 */
	public PopulationCreator(final String networkFileName, final String zoneShapeFileName,
			final String zonalCoordinateSystem, final String populationFileName) {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new MatsimNetworkReader(this.scenario.getNetwork())).readFile(networkFileName);
		this.zonalSystem = new ZonalSystem(zoneShapeFileName, zonalCoordinateSystem);
		this.zonalSystem.addNetwork(this.scenario.getNetwork(), StockholmTransformationFactory.WGS84_SWEREF99);
		Logger.getLogger(this.getClass().getName())
				.info("number of zones in zonal system is " + this.zonalSystem.getId2zoneView().size());

		// 2020-08-14: changed while moving to MATSim 12
		// OLD new
		// ObjectAttributesXmlReader(this.scenario.getPopulation().getPersonAttributes()).readFile(populationFileName);
		new ObjectAttributesXmlReader(this.personAttributes).readFile(populationFileName);
	}

	public void setBuildingsFileName(final String buildingShapeFileName) {
		this.zonalSystem.addBuildings(buildingShapeFileName);
	}

	public void setPopulationSampleFactor(final double populationSampleFactor) {
		this.populationSampleFactor = populationSampleFactor;
	}

	public void removeExpandedLinks(final ObjectAttributes linkAttributes) {
		final Set<String> tmLinkIds = new LinkedHashSet<String>(ObjectAttributeUtils2.allObjectKeys(linkAttributes));
		final Set<Id<Link>> removeTheseLinkIds = new LinkedHashSet<Id<Link>>();
		for (Id<Link> candidateId : this.scenario.getNetwork().getLinks().keySet()) {
			if (!tmLinkIds.contains(candidateId.toString())) {
				removeTheseLinkIds.add(candidateId);
			}
		}
		Logger.getLogger(this.getClass().getName())
				.info("Excluding " + removeTheseLinkIds.size() + " expanded links from being activity locations.");
		for (Id<Link> linkId : removeTheseLinkIds) {
			this.scenario.getNetwork().removeLink(linkId);
		}
	}

	// -------------------- INTERNALS --------------------

	private void addHomeActivity(final Plan plan, final Coord homeCoord, final Double endTime_s, final String type) {
		final Activity home = this.scenario.getPopulation().getFactory().createActivityFromCoord(type, homeCoord);
		if (endTime_s != null) {
			home.setEndTime(endTime_s);
		}
		plan.addActivity(home);
	}

	private void addTour(final Plan plan, final String type, final Coord actCoord, final String mode,
			final Double endTime_s) {

		// leg to activity
		final Leg homeToAct = this.scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(homeToAct);

		// activity itself
		final Activity act = this.scenario.getPopulation().getFactory().createActivityFromCoord(type, actCoord);
		if (endTime_s != null) {
			act.setEndTime(endTime_s);
		}
		plan.addActivity(act);

		// leg back home
		final Leg actToHome = this.scenario.getPopulation().getFactory().createLeg(mode);
		plan.addLeg(actToHome);
	}

	private Object attr(final String who, final String what) {
		// 2020-08-14: changed while moving to MATSim 12
		// OLD return
		// this.scenario.getPopulation().getPersonAttributes().getAttribute(who, what);
		return this.personAttributes.getAttribute(who, what);
	}

	private Person newPerson(final String personId, final XY2Links xy2links,
			final CoordinateTransformation coordinateTransform) {

		/*
		 * (0) The (necessary) home location.
		 */

		final Coord homeCoord;
		final Zone homeZone = this.zonalSystem
				.getZone(this.attr(personId, RegentDictionary.REGENT_HOMEZONE_ATTRIBUTE).toString());
		if (!this.zonalSystem.getNodes(homeZone).isEmpty()) {
			final String homeBuildingType = this.attr(personId, RegentDictionary.REGENT_HOUSINGTYPE_ATTRIBUTE)
					.toString();
			homeCoord = coordinateTransform
					.transform(drawPointFromGeometry(homeZone.drawHomeGeometry(homeBuildingType)));
			if (homeCoord == null) {
				return null;
			}
		} else {
			return null;
		}

		/*
		 * (1) Create a person with socio-demographics and a give it a single (so far
		 * empty) plan.
		 */

		final Person person = this.scenario.getPopulation().getFactory().createPerson(Id.createPersonId(personId));

		PersonUtils.setSex(person, this.attr(personId, "sex").toString());
		PersonUtils.setCarAvail(person, this.attr(personId, "car").toString());
		PersonUtils.setAge(person, REFERENCE_YEAR - (Integer) this.attr(personId, "birthyear"));
		person.getAttributes().putAttribute(income_SEK_yr, this.attr(personId, "income"));

		final Plan plan = this.scenario.getPopulation().getFactory().createPlan();
		person.addPlan(plan);

		/*
		 * (2) Construct plan based on what tours are made.
		 */

		final Coord workCoord;
		final String workTourModeMATSim = RegentDictionary.regent2matsim
				.get(this.attr(personId, RegentDictionary.REGENT_WORKTOURMODE_ATTRIBUTE).toString());
		if (workTourModeMATSim != null) {
			final Zone workZone = this.zonalSystem
					.getZone(this.attr(personId, RegentDictionary.REGENT_WORKZONE_ATTRIBUTE).toString());
			if (!this.zonalSystem.getNodes(workZone).isEmpty()) {
				workCoord = coordinateTransform.transform(drawPointFromGeometry(workZone.drawWorkGeometry()));
			} else {
				workCoord = null;
			}
		} else {
			workCoord = null;
		}
		PersonUtils.setEmployed(person, workCoord != null);

		final Coord otherCoord;
		final String otherTourModeMATSim = RegentDictionary.regent2matsim
				.get(this.attr(personId, RegentDictionary.REGENT_OTHERTOURMODE_ATTRIBUTE).toString());
		if (otherTourModeMATSim != null) {
			final Zone otherZone = this.zonalSystem
					.getZone(this.attr(personId, RegentDictionary.REGENT_OTHERZONE_ATTRIBUTE).toString());
			if (!this.zonalSystem.getNodes(otherZone).isEmpty()) {
				otherCoord = coordinateTransform.transform(drawPointFromGeometry(otherZone.drawWorkGeometry()));
			} else {
				otherCoord = null;
			}
		} else {
			otherCoord = null;
		}
		PersonUtils.setCarAvail(person,
				new Boolean(
						TransportMode.car.equals(workTourModeMATSim) || TransportMode.car.equals(otherTourModeMATSim))
								.toString());

		if ((workCoord != null) && (otherCoord == null)) {

			/*
			 * HOME - WORK - HOME
			 */

			final double totalOvernightTime_s = 24.0 * 3600.0 - 2.0 * this.tripDuration_s - 1.0 * this.workDuration_s;
			final double initialHomeEndTime_s = totalOvernightTime_s / 2.0;

			this.addHomeActivity(plan, homeCoord, initialHomeEndTime_s, HOME);

			final double workEndTime_s = initialHomeEndTime_s + this.tripDuration_s + this.workDuration_s;
			this.addTour(plan, WORK, workCoord, workTourModeMATSim, workEndTime_s);

			this.addHomeActivity(plan, homeCoord, null, HOME);

		} else if ((workCoord == null) && (otherCoord != null)) {

			/*
			 * HOME - OTHER - HOME
			 */

			final double totalOvernightTime_s = 24.0 * 3600.0 - 2.0 * this.tripDuration_s - 1.0 * this.otherDuration_s;
			final double initialHomeEndTime_s = totalOvernightTime_s / 2.0;

			this.addHomeActivity(plan, homeCoord, initialHomeEndTime_s, HOME);

			final double otherEndTime_s = initialHomeEndTime_s + this.tripDuration_s + this.otherDuration_s;
			this.addTour(plan, OTHER, otherCoord, otherTourModeMATSim, otherEndTime_s);

			this.addHomeActivity(plan, homeCoord, null, HOME);

		} else if ((homeCoord != null) && (workCoord != null)) {

			/*
			 * HOME - WORK - HOME - OTHER - HOME
			 */

			final double totalOvernightTime_s = 24.0 * 3600.0 - 4.0 * this.tripDuration_s - 1.0 * this.workDuration_s
					- 1.0 * this.intermediateHomeDuration_s - 1.0 * this.otherDuration_s;
			final double initialHomeEndTime_s = totalOvernightTime_s / 2.0;
			this.addHomeActivity(plan, homeCoord, initialHomeEndTime_s, HOME);

			final double workEndTime_s = initialHomeEndTime_s + this.tripDuration_s + this.workDuration_s;
			this.addTour(plan, WORK, workCoord, workTourModeMATSim, workEndTime_s);

			final double intermediateHomeEndTime_s = workEndTime_s + this.tripDuration_s
					+ this.intermediateHomeDuration_s;
			// TODO NEW 2018-12-06
			// this.addHomeActivity(plan, homeCoord, intermediateHomeEndTime_s, HOME);
			this.addHomeActivity(plan, homeCoord, intermediateHomeEndTime_s, INTERMEDIATE_HOME);

			final double otherEndTime_s = intermediateHomeEndTime_s + this.tripDuration_s + this.otherDuration_s;
			this.addTour(plan, OTHER, otherCoord, otherTourModeMATSim, otherEndTime_s);

			this.addHomeActivity(plan, homeCoord, null, HOME);

		} else {

			return null;

		}

		/*
		 * Assign activity coordinates to links.
		 */
		xy2links.run(person);

		return person;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run(final String initialPlansFile) throws FileNotFoundException {

		final XY2Links xy2links = new XY2Links(this.scenario);
		final CoordinateTransformation coordinateTransform = getCoordinateTransformation(WGS84_EPSG3857,
				WGS84_SWEREF99);

		// 2020-08-14: changed while moving to MATSim 12
		// OLD final List<String> allPersonIds =
		// allObjectKeys(this.scenario.getPopulation().getPersonAttributes());
		final List<String> allPersonIds = allObjectKeys(this.personAttributes);

		Collections.shuffle(allPersonIds);

		for (String personId : new FractionalIterable<>(allPersonIds, this.populationSampleFactor)) {

			final Person person = this.newPerson(personId, xy2links, coordinateTransform);
			if (person != null) {
				// Logger.getLogger(this.getClass().getName()).info(
				// "creating person " + personId);
				this.scenario.getPopulation().addPerson(person);
			}
		}

		PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), this.scenario.getNetwork());
		popwriter.write(initialPlansFile);
	}
}
