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
package org.matsim.contrib.greedo.logging;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.LogDataWrapper;

import floetteroed.utilities.math.Covariance;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Vector;
import floetteroed.utilities.statisticslogging.Statistic;
import utils.MovingWindowAverage;

/**
 * 
 * Validates the assumption of
 * 
 * replanningRate ~ expectedUtilityChange / similarity
 * 
 * given a 1/age weighting. Translates the above into the equivalent statement
 * 
 * ageAtReplanning ~ similarity / expectedUtilityChange
 * 
 * Computes the correlation coefficient (across the population in a given
 * iteration) between (average)AgeAtReplanning (per individual and possibly over
 * iterations) and (average)Similarity / (average)ExpectedUtilityChange (both
 * per individual possibly over iterations).
 *
 * @author Gunnar Flötteröd
 *
 */
public class AsymptoticAgeLogger {

	// -------------------- INNER CLASS --------------------

	private class Entry {

		private final MovingWindowAverage expectedUtilityChanges;
		private final MovingWindowAverage similarities;
		private final MovingWindowAverage ages;

		Entry() {
			this.expectedUtilityChanges = new MovingWindowAverage(1, Integer.MAX_VALUE, relativeMemoryLength);
			this.similarities = new MovingWindowAverage(1, Integer.MAX_VALUE, relativeMemoryLength);
			this.ages = new MovingWindowAverage(1, Integer.MAX_VALUE, relativeMemoryLength);
		}

		void update(final double expectedUtilityChange, final double similarity, final int age) {
			this.expectedUtilityChanges.add(expectedUtilityChange);
			this.similarities.add(similarity);
			this.ages.add(age);
		}

		double getLastExpectedUtilityChange() {
			return this.expectedUtilityChanges.mostRecentValue();
		}

		double getLastSimilarity() {
			return this.similarities.mostRecentValue();
		}

		double getLastAge() {
			return this.ages.mostRecentValue();
		}

		double getAvgExpectedUtilityChange() {
			return this.expectedUtilityChanges.average();
		}

		double getAvgSimilarity() {
			return this.similarities.average();
		}

		double getAvgAge() {
			return this.ages.average();
		}
	}

	// -------------------- CONSTANTS --------------------

	private final double relativeMemoryLength;

	private final File folder;

	private final String prefix;

	private final String postfix;

	// -------------------- MEMBERS --------------------

	private final Map<Id<Person>, Entry> personId2entry = new LinkedHashMap<>();

	private Double age_vs_SimilarityByDeltaUtility_correlation = null;

	private Double avgAge_vs_AvgSimilarityByAvgDeltaUtility_correlation = null;

	private Double ageTimesDeltaUtility_vs_Similarity_correlation = null;

	private Double avgAgeTimesAvgDeltaUtility_vs_AvgSimilarity_correlation = null;

	// -------------------- CONSTRUCTION --------------------

