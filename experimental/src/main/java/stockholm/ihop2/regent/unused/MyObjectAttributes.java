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
package stockholm.ihop2.regent.unused;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.utils.objectattributes.ObjectAttributes;

@Deprecated
public class MyObjectAttributes extends ObjectAttributes {

	final Set<String> allObjectIds = new LinkedHashSet<String>();
	
	final Set<String> allAttributes = new LinkedHashSet<String>();
	
	@Override
	public Object putAttribute(final String objectId, final String attribute, final Object value) {

		this.allObjectIds.add(objectId);
		this.allAttributes.add(attribute);
		
		return super.putAttribute(objectId, attribute, value);
	}

}
