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
package org.matsim.contrib.greedo.deprecated;
//
//import static java.lang.Math.log;
//
//import org.matsim.contrib.greedo.GreedoConfigGroup;
//import org.matsim.contrib.greedo.LogDataWrapper;
//
//import floetteroed.utilities.math.Covariance;
//import floetteroed.utilities.math.Matrix;
//import floetteroed.utilities.math.Regression;
//import floetteroed.utilities.math.Vector;
//import floetteroed.utilities.statisticslogging.Statistic;
//import utils.MovingWindowAverage;
//
///**
// * Estimates a model of the following form:
// * 
// * (1 / beta) * deltaX2 + (-delta / beta) = deltaU - deltaU*
// * 
// * 
// * The regression coefficients are given by:
// * 
// * coeff0 = 1 / beta
// * 
// * coeff1 = -delta / beta
// * 
// * 
// * One hence obtains:
// * 
// * beta = 1 / coeff0
// * 
// * delta = -coeff1 * beta = -coeff1 / coeff0
// * 
// *
// * @author Gunnar Flötteröd
// *
// */
class ReplanningEfficiencyEstimator_DEPRECATED {
//
//	// -------------------- CONSTANTS --------------------
//
//	private final boolean constrainDeltaToZero;
//
//	private final boolean acceptNegativeDisappointment;
//
//	// -------------------- MEMBERS --------------------
//
//	private final MovingWindowAverage anticipatedSlotUsageChanges2;
//
//	private final MovingWindowAverage anticipatedMinusRealizedUtilityChanges;
//
//	private Double beta = null;
//
//	private Double delta = null;
//
//	private Double correlation = null;
//
//	private Double currentPredictedTotalUtilityImprovement = null;
//
//	private Double upcomingPredictedTotalUtilityImprovement = null;
//
//	// -------------------- CONSTRUCTION --------------------
//
//	ReplanningEfficiencyEstimator_DEPRECATED(final int minObservationCnt, final double maxRelativeMemoryLength,
//			final boolean constrainDeltaToZero, final boolean acceptNegativeDisappointment) {
//		this.anticipatedSlotUsageChanges2 = new MovingWindowAverage(minObservationCnt, Integer.MAX_VALUE,
//				maxRelativeMemoryLength);
//		this.anticipatedMinusRealizedUtilityChanges = new MovingWindowAverage(minObservationCnt, Integer.MAX_VALUE,
//				maxRelativeMemoryLength);
//		this.constrainDeltaToZero = constrainDeltaToZero;
//		this.acceptNegativeDisappointment = acceptNegativeDisappointment;
//	}
//
//	ReplanningEfficiencyEstimator_DEPRECATED(final GreedoConfigGroup greedoConfig) {
//		this(greedoConfig.getMinAbsoluteMemoryLength(), greedoConfig.getMaxRelativeMemoryLength(),
//				greedoConfig.getConstrainDeltaToZero(), greedoConfig.getAcceptNegativeDisappointment());
//	}
//
//	// -------------------- INTERNALS --------------------
//
//	private Vector regrInput(final double deltaX2) {
//		if (this.constrainDeltaToZero) {
//			return new Vector(deltaX2);
//		} else {
//			return new Vector(deltaX2, 1.0);
//		}
//	}
//
//	// -------------------- IMPLEMENTATION --------------------
//
//	void update(final LogDataWrapper logDataWrapper) {
//		this.update(logDataWrapper.getReplanningSummaryStatistics().sumOfReplannerUtilityChanges,
//				logDataWrapper.getUtilitySummaryStatistics().realizedUtilityChangeSum,
//				// logDataWrapper.getReplanningSummaryStatistics().sumOfWeightedReplannerCountDifferences2
//				logDataWrapper.getReplanningSummaryStatistics().sumOfLocationWeightedReplannerCountDifferences2);
//	}
//
//	private void update(final Double anticipatedUtilityChange, final Double realizedUtilityChange,
//			final Double anticipatedSlotUsageChange2) {
//
//		if ((anticipatedUtilityChange != null) && (realizedUtilityChange != null)
//				&& (anticipatedSlotUsageChange2 != null)
//				&& (this.acceptNegativeDisappointment || (anticipatedUtilityChange - realizedUtilityChange >= 0))) {
//
//			this.anticipatedSlotUsageChanges2.add(anticipatedSlotUsageChange2);
//			this.anticipatedMinusRealizedUtilityChanges.add(anticipatedUtilityChange - realizedUtilityChange);
//			final Double[] deltaX2 = this.anticipatedSlotUsageChanges2.getDataAsDoubleArray();
//			final Double[] deltaDeltaU = this.anticipatedMinusRealizedUtilityChanges.getDataAsDoubleArray();
//			final Regression regr = new Regression(1.0, this.constrainDeltaToZero ? 1 : 2);
//			final Covariance cov = new Covariance(2, 2);
//			for (int i = 0; i < deltaX2.length; i++) {
//				regr.update(this.regrInput(deltaX2[i]), deltaDeltaU[i]);
//				final Vector covInput = new Vector(deltaX2[i], deltaDeltaU[i]);
//				cov.add(covInput, covInput);
//			}
//
//			if (this.hadEnoughData()) {
//				this.beta = (1.0 / regr.getCoefficients().get(0));
//				this.delta = (this.constrainDeltaToZero ? 0.0 : (-regr.getCoefficients().get(1) * this.beta));
//				final Matrix _C = cov.getCovariance();
//				this.correlation = _C.get(1, 0) / Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
//				this.currentPredictedTotalUtilityImprovement = this.upcomingPredictedTotalUtilityImprovement;
//			}
//			this.upcomingPredictedTotalUtilityImprovement = regr.predict(this.regrInput(anticipatedSlotUsageChange2));
//		}
//	}
//
//	private boolean hadEnoughData() {
//		return (this.anticipatedSlotUsageChanges2.size() >= this.anticipatedSlotUsageChanges2.getMinLength());
//	}
//
//	Double getBeta() {
//		return this.beta;
//	}
//
//	Double getDelta() {
//		return this.delta;
//	}
//
//	// --------------- IMPLEMENTATION OF Statistic FACTORIES ---------------
//
//	public Statistic<LogDataWrapper> newDeltaX2vsDeltaDeltaUStatistic() {
//		return new Statistic<LogDataWrapper>() {
//
//			@Override
//			public String label() {
//				return "Corr(DeltaX2,DeltaU-DeltaU*)";
//			}
//
//			@Override
//			public String value(LogDataWrapper arg0) {
//				return Statistic.toString(correlation);
//			}
//		};
//	}
//
//	public Statistic<LogDataWrapper> newAvgPredictedDeltaUtility() {
//		return new Statistic<LogDataWrapper>() {
//
//			@Override
//			public String label() {
//				return "PredictedAvgUtilityImprovement";
//			}
//
//			@Override
//			public String value(LogDataWrapper arg0) {
//				if (currentPredictedTotalUtilityImprovement != null) {
//					return Statistic.toString(currentPredictedTotalUtilityImprovement
//							/ arg0.getReplanningSummaryStatistics().getNumberOfReplanningCandidates());
//				} else {
//					return Statistic.toString(null);
//				}
//			}
//		};
//	}
//
//	public Statistic<LogDataWrapper> newBetaStatistic() {
//		return new Statistic<LogDataWrapper>() {
//			@Override
//			public String label() {
//				return "Beta";
//			}
//
//			@Override
//			public String value(LogDataWrapper arg0) {
//				return Statistic.toString(beta);
//			}
//		};
//	}
//
//	public Statistic<LogDataWrapper> newDeltaStatistic() {
//		return new Statistic<LogDataWrapper>() {
//			@Override
//			public String label() {
//				return "Delta";
//			}
//
//			@Override
//			public String value(LogDataWrapper arg0) {
//				return Statistic.toString(delta);
//			}
//		};
//	}
//
//	public Statistic<LogDataWrapper> newAgeWeightStatistic(final int epsilonPercent) {
//		return new Statistic<LogDataWrapper>() {
//			@Override
//			public String label() {
//				return "AlphaFactor(eps=" + epsilonPercent + "%)";
//			}
//
//			@Override
//			public String value(LogDataWrapper arg0) {
//
//				final Double beta = getBeta();
//				if ((beta == null) || (beta <= 0)) {
//					return Statistic.toString(null);
//				}
//
//				final Double _DeltaU = arg0.getReplanningSummaryStatistics().sumOfReplannerUtilityChanges;
//				if (_DeltaU == null) {
//					return Statistic.toString(null);
//				}
//
//				final Double _DeltaX2unweighted = arg0
//						.getReplanningSummaryStatistics().sumOfLocationWeightedReplannerCountDifferences2;
//				if (_DeltaX2unweighted == null) {
//					return Statistic.toString(null);
//				}
//
//				final Double _DeltaX2weighted = arg0
//						.getReplanningSummaryStatistics().sumOfWeightedReplannerCountDifferences2;
//				if (_DeltaX2weighted == null) {
//					return Statistic.toString(null);
//				}
//
//				final Double _DeltaUstar = _DeltaU - _DeltaX2unweighted / beta;
//
//				final double epsilon = epsilonPercent / 100.0;
//				if (_DeltaU - (1.0 + epsilon) * _DeltaUstar <= 0) {
//					return Statistic.toString(null);
//				}
//
//				final double alpha = log(1.0 - epsilon) / (log(_DeltaX2weighted) - log(_DeltaX2unweighted));
//
//				// final double alpha = (log(beta) + log(_DeltaU - (1.0 + epsilon) *
//				// _DeltaUstar)
//				// - log(_DeltaX2unweighted)) / (log(_DeltaX2weighted) -
//				// log(_DeltaX2unweighted));
//
//				return Statistic.toString(alpha);
//			}
//		};
//	}
}
