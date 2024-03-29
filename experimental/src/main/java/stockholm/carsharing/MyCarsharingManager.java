package stockholm.carsharing;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.events.NoVehicleCarSharingEvent;
import org.matsim.contrib.carsharing.events.StartRentalEvent;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemand;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgent;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.CompanyAgent;
import org.matsim.contrib.carsharing.manager.supply.CompanyContainer;
import org.matsim.contrib.carsharing.manager.supply.OneWayContainer;
import org.matsim.contrib.carsharing.manager.supply.TwoWayContainer;
import org.matsim.contrib.carsharing.manager.supply.VehiclesContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.router.CarsharingRoute;
import org.matsim.contrib.carsharing.stations.CarsharingStation;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.contrib.carsharing.vehicles.StationBasedVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Class containing all the information about carsharing supply and demand.
 * 
 * @author balac
 */
public class MyCarsharingManager implements CarsharingManagerInterface, IterationStartsListener {

	@Inject
	@Named("carnetwork")
	private Network network;
	@Inject
	private CurrentTotalDemand currentDemand;
	@Inject
	private KeepingTheCarModel keepTheCarModel;
	@Inject
	private ChooseTheCompany chooseCompany;
	@Inject
	private ChooseVehicleType chooseVehicleType;
	@Inject
	private CarsharingSupplyInterface carsharingSupplyContainer;
	@Inject
	private EventsManager eventsManager;
	@Inject
	private RouterProvider routerProvider;
	@Inject
	private VehicleChoiceAgent vehicleChoiceAgent;

	static int numberOfRequests = 0;

	public MyCarsharingManager() {
//		System.out.println("HERE I AM.");
//		System.exit(-1);
	}

