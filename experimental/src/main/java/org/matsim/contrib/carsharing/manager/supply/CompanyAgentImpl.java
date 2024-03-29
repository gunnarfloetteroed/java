package org.matsim.contrib.carsharing.manager.supply;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public class CompanyAgentImpl implements CompanyAgent {
	
	private CompanyContainer companyContainer;
	
	public CompanyAgentImpl(CompanyContainer companyContainer, String strategyType) {
		this.companyContainer = companyContainer;
//		System.out.println("Here I am: " + this.getClass().getSimpleName());
//		System.exit(-1);
	}
	
	@Override
	public CSVehicle vehicleRequest(Id<Person> personId, Link locationLink, Link destinationLink,
			String carsharingType, String vehicleType) {

		VehiclesContainer vehiclesContainer = companyContainer.getVehicleContainer(carsharingType);
		System.out.println(" -> vehicles container reference: " + vehiclesContainer);
		
		if (vehiclesContainer != null) {
			
			//Depending on the company strategy
			//here the company just provides the closest vehicle in the search radius
			// CSVehicle vehicle = vehiclesContainer.findClosestAvailableVehicle(locationLink, vehicleType, 500.0);
			CSVehicle vehicle = vehiclesContainer.findClosestAvailableVehicle(locationLink, vehicleType, 1e6);
			
			return vehicle;
		}
		
		else
			return null;
		
	}	
	
}
