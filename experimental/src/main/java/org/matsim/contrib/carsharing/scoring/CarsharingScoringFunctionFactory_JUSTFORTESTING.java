package org.matsim.contrib.carsharing.scoring;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;

public class CarsharingScoringFunctionFactory_JUSTFORTESTING implements ScoringFunctionFactory {
	
	private final Scenario scenario;
	private final ScoringParametersForPerson params;
	private final DemandHandler demandHandler;
	private final CostsCalculatorContainer costsCalculatorContainer;
	private final CarsharingSupplyInterface carsharingSupplyContainer;
	@Inject
	CarsharingScoringFunctionFactory_JUSTFORTESTING( final Scenario sc, final DemandHandler demandHandler,
			final CostsCalculatorContainer costsCalculatorContainer, final CarsharingSupplyInterface carsharingSupplyContainer) {
		this.scenario = sc;
		this.params = new SubpopulationScoringParameters( sc );
		this.demandHandler = demandHandler;
		this.costsCalculatorContainer = costsCalculatorContainer;
		this.carsharingSupplyContainer = carsharingSupplyContainer;
	}


	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction scoringFunctionSum = new SumScoringFunction();
	    //this is the main difference, since we need a special scoring for carsharing legs

		scoringFunctionSum.addScoringFunction(
	    new CarsharingLegScoringFunction( params.getScoringParameters( person ),
	    								 this.scenario.getConfig(),
	    								 this.scenario.getNetwork(), this.demandHandler, this.costsCalculatorContainer, 
	    								 this.carsharingSupplyContainer, person));
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelLegScoring(
						params.getScoringParameters( person ),
						this.scenario.getNetwork(),
						this.scenario.getConfig().transit().getTransitModes())
			    );
		//the remaining scoring functions can be changed and adapted to the needs of the user
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelActivityScoring(
						params.getScoringParameters(
								person ) ) );
		scoringFunctionSum.addScoringFunction(
				new CharyparNagelAgentStuckScoring(
						params.getScoringParameters(
								person ) ) );
		
	    // return scoringFunctionSum;
		return new ScoringFunction() {

			@Override
			public void handleActivity(Activity activity) {
				scoringFunctionSum.handleActivity(activity);
			}

			@Override
			public void handleLeg(Leg leg) {
				scoredModes.add(leg.getMode());
				System.out.println("  scored modes:  " + scoredModes);
				System.out.print("  available parameters: ");
				for (String val : params.getScoringParameters(person).modeParams.keySet()) {
					System.out.print(val + " ");
				}
				System.out.println();
				scoringFunctionSum.handleLeg(leg);
			}

			@Override
			public void agentStuck(double time) {
				scoringFunctionSum.agentStuck(time);
			}

			@Override
			public void addMoney(double amount) {
				scoringFunctionSum.addMoney(amount);
			}

			@Override
			public void finish() {
				scoringFunctionSum.finish();
			}

			@Override
			public double getScore() {
				return scoringFunctionSum.getScore();
			}

			@Override
			public void handleEvent(Event event) {
				scoringFunctionSum.handleEvent(event);
			}			
		};		
	  }
	
	static Set<String> scoredModes = new LinkedHashSet<>();
	
}
