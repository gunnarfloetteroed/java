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
package stockholm.norrkoping;

import java.util.Scanner;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.greedo.Greedo;
import org.matsim.contrib.greedo.GreedoConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.filter.NetworkNodeFilter;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.algorithms.XY2Links;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Provides;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import floetteroed.utilities.Units;
import stockholm.saleem.StockholmTransformationFactory;
import stockholm.ihop2.regent.demandreading.ZonalSystem;
import stockholm.ihop2.regent.demandreading.Zone;
import stockholm.utils.ShapeUtils;
import stockholm.wum.analysis.PopulationSampler;
import stockholm.wum.creation.CropTransitSystem;
import stockholm.wum.creation.DetailPTVehicles;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class NorrkopingProductionRunner {

	static final String swedenNetworkFile = "/Users/GunnarF/OneDrive - VTI/My Data/sweden/sweden-latest.xml.gz";
	static final String swedenScheduleFile = "/Users/GunnarF/OneDrive - VTI/My Data/sweden/transitSchedule-sweden-20190214.xml.gz";
	static final String swedenTransitVehicleFile = "/Users/GunnarF/OneDrive - VTI/My Data/sweden/transitVehicles-sweden-20190214.xml.gz";

	static final String transitVehicleTypeDefinitionsFileName = "C:\\Users\\GunnarF\\OneDrive - VTI\\My Data\\wum\\data\\input\\transitVehicles_only-types.xml";

	static final String norrkopingZoneShapeFile = "C:\\Users\\GunnarF\\OneDrive - VTI\\My Data\\norrkoping\\input\\od_zone_norrk.shp";

	static final String norrkopingNetworkFile = "/Users/GunnarF/OneDrive - VTI/My Data/norrkoping/norrkoping-network.xml.gz";
	static final String norrkopingTransitScheduleFile = "/Users/GunnarF/OneDrive - VTI/My Data/norrkoping/transitSchedule-norrkoping-20190214.xml.gz";
	static final String norrkopingTransitVehicleFile = "/Users/GunnarF/OneDrive - VTI/My Data/norrkoping/transitVehicles-norrkoping-20190214.xml.gz";
	static final String norrkopingPopulationFile = "/Users/GunnarF/OneDrive - VTI/My Data/norrkoping/norrkoping-plans.xml.gz";
	static final String norrkoping1PctPopulationFile = "/Users/GunnarF/OneDrive - VTI/My Data/norrkoping/norrkoping-plans.1pct.xml.gz";
	static final String norrkoping25PctPopulationFile = "/Users/GunnarF/OneDrive - VTI/My Data/norrkoping/norrkoping-plans.25pct.xml.gz";

	static void cutFromSwedenCarOnly(final double xMin, final double xMax, final double yMin, final double yMax) {

		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile(swedenNetworkFile);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		System.out.println("raw data: " + scenario.getNetwork().getNodes().size());

		final NetworkFilterManager filters = new NetworkFilterManager(scenario.getNetwork());
		final NetworkNodeFilter nodeFilter = new NetworkNodeFilter() {
			@Override
			public boolean judgeNode(Node n) {
				final double x = n.getCoord().getX();
				final double y = n.getCoord().getY();
				return ((xMin <= x) && (x <= xMax) && (yMin <= y) && (y <= yMax));
			}
		};
		filters.addNodeFilter(nodeFilter);
		filters.addLinkFilter(new NetworkLinkFilter() {
			@Override
			public boolean judgeLink(Link l) {
				return (nodeFilter.judgeNode(l.getFromNode()) && nodeFilter.judgeNode(l.getToNode()));
			}
		});
		final Network net = filters.applyFilters();
		System.out.println("after filtering: " + net.getNodes().size());

		new NetworkCleaner().run(net);
		System.out.println("after cleaning: " + net.getNodes().size());

		new NetworkWriter(net).write(norrkopingNetworkFile);
	}

	static void cutFromSwedenPTOnly() {

		final ZonalSystem zonalSystem = new ZonalSystem(norrkopingZoneShapeFile,
				StockholmTransformationFactory.WGS84_SWEREF99, "id");

		final CropTransitSystem cropTransit = new CropTransitSystem(zonalSystem, swedenScheduleFile,
				swedenTransitVehicleFile, StockholmTransformationFactory.getCoordinateTransformation(
						StockholmTransformationFactory.WGS84_SWEREF99, StockholmTransformationFactory.WGS84_SWEREF99));
		cropTransit.run(norrkopingTransitScheduleFile, norrkopingTransitVehicleFile);

		final DetailPTVehicles detailPT = new DetailPTVehicles(transitVehicleTypeDefinitionsFileName,
				norrkopingTransitScheduleFile, swedenTransitVehicleFile);
		detailPT.run(norrkopingTransitVehicleFile);
	}

	public static void createDemand(final double demandUpscale) {

		System.out.println("Comment this out -- danger to overwrite existing population.");
		System.exit(0);

		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile(norrkopingNetworkFile);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final ZonalSystem zonalSystem = new ZonalSystem(norrkopingZoneShapeFile,
				StockholmTransformationFactory.WGS84_SWEREF99, "id");

		final Scanner scanner = new Scanner(System.in);
		System.out.println("DB user name: ");
		final String user = scanner.nextLine();
		System.out.println("password: ");
		final String passwd = scanner.nextLine();
		scanner.close();
		final DatabaseODMatrix od = new DatabaseODMatrix(user, passwd, "localhost", 5432); // 5455);

		final int timeBinCnt = od.getNumberOfTimeBins();
		final double timeBinSize_s = Units.S_PER_D / timeBinCnt;

		final int zoneCnt = zonalSystem.getId2zoneView().size();
		int processedOriginZones = 0;
		long personCnt = 0;
		for (Zone fromZone : zonalSystem.getId2zoneView().values()) {
			System.out.println(((100 * processedOriginZones++) / zoneCnt) + "% DONE");
			for (Zone toZone : zonalSystem.getId2zoneView().values()) {
				for (int timeBin = 0; timeBin < timeBinCnt; timeBin++) {
					final double demand = demandUpscale
							* od.getDemandPerHour(fromZone.getId(), toZone.getId(), timeBin);
					if (demand > 0) {
						for (int i = 0; i < demand; i++) {
							addTripMaker(scenario, Id.createPersonId(personCnt++), zonalSystem, fromZone, toZone,
									(Math.random() + timeBin) * timeBinSize_s);
						}
						if (Math.random() < (demand - (int) demand)) {
							addTripMaker(scenario, Id.createPersonId(personCnt++), zonalSystem, fromZone, toZone,
									(Math.random() + timeBin) * timeBinSize_s);
						}
					}
				}
			}
		}

		final PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.writeV6(norrkopingPopulationFile);
	}

	private static void addTripMaker(Scenario scenario, Id<Person> personId, ZonalSystem zonalSystem, Zone fromZone,
			Zone toZone, double dptTime_s) {
		final Person person = scenario.getPopulation().getFactory().createPerson(personId);
		final Plan plan = scenario.getPopulation().getFactory().createPlan();
		person.addPlan(plan);
		{
			final Coord fromCoord = ShapeUtils.drawPointFromGeometry(fromZone.getGeometry());
			final Activity start = scenario.getPopulation().getFactory().createActivityFromCoord("start", fromCoord);
			start.setEndTime(dptTime_s);
			plan.addActivity(start);
		}
		plan.addLeg(scenario.getPopulation().getFactory().createLeg("car"));
		{
			final Coord toCoord = ShapeUtils.drawPointFromGeometry(toZone.getGeometry());
			final Activity end = scenario.getPopulation().getFactory().createActivityFromCoord("end", toCoord);
			plan.addActivity(end);
		}
		scenario.getPopulation().addPerson(person);
	}

	static void runXY2Links() {
		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile(norrkopingNetworkFile);
		config.plans().setInputFile(norrkopingPopulationFile);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// new NetworkCleaner().run(scenario.getNetwork());

		final XY2Links xy2links = new XY2Links(scenario);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			System.out.println(person.getId());
			xy2links.run(person);
		}
		final PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.writeV6(norrkopingPopulationFile);
	}

	static void reducePopulation() {
		PopulationSampler.main(new String[] { norrkopingPopulationFile, norrkoping25PctPopulationFile, "0.25" });
	}

	static void runSimulation(final String configFileName) {

		final Config config = ConfigUtils.loadConfig(configFileName, new SwissRailRaptorConfigGroup(),
				new SBBTransitConfigGroup(), new GreedoConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Greedo greedo = new Greedo();
		greedo.meet(config);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		createHugeCapacityPTSystem(scenario);
		new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "tr_").createNetwork();
		greedo.meet(scenario);

		final Controler controler = new Controler(scenario);
		// controler.addOverridingModule(new
		// SampersDifferentiatedPTScoringFunctionModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.install(new SBBTransitModule());
				this.install(new SwissRailRaptorModule());
			}

			@Provides
			QSimComponentsConfig provideQSimComponentsConfig() {
				QSimComponentsConfig components = new QSimComponentsConfig();
				new StandardQSimComponentConfigurator(config).configure(components);
				SBBTransitEngineQSimModule.configure(components);
				return components;
			}
		});

		greedo.meet(controler);

		// controler.addOverridingModule(new AbstractModule() {
		// @Override
		// public void install() {
		// bind(ModeASCContainer.class);
		// }
		// });

		// controler.addOverridingModule(new AbstractModule() {
		// @Override
		// public void install() {
		// addControlerListenerBinding().to(WUMASCInstaller.class);
		// }
		// });

		controler.run();
	}

	private static void createHugeCapacityPTSystem(final Scenario scenario) {
		Logger.getLogger(NorrkopingProductionRunner.class).warn("Creating huge-capacity PT system.");

		for (VehicleType vehicleType : scenario.getTransitVehicles().getVehicleTypes().values()) {
			final VehicleCapacity capacity = vehicleType.getCapacity();
			capacity.setSeats(100 * 1000);
			capacity.setStandingRoom(100 * 1000);
			vehicleType.setAccessTime(0);
			vehicleType.setEgressTime(0);
			// PCU equivalents -- attempting to cause a failure if used
			vehicleType.setPcuEquivalents(Double.NaN);
		}

		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					stop.setAwaitDepartureTime(true);
				}
			}
		}
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final double xMin = 531377;
		final double xMax = 622208;
		final double yMin = 6474497;
		final double yMax = 6524013;

		final double demandUpscale = 100.0 / 18.0;

		// cutFromSwedenCarOnly(xMin, xMax, yMin, yMax);
		// cutFromSwedenPTOnly(xMin, xMax, yMin, yMax);
		// createDemand(demandUpscale);
		// runXY2Links();

		// reducePopulation();
		runSimulation(args[0]);

		System.out.println("... DONE");
	}

}
