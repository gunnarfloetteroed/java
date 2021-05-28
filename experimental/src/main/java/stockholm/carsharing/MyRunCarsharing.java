package stockholm.carsharing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.events.handlers.PersonArrivalDepartureHandler;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemand;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemandImpl;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.RentalInfo;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgent;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgentImpl;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipReader;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTrip;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTripImpl;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.routers.RouterProviderImpl;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CompanyCosts;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculation;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.qsim.CarSharingQSimModule;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.router.CarsharingRoute;
import org.matsim.contrib.carsharing.router.CarsharingRouteFactory;
import org.matsim.contrib.carsharing.router.OneWayCarsharingRoutingModule;
import org.matsim.contrib.carsharing.router.TwoWayCarsharingRoutingModule;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * Things to keep in mind:
 * 
 * -- As it is coded now, every traveler needs a (possibly empty) entry in the
 * membership file.
 *
 * -- Day2day replanning only creates empty routes, which are computed
 * within-day. Hence the need for DVRP. The parametrization is now such that one
 * only uses the travel times of the previous iteration, hence effectively like
 * in day2day.
 * 
 * -- Carsharing scoring extends Charypar-Nagel and adds CS-specific terms from
 * external (to the scoring) bookkeeping objects. One hence can configure
 * CS-specific time-, ... parameters in the same way as for private car in the
 * config, and additionally configure provider-specific (monetary) terms.
 * 
 * -- Search distances (for available car etc) are HARDCODED. Hence own versions
 * of the respective classes: CompanyAgentImpl (replaces original in
 * carsharing.manager.supply), MyCarsharingManager (bound instead of
 * CarsharingManager). TODO Not sure if all occurrences were found.
 * 
 * -- Most parameters in config modules {One,Two}WayCarsharing are never used;
 * exception is the useXYZ switch. TODO: These modules allow to define search
 * radii for my non-hardcoded replacement classes (see item before).
 *
 * -- TODO Check effect of settings in subtourModeChoice config module, in
 * particular w.r.t. interplay with car sharing mode choice.
 * 
 * -- Leg modes (e.g. used in scoring) are "oneway_vehicle", "twoway_vehicle".
 * 
 * -- Config module plansCalcRoute defines only "car" as "networkModes". TODO
 * Not sure why this works, perhaps because of the additional within-day routing
 * logic.
 * 
 * -- TODO Effect of interaction activities in scoring unclear. Do they always have
 * length zero and can be ignored?
 * 
 * -- TODO Not sure if one now can go beyond 1 thread in qSim, there is a
 * comment somewhere from Kai suggesting that.
 * 
 * -- Consider eventually moving this into the "scenarios" repository.
 * 
 * @author balac
 * @author Gunnar Flötteröd
 */
public class MyRunCarsharing {

	static int routeCarsharingRequests = 0;
	static int oneWayRouteRequests = 0;
	static int twoWayRouteRequests = 0;

	public static void main(String[] args) {
		Logger.getLogger("org.matsim.core.controler.Injector").setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);
		System.out.println(config.planCalcScore().getAllModes());
		// System.exit(0);

		if (Integer.parseInt(config.getModule("qsim").getValue("numberOfThreads")) > 1)
			Logger.getLogger("org.matsim.core.controler").warn(
					"Carsharing contrib is not stable for parallel qsim!! If the error occures please use 1 as the number of threads.");

		CarsharingUtils.addConfigModules(config);

		final Scenario sc = ScenarioUtils.loadScenario(config);
		CarsharingDemandGenerator.keepOnlyStrictCarUsers(sc.getPopulation());
		sc.getPopulation().getFactory().getRouteFactories().setRouteFactory(CarsharingRoute.class,
				new CarsharingRouteFactory());

		final Controler controler = new Controler(sc);
		installCarSharing(controler);

