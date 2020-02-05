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
package org.matsim.contrib.opdyts.buildingblocks.decisionvariables.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RandomCombinationRandomizer extends CompositeDecisionVariableRandomizer {

	private final int numberOfVariations;

	private final double innovationProba;

	public RandomCombinationRandomizer(final int numberOfVariations, final double innovationProba) {
		this.numberOfVariations = numberOfVariations;
		this.innovationProba = innovationProba;
	}

	@Override
	protected Collection<List<SelfRandomizingDecisionVariable<?>>> selectDecisionVariableCombinations(
			final List<Collection<SelfRandomizingDecisionVariable<?>>> variations,
			final List<SelfRandomizingDecisionVariable<?>> original) {

		// Using a set to avoid duplicates. 
		// Recombination of instances allows to check for reference-equality.
		final Collection<List<SelfRandomizingDecisionVariable<?>>> result = new LinkedHashSet<>();
		while (result.size() < this.numberOfVariations) {

			final List<SelfRandomizingDecisionVariable<?>> variation1 = new ArrayList<>(original);
			final List<SelfRandomizingDecisionVariable<?>> variation2 = new ArrayList<>(original);

			for (int i = 0; i < original.size(); i++) {
				if (MatsimRandom.getRandom().nextDouble() < this.innovationProba) {
					if (variations.get(i).size() == 2) {
						final Iterator<SelfRandomizingDecisionVariable<?>> it = variations.get(i).iterator();
						if (MatsimRandom.getRandom().nextDouble() < 0.5) {
							variation1.set(i, it.next());
							variation2.set(i, it.next());
						} else {
							variation2.set(i, it.next());
							variation1.set(i, it.next());
						}
					} else {
						throw new RuntimeException("A " + SelfRandomizingDecisionVariable.class.getSimpleName()
								+ " must provide two (symmetric) variations.");
					}
				}
			}

			result.add(variation1);
			result.add(variation2);
		}

		return result;
	}
}
