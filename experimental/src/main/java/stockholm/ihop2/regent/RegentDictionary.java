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
package stockholm.ihop2.regent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RegentDictionary {

	public static final String REGENT_BIRTHYEAR_ATTRIBUTE = "birthyear";

	public static final String REGENT_SEX_ATTRIBUTE = "sex";

	public static final String REGENT_INCOME_ATTRIBUTE = "income";

	public static final String REGENT_HOUSINGTYPE_ATTRIBUTE = "housingtype";

	public static final String REGENT_HOMEZONE_ATTRIBUTE = "homezone";

	public static final String REGENT_WORKZONE_ATTRIBUTE = "workzone";

	public static final String REGENT_OTHERZONE_ATTRIBUTE = "otherzone";

	public static final String REGENT_WORKTOURMODE_ATTRIBUTE = "worktourmode";

	public static final String REGENT_OTHERTOURMODE_ATTRIBUTE = "othertourmode";

	public static final String REGENT_CAR_ATTRIBUTEVALUE = "Car";

	public static final String REGENT_PT_ATTRIBUTEVALUE = "PublicTransport";

	public static final String REGENT_BICYCLE_ATTRIBUTVALUE = "Bicycle";

	public static final String REGENT_WALK_ATTRIBUTVALUE = "Walk";

	public static final String REGENT_NONE_ATTRIBUTVALUE = "None";

	public static final String REGENT_CARPASSENGER_ATTRIBUTVALUE = "CarPassenger";

	static {
		final Map<String, String> regent2matsimTmp = new LinkedHashMap<>();
		final Map<String, String> matsim2regentTmp = new LinkedHashMap<>();

		regent2matsimTmp.put(REGENT_CAR_ATTRIBUTEVALUE, TransportMode.car);
		matsim2regentTmp.put(TransportMode.car, REGENT_CAR_ATTRIBUTEVALUE);

		regent2matsimTmp.put(REGENT_PT_ATTRIBUTEVALUE, TransportMode.pt);
		matsim2regentTmp.put(TransportMode.pt, REGENT_PT_ATTRIBUTEVALUE);

		regent2matsimTmp.put(REGENT_BICYCLE_ATTRIBUTVALUE, TransportMode.bike);
		matsim2regentTmp.put(TransportMode.bike, REGENT_BICYCLE_ATTRIBUTVALUE);

		regent2matsimTmp.put(REGENT_WALK_ATTRIBUTVALUE, TransportMode.walk);
		matsim2regentTmp.put(TransportMode.walk, REGENT_WALK_ATTRIBUTVALUE);

		regent2matsimTmp.put(REGENT_NONE_ATTRIBUTVALUE, null);
		regent2matsimTmp.put(REGENT_CARPASSENGER_ATTRIBUTVALUE, null);

		regent2matsim = Collections.unmodifiableMap(regent2matsimTmp);
		matsim2regent = Collections.unmodifiableMap(matsim2regentTmp);
	}

	private RegentDictionary() {
	}

	public static final Map<String, String> regent2matsim;
	public static final Map<String, String> matsim2regent;

}
