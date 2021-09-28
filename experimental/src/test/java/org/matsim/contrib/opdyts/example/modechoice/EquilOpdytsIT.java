/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.opdyts.example.modechoice;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.opdyts.MATSimOpdytsRunner;
import org.matsim.contrib.opdyts.OpdytsConfigGroup;
import org.matsim.contrib.opdyts.buildingblocks.decisionvariables.utils.EveryIterationScoringParameters;
import org.matsim.contrib.opdyts.experimental.OpdytsExperimentalConfigGroup;
import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.microstate.MATSimStateFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Since, only network modes correctly identified, only car, bike modes are tested.
 *
 * Created by amit on 02.05.17.
 */

public class EquilOpdytsIT {

    @Rule
    public MatsimTestUtils helper = new MatsimTestUtils();

    private static URL EQUIL_DIR = ExamplesUtils.getTestScenarioURL("equil-mixedTraffic");

    // @Test
    public void runTest(){
        List<String> modes2consider = Arrays.asList("car","bicycle");

        String outDir = helper.getOutputDirectory();
        Config config = setUpAndReturnConfig(modes2consider);
        config.controler().setOutputDirectory(outDir);

        //== relaxing
        config.controler().setOutputDirectory(outDir+"/relaxedPlans/");
        relaxPlansAndUpdateConfig(config, modes2consider);

        //opdyts run
        config.plans().setInputFile(null);
        config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        new PopulationReader(scenario).readFile(
                new File(config.controler().getOutputDirectory()+"/output_plans.xml.gz").getAbsolutePath());
        scenario.getConfig().controler().setOutputDirectory(outDir+"/opdytsRun/");
        runOpdyts(modes2consider,scenario);
    }

    private void runOpdyts(final List<String> modes2consider, final Scenario scenario){

        String outputDirectory = scenario.getConfig().controler().getOutputDirectory();
        double stepSize = 1.0;
        int opdytsTransitions = 4;

        OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), OpdytsConfigGroup.class);
        opdytsConfigGroup.setNumberOfIterationsForAveraging(5);
        opdytsConfigGroup.setNumberOfIterationsForConvergence(15);

        opdytsConfigGroup.setMaxIteration(opdytsTransitions);
        // opdytsConfigGroup.setOutputDirectory(outputDirectory);
//        opdytsConfigGroup.setDecisionVariableStepSize(stepSize);
        opdytsConfigGroup.setUseAllWarmUpIterations(false);
        opdytsConfigGroup.setWarmUpIterations(5); //this should be tested (parametrized).
//        opdytsConfigGroup.setPopulationSize(2);
        
        OpdytsExperimentalConfigGroup opdytsExperimentalConfigGroup = new OpdytsExperimentalConfigGroup();
        opdytsExperimentalConfigGroup.setDecisionVariableStepSize(stepSize);        
        opdytsExperimentalConfigGroup.setPopulationSize(2);
        
