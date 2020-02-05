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
package org.matsim.contrib.greedo.analysis;

import org.matsim.contrib.greedo.logging.AvgAnticipatedDeltaUtility;
import org.matsim.contrib.greedo.logging.AvgRealizedUtility;
import org.matsim.contrib.greedo.logging.LambdaRealized;
import org.matsim.contrib.greedo.logging.MATSimIteration;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class AccelerationLogHandler extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- MEMBERS --------------------

	private final Double[] betas;
	private final Double[] realizedLambdas;
	private final Double[] realizedUtilities;
	private final Double[] expectedUtilityChanges;
//	private final Double[] performanceCorrelations;
	private final Double[] ageCorrelations;
	private final Double[] agePercentiles10;
	private final Double[] agePercentiles20;
	private final Double[] agePercentiles30;
	private final Double[] agePercentiles40;
	private final Double[] agePercentiles50;
	private final Double[] agePercentiles60;
	private final Double[] agePercentiles70;
	private final Double[] agePercentiles80;
	private final Double[] agePercentiles90;

	// -------------------- CONSTRUCTION --------------------

	public AccelerationLogHandler(final Double[] betas, final Double[] realizedLambdas,
			final Double[] realizedUtilities, final Double[] expectedUtilityChanges,
//			final Double[] performanceCorrelations, 
			final Double[] ageCorrelations, final Double[] agePercentiles10,
			final Double[] agePercentiles20, final Double[] agePercentiles30, final Double[] agePercentiles40,
			final Double[] agePercentiles50, final Double[] agePercentiles60, final Double[] agePercentiles70,
			final Double[] agePercentiles80, final Double[] agePercentiles90) {
		this.betas = betas;
		this.realizedLambdas = realizedLambdas;
		this.realizedUtilities = realizedUtilities;
		this.expectedUtilityChanges = expectedUtilityChanges;
//		this.performanceCorrelations = performanceCorrelations;
		this.ageCorrelations = ageCorrelations;
		this.agePercentiles10 = agePercentiles10;
		this.agePercentiles20 = agePercentiles20;
		this.agePercentiles30 = agePercentiles30;
		this.agePercentiles40 = agePercentiles40;
		this.agePercentiles50 = agePercentiles50;
		this.agePercentiles60 = agePercentiles60;
		this.agePercentiles70 = agePercentiles70;
		this.agePercentiles80 = agePercentiles80;
		this.agePercentiles90 = agePercentiles90;
	}

	// -------------------- INTERNALS --------------------

	private Double doubleOrNull(final String val) {
		if ((val == null) || "".equals(val)) {
			return null;
		} else {
			return Double.parseDouble(val);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public void startDataRow(final String[] row) {
		final int iteration = this.getIntValue(MATSimIteration.class.getSimpleName());
		this.betas[iteration] = this.doubleOrNull(this.getStringValue("Beta"));
		this.realizedLambdas[iteration] = this.doubleOrNull(this.getStringValue(LambdaRealized.class.getSimpleName()));
		this.realizedUtilities[iteration] = this
				.doubleOrNull(this.getStringValue(AvgRealizedUtility.class.getSimpleName()));

		if (this.label2index.containsKey(AvgAnticipatedDeltaUtility.class.getSimpleName())) {
			this.expectedUtilityChanges[iteration] = this
					.doubleOrNull(this.getStringValue(AvgAnticipatedDeltaUtility.class.getSimpleName()));
		} else {
			this.expectedUtilityChanges[iteration] = null;
		}

//		this.performanceCorrelations[iteration] = this
//				.doubleOrNull(this.getStringValue("Corr(DeltaX2,DeltaU-DeltaU*)"));
		this.ageCorrelations[iteration] = this
				.doubleOrNull(this.getStringValue("Corr(Age*ExpDeltaUtility;Similarity)"));
		
		this.agePercentiles10[iteration] = this.doubleOrNull(this.getStringValue("agePercentile10"));
		this.agePercentiles20[iteration] = this.doubleOrNull(this.getStringValue("agePercentile20"));
		this.agePercentiles30[iteration] = this.doubleOrNull(this.getStringValue("agePercentile30"));
		this.agePercentiles40[iteration] = this.doubleOrNull(this.getStringValue("agePercentile40"));
		this.agePercentiles50[iteration] = this.doubleOrNull(this.getStringValue("agePercentile50"));
		this.agePercentiles60[iteration] = this.doubleOrNull(this.getStringValue("agePercentile60"));
		this.agePercentiles70[iteration] = this.doubleOrNull(this.getStringValue("agePercentile70"));
		this.agePercentiles80[iteration] = this.doubleOrNull(this.getStringValue("agePercentile80"));
		this.agePercentiles90[iteration] = this.doubleOrNull(this.getStringValue("agePercentile90"));

	}
}
