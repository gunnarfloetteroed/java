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
package stockholm.wum;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.greedo.Greedo;
import org.matsim.contrib.greedo.GreedoConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricingConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricingModule;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.handler.BoardingDeniedEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Provides;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import modalsharecalibrator.CalibrationModeExtractor;
import modalsharecalibrator.ModalShareCalibrationConfigGroup;
import modalsharecalibrator.ModeASCContainer;
import modalsharecalibrator.WireModalShareCalibratorIntoMATSimControlerListener;
import stockholm.ihop4.sampersutilities.SampersDifferentiatedPTScoringFunctionModule;
import stockholm.wum.utils.PTInteractionsRemover;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WUMProductionRunner {

	static final String temporaryPath = "/Users/GunnarF/NoBackup/data-workspace/wum/";
	// static final String archivePath = "/Users/GunnarF/OneDrive - VTI/My
	// Data/wum/";

	public static void scaleTransitCapacities(final Scenario scenario, final double factor) {
		for (VehicleType vehicleType : scenario.getTransitVehicles().getVehicleTypes().values()) {
			// vehicle capacities (in person units) get scaled DOWN
			final VehicleCapacity capacity = vehicleType.getCapacity();
			capacity.setSeats((int) Math.ceil(capacity.getSeats() * factor));
			capacity.setStandingRoom((int) Math.ceil(capacity.getStandingRoom() * factor));
			// access and egress times per person get scaled UP
			vehicleType.setAccessTime(vehicleType.getAccessTime() / factor);
			vehicleType.setEgressTime(vehicleType.getEgressTime() / factor);
			// PCU equivalents -- attempting to cause a failure if used
			vehicleType.setPcuEquivalents(Double.NaN);
		}
	}

	public static void removeModeInformation(final Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						final Leg leg = (Leg) planElement;
						leg.setMode(TransportMode.car);
						leg.setRoute(null);
					}
				}
			}
		}
	}

	static void fixCarAvailability(final Scenario scenario) {
		int drivers = 0;
		int nonDrivers = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if ("true".equals(person.getAttributes().getAttribute("carAvail"))) {
				PersonUtils.setCarAvail(person, "yes");
				drivers++;
			} else {
				PersonUtils.setCarAvail(person, "never");
				nonDrivers++;
			}
		}
		Logger.getLogger(WUMProductionRunner.class).info(drivers + " drivers, " + nonDrivers + " non-drivers");
	}

	static void runProductionScenarioWithSampersDynamics(final String configFileName) {

		final boolean terminateUponBoardingDenied = false;
		final boolean useGreedo = true;

		final Config config = ConfigUtils.loadConfig(configFileName, new SwissRailRaptorConfigGroup(),
				new SBBTransitConfigGroup(), new RoadPricingConfigGroup(), new ModalShareCalibrationConfigGroup(),
				new GreedoConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Greedo greedo;
		if (useGreedo) {
			greedo = new Greedo();
			greedo.meet(config);
		}

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			PTInteractionsRemover.run(person, false);
		}

		scaleTransitCapacities(scenario, config.qsim().getStorageCapFactor());
		fixCarAvailability(scenario);
		new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "tr_").createNetwork();
		if (useGreedo) {
			greedo.meet(scenario);
		}

		// {
		// ValidationResult result =
		// TransitScheduleValidator.validateAll(scenario.getTransitSchedule(),
		// scenario.getNetwork());
		// TransitScheduleValidator.printResult(result);
		// }

		final Controler controler = new Controler(scenario);

		controler.addOverridingModule(new RoadPricingModule());

		controler.addOverridingModule(new SampersDifferentiatedPTScoringFunctionModule());
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

		if (useGreedo) {
			greedo.meet(controler);
		}

		if (terminateUponBoardingDenied) {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(new BoardingDeniedEventHandler() {
						@Override
						public void handleEvent(BoardingDeniedEvent e) {
							System.out.println("BOARDING DENIED EVENT!");
							System.out.println("time:   " + e.getTime());
							System.out.println("person: " + e.getPersonId());
							System.out.println("vehicle: " + e.getVehicleId());
							System.exit(0);
						}
					});
				}
			});
		}

		// the following for modal share calibration
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ModeASCContainer.class);
			}
		});
		if (ConfigUtils.addOrGetModule(config, ModalShareCalibrationConfigGroup.class).isOn()) {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(CalibrationModeExtractor.class).toInstance(new WUMModeExtractor(
							ConfigUtils.addOrGetModule(getConfig(), ModalShareCalibrationConfigGroup.class), "home",
							"work", "other", "intermediate_home"));
					addControlerListenerBinding().to(WireModalShareCalibratorIntoMATSimControlerListener.class);
				}
			});
		}

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().to(WUMASCInstaller.class);
			}
		});

		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				Logger.getLogger(EventsManagerImpl.class).setLevel(Level.OFF);
			}
		});

		controler.run();
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		runProductionScenarioWithSampersDynamics(args[0]);
		System.out.println("... DONE");
	}

}
