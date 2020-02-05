package org.matsim.contrib.ier.run;

import java.net.URL;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ier.IERModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

public class RunExample {
	static public void main(String[] args) {
		URL configURL = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("siouxfalls-2014"), "config_default.xml");
		// URL configURL = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"),
		// "config.xml");

		Config config = ConfigUtils.loadConfig(configURL);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("output");

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

		controller.run();
	}
}
