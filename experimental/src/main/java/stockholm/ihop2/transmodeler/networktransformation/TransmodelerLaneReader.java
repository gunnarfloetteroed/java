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
package stockholm.ihop2.transmodeler.networktransformation;

import static stockholm.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.newUnidirectionalId;
import static stockholm.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import stockholm.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.DIR;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerLaneReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private final String idLabel = "ID";

	private final String dirLabel = "Dir";

	private final String segmentLabel = "Segment";

	// -------------------- MEMBERS --------------------

	private final Map<String, TransmodelerLink> unidirSegmentId2link;

	// lane IDs are unique; no directional information needed
	final Map<String, TransmodelerLink> upstrLaneId2link = new LinkedHashMap<>();
	final Map<String, TransmodelerLink> downstrLaneId2link = new LinkedHashMap<>();

	private int ignoredLaneCnt;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerLaneReader(final String lanesFileName,
			final Map<String, TransmodelerLink> unidirSegmentId2link)
			throws IOException {
		this.unidirSegmentId2link = unidirSegmentId2link;
		this.ignoredLaneCnt = 0;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(lanesFileName, this);
	}

	// -------------------- MISC --------------------

	int getIgnoredLaneCnt() {
		return this.ignoredLaneCnt;
	}

	private void addLane(final String laneId, final String bidirSegmentId,
			final DIR dir) {
		final String unidirSegmentId = newUnidirectionalId(bidirSegmentId, dir);
		final TransmodelerLink link = this.unidirSegmentId2link
				.get(unidirSegmentId);
		if (link == null) {
			this.ignoredLaneCnt++;
		} else {
			final boolean segmentIsUpstream = link
					.segmentIsUpstream(unidirSegmentId);
			final boolean segmentIsDownstream = link
					.segmentIsDownstream(unidirSegmentId);
			if (segmentIsUpstream) {
				this.upstrLaneId2link.put(laneId, link);
				System.out.println("added upstream lane " + laneId);
			}
			if (segmentIsDownstream) {
				this.downstrLaneId2link.put(laneId, link);
				System.out.println("added downstream lane " + laneId);
			}
			if (!segmentIsUpstream && !segmentIsDownstream) {
				this.ignoredLaneCnt++;
			}
		}
	}

	// --------------- OVERRIDING OF AbstractTabularFileHandler ---------------

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {
		final String laneId = row[this.index(this.idLabel)];
		final String bidirSegmentId = row[this.index(this.segmentLabel)];
		final String dir = row[this.index(this.dirLabel)];
		if ("1".equals(dir) || "0".equals(dir)) {
			this.addLane(laneId, bidirSegmentId, DIR.AB);
		}
		if ("-1".equals(dir) || "0".equals(dir)) {
			this.addLane(laneId, bidirSegmentId, DIR.BA);
		}
	}
}
