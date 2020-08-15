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
package stockholm.ihop4.sampersutilities;

import java.util.LinkedList;
import java.util.Set;
import java.util.function.Consumer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.attributable.Attributes;

import floetteroed.utilities.Units;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class MultiLegPTTripSummarizer {

	// -------------------- CONSTANTS --------------------

	private final Set<String> ptSubmodes;

	private final Consumer<Activity> actConsumer;

	private final Consumer<Leg> legConsumer;

	// -------------------- MEMBERS --------------------

	private double pTAccessEgressTimeMultiplier = 1.0;
	private double pTFirstWaitingTimeMultiplier = 1.0;
	private double pTInVehicleTimeMultiplier = 1.0;
	private double pTTransferTimeMultiplier = 1.0;
	private double pTTransferPenalty_min = 0.0;

	private final LinkedList<Leg> tmpLegs = new LinkedList<>();

	private final LinkedList<Activity> tmpActs = new LinkedList<>();

	// -------------------- CONSTRUCTION AND CONFIGURATION --------------------

	public MultiLegPTTripSummarizer(final Set<String> ptSubmodes, final Consumer<Activity> actConsumer,
			final Consumer<Leg> legConsumer) {
		this.ptSubmodes = ptSubmodes;
		this.actConsumer = ((actConsumer != null) ? actConsumer : (a -> {
		}));
		this.legConsumer = ((legConsumer != null) ? legConsumer : (l -> {
		}));
	}

	public void setPTAccessEgressTimeMultiplier(final double multiplier) {
		this.pTAccessEgressTimeMultiplier = multiplier;
	}

	public void setPTFirstWaitingTimeMultiplier(final double multiplier) {
		this.pTFirstWaitingTimeMultiplier = multiplier;
	}

	public void setPTInVehicleTimeMultiplier(final double multiplier) {
		this.pTInVehicleTimeMultiplier = multiplier;
	}

	public void setPTTransferTimeMultiplier(final double multiplier) {
		this.pTTransferTimeMultiplier = multiplier;
	}

	public void setPTTransferPenalty_min(final double penalty_min) {
		this.pTTransferPenalty_min = penalty_min;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void handleLeg(final Leg leg) {
		// The next activity defines how to handle this.
		this.tmpLegs.add(leg);
	}

	public void handleActivity(final Activity act) {

		if (PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType())) {

			// We are in the middle of a transit leg. Just memorize, do nothing yet.
			this.tmpActs.add(act);

		} else {
			
			if (this.tmpLegs.size() == 1) {

				// The previous leg contains a single trip. Since there is no access/egress,
				// this is interpreted as a non-PT trip.
				final Leg leg = this.tmpLegs.getFirst();

				// The transit router may produce pure (transit) walk trips. Interpret as a
				// non-PT walking trip. (MATSim-type justification: Keep "Scoring" independent
				// of "innovation".)
				if (TransportMode.transit_walk.equals(leg.getMode())) {
					leg.setMode(TransportMode.walk);
				}

				// Any other PT mode should not be possible.
				if (TransportMode.pt.equals(leg.getMode()) || this.ptSubmodes.contains(leg.getMode())) {
					throw new RuntimeException("Encountered single-trip leg with mode: " + leg.getMode());
				}

				this.legConsumer.accept(leg);

			} else if (this.tmpLegs.size() > 1) {

				// More than one trip in the previous leg: Expected to be a PT trip chain.

				// "anslutningstid"
				if (!TransportMode.access_walk.equals(this.tmpLegs.getFirst().getMode())) {
					throw new RuntimeException("Expected " + TransportMode.access_walk + " but received "
							+ this.tmpLegs.getFirst().getMode() + " in the first leg.");
				}
				if (!TransportMode.egress_walk.equals(this.tmpLegs.getLast().getMode())) {
					throw new RuntimeException("Expected " + TransportMode.egress_walk + " but received "
							+ this.tmpLegs.getLast().getMode() + " in the last leg.");
				}
				final double accessEgressTime_s = this.tmpLegs.getFirst().getTravelTime().seconds()
						+ this.tmpLegs.getLast().getTravelTime().seconds();

				// "första väntetid"
				final double firstWaitingTime_s = this.tmpActs.getFirst().getEndTime().seconds()
						- this.tmpActs.getFirst().getStartTime().seconds();

				double distance_m = 0.0; // "avstånd"
				double transferTime_s = 0; // "bytestid"
				double inVehicleTime_s = 0; // "restid i fordonet"
				for (int i = 1; i < this.tmpLegs.size() - 1; i++) {
					final Leg leg = this.tmpLegs.get(i);
					distance_m += leg.getRoute().getDistance();
					if (TransportMode.transit_walk.equals(leg.getMode())) {
						transferTime_s += leg.getTravelTime().seconds();
					} else if (this.ptSubmodes.contains(leg.getMode())) {
						inVehicleTime_s += leg.getTravelTime().seconds();
					} else {
						throw new RuntimeException("Unknown PT trip-chain mode: " + leg.getMode());
					}
				}
				final int numberOfTransfers = this.tmpActs.size(); // "antal byten"
				for (Activity transfer : this.tmpActs) {
					transferTime_s += transfer.getEndTime().seconds() - transfer.getStartTime().seconds();
				}

				final double generalizedTravelTime_s = this.pTAccessEgressTimeMultiplier * accessEgressTime_s
						+ this.pTFirstWaitingTimeMultiplier * firstWaitingTime_s
						+ this.pTInVehicleTimeMultiplier * inVehicleTime_s
						+ this.pTTransferTimeMultiplier * transferTime_s
						+ (Units.S_PER_MIN * this.pTTransferPenalty_min) * numberOfTransfers;

				final PTSummaryLeg summaryLeg = new PTSummaryLeg(generalizedTravelTime_s, distance_m,
						this.tmpLegs.getFirst().getRoute().getStartLinkId(),
						this.tmpLegs.getLast().getRoute().getEndLinkId());
				
				this.legConsumer.accept(summaryLeg);
			}

			this.actConsumer.accept(act);
			
			this.tmpLegs.clear();
			this.tmpActs.clear();
		}
	}

	// 2020-08-14: changed while moving to MATSim 12
	class PTSummaryLeg implements Leg {

		// OLD: private final double totalTravelTime_s;
		private final OptionalTime totalTravelTime_s;

		private final double totalDistance_m;

		private final Id<Link> startLinkId;
		private final Id<Link> endLinkId;

		private PTSummaryLeg(final double totalTravelTime_s, final double totalDistance_m,
				final Id<Link> startLinkId, final Id<Link> endLinkId) {
			// this.totalTravelTime_s = totalTravelTime_s;
			this.totalTravelTime_s = OptionalTime.defined(totalTravelTime_s);
			this.totalDistance_m = totalDistance_m;
			this.startLinkId = startLinkId;
			this.endLinkId = endLinkId;
		}

		@Override
		public String getMode() {
			return TransportMode.pt;
		}

		@Override
		public Route getRoute() {
			return new Route() {

				@Override
				public double getDistance() {
					return totalDistance_m;
				}

				@Override
				public Id<Link> getStartLinkId() {
					return startLinkId;
				}

				@Override
				public Id<Link> getEndLinkId() {
					return endLinkId;
				}

				@Override
				public void setDistance(double distance) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void setTravelTime(double travelTime) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void setStartLinkId(Id<Link> linkId) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void setEndLinkId(Id<Link> linkId) {
					throw new UnsupportedOperationException();
				}

				@Override
				public String getRouteDescription() {
					throw new UnsupportedOperationException();
				}

				@Override
				public void setRouteDescription(String routeDescription) {
					throw new UnsupportedOperationException();
				}

				@Override
				public String getRouteType() {
					throw new UnsupportedOperationException();
				}

				@Override
				public Route clone() {
					throw new UnsupportedOperationException();
				}

				@Override
				public OptionalTime getTravelTime() {
					return totalTravelTime_s;
				}

				@Override
				public void setTravelTimeUndefined() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public Attributes getAttributes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setMode(String mode) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setRoute(Route route) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setDepartureTime(double seconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setTravelTime(double seconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public OptionalTime getDepartureTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setDepartureTimeUndefined() {
			throw new UnsupportedOperationException();
		}

		@Override
		public OptionalTime getTravelTime() {
			return this.totalTravelTime_s;
		}

		@Override
		public void setTravelTimeUndefined() {
			throw new UnsupportedOperationException();
		}
	}
}
