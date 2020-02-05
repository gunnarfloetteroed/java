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

@Deprecated // just so that I know when I happen to use it
public class RegentPerson {

	private final String id;
	private final String homeZone;
	private final String workZone;
	private final String workTourMode;
	private final String sex;
	private final String housingType;
	private final Integer income;
	private final Integer birthYear;

	public RegentPerson(final String id, final String homeZone,
			final String workZone, final String workTourMode,
			final String income, final String sex, final String housingType,
			final String birthYear) {

		this.id = id;
		this.homeZone = homeZone;
		this.workZone = workZone;
		this.workTourMode = workTourMode;
		this.sex = sex;
		this.housingType = housingType;
		this.income = (income == null ? null : Integer.parseInt(income));
		this.birthYear = (birthYear == null ? null : Integer
				.parseInt(birthYear));
	}

	@Override
	public String toString() {
		return "ID = " + this.id + ", homeZone = " + this.homeZone
				+ ", workZone = " + this.workZone + ", workTourMode = "
				+ this.workTourMode + ", sex = " + this.sex
				+ ", housingType = " + this.housingType + ", income = "
				+ this.income + ", birthYear = " + this.birthYear;

	}

	public boolean isComplete() {
		return (this.id != null && this.homeZone != null
				&& this.workZone != null && this.workTourMode != null
				&& this.sex != null && this.housingType != null
				&& this.income != null && this.birthYear != null);
	}

	public String getId() {
		return this.id;
	}

	public String getHomeZone() {
		return this.homeZone;
	}

	public String getWorkZone() {
		return this.workZone;
	}

	public String getWorkTourMode() {
		return this.workTourMode;
	}

	public Integer getBirthYear() {
		return this.birthYear;
	}

	public Integer getIncome() {
		return this.income;
	}

	public String getSex() {
		return this.sex;
	}

	public String getHousingType() {
		return this.housingType;
	}
}
