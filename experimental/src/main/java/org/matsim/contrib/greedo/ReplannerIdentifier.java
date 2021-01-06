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
package org.matsim.contrib.greedo;

import static floetteroed.utilities.DynamicDataUtils.newWeightedSum;
import static java.util.Collections.unmodifiableMap;
import static org.matsim.contrib.greedo.datastructures.SlotUsageUtilities.newTotals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.datastructures.SpaceTimeCounts;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ReplannerIdentifier {

	// -------------------- CONSTANTS --------------------

	private final Logger log = Logger.getLogger(this.getClass());

	// -------------------- MEMBERS --------------------

	private final GreedoConfigGroup greedoConfig;

	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage;
	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2hypothetialSlotUsage;

	private final Map<Id<Person>, Double> personId2hypotheticalUtilityChange;
	private final Map<Id<Person>, Double> personId2currentUtility;

	private final Map<Id<?>, Double> _B;

	private final double anticipatedMeanReplanningRate;

	private final Network network;

	private final Map<Id<Person>, Double> personId2cn;

	private DynamicData<Id<?>> changeResiduals;

	private SummaryStatistics lastReplanningSummaryStats = null;

	// -------------------- CONSTRUCTION --------------------

	ReplannerIdentifier(final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2hypotheticalSlotUsage,
			final Map<Id<Person>, Double> personId2hypotheticalUtilityChange,
			final Map<Id<Person>, Double> personId2currentUtility, final GreedoConfigGroup greedoConfig,
			final Map<Id<?>, Double> _B, final double anticipatedMeanReplanningRate, final Network network,
			final Map<Id<Person>, Double> personId2cn) {

		this.greedoConfig = greedoConfig;
		this._B = _B;
		this.anticipatedMeanReplanningRate = anticipatedMeanReplanningRate;
		this.network = network;
		this.personId2cn = personId2cn;

		this.personId2physicalSlotUsage = personId2physicalSlotUsage;
		this.personId2hypothetialSlotUsage = personId2hypotheticalSlotUsage;

		this.personId2hypotheticalUtilityChange = personId2hypotheticalUtilityChange;
		this.personId2currentUtility = personId2currentUtility;

		this.log.info("number of entries (persons) in different maps:");
		this.log.info("  personId2physicalSlotUsage.size() = " + personId2physicalSlotUsage.size());
		this.log.info("  personId2hypothetialSlotUsage.size() = " + personId2hypotheticalSlotUsage.size());
		this.log.info("  personId2currentUtility.size() = " + personId2currentUtility.size());
		this.log.info("  personId2hypotheticalUtilityChange.size() = " + personId2hypotheticalUtilityChange.size());

		{
			final DynamicData<Id<?>> currentWeightedCounts = newTotals(this.greedoConfig.newTimeDiscretization(),
					this.personId2physicalSlotUsage.values(), true, true);
			final DynamicData<Id<?>> upcomingWeightedCounts = newTotals(this.greedoConfig.newTimeDiscretization(),
					this.personId2hypothetialSlotUsage.values(), true, true);
			this.changeResiduals = newWeightedSum(upcomingWeightedCounts, +anticipatedMeanReplanningRate,
					currentWeightedCounts, -anticipatedMeanReplanningRate);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Id<Person>> drawReplanners() {

		double replannerHypotheticalUtilityChangeSum = 0.0;
		double nonReplannerHypotheticalUtilityChangeSum = 0.0;
		double replannerSizeSum = 0.0;
		double nonReplannerSizeSum = 0.0;
		int doesNothingCnt = 0;

		final Map<Id<Person>, SpaceTimeCounts<Id<?>>> personId2interactions = new LinkedHashMap<>();
		final Map<Id<Person>, Double> personId2Dn0 = new LinkedHashMap<>();
		final Map<Id<Person>, Double> personId2Dn1 = new LinkedHashMap<>();
		final Map<Id<Person>, Double> personId2Tn = new LinkedHashMap<>();

		final Set<Id<Person>> replanners = new LinkedHashSet<>();

		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.personId2hypotheticalUtilityChange.keySet());
		Collections.shuffle(allPersonIdsShuffled);

		for (Id<Person> personId : allPersonIdsShuffled) {

			final ScoreUpdater<Id<?>> scoreUpdater = new ScoreUpdater<Id<?>>(
					this.personId2physicalSlotUsage.get(personId), this.personId2hypothetialSlotUsage.get(personId),
					this.personId2hypotheticalUtilityChange.get(personId), this.changeResiduals,
					this.anticipatedMeanReplanningRate, this._B, this.greedoConfig, this.network);
//			final boolean isReplanner = this.greedoConfig.getReplannerIdentifierRecipe().isReplanner(personId,
//					scoreUpdater.getScoreChangeIfOne(),
//					scoreUpdater.getScoreChangeIfZero() - this.personId2cn.getOrDefault(personId, 0.0),
//					this.personId2currentUtility.get(personId), this.personId2hypotheticalUtilityChange.get(personId));
			final boolean isReplanner = this.greedoConfig.getReplannerIdentifierRecipe().isReplanner(personId,
					scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero()
							- (this.greedoConfig.getUseCinT() ? this.personId2cn.getOrDefault(personId, 0.0) : 0.0),
					this.personId2currentUtility.get(personId), this.personId2hypotheticalUtilityChange.get(personId));
			scoreUpdater.updateResiduals(isReplanner ? 1.0 : 0.0);

			if (isReplanner) {
				replanners.add(personId);
				replannerHypotheticalUtilityChangeSum += this.personId2hypotheticalUtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					replannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
				}
			} else {
				nonReplannerHypotheticalUtilityChangeSum += this.personId2hypotheticalUtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					nonReplannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
				}
			}

			if (scoreUpdater.doesNothing()) {
				doesNothingCnt++;
			}

			personId2interactions.put(personId, scoreUpdater.getRealizedInteractions(isReplanner));
			personId2Dn0.put(personId, scoreUpdater.getDn0());
			personId2Dn1.put(personId, scoreUpdater.getDn1());
			personId2Tn.put(personId, scoreUpdater.getTn());
		}

		this.lastReplanningSummaryStats = new SummaryStatistics(replannerHypotheticalUtilityChangeSum,
				nonReplannerHypotheticalUtilityChangeSum, replannerSizeSum, nonReplannerSizeSum, replanners.size(),
				this.personId2hypotheticalUtilityChange.size() - replanners.size(),
				this.greedoConfig.getReplannerIdentifierRecipe().getDeployedRecipeName(), (double) doesNothingCnt,
				personId2interactions, personId2Dn0, personId2Dn1, personId2Tn);

		return replanners;
	}

	// -------------------- INNER CLASS --------------------

	SummaryStatistics getSummaryStatistics(final Set<Id<Person>> replanners,
			final Map<Id<Person>, Integer> personId2age) {
		this.lastReplanningSummaryStats.setReplannerId2ageAtReplanning(
				personId2age.entrySet().stream().filter(entry -> replanners.contains(entry.getKey()))
						.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())));
		return this.lastReplanningSummaryStats;
	}

	public static class SummaryStatistics {

		public final Double sumOfReplannerUtilityChanges;
		public final Double sumOfNonReplannerUtilityChanges;
		public final Double replannerSizeSum;
		public final Double nonReplannerSizeSum;
		public final Integer numberOfReplanners;
		public final Integer numberOfNonReplanners;
		public final String replannerIdentifierRecipeName;
		public final Double doesNothingCnt;

		private final Map<Id<Person>, SpaceTimeCounts<Id<?>>> personId2interactionsView;
		private final Map<Id<Person>, Double> personId2Dn0View;
		private final Map<Id<Person>, Double> personId2Dn1View;
		private final Map<Id<Person>, Double> personId2TnView;

		private Map<Id<Person>, Integer> replannerId2ageAtReplanningView = null;

		SummaryStatistics() {
			this(null, null, null, null, null, null, null, null, new LinkedHashMap<>(), new LinkedHashMap<>(),
					new LinkedHashMap<>(), new LinkedHashMap<>());
		}

		private SummaryStatistics(final Double sumOfReplannerUtilityChanges,
				final Double sumOfNonReplannerUtilityChanges, final Double replannerSizeSum,
				final Double nonReplannerSizeSum, final Integer numberOfReplanners, final Integer numberOfNonReplanners,
				final String replannerIdentifierRecipeName, final Double doesNothingCnt,
				final Map<Id<Person>, SpaceTimeCounts<Id<?>>> personId2interactions,
				final Map<Id<Person>, Double> personId2Dn0, final Map<Id<Person>, Double> personId2Dn1,
				final Map<Id<Person>, Double> personId2Tn) {
			this.sumOfReplannerUtilityChanges = sumOfReplannerUtilityChanges;
			this.sumOfNonReplannerUtilityChanges = sumOfNonReplannerUtilityChanges;
			this.replannerSizeSum = replannerSizeSum;
			this.nonReplannerSizeSum = nonReplannerSizeSum;
			this.numberOfReplanners = numberOfReplanners;
			this.numberOfNonReplanners = numberOfNonReplanners;
			this.replannerIdentifierRecipeName = replannerIdentifierRecipeName;
			this.doesNothingCnt = doesNothingCnt;
			this.personId2interactionsView = unmodifiableMap(personId2interactions);
			this.personId2Dn0View = unmodifiableMap(personId2Dn0);
			this.personId2Dn1View = unmodifiableMap(personId2Dn1);
			this.personId2TnView = unmodifiableMap(personId2Tn);
		}

		private void setReplannerId2ageAtReplanning(final Map<Id<Person>, Integer> replannerId2ageAtReplanning) {
			this.replannerId2ageAtReplanningView = Collections.unmodifiableMap(replannerId2ageAtReplanning);
		}

		public Map<Id<Person>, Integer> getReplannerId2ageAtReplanningView() {
			return this.replannerId2ageAtReplanningView;
		}

		public Map<Id<Person>, SpaceTimeCounts<Id<?>>> getPersonId2InteractionsView() {
			return this.personId2interactionsView;
		}

		public Map<Id<Person>, Double> getPersonId2Dn0View() {
			return this.personId2Dn0View;
		}

		public Map<Id<Person>, Double> getPersonId2Dn1View() {
			return this.personId2Dn1View;
		}

		public Map<Id<Person>, Double> getPersonId2Dn1MinusDn0View() {
			final LinkedHashMap<Id<Person>, Double> result = new LinkedHashMap<>();
			result.putAll(this.personId2Dn1View);
			for (Map.Entry<Id<Person>, Double> entry0 : this.personId2Dn0View.entrySet()) {
				result.put(entry0.getKey(), result.getOrDefault(entry0.getKey(), 0.0) - entry0.getValue());
			}
			return result;
		}

		public Map<Id<Person>, Double> getPersonId2TnView() {
			return this.personId2TnView;
		}

		public Double getSumOfAnticipatedUtilityChanges() {
			if ((this.sumOfReplannerUtilityChanges != null) && (this.sumOfNonReplannerUtilityChanges != null)) {
				return (this.sumOfReplannerUtilityChanges + this.sumOfNonReplannerUtilityChanges);
			} else {
				return null;
			}
		}

		public Integer getNumberOfReplanningCandidates() {
			if ((this.numberOfReplanners != null) && (this.numberOfNonReplanners != null)) {
				return (this.numberOfReplanners + this.numberOfNonReplanners);
			} else {
				return null;
			}
		}
	}
}
