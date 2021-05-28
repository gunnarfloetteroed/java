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
package floetteroed.utilities;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Gunnar Flötteröd
 *
 *         TODO Move this to the utilities-project.
 * 
 */
public class SetUtils {

	public static <T> Set<T> union(final Set<? extends T> arg0, final Set<? extends T> arg1) {
		final Set<T> result = new LinkedHashSet<>(arg0);
		result.addAll(arg1);
		return result;
	}

	public static <T> Set<T> intersect(final Set<? extends T> arg0, final Set<? extends T> arg1) {
		final Set<T> result = new LinkedHashSet<>(arg0);
		result.retainAll(arg1);
		return result;
	}

	public static <T> Set<T> difference(final Collection<? extends T> minuend,
			final Collection<? extends T> subtrahend) {
		final Set<T> result = new LinkedHashSet<>(minuend);
		result.removeAll(subtrahend);
		return result;
	}

}