		controler.run();
	}

	public static void installCarSharing(final Controler controler) {

		final Scenario scenario = controler.getScenario();
		final TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		final Set<String> carMode = new HashSet<>();
		carMode.add("car");
		final Network carNetwork = NetworkUtils.createNetwork();
		filter.filter(carNetwork, carMode);

		final CarsharingConfigGroup carSharingConfig = (CarsharingConfigGroup) scenario.getConfig()
				.getModule(CarsharingConfigGroup.GROUP_NAME);

		final CarsharingXmlReaderNew reader = new CarsharingXmlReaderNew(carNetwork);
		reader.readFile(carSharingConfig.getvehiclelocations()); // loads the *stations*
		final Set<String> carsharingCompanies = reader.getCompanies().keySet(); // for us, only "m"

		// >>>>> FIGURING OUT WHAT IS GOING ON >>>>>

		System.out.println("Number of vehicles: " + reader.getAllVehicles().size());
		System.out.println("Number of locations: " + reader.getAllVehicleLocations().size());
		System.out.println("Number of oneway vehicles: " + reader.getOwvehicleIdMap().size());
		System.out.println("Number of oneway stations: " + reader.getOnewaycarsharingstationsMap().size());
		// System.exit(-1);

//		for (CompanyContainer companyContainer : reader.getCompanies().values()) {
//			companyContainer.getVehicleContainer("oneway");
//		}

		// <<<<< FIGURING OUT ... <<<<<

		MembershipReader membershipReader = new MembershipReader();
		membershipReader.readFile(carSharingConfig.getmembership());
		final MembershipContainer memberships = membershipReader.getMembershipContainer();

		final CostsCalculatorContainer costsCalculatorContainer = createMyCompanyCostsStructure(carsharingCompanies);

		final CarsharingListener carsharingListener = new CarsharingListener();

		// TODO The following was already commented out in the example. Unsure...
		// final CarsharingSupplyInterface carsharingSupplyContainer = new
		// CarsharingSupplyContainer(controler.getScenario());

		final KeepingTheCarModel keepingCarModel = new MyKeepingTheCarModel();
		final ChooseTheCompany chooseCompany = new MyCompanyChoiceModel();
		final ChooseVehicleType chooseVehicleType = new MyVehicleChoiceModel();

		// This binds to a CS-specific route provider interface.
		final RouterProvider routerProvider = new RouterProviderImpl();

		final CurrentTotalDemand currentTotalDemand = new CurrentTotalDemandImpl(carNetwork);

		// TODO The following was already commented out in the example. Unsure...
		// final CarsharingManagerInterface carsharingManager = new
		// CarsharingManagerNew();

		// TODO Uses the following: @Inject @Named("ff") private TravelTime travelTimes;
		final RouteCarsharingTrip routeCarsharingTrip = new RouteCarsharingTripImpl();

		// @Inject @Named("carnetwork") private Network network;
		final VehicleChoiceAgent vehicleChoiceAgent = new VehicleChoiceAgentImpl();

		// ===adding carsharing objects on supply and demand infrastructure ===

		controler.addOverridingQSimModule(new CarSharingQSimModule());
		controler.addOverridingModule(new DvrpTravelTimeModule());
		controler.configureQSimComponents(CarSharingQSimModule::configureComponents);

		// ~~~~~~~~~~ CONTINUE HERE. FIGURE OUT WHEN REPLANNING OCCURS! ~~~~~~~~~~

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(KeepingTheCarModel.class).toInstance(keepingCarModel);
				bind(ChooseTheCompany.class).toInstance(chooseCompany);
				bind(ChooseVehicleType.class).toInstance(chooseVehicleType);
				bind(RouterProvider.class).toInstance(routerProvider);
				bind(CurrentTotalDemand.class).toInstance(currentTotalDemand);

				bind(RouteCarsharingTrip.class).toInstance(routeCarsharingTrip);
//				bind(RouteCarsharingTrip.class).toInstance(new RouteCarsharingTrip() {
//					@Override
//					public List<PlanElement> routeCarsharingTrip(Plan plan, Leg legToBeRouted, double time,
//							CSVehicle vehicle, Link vehicleLinkLocation, Link parkingLocation,
//							boolean keepTheCarForLaterUse, boolean hasVehicle) {
//						System.out.println("route carsharing requests: " + (++routeCarsharingRequests));
//						return routeCarsharingTrip.routeCarsharingTrip(plan, legToBeRouted, time, vehicle,
//								vehicleLinkLocation, parkingLocation, keepTheCarForLaterUse, hasVehicle);
//					}
//
//				});

				bind(CostsCalculatorContainer.class).toInstance(costsCalculatorContainer);
				bind(MembershipContainer.class).toInstance(memberships);
				bind(CarsharingSupplyInterface.class).to(CarsharingSupplyContainer.class);

//				bind(CarsharingManagerInterface.class).to(CarsharingManagerNew.class);
				bind(CarsharingManagerInterface.class).to(MyCarsharingManager.class);

				bind(VehicleChoiceAgent.class).toInstance(vehicleChoiceAgent);
				bind(DemandHandler.class).asEagerSingleton();
				bind(Network.class).annotatedWith(Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING))
						.to(Network.class);

				bind(Network.class).annotatedWith(Names.named("carnetwork")).toInstance(carNetwork);
				bind(TravelTime.class).annotatedWith(Names.named("ff"))
						.to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
			}
		});

		// === carsharing specific replanning strategies ===

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to(RandomTripToCarsharingStrategy.class);
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy")
						.to(CarsharingSubtourModeChoiceStrategy.class);
			}
		});

		// === adding qsimfactory, controller listeners and event handlers
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toInstance(carsharingListener);

				// addControlerListenerBinding().to(CarsharingManagerNew.class);
				addControlerListenerBinding().to(MyCarsharingManager.class);

				// bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
				addEventHandlerBinding().to(PersonArrivalDepartureHandler.class);
				addEventHandlerBinding().to(DemandHandler.class);
			}
		});
		// === adding carsharing specific scoring factory ===
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {

				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
			}
		});

		// === routing moduels for carsharing trips ===

		// controler.addOverridingModule(CarsharingUtils.createRoutingModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				// addRoutingModuleBinding("twoway").toInstance(new
				// TwoWayCarsharingRoutingModule());
				addRoutingModuleBinding("twoway").toInstance(new RoutingModule() {
					private RoutingModule twoWayModule = new TwoWayCarsharingRoutingModule();

					@Override
					public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility,
							double departureTime, Person person) {
						System.out.println("two-way route requests : " + (++twoWayRouteRequests));
						return this.twoWayModule.calcRoute(fromFacility, toFacility, departureTime, person);
					}

				});

				// addRoutingModuleBinding("freefloating").toInstance(new
				// FreeFloatingRoutingModule());

				// addRoutingModuleBinding("oneway").toInstance(new
				// OneWayCarsharingRoutingModule());
				addRoutingModuleBinding("oneway").toInstance(new RoutingModule() {
					private RoutingModule oneWayModule = new OneWayCarsharingRoutingModule();

					@Override
					public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility,
							double departureTime, Person person) {
						System.out.println("one-way route requests : " + (++oneWayRouteRequests));
						return this.oneWayModule.calcRoute(fromFacility, toFacility, departureTime, person);
					}

				});

				bind(MainModeIdentifier.class).toInstance(new MainModeIdentifier() {
					final MainModeIdentifier defaultModeIdentifier = new MainModeIdentifierImpl();

					@Override
					public String identifyMainMode(final List<? extends PlanElement> tripElements) {
						// we still need to provide a way to identify our trips
						// as being twowaycarsharing trips.
						// This is for instance used at re-routing.
						for (PlanElement pe : tripElements) {
							if (pe instanceof Leg && ((Leg) pe).getMode().equals("twoway")) {
								return "twoway";
							} else if (pe instanceof Leg && ((Leg) pe).getMode().equals("oneway")) {
								return "oneway";
							} else if (pe instanceof Leg && ((Leg) pe).getMode().equals("freefloating")) {
								return "freefloating";

							}
						}
						// if the trip doesn't contain a carsharing leg,
						// fall back to the default identification method.
						return defaultModeIdentifier.identifyMainMode(tripElements);
					}
				});
			}
		});
	}

