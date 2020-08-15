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
package stockholm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.utils.CreatePseudoNetwork;

import com.google.inject.Provides;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
//import ch.sbb.matsim.config.SBBTransitConfigGroup;
//import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
//import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RunSweden1It {

	public static void main(String[] args) {

		System.exit(0);
		
		Config config = ConfigUtils.createConfig();

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("/Users/GunnarF/OneDrive - VTI/My Data/sweden/output/");

		config.network().setInputFile("/Users/GunnarF/OneDrive - VTI/My Data/sweden/sweden-latest.xml.gz");
		// config.plans().setInputFile("/Users/GunnarF/OneDrive - VTI/My
		// Data/sweden/no-population.xml");

		config.qsim().setStartTime(0);
		config.qsim().setEndTime(24 * 3600.);

		config.transit().setUseTransit(true);
		// config.transit().setTransitModes(new HashSet<>(Arrays.asList(new String[] { "pt" })));
		config.transit()
				.setVehiclesFile("/Users/GunnarF/OneDrive - VTI/My Data/sweden/transitVehicles-sweden-20190214.xml.gz");
		config.transit().setTransitScheduleFile(
				"/Users/GunnarF/OneDrive - VTI/My Data/sweden/transitSchedule-sweden-20190214.xml.gz");

		ConfigUtils.addOrGetModule(config, SBBTransitConfigGroup.class).setCreateLinkEventsInterval(1);
		ConfigUtils.addOrGetModule(config, SBBTransitConfigGroup.class).setDeterministicServiceModes(new HashSet<>(
				Arrays.asList(new String[] { "bus", "subway", "rail", "ferry", "Communal Taxi Service", "tram" })));

		Scenario scenario = ScenarioUtils.loadScenario(config);

		final Set<String> modes = new HashSet<>();
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			System.out.println(line.getId());
			for (TransitRoute route : line.getRoutes().values()) {
				modes.add(route.getTransportMode());
			}
		}
		System.out.println(modes);
		// System.exit(0);

		new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "tr_").createNetwork();

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.install(new SBBTransitModule());
				this.install(new SwissRailRaptorModule()); // does not do anything, though
			}

			@Provides
			QSimComponentsConfig provideQSimComponentsConfig() {
				QSimComponentsConfig components = new QSimComponentsConfig();
				new StandardQSimComponentConfigurator(config).configure(components);
				SBBTransitEngineQSimModule.configure(components);
				return components;
			}
		});
		controler.run();

	}

}
