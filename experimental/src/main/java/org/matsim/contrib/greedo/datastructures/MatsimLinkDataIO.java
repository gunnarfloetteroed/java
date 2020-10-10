/*
 * Copyright 2020 Gunnar Flötteröd
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
package org.matsim.contrib.greedo.datastructures;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import floetteroed.utilities.DynamicDataXMLFileIO;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
@SuppressWarnings("serial")
public class MatsimLinkDataIO extends DynamicDataXMLFileIO<Id<Link>> {

	@Override
	protected String key2attrValue(Id<Link> key) {
		return key.toString();
	}

	@Override
	protected Id<Link> attrValue2key(String string) {
		return Id.createLinkId(string);
	}

}

