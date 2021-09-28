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
package org.matsim.contrib.opdyts.buildingblocks.decisionvariables.activitytimes;

import java.net.URL;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.utils.EveryIterationScoringParameters;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class OpeningTimesIntegrationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private static URL EQUIL_DIR = ExamplesUtils.getTestScenarioURL("equil");

	/*
	 * Tests if runtime opening time changes in the config take effect in the
	 * controler.
	 */
	@Test
	public void testOpeningTimeModifiability() {

		// Config config = ConfigUtils.loadConfig(IOUtils.newUrl(EQUIL_DIR, "config.xml"));
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(EQUIL_DIR, "config.xml"));
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(3);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
			}
		});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addControlerListenerBinding().toInstance(new BeforeMobsimListener() {
					@Inject
					ScoringParametersForPerson personScoring;
					@Inject
					private MatsimServices matsimService;

					@Override
					public void notifyBeforeMobsim(BeforeMobsimEvent event) {

						double rnd1 = MatsimRandom.getRandom().nextDouble() * 24 * 3600;
						double rnd2 = MatsimRandom.getRandom().nextDouble() * 24 * 3600;
						double rndOpeningTime_s = Math.min(rnd1, rnd2);
						double rndClosingTime_s = Math.max(rnd1, rnd2);

						// insert the random opening times into the *config*
						PlanCalcScoreConfigGroup planCalcScoreConf = this.matsimService.getConfig().planCalcScore();
						ActivityParams workParamsInConfig = planCalcScoreConf.getActivityParams("w");
						workParamsInConfig.setOpeningTime(rndOpeningTime_s);
						workParamsInConfig.setClosingTime(rndClosingTime_s);

						// check that they have made their way through to the *controler*
						ScoringParameters scoringParams = this.personScoring.getScoringParameters(this.matsimService
								.getScenario().getPopulation().getPersons().values().iterator().next());
						ActivityUtilityParameters workParamsInControler = scoringParams.utilParams.get("w");
						Assert.assertEquals(rndOpeningTime_s, workParamsInControler.getOpeningTime().seconds(), 1e-8);
						Assert.assertEquals(rndClosingTime_s, workParamsInControler.getClosingTime().seconds(), 1e-8);
					}
				});
			}
		});

		controler.run();
	}
}
