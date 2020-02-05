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
package utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.misc.StringUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class MyConfigUtils {

	private static final char separator = ',';

	public static String listToString(final List<String> list) {
		final StringBuilder builder = new StringBuilder();
		if (list.size() > 0) {
			builder.append(list.get(0));
		}
		for (int i = 1; i < list.size(); i++) {
			builder.append(separator);
			builder.append(list.get(i));
		}
		return builder.toString();
	}

	public static List<String> stringToList(final String string) {
		final ArrayList<String> result = new ArrayList<>();
		// TODO Not ideal to have a dependence on MATSim utilities here.
		for (String part : StringUtils.explode(string, separator)) {
			result.add(part.trim().intern());
		}
		result.trimToSize();
		return result;
	}

}
