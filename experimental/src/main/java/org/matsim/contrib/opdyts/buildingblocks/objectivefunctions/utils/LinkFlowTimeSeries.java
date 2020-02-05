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
package org.matsim.contrib.opdyts.buildingblocks.objectivefunctions.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

import floetteroed.utilities.Time;
import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LinkFlowTimeSeries implements LinkEnterEventHandler {

	// -------------------- CONSTANTS --------------------

	private final TimeDiscretization timeDiscr;

	// -------------------- MEMBERS --------------------

	private final Set<Id<Link>> relevantLinkIds = new LinkedHashSet<>();

	private final int[] counts;

	private String logFileName = null;

	private boolean justStarted = true;

	// -------------------- CONSTRUCTION --------------------

	public LinkFlowTimeSeries(final TimeDiscretization timeDiscr) {
		this.timeDiscr = timeDiscr;
		this.counts = new int[timeDiscr.getBinCnt()];
	}

	public void addObservedLink(final Id<Link> linkId) {
		this.relevantLinkIds.add(linkId);
	}

	public void addObservedLink(final String linkName) {
		this.addObservedLink(Id.createLinkId(linkName));
	}

	public void setLogFileName(final String logFileName) {
		this.logFileName = logFileName;
	}

	// --------------- IMPLEMENTATION OF LinkEnterEventHandler ---------------

	@Override
	public void reset(final int iteration) {
		if (this.logFileName != null) {
			final StringBuffer line = new StringBuffer();
			if (this.justStarted) {
				this.justStarted = false;
				final File file = new File(this.logFileName);
				if (file.exists()) {
					file.delete();
				}
				for (int bin = 0; bin < this.timeDiscr.getBinCnt(); bin++) {
					line.append("[");
					line.append(Time.strFromSec(this.timeDiscr.getBinStartTime_s(bin)));
					line.append(",");
					line.append(Time.strFromSec(this.timeDiscr.getBinEndTime_s(bin)));
					line.append(")");
					line.append("\t");
				}
			} else {
				for (int bin = 0; bin < this.timeDiscr.getBinCnt(); bin++) {
					line.append(this.counts[bin]);
					line.append("\t");
				}
			}
			try {
				FileUtils.writeLines(new File(this.logFileName), Arrays.asList(line.toString()), true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		Arrays.fill(this.counts, 0);
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		if (this.relevantLinkIds.contains(event.getLinkId())) {
			final int bin = this.timeDiscr.getBin(event.getTime());
			if ((bin >= 0) && (bin < this.timeDiscr.getBinCnt())) {
				this.counts[bin]++;
			}
		}
	}
}
