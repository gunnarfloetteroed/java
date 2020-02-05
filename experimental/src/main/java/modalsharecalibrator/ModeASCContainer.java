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
package modalsharecalibrator;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.inject.Singleton;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
@Singleton
public class ModeASCContainer {

	private final Map<String, Double> mode2asc = new LinkedHashMap<>();

	public ModeASCContainer() {
	}

	public synchronized double getASC(final String mode) {
		return this.mode2asc.getOrDefault(mode, 0.0);
	}

	public synchronized void setASC(final String mode, final double asc) {
		this.mode2asc.put(mode, asc);
		Logger.getLogger(this.getClass()).info("Set ASC for mode " + mode + " to " + this.getASC(mode));
	}

}