//        MATSimOpdytsRunner<ModeChoiceDecisionVariable> runner = new MATSimOpdytsRunner<>(scenario);
//
//        MATSimSimulationWrapper<ModeChoiceDecisionVariable> simulator = new MATSimSimulationWrapper<ModeChoiceDecisionVariable>(new MATSimStateFactoryImpl<>(), scenario);
//        simulator.addOverridingModule(new AbstractModule() {
//
//            @Override
//            public void install() {
//                bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
//            }
//        });
//        runner.addNetworkModeOccupancyAnalyzr(simulator);

      MATSimOpdytsRunner<ModeChoiceDecisionVariable, MATSimState> runner = new MATSimOpdytsRunner<>(scenario, new MATSimStateFactoryImpl<>());
      // MATSimSimulationWrapper<ModeChoiceDecisionVariable> simulator = new MATSimSimulationWrapper<ModeChoiceDecisionVariable>(new MATSimStateFactoryImpl<>(), scenario);
      // runner.addNetworkModeOccupancyAnalyzr(simulator);
      // runner.setMATSimSimulationWrapper(simulator);
      runner.addOverridingModule(new AbstractModule() {
          @Override
          public void install() {
              bind(ScoringParametersForPerson.class).to(EveryIterationScoringParameters.class);
          }
      });
      
      runner.setFreezeRandomSeed(true);
      
        ModeChoiceDecisionVariable initialDecisionVariable = new ModeChoiceDecisionVariable(scenario.getConfig().planCalcScore(), scenario);
        ModeChoiceObjectiveFunction modeChoiceObjectiveFunction = new ModeChoiceObjectiveFunction(new MainModeIdentifier() {
            @Override
            public String identifyMainMode(List<? extends PlanElement> tripElements) {
                for (PlanElement pe : tripElements) {
                    if (pe instanceof Leg) return ((Leg) pe).getMode();
                }
                throw new RuntimeException("No main mode is found.");
            }
        }, modes2consider);

        runner.run(// simulator,
                new ModeChoiceRandomizer(scenario, modes2consider, opdytsExperimentalConfigGroup),
                initialDecisionVariable,
                modeChoiceObjectiveFunction
//                ,
//                outputDirectory
                );

        //checks
        //check the max opdyts transition
        Assert.assertEquals("Maximum number of OpDyTS transitions are wrong.", new File(outputDirectory).listFiles(File::isDirectory).length, opdytsTransitions); // additional directory for relaxed plans.

        // axial_fixed, check step size
        double maxASCChange = stepSize;
        double bikeInitialASC = 0.;
        double bestASCAfterItr = 0;
        for (int i=0; i<opdytsTransitions;i++){
            bestASCAfterItr = getBestOverallDecisionVariable(outputDirectory+"/opdyts.log",String.valueOf(i));
            Assert.assertTrue("Change in ASC for " + i + " opdyts transition is wrong.", Math.abs( bestASCAfterItr - bikeInitialASC) <= maxASCChange);
            bikeInitialASC = bestASCAfterItr;
        }
        //in order to get 75% bicycle trips, it must be positive
        Assert.assertEquals("The best overall solution is wrong.", bestASCAfterItr, opdytsTransitions-1, MatsimTestUtils.EPSILON ); // stepSize=1 and in every transition bicycle ASC increases

        double valueOfObjFun = getValueOfObjFun(outputDirectory+"/opdyts.log");
//        Assert.assertEquals("The best overall objective function", valueOfObjFun, 0.0067, 0.0001 ); // accuracy up to 4 decimal places (for 6 opdyts transitions)
//         Assert.assertEquals("The best overall objective function", valueOfObjFun, 0.0245, 0.0001 ); // accuracy up to 4 decimal places (for 5 opdyts transitions)
        Assert.assertEquals("The best overall objective function", valueOfObjFun, 0.0597, 0.0001 ); // accuracy up to 4 decimal places // values corresponding to one third population

        double bicycleShare = getBicycleShareFromModeStatsFile(outputDirectory+"/_"+String.valueOf(opdytsTransitions-1)+"/modestats.txt");
