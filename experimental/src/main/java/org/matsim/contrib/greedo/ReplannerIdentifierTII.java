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

import static org.matsim.contrib.greedo.datastructures.SlotUsageUtilities.addIndicatorsToTotalsTreatingNullAsZero;
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
import floetteroed.utilities.DynamicDataUtils;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ReplannerIdentifierTII {

	// -------------------- CONSTANTS --------------------

	private final Logger log = Logger.getLogger(this.getClass());

	// -------------------- MEMBERS --------------------

	private final GreedoConfigGroup greedoConfig;

	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage;
	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2hypothetialSlotUsage;

	private final Map<Id<Person>, Double> personId2hypotheticalUtilityChange;
	private final Map<Id<Person>, Double> personId2currentUtility;

	private final DynamicData<Id<?>> _B;

	private final double lambdaBar;

	private DynamicData<Id<?>> presenceResiduals;
	private DynamicData<Id<?>> changeResiduals;

	private Map<Id<Person>, SpaceTimeCounts<Id<?>>> personId2interactions = new LinkedHashMap<>();
	private Map<Id<Person>, Double> personId2Dn0 = new LinkedHashMap<>();
	private Map<Id<Person>, Double> personId2Tn = new LinkedHashMap<>();

	private SummaryStatistics lastExpectations = null;

	// -------------------- CONSTRUCTION --------------------

	ReplannerIdentifierTII(final GreedoConfigGroup greedoConfig,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2hypotheticalSlotUsage,
			final Map<Id<Person>, Double> personId2hypotheticalUtilityChange,
			final Map<Id<Person>, Double> personId2currentUtility, final DynamicData<Id<?>> _B,
			final double lambdaBar) {

		this._B = _B;
		this.lambdaBar = lambdaBar;
		this.greedoConfig = greedoConfig;

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
			this.changeResiduals = DynamicDataUtils.newWeightedSum(upcomingWeightedCounts, +lambdaBar,
					currentWeightedCounts, -lambdaBar);
		}

		{
			final DynamicData<Id<?>> currentUnweightedCounts = newTotals(this.greedoConfig.newTimeDiscretization(),
					this.personId2physicalSlotUsage.values(), true, false);
			final DynamicData<Id<?>> upcomingUnweightedCounts = newTotals(this.greedoConfig.newTimeDiscretization(),
					this.personId2hypothetialSlotUsage.values(), true, false);
			this.presenceResiduals = DynamicDataUtils.newWeightedSum(currentUnweightedCounts, (1.0 - lambdaBar),
					upcomingUnweightedCounts, lambdaBar);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Id<Person>> drawReplanners(Network network, final Map<Id<Person>, Double> personId2cn) {

		final DynamicData<Id<?>> weightedReplannerCountDifferences = new DynamicData<>(
				this.greedoConfig.newTimeDiscretization());
		final DynamicData<Id<?>> weightedNonReplannerCountDifferences = new DynamicData<>(
				this.greedoConfig.newTimeDiscretization());

		final DynamicData<Id<?>> locationWeightedReplannerCountDifferences = new DynamicData<>(
				this.greedoConfig.newTimeDiscretization());

		double replannerUtilityChangeSum = 0.0;
		double nonReplannerUtilityChangeSum = 0.0;
		double replannerSizeSum = 0.0;
		double nonReplannerSizeSum = 0.0;

		this.personId2Dn0.clear();
		this.personId2Tn.clear();

		final Set<Id<Person>> replanners = new LinkedHashSet<>();

		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.personId2hypotheticalUtilityChange.keySet());
		Collections.shuffle(allPersonIdsShuffled);
		int replanned = 0;
		for (Id<Person> personId : allPersonIdsShuffled) {
			this.log.info("replanning " + (++replanned) + " of " + allPersonIdsShuffled.size());

			final ScoreUpdaterTII<Id<?>> scoreUpdater = new ScoreUpdaterTII<Id<?>>(
					this.personId2physicalSlotUsage.get(personId), this.personId2hypothetialSlotUsage.get(personId),
					this.lambdaBar, this.personId2hypotheticalUtilityChange.get(personId), this._B,
					this.presenceResiduals, this.changeResiduals, this.greedoConfig, network);

			boolean isReplanner = this.greedoConfig.getReplannerIdentifierRecipe().isReplanner(personId,
					scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero() - personId2cn.getOrDefault(personId, 0.0),
					this.personId2currentUtility.get(personId), this.personId2hypotheticalUtilityChange.get(personId));

			this.personId2interactions.put(personId, scoreUpdater.getRealizedInteractions(isReplanner));
			this.personId2Dn0.put(personId, scoreUpdater.Dn0);
			this.personId2Tn.put(personId, scoreUpdater.Tn);

			if (isReplanner) {
				replanners.add(personId);
				addIndicatorsToTotalsTreatingNullAsZero(weightedReplannerCountDifferences,
						this.personId2hypothetialSlotUsage.get(personId), +1.0, true, true);
				addIndicatorsToTotalsTreatingNullAsZero(weightedReplannerCountDifferences,
						this.personId2physicalSlotUsage.get(personId), -1.0, true, true);

				addIndicatorsToTotalsTreatingNullAsZero(locationWeightedReplannerCountDifferences,
						this.personId2hypothetialSlotUsage.get(personId), +1.0, false, true);
				addIndicatorsToTotalsTreatingNullAsZero(locationWeightedReplannerCountDifferences,
						this.personId2physicalSlotUsage.get(personId), -1.0, false, true);

				replannerUtilityChangeSum += this.personId2hypotheticalUtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					replannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
				}
			} else {
				addIndicatorsToTotalsTreatingNullAsZero(weightedNonReplannerCountDifferences,
						this.personId2hypothetialSlotUsage.get(personId), +1.0, true, true);
				addIndicatorsToTotalsTreatingNullAsZero(weightedNonReplannerCountDifferences,
						this.personId2physicalSlotUsage.get(personId), -1.0, true, true);

				nonReplannerUtilityChangeSum += this.personId2hypotheticalUtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					nonReplannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
				}
			}

			scoreUpdater.updateResiduals(isReplanner ? 1.0 : 0.0);
		}

		final Map<Id<Person>, Double> personId2similarity = new LinkedHashMap<>();
		for (Id<Person> personId : this.personId2hypotheticalUtilityChange.keySet()) {
			final SpaceTimeIndicators<Id<?>> hypotheticalSlotUsage = this.personId2hypothetialSlotUsage.get(personId);
			final SpaceTimeIndicators<Id<?>> physicalSlotUsage = this.personId2physicalSlotUsage.get(personId);
			double similarityNumerator = 0.0;
			for (int timeBin = 0; timeBin < this.greedoConfig.getBinCnt(); timeBin++) {
				if (hypotheticalSlotUsage != null) {
					for (SpaceTimeIndicators<Id<?>>.Visit hypotheticalVisit : hypotheticalSlotUsage
							.getVisits(timeBin)) {
						similarityNumerator += weightedReplannerCountDifferences
								.getBinValue(hypotheticalVisit.spaceObject, timeBin);
					}
				}
				if (physicalSlotUsage != null) {
					for (SpaceTimeIndicators<Id<?>>.Visit physicalVisit : physicalSlotUsage.getVisits(timeBin)) {
						similarityNumerator -= weightedReplannerCountDifferences.getBinValue(physicalVisit.spaceObject,
								timeBin);
					}
				}
			}
			personId2similarity.put(personId, similarityNumerator / this.personId2hypotheticalUtilityChange.size());
		}

		this.lastExpectations = new SummaryStatistics(this.lambdaBar, null, replannerUtilityChangeSum,
				nonReplannerUtilityChangeSum, null, DynamicDataUtils.sumOfEntries2(weightedReplannerCountDifferences),
				DynamicDataUtils.sumOfEntries2(weightedNonReplannerCountDifferences),
				DynamicDataUtils.sumOfEntries2(locationWeightedReplannerCountDifferences), replannerSizeSum,
				nonReplannerSizeSum, replanners.size(),
				this.personId2hypotheticalUtilityChange.size() - replanners.size(), personId2similarity,
				this.greedoConfig.getReplannerIdentifierRecipe().getDeployedRecipeName());

		return replanners;
	}

	Map<Id<Person>, SpaceTimeCounts<Id<?>>> getPersonId2InteractionsView() {
		return Collections.unmodifiableMap(this.personId2interactions);
	}

	public Map<Id<Person>, Double> getPersonId2Dn0() {
		return Collections.unmodifiableMap(personId2Dn0);
	}

	public Map<Id<Person>, Double> getPersonId2Tn() {
		return Collections.unmodifiableMap(personId2Tn);
	}

	// -------------------- INNER CLASS --------------------

	SummaryStatistics getSummaryStatistics(final Set<Id<Person>> replanners,
			final Map<Id<Person>, Integer> personId2age) {
		this.lastExpectations.setReplannerId2ageAtReplanning(
				personId2age.entrySet().stream().filter(entry -> replanners.contains(entry.getKey()))
						.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())));
		return this.lastExpectations;
	}

	public static class SummaryStatistics {

		public final Double lambdaBar;
		public final Double beta;
		public final Double sumOfReplannerUtilityChanges;
		public final Double sumOfNonReplannerUtilityChanges;
		public final Double sumOfWeightedCountDifferences2;
		public final Double sumOfWeightedReplannerCountDifferences2;
		public final Double sumOfWeightedNonReplannerCountDifferences2;
		public final Double sumOfLocationWeightedReplannerCountDifferences2;
		public final Double replannerSizeSum;
		public final Double nonReplannerSizeSum;
		public final Integer numberOfReplanners;
		public final Integer numberOfNonReplanners;
		public final Map<Id<Person>, Double> personId2similarity;
		public final String replannerIdentifierRecipeName;
		private Map<Id<Person>, Integer> replannerId2ageAtReplanning;

		SummaryStatistics() {
			this(null, null, null, null, null, null, null, null, null, null, null, null, new LinkedHashMap<>(), null);
		}

		private SummaryStatistics(final Double lambdaBar, final Double beta, final Double sumOfReplannerUtilityChanges,
				final Double sumOfNonReplannerUtilityChanges, final Double sumOfWeightedCountDifferences2,
				final Double sumOfWeightedReplannerCountDifferences2,
				final Double sumOfWeightedNonReplannerCountDifferences2,
				final Double sumOfLocationWeightedReplannerCountDifferences2, final Double replannerSizeSum,
				final Double nonReplannerSizeSum, final Integer numberOfReplanners, final Integer numberOfNonReplanners,
				final Map<Id<Person>, Double> personId2similarity, final String replannerIdentifierRecipeName) {
			this.lambdaBar = lambdaBar;
			this.beta = beta;
			this.sumOfReplannerUtilityChanges = sumOfReplannerUtilityChanges;
			this.sumOfNonReplannerUtilityChanges = sumOfNonReplannerUtilityChanges;
			this.sumOfWeightedCountDifferences2 = sumOfWeightedCountDifferences2;
			this.sumOfWeightedReplannerCountDifferences2 = sumOfWeightedReplannerCountDifferences2;
			this.sumOfWeightedNonReplannerCountDifferences2 = sumOfWeightedNonReplannerCountDifferences2;
			this.sumOfLocationWeightedReplannerCountDifferences2 = sumOfLocationWeightedReplannerCountDifferences2;
			this.replannerSizeSum = replannerSizeSum;
			this.nonReplannerSizeSum = nonReplannerSizeSum;
			this.numberOfReplanners = numberOfReplanners;
			this.numberOfNonReplanners = numberOfNonReplanners;
			this.personId2similarity = Collections.unmodifiableMap(personId2similarity);
			this.replannerIdentifierRecipeName = replannerIdentifierRecipeName;
			this.replannerId2ageAtReplanning = new LinkedHashMap<>();
		}

		private void setReplannerId2ageAtReplanning(final Map<Id<Person>, Integer> replannerId2ageAtReplanning) {
			this.replannerId2ageAtReplanning = Collections.unmodifiableMap(replannerId2ageAtReplanning);
		}

		public Map<Id<Person>, Integer> getReplannerId2ageAtReplanning() {
			return this.replannerId2ageAtReplanning;
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
