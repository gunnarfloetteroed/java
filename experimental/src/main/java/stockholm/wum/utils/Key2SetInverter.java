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
package stockholm.wum.utils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Gunnar Flötteröd
 *
 *         TODO Move this to utilities package.
 */
public class Key2SetInverter {

	public static <K, E> Map<E, Set<K>> getInversion(final Map<K, Set<E>> originalMap) {
		final Map<E, Set<K>> invertedMap = new LinkedHashMap<>();
		for (Map.Entry<K, Set<E>> originalEntry : originalMap.entrySet()) {
			for (E element : originalEntry.getValue()) {
				Set<K> keySet = invertedMap.get(element);
				if (keySet == null) {
					keySet = new LinkedHashSet<>();
					invertedMap.put(element, keySet);
				}
				keySet.add(originalEntry.getKey());
			}
		}
		return invertedMap;
	}
	
	public static <K, E> Set<K> getKeysMatchingSetElement(final E element, final Map<K, Set<E>> key2set) {
		final Set<K> keys = new LinkedHashSet<>();
		for (Map.Entry<K, Set<E>> entry : key2set.entrySet()) {
			if (entry.getValue().contains(element)) {
				keys.add(entry.getKey());
			}
		}
		return keys;
	}

}