//        Assert.assertTrue("Resulting bicycle share is wrong.", bicycleShare > 0.70 && bicycleShare < 0.80 ); // (for opdyts transitions =5/6)
        Assert.assertTrue("Resulting bicycle share is wrong. Share =="+ bicycleShare, bicycleShare > 0.60 && bicycleShare < 0.70 );
    }

    private double getBicycleShareFromModeStatsFile(String file){
        double share = 0;
        boolean header = true;
        BufferedReader reader = IOUtils.getBufferedReader(file);
        try {
            String line = reader.readLine();
            while (line!=null){
                if ( header ) {
                    header = false;
                    line = reader.readLine();
                } else {
                    String parts[] = line.split("\t");
                    share = Double.valueOf(parts[1]);
                    line = reader.readLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
        return share;
    }

    private double getValueOfObjFun(String logFile){
        double value = Double.MAX_VALUE;
        BufferedReader reader = IOUtils.getBufferedReader(logFile);
        boolean header = true;
        try {
            String line = reader.readLine();
            while (line!=null){
                String parts [] = line.split("\t");
                if ( header || parts[3].isEmpty()) {
                    header = false;
                    line = reader.readLine();
                } else {
                    value = Double.valueOf(parts[3]);
                    line = reader.readLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
        return value;
    }

    /**
     * Best decision variable after given opdytsTransition
     * @param logFile
     * @param opdytsTransition
     */
    private double getBestOverallDecisionVariable(String logFile, String opdytsTransition){
        BufferedReader reader = IOUtils.getBufferedReader(logFile);
        try {
            String line = reader.readLine();
            while (line!=null){
                String parts [] = line.split("\t");
                if (parts[1].equals(opdytsTransition)) {
                    String dvString = parts[2];
                    String bikeParams = dvString.substring(dvString.lastIndexOf("bicycle:")+"bicycle:".length()+1);
                    String bikeASC = bikeParams.substring(0, bikeParams.indexOf('+'));
                    return Double.valueOf(bikeASC);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
        return 0.;
    }

    private void relaxPlansAndUpdateConfig(final Config config, final List<String> modes2consider){
        String relaxedDir = config.controler().getOutputDirectory() ;
        config.controler().setOutputDirectory(relaxedDir);
        config.controler().setLastIteration(10);
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

        Scenario scenarioPlansRelaxor = ScenarioUtils.loadScenario(config);
        // following is taken from KNBerlinControler.prepareScenario(...);
        // modify equil plans:
        double time = 6*3600. ;
        for ( Person person : scenarioPlansRelaxor.getPopulation().getPersons().values() ) {
            Plan plan = person.getSelectedPlan() ;
            Activity activity = (Activity) plan.getPlanElements().get(0) ;
            activity.setEndTime(time);
            time++ ;
        }

        Controler controler = new Controler(scenarioPlansRelaxor);
        controler.run();
    }

    private Config setUpAndReturnConfig(final List<String> modes2consider){

        // Config config = ConfigUtils.loadConfig(IOUtils.newUrl(EQUIL_DIR,"config-with-mode-vehicles.xml"), new OpdytsConfigGroup());
        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(EQUIL_DIR,"config-with-mode-vehicles.xml"), new OpdytsConfigGroup());
        config.plans().setInputFile("plans2000.xml.gz");

        //== default config has limited inputs
        StrategyConfigGroup strategies = config.strategy();
        strategies.clearStrategySettings();

        config.changeMode().setModes( modes2consider.toArray(new String [modes2consider.size()]));
        StrategyConfigGroup.StrategySettings modeChoice = new StrategyConfigGroup.StrategySettings();
        modeChoice.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode);
        modeChoice.setWeight(0.1);
        config.strategy().addStrategySettings(modeChoice);

        StrategyConfigGroup.StrategySettings expChangeBeta = new StrategyConfigGroup.StrategySettings();
        expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        expChangeBeta.setWeight(0.9);
        config.strategy().addStrategySettings(expChangeBeta);
        //==

        //== planCalcScore params (initialize will all defaults).
        for ( PlanCalcScoreConfigGroup.ActivityParams params : config.planCalcScore().getActivityParams() ) {
            params.setTypicalDurationScoreComputation( PlanCalcScoreConfigGroup.TypicalDurationScoreComputation.relative );
        }

        // remove other mode params
        PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
        for ( PlanCalcScoreConfigGroup.ModeParams params : planCalcScoreConfigGroup.getModes().values() ) {
            planCalcScoreConfigGroup.removeParameterSet(params);
        }

        PlanCalcScoreConfigGroup.ModeParams mpCar = new PlanCalcScoreConfigGroup.ModeParams(modes2consider.get(0));
        PlanCalcScoreConfigGroup.ModeParams mpBike = new PlanCalcScoreConfigGroup.ModeParams(modes2consider.get(1));
        mpBike.setMarginalUtilityOfTraveling(0.);


        planCalcScoreConfigGroup.addModeParams(mpCar);
        planCalcScoreConfigGroup.addModeParams(mpBike);
        //==

        //==
        config.qsim().setTrafficDynamics( QSimConfigGroup.TrafficDynamics.withHoles );
        config.qsim().setUsingFastCapacityUpdate(true);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        return config;
    }
}