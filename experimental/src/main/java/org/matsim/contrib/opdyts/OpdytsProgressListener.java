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
package org.matsim.contrib.opdyts;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public interface OpdytsProgressListener {

	public default void callToNotifyStartup_opdyts(StartupEvent event) {
	}

	public default void callToNotifyBeforeMobsim_opdyts(BeforeMobsimEvent event) {
	}

	public default void callToNotifyAfterMobsim_opdyts(AfterMobsimEvent event) {
	}

	public default void expectToBeBeforePhysicalMobsimRun(int iteration) {
	}

	public default void beforeVeryFirstPhysicalMobsimRun(int iteration) {
	}

	public default void beforeOtherThanVeryFirstPhysicalMobsimRun(int iteration) {
	}

	public default void extractedStateAndCalledTrajectorySampler(int iteration) {
	}

	public default void clearedAndAddedMacroStateAnalyzers(int iteration) {
	}

	public default void expectToBeAfterAPhysicalMobsimRun(int iteration) {
	}

	public default void removedButDidNotClearMacroStateAnalyzers(int iteration) {
	}

}
