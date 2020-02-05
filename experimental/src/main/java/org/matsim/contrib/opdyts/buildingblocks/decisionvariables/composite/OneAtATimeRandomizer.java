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
import java.util.List;

/**
 * TODO This recombines existing objects, meaning that these objects must be
 * state-free!
 *
 * @author Gunnar Flötteröd
 *
 */
public class OneAtATimeRandomizer extends CompositeDecisionVariableRandomizer {

	@Override
	protected Collection<List<SelfRandomizingDecisionVariable<?>>> selectDecisionVariableCombinations(
			final List<Collection<SelfRandomizingDecisionVariable<?>>> variations,
			final List<SelfRandomizingDecisionVariable<?>> original) {
		final Collection<List<SelfRandomizingDecisionVariable<?>>> result = new ArrayList<>();
		for (int i = 0; i < original.size(); i++) {
			if (variations.get(i).size() == 2) {
				final List<SelfRandomizingDecisionVariable<?>> variation1 = new ArrayList<>(original);
				final List<SelfRandomizingDecisionVariable<?>> variation2 = new ArrayList<>(original);
				final Iterator<SelfRandomizingDecisionVariable<?>> it = variations.get(i).iterator();
				variation1.set(i, it.next());
				variation2.set(i, it.next());
				result.add(variation1);
				result.add(variation2);
			} else {
				throw new RuntimeException("A " + SelfRandomizingDecisionVariable.class.getSimpleName()
						+ " must provide two (symmetric) variations.");
			}
		}
		return result;
	}
}