	public AsymptoticAgeLogger(final double relativeMemoryLength, final File folder, final String prefix,
			final String postFix) {
		this.relativeMemoryLength = relativeMemoryLength;
		this.folder = folder;
		this.prefix = prefix;
		this.postfix = postFix;

		if (!folder.exists()) {
			try {
				FileUtils.forceMkdir(folder);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// -------------------- INTERNALS / HELPERS --------------------

	public static File fullFileName(final File folder, final String prefix, final int iteration, final String postfix) {
		return FileUtils.getFile(folder, prefix + iteration + postfix);
	}

	private File fullFileName(final int iteration) {
		return fullFileName(this.folder, this.prefix, iteration, this.postfix);
	}

	private Entry getOrCreateEntry(final Id<Person> personId) {
		Entry entry = this.personId2entry.get(personId);
		if (entry == null) {
			entry = new Entry();
			this.personId2entry.put(personId, entry);
		}
		return entry;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void update(final LogDataWrapper logDataWrapper) {
		this.update(logDataWrapper.getReplanningSummaryStatistics().getReplannerId2ageAtReplanning(),
				logDataWrapper.getPersonId2expectedUtilityChange(),
				logDataWrapper.getReplanningSummaryStatistics().personId2similarity,
				logDataWrapper.getReplanningSummaryStatistics().getReplannerId2ageAtReplanning().keySet(),
				logDataWrapper.getIteration());
	}

	private void update(final Map<Id<Person>, Integer> personId2ageAtReplanning,
			final Map<Id<Person>, Double> personId2expectedUtilityChange,
			final Map<Id<Person>, Double> personId2similarity, final Set<Id<Person>> replannerIds, final int iteration) {

		final Covariance ageTimesDeltaUtility_vs_Similarity_covariance = new Covariance(2, 2);

		for (Id<Person> replannerId : replannerIds) {
			final Double expectedUtilityChange = personId2expectedUtilityChange.get(replannerId);
			final Double similarity = personId2similarity.get(replannerId);
			final Integer age = personId2ageAtReplanning.get(replannerId);

			if ((similarity != null) && (expectedUtilityChange != null) && (age != null)) {
				final Entry entry = this.getOrCreateEntry(replannerId);
				entry.update(expectedUtilityChange, similarity, age);

				final double ageTimesDeltaUtilityTimesBeta = entry.getLastAge() * entry.getLastExpectedUtilityChange();
				final Vector x = new Vector(ageTimesDeltaUtilityTimesBeta, entry.getLastSimilarity());
				ageTimesDeltaUtility_vs_Similarity_covariance.add(x, x);
			}
		}

		final Matrix _C = ageTimesDeltaUtility_vs_Similarity_covariance.getCovariance();
		this.ageTimesDeltaUtility_vs_Similarity_correlation = _C.get(1, 0)
				/ Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
	}

	// private void dump(final Map<Id<Person>, Integer> personId2ageAtReplanning,
	// final Map<Id<Person>, Double> personId2expectedUtilityChange,
	// final Map<Id<Person>, Double> personId2similarity, final Set<Id<Person>>
	// replannerIds,
	// final int iteration, final double beta) {
	// try {
	//
	// final PrintWriter writer = new PrintWriter(this.fullFileName(iteration));
	//
	// writer.print("ageAtReplanning\tsimilarity/expDeltaUtility");
	// writer.print("\t");
	// writer.print("<ageAtReplanning>\t<similarity>/<expDeltaUtility>");
	// writer.print("\t");
	// writer.print("ageAtReplanning*expDeltaUtility\tsimilarity");
	// writer.print("\t");
	// writer.print("<ageAtReplanning>*<expDeltaUtility>\t<similarity>");
	// writer.println();
	//
	// final Covariance age_vs_SimilarityByDeltaUtility_covariance = new
	// Covariance(2, 2);
	// final Covariance avgAge_vs_AvgSimilarityByAvgDeltaUtility_covariance = new
	// Covariance(2, 2);
	// final Covariance ageTimesDeltaUtility_vs_Similarity_covariance = new
	// Covariance(2, 2);
	// final Covariance avgAgeTimesAvgDeltaUtility_vs_AvgSimilarity_covariance = new
	// Covariance(2, 2);
	//
	// for (Id<Person> replannerId : replannerIds) {
	// final Double expectedUtilityChange =
	// personId2expectedUtilityChange.get(replannerId);
	// final Double similarity = personId2similarity.get(replannerId);
	// final Integer age = personId2ageAtReplanning.get(replannerId);
	//
	// if ((similarity != null) && (expectedUtilityChange != null) && (age != null))
	// {
	// final Entry entry = this.getOrCreateEntry(replannerId);
	// entry.update(expectedUtilityChange, similarity, age, beta);
	//
	// {
	// final double similarityByUtility = entry.getLastSimilarity()
	// / entry.getLastExpectedUtilityChange();
	// writer.print(entry.getLastAge() + "\t" + similarityByUtility);
	// final Vector x = new Vector(entry.getLastAge(), similarityByUtility);
	// age_vs_SimilarityByDeltaUtility_covariance.add(x, x);
	// }
	// writer.print("\t");
	// {
	// final double avgSimilarityByAvgUtility = entry.getAvgSimilarity()
	// / entry.getAvgExpectedUtilityChange();
	// writer.print(entry.getAvgAge() + "\t" + avgSimilarityByAvgUtility);
	// final Vector x = new Vector(entry.getAvgAge(), avgSimilarityByAvgUtility);
	// avgAge_vs_AvgSimilarityByAvgDeltaUtility_covariance.add(x, x);
	// }
	// writer.print("\t");
	// {
	// final double ageTimesDeltaUtility = entry.getLastAge() *
	// entry.getLastExpectedUtilityChange();
	// writer.print(ageTimesDeltaUtility + "\t" + entry.getLastSimilarity());
	// final Vector x = new Vector(ageTimesDeltaUtility, entry.getLastSimilarity());
	// ageTimesDeltaUtility_vs_Similarity_covariance.add(x, x);
	// }
	// writer.print("\t");
	// {
	// final double avgAgeTimesAvgDeltaUtility = entry.getAvgAge()
	// * entry.getAvgExpectedUtilityChange();
	// writer.print(avgAgeTimesAvgDeltaUtility + "\t" + entry.getAvgSimilarity());
	// final Vector x = new Vector(avgAgeTimesAvgDeltaUtility,
	// entry.getAvgSimilarity());
	// avgAgeTimesAvgDeltaUtility_vs_AvgSimilarity_covariance.add(x, x);
	// }
	// writer.println();
	// }
	// }
	// writer.flush();
	// writer.close();
	//
	// {
	// final Matrix _C = age_vs_SimilarityByDeltaUtility_covariance.getCovariance();
	// this.age_vs_SimilarityByDeltaUtility_correlation = _C.get(1, 0)
	// / Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
	// }
	// {
	// final Matrix _C =
	// avgAge_vs_AvgSimilarityByAvgDeltaUtility_covariance.getCovariance();
	// this.avgAge_vs_AvgSimilarityByAvgDeltaUtility_correlation = _C.get(1, 0)
	// / Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
	// }
	// {
	// final Matrix _C =
	// ageTimesDeltaUtility_vs_Similarity_covariance.getCovariance();
	// this.ageTimesDeltaUtility_vs_Similarity_correlation = _C.get(1, 0)
	// / Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
	// }
	// {
	// final Matrix _C =
	// avgAgeTimesAvgDeltaUtility_vs_AvgSimilarity_covariance.getCovariance();
	// this.avgAgeTimesAvgDeltaUtility_vs_AvgSimilarity_correlation = _C.get(1, 0)
	// / Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
	// }
	//
	// } catch (FileNotFoundException e) {
	// throw new RuntimeException(e);
	// }
	// }

	// -------------------- Statistic FACTORY --------------------

	@Deprecated
	public Statistic<LogDataWrapper> newAgeVsSimilarityByExpDeltaUtilityCorrelationStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(Age;Similarity/ExpDeltaUtility)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(age_vs_SimilarityByDeltaUtility_correlation);
			}
		};
	}

	@Deprecated
	public Statistic<LogDataWrapper> newAvgAgeVsAvgSimilarityByAvgExpDeltaUtilityCorrelationStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(<Age>;<Similarity>/<ExpDeltaUtility>)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(avgAge_vs_AvgSimilarityByAvgDeltaUtility_correlation);
			}
		};
	}

	public Statistic<LogDataWrapper> newAgeTimesExpDeltaUtilityVsSimilarityCorrelationStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(Age*ExpDeltaUtility;Similarity)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(ageTimesDeltaUtility_vs_Similarity_correlation);
			}
		};
	}

	@Deprecated
	public Statistic<LogDataWrapper> newAvgAgeTimesAvgExpDeltaUtilityVsAvgSimilarityCorrelationStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(<Age>*<ExpDeltaUtility>;<Similarity>)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(avgAgeTimesAvgDeltaUtility_vs_AvgSimilarity_correlation);
			}
		};
	}
}
