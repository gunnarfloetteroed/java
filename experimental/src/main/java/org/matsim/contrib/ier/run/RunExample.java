package org.matsim.contrib.ier.run;

import java.net.URL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ier.IERModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

public class RunExample {
	static public void main(String[] args) {
		// 2020-08-14: changed (based on heavy guessing) while moving to MATSim 12
		// OLD URL configURL = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml");
		URL configURL = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml");

		Config config = ConfigUtils.loadConfig(configURL);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("output");

		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.minOfDurationAndEndTime);
		ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;
						leg.setMode("car");
						leg.setRoute(null);
					}
				}
			}
		}

		config.subtourModeChoice().setModes(new String[] { "car" });
		config.subtourModeChoice().setChainBasedModes(new String[] { "car" });

		Controler controller = new Controler(scenario);
		controller.addOverridingModule(new IERModule());

		controller.addControlerListener(new StartupListener() {			
			@Override
			public void notifyStartup(StartupEvent event) {
				Logger.getLogger(EventsManagerImpl.class).setLevel(Level.OFF);								
			}
		});

		controller.run();
	}
}
