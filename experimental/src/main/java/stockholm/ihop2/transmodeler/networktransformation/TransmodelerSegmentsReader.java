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

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static stockholm.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.newUnidirectionalId;
import static stockholm.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.newUnidirectionalLinkId;
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
class TransmodelerSegmentsReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private final String idLabel = "ID";

	private final String dirLabel = "Dir";

	private final String abLabel = "AB";

	private final String baLabel = "BA";

	private final String linkLabel = "Link";

	private final String lengthLabel = "Length";

	private final String lanesAbLabel = "Lanes_AB";

	private final String lanesBaLabel = "Lanes_BA";

	private final String positionLabel = "Position";

	// -------------------- MEMBERS --------------------

	private final Map<String, TransmodelerLink> linkId2link;

	final Map<String, TransmodelerLink> unidirSegmentId2link = new LinkedHashMap<>();

	private int ignoredSegmentCnt;
	
	private SegmentAnalyzer segmentAnalyzer = null;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerSegmentsReader(final String segmentFileName,
			final Map<String, TransmodelerLink> linkId2link,
			final SegmentAnalyzer segmentAnalyzer) throws IOException {
		this.linkId2link = linkId2link;
		this.segmentAnalyzer = segmentAnalyzer;
		this.ignoredSegmentCnt = 0;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(segmentFileName, this);
	}

	// -------------------- MISC --------------------

	int getIgnoredSegmentCnt() {
		return this.ignoredSegmentCnt;
	}

	// -------------------- INTERNALS --------------------

	private void addSegment(final String bidirectionalSegmentId,
			final String bidirectionalLinkId, final DIR dir,
			final String abDir, final String baDir, final int lanes,
			final double length, final int position) {
		final TransmodelerLink link = this.linkId2link
				.get(newUnidirectionalLinkId(bidirectionalLinkId, dir, abDir,
						baDir));
		if (link == null) {
			this.ignoredSegmentCnt++;
			System.out.println("ignored segment: " + bidirectionalSegmentId);
		} else {
			final String unidirectionalSegmentId = newUnidirectionalId(
					bidirectionalSegmentId, dir);
			final TransmodelerSegment segment = new TransmodelerSegment(
					unidirectionalSegmentId, lanes, length, position);
			this.unidirSegmentId2link.put(unidirectionalSegmentId, link);
			link.segments.add(segment);
			System.out.println("read segment: " + segment);
			
			if (this.segmentAnalyzer != null) {
				this.segmentAnalyzer.add(bidirectionalSegmentId, 
						unidirectionalSegmentId, link.getId());
			}
		}
	}

	// ---------- OVERRIDING OF AbstractTabularFileHandler ----------

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {

		final String bidirectionalSegmentId = row[this.index(this.idLabel)];
		final String bidirectionalLinkId = row[this.index(this.linkLabel)];
		final int position = parseInt(row[this.index(this.positionLabel)]);
		final double length = parseDouble(row[this.index(this.lengthLabel)]);
		final String dir = row[this.index(this.dirLabel)];
		final String ab = unquote(row[this.index(this.abLabel)]);
		final String ba = unquote(row[this.index(this.baLabel)]);

		if ("1".equals(dir) || "0".equals(dir)) {
			this.addSegment(bidirectionalSegmentId, bidirectionalLinkId,
					DIR.AB, ab, ba,
					parseInt(row[this.index(this.lanesAbLabel)]), length,
					position);
		}

		if ("-1".equals(dir) || "0".equals(dir)) {
			this.addSegment(bidirectionalSegmentId, bidirectionalLinkId,
					DIR.BA, ab, ba,
					parseInt(row[this.index(this.lanesBaLabel)]), length,
					position);
		}
	}
}