// -------------------- COMPANY COST STRUCTURE --------------------

	static class MyCompanyCostCalculation implements CostCalculation {

		private final static double betaTT = 1.0;
		private final static double betaRentalTIme = 1.0;
		private final static double scaleTOMatchCar = 4.0;

		@Override
		public double getCost(RentalInfo rentalInfo) {
			double rentalTIme = rentalInfo.getEndTime() - rentalInfo.getStartTime();
			double inVehicleTime = rentalInfo.getInVehicleTime();
			return MyCompanyCostCalculation.scaleTOMatchCar
					* (inVehicleTime / 60.0 * 0.3 + (rentalTIme - inVehicleTime) / 60.0 * 0.15);
		}

	}

	static private CostsCalculatorContainer createMyCompanyCostsStructure(final Set<String> companies) {

		if ((companies.size() != 1) || (!"m".equals(companies.iterator().next()))) {
			throw new RuntimeException("Unknown company.");
		}

		final Map<String, CostCalculation> costCalculations = new HashMap<String, CostCalculation>();
		costCalculations.put("freefloating", new MyCompanyCostCalculation()); // TODO redundant
		costCalculations.put("twoway", new MyCompanyCostCalculation());
		costCalculations.put("oneway", new MyCompanyCostCalculation());

		final CompanyCosts companyCosts = new CompanyCosts(costCalculations);
		final CostsCalculatorContainer companyCostsContainer = new CostsCalculatorContainer();
		companyCostsContainer.getCompanyCostsMap().put("m", companyCosts);

		return companyCostsContainer;
	}

	// -------------------- KEEPING THE CAR MODEL --------------------

	static class MyKeepingTheCarModel implements KeepingTheCarModel {

		public MyKeepingTheCarModel() {
		}

		@Override
		public boolean keepTheCarDuringNextActivity(double durationOfActivity, Person person, String csType) {
			return false;
		}
	}

	// -------------------- COMPANY CHOICE MODEL --------------------

	static class MyCompanyChoiceModel implements ChooseTheCompany {

		@Inject
		private MembershipContainer memberships;

		@Override
		public String pickACompany(Plan plan, Leg leg, double now, String type) {
			Person person = plan.getPerson();
			Id<Person> personId = person.getId();
			String mode = leg.getMode();
			Set<String> availableCompanies = this.memberships.getPerPersonMemberships().get(personId)
					.getMembershipsPerCSType().get(mode);
			int index = MatsimRandom.getRandom().nextInt(availableCompanies.size());
			return (String) availableCompanies.toArray()[index];
		}
	}

	// -------------------- VEHICLE CHOICE MODEL --------------------

	static class MyVehicleChoiceModel implements ChooseVehicleType {
		@Inject
		CarsharingSupplyInterface carsharingSupply;
		@Inject
		MembershipContainer membershipContainer;

		@Override
		public String getPreferredVehicleType(Plan plan, Leg currentLeg) {
			return "car";
		}
	}
}