	@Override
	public List<PlanElement> reserveAndrouteCarsharingTrip(final Plan plan, String carsharingType, Leg legToBeRouted,
			Double time) {

		System.out.println("number of requests = " + (++numberOfRequests));

		Person person = plan.getPerson();
		Link startLink = network.getLinks().get(legToBeRouted.getRoute().getStartLinkId());
		Link destinationLink = network.getLinks().get(legToBeRouted.getRoute().getEndLinkId());

		// === get the vehicle for the trip if the agent has it already ===
		CSVehicle vehicle = getVehicleAtLocation(startLink, plan.getPerson(), carsharingType);
		System.out.println(" -> vehicle = " + ((vehicle == null) ? "null" : vehicle.getVehicleId()));

		boolean willHaveATripFromLocation = willUseTheVehicleLaterFromLocation(destinationLink.getId(), plan,
				legToBeRouted);
		double durationOfNextActivity = getDurationOfNextActivity(plan, legToBeRouted, time);
		boolean keepTheCar;
		if (((CarsharingRoute) legToBeRouted.getRoute()).isOldRoute())
			keepTheCar = ((CarsharingRoute) legToBeRouted.getRoute()).isKeepthecar();
		else
			keepTheCar = keepTheCarModel.keepTheCarDuringNextActivity(durationOfNextActivity, plan.getPerson(),
					carsharingType);
		// TODO: create a method for getting the search distance
		// double searchDistance = 1000.0;
		double searchDistance = 1e6;

		if (vehicle != null) {

			if ((willHaveATripFromLocation && keepTheCar)
					|| (willHaveATripFromLocation && carsharingType.equals("twoway"))) {
				((CarsharingRoute) legToBeRouted.getRoute()).setKeepthecar(true);

				List<PlanElement> result = this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted,
						carsharingType, vehicle, startLink, destinationLink, true, true);
				System.out.println(" -> routed trip = " + result);
				return result;
			} else {

				if (carsharingType.equals("oneway")) {

					CompanyContainer companyContainer = this.carsharingSupplyContainer
							.getCompany(vehicle.getCompanyId());
					VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
					Link parkingLocation = vehiclesContainer.findClosestAvailableParkingLocation(destinationLink,
							searchDistance);

					if (parkingLocation == null) {
						System.out.println(" -> no parking location, return null");
						return null;
					}
					destinationLink = parkingLocation;
					vehiclesContainer.reserveParking(destinationLink);

				} else if (carsharingType.equals("twoway")) {
					CarsharingStation parkingStation = ((TwoWayContainer) this.carsharingSupplyContainer
							.getCompany(vehicle.getCompanyId()).getVehicleContainer(carsharingType))
									.getTwowaycarsharingstationsMap()
									.get(((StationBasedVehicle) vehicle).getStationId());
					destinationLink = parkingStation.getLink();
				}

				List<PlanElement> result = this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted,
						carsharingType, vehicle, startLink, destinationLink, false, true);
				System.out.println(" -> routed trip = " + result);
				return result;
			}
		} else {

			// === agent does not hold the vehicle, therefore must find a one
			// from the supply side===
			// === here he chooses the company, type of vehicle and in the end
			// vehicle ===
			// === possibly this could be moved to one method which decides
			// based on the supply which vehicle to take
			String typeOfVehicle = chooseVehicleType.getPreferredVehicleType(plan, legToBeRouted);
			// CompanyAgent companyAgent =
			// this.carsharingSupplyContainer.getCompanyAgents().
			// get(((CarsharingRoute)legToBeRouted.getRoute()).getCompany());
			List<CSVehicle> offeredVehicles = new ArrayList<>();
			for (CompanyAgent companyAgent : this.carsharingSupplyContainer.getCompanyAgents().values()) {

				CSVehicle offeredVehicle = companyAgent.vehicleRequest(person.getId(), startLink, destinationLink,
						carsharingType, typeOfVehicle);

				if (offeredVehicle != null)
					offeredVehicles.add(offeredVehicle);
			}

			if (offeredVehicles.size() == 0) {
				eventsManager.processEvent(
						new NoVehicleCarSharingEvent(time, carsharingType, "", startLink, destinationLink));
				System.out.println(" -> no offered vehicle, returning null");
				return null;
			}

			// TODO: offer the possible cars to the agent, from which the agent
			// can choose
			CSVehicle chosenVehicle = vehicleChoiceAgent.chooseVehicle(offeredVehicles, startLink, legToBeRouted, time,
					plan.getPerson());

			if (chosenVehicle == null) {
				eventsManager.processEvent(
						new NoVehicleCarSharingEvent(time, carsharingType, "", startLink, destinationLink));
				System.out.println(" -> no chosen vehicle, returning null");
				return null;
			} else {

				CompanyContainer companyContainer = this.carsharingSupplyContainer
						.getCompany(chosenVehicle.getCompanyId());
				VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
				Link stationLink = vehiclesContainer.getVehicleLocation(chosenVehicle);

				if (false == companyContainer.reserveVehicle(chosenVehicle)) {
					eventsManager.processEvent(new NoVehicleCarSharingEvent(time, carsharingType,
							chosenVehicle.getCompanyId(), startLink, destinationLink));
					System.out.println(" -> no vehicle not reserved, returning null");
					return null;
				}

				eventsManager.processEvent(new StartRentalEvent(time, carsharingType, chosenVehicle.getCompanyId(),
						startLink, stationLink, destinationLink, person.getId(), chosenVehicle.getVehicleId()));

				if ((willHaveATripFromLocation && keepTheCar)
						|| (willHaveATripFromLocation && carsharingType.equals("twoway"))) {
					((CarsharingRoute) legToBeRouted.getRoute()).setKeepthecar(true);

					List<PlanElement> result = this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted,
							carsharingType, chosenVehicle, stationLink, destinationLink, true, false);
					System.out.println(" -> routed trip = " + result);
					return result;
				} else {
					if ((carsharingType.equals("oneway")) || (carsharingType.equals("freefloating"))) {
						Link parkingStationLink = this.carsharingSupplyContainer.findClosestAvailableParkingSpace(
								destinationLink, carsharingType, chosenVehicle.getCompanyId(), searchDistance);
						if (parkingStationLink == null) {
							System.out.println(" -> no parkingStationLink, returning null");
							return null;
						}

						vehiclesContainer.reserveParking(parkingStationLink);

						destinationLink = parkingStationLink;
					}

					List<PlanElement> result = this.routerProvider.routeCarsharingTrip(plan, time, legToBeRouted,
							carsharingType, chosenVehicle, stationLink, destinationLink, false, false);
					System.out.println(" -> routed trip = " + result);
					return result;
				}

			}

		}
	}

	private double getDurationOfNextActivity(Plan plan, Leg legToBeRouted, double time) {

		int index = plan.getPlanElements().indexOf(legToBeRouted);

		Activity a = (Activity) plan.getPlanElements().get(index + 1);

		// return a.getEndTime() - time > 0.0 ? a.getEndTime() - time : 0.0;
		return a.getEndTime().isUndefined() ? 0 : a.getEndTime().seconds() - time;
	}

	private boolean willUseTheVehicleLaterFromLocation(Id<Link> linkId, Plan plan, Leg currentLeg) {
		boolean willUseVehicle = false;

		String mode = currentLeg.getMode();
		List<PlanElement> planElements = plan.getPlanElements();

		int index = planElements.indexOf(currentLeg) + 1;

		for (int i = index; i < planElements.size(); i++) {

			if (planElements.get(i) instanceof Leg) {

				if (((Leg) planElements.get(i)).getMode().equals(mode)) {

					if (((Leg) planElements.get(i)).getRoute().getStartLinkId().toString().equals(linkId.toString())) {

						willUseVehicle = true;
					}
				}
			}
		}
		return willUseVehicle;
	}

	private CSVehicle getVehicleAtLocation(Link currentLink, Person person, String carsharingType) {

		return this.currentDemand.getVehicleOnLink(person.getId(), currentLink, carsharingType);

	}

	@Override
	public void freeParkingSpot(String vehicleId, Id<Link> linkId) {

		CSVehicle vehicle = this.carsharingSupplyContainer.getVehicleWithId(vehicleId);
		CompanyContainer companyContainer = this.carsharingSupplyContainer.getCompany(vehicle.getCompanyId());
		VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(vehicle.getCsType());

		((OneWayContainer) vehiclesContainer).freeParkingSpot(vehicle);
	}

	@Override
	public boolean parkVehicle(String vehicleId, Id<Link> linkId) {
		CSVehicle vehicle = this.carsharingSupplyContainer.getVehicleWithId(vehicleId);
		Link link = network.getLinks().get(linkId);
		this.carsharingSupplyContainer.getCompany(vehicle.getCompanyId()).parkVehicle(vehicle, link);
		return false;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.carsharingSupplyContainer.populateSupply();
		this.currentDemand.reset();
	}
}
