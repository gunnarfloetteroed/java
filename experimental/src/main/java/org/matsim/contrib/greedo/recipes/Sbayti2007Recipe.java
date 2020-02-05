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
package org.matsim.contrib.greedo.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.LogDataWrapper;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Sbayti2007Recipe implements ReplannerIdentifierRecipe {

	// -------------------- MEMBERS --------------------

	private Set<Id<Person>> replannerIds = null;

	// -------------------- CONSTRUCTION --------------------

	public Sbayti2007Recipe() {
	}

	// --------------- IMPLEMENTATION OF ReplannerIdentifierRecipe ---------------

	@Override
	public void update(final LogDataWrapper logDataWrapper) {
		final List<Map.Entry<Id<Person>, Double>> entryList = new ArrayList<>(
				logDataWrapper.getPersonId2expectedUtilityChange().entrySet());
		Collections.sort(entryList, new Comparator<Map.Entry<?, Double>>() {
			@Override
			public int compare(final Entry<?, Double> o1, final Entry<?, Double> o2) {
				return o2.getValue().compareTo(o1.getValue()); // largest values first
			}
		});
		final double meanLambda = logDataWrapper.getGreedoConfig().getMSAReplanningRate(logDataWrapper.getIteration());
		this.replannerIds = new LinkedHashSet<>();
		for (int i = 0; i < meanLambda * entryList.size(); i++) {
			this.replannerIds.add(entryList.get(i).getKey());
		}

	}

	@Override
	public boolean isReplanner(final Id<Person> personId, final double deltaScoreIfYes, final double deltaScoreIfNo,
			final double currentUtility, final double anticipatedUtilityChange) {
		return this.replannerIds.contains(personId);
	}

	@Override
	public String getDeployedRecipeName() {
		return this.getClass().getSimpleName();
	}
}
