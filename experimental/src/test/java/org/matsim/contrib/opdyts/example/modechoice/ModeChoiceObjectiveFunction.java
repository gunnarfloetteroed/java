/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import java.util.List;

import org.matsim.contrib.opdyts.microstate.MATSimState;
import org.matsim.contrib.opdyts.objectivefunction.MATSimObjectiveFunction;
import org.matsim.core.router.MainModeIdentifier;

/**
 *
 * @author Kai Nagel based on Gunnar Flötteröd
 *
 */
public class ModeChoiceObjectiveFunction implements MATSimObjectiveFunction<MATSimState> {
//
//    private static final Logger LOGGER = Logger.getLogger(ModeChoiceObjectiveFunction.class);
//
//    private final MainModeIdentifier mainModeIdentifier ;
//
//    @Inject private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
//    @Inject private TripRouter tripRouter ;
//    @Inject private Network network ;
//    // Documentation: "Guice injects ... fields of all values that are bound using toInstance(). These are injected at injector-creation time."
//    // https://github.com/google/guice/wiki/InjectionPoints
//    // I read that as "the fields are injected (every time again) when the instance is injected".
//    // This is the behavior that we want here.  kai, sep'16
//
//    private Databins<String> simStatsContainer = null;
//    private DataMap<String> sumsContainer  = null ;
//    private Databins<String> refStatsContainer = null ;

    ModeChoiceObjectiveFunction(MainModeIdentifier mainModeIdentifier, List<String> modes) {
//        // define the bin boundaries:
//        double[] dataBoundariesTmp = {0.} ;
//        simStatsContainer =  new Databins<>( "simStats", dataBoundariesTmp ) ;
//
//        this.refStatsContainer = new Databins<>( "measuredStats", dataBoundariesTmp ) ;
//
//        this.refStatsContainer.addValue(modes.get(0), 0, 1000.0);
//        this.refStatsContainer.addValue(modes.get(1), 0, 3000.0);
//
//        this.mainModeIdentifier = mainModeIdentifier;
    }

//    public ModeChoiceObjectiveFunction() {
//        this( new TransportPlanningMainModeIdentifier(), Arrays.asList(TransportMode.car, TransportMode.bike) );
//    }
//
    @Override
    public double value(MATSimState matSimState) {
//        resetContainers();
//
//        // MATSimState matSimState = (MATSimState) state;
//        Set<Id<Person>> persons = matSimState.getPersonIdView();
//
//        for (Id<Person> personId : persons) {
//            Plan plan = matSimState.getSelectedPlan(personId);
//            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan, tripRouter.getStageActivityTypes());
//            for (TripStructureUtils.Trip trip : trips) {
//                List<String> tripTypes = new ArrayList<>();
//                String mode = mainModeIdentifier.identifyMainMode(trip.getLegsOnly());
//                tripTypes.add(mode);
//                double item = calcBeelineDistance(trip.getOriginActivity(), trip.getDestinationActivity());
//                addItemToAllRegisteredTypes(tripTypes, item);
//            }
//        }
//
        double objectiveFnValue =0.;
//        double realValueSum = 0;
//
//        for ( Map.Entry<String, double[]> theEntry : simStatsContainer.entrySet() ) {
//            String mode = theEntry.getKey() ;
//            LOGGER.warn("mode=" + mode);
//            double[] value = theEntry.getValue() ;
//            double[] reaVal = this.refStatsContainer.getValues(mode) ;
//            for ( int ii=0 ; ii<value.length ; ii++ ) {
//                double diff = value[ii] - reaVal[ii] ;
//                objectiveFnValue += diff * diff ;
//                realValueSum += reaVal[ii] ;
//            }
//        }
//        objectiveFnValue /= (realValueSum * realValueSum);
        return objectiveFnValue ;
    }
//
//    private void resetContainers() {
//        this.simStatsContainer.clear();
//        if ( this.sumsContainer==null ) {
//            this.sumsContainer = new DataMap<>() ;
//        }
//        this.sumsContainer.clear() ;
//    }
//
//    private void addItemToAllRegisteredTypes(List<String> filters, double item) {
//        // ... go through all filter to which the item belongs ...
//        for ( String filter : filters ) {
//            // ...  add the "item" to the correct bin in the container:
//            int idx = this.simStatsContainer.getIndex(item) ;
//            this.simStatsContainer.inc( filter, idx ) ;
//
//            // also add it to the sums container:
//            this.sumsContainer.addValue( filter, item ) ;
//        }
//    }
//
//    private static int noCoordCnt = 0 ;
//    private double calcBeelineDistance(final Activity fromAct, final Activity toAct) {
//        double item;
//        if ( fromAct.getCoord()!=null && toAct.getCoord()!=null ) {
//            item = CoordUtils.calcEuclideanDistance(fromAct.getCoord(), toAct.getCoord()) ;
//        } else {
//            if ( noCoordCnt < 1 ) {
//                noCoordCnt ++ ;
//                LOGGER.warn("either fromAct or to Act has no Coord; using link coordinates as substitutes.") ;
//                LOGGER.warn(Gbl.ONLYONCE ) ;
//            }
//            Link fromLink = network.getLinks().get( fromAct.getLinkId() ) ;
//            Link   toLink = network.getLinks().get(   toAct.getLinkId() ) ;
//            item = CoordUtils.calcEuclideanDistance( fromLink.getCoord(), toLink.getCoord() ) ;
//        }
//        return item;
//    }
}