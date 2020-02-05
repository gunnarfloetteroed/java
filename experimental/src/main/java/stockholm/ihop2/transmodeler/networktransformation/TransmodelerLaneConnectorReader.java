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
import static stockholm.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;

import java.io.IOException;
import java.util.Map;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerLaneConnectorReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private final String upstreamLaneLabel = "Upstream Lane";

	private final String downstreamLaneLabel = "Downstream Lane";

	private final String lengthLabel = "Length";

	// -------------------- MEMBERS --------------------

	private final Map<String, TransmodelerLink> unidirUpstrLaneId2link;

	private final Map<String, TransmodelerLink> unidirDownstrLaneId2link;

	private int loadedConnectionCnt;

	private int ignoredConnectionCnt;

	// -------------------- CONSTRUCTION --------------------

	TransmodelerLaneConnectorReader(final String turningMovesFileName,
			final Map<String, TransmodelerLink> unidirUpstreamLaneId2link,
			final Map<String, TransmodelerLink> unidirDownstreamLaneId2link)
			throws IOException {
		this.unidirUpstrLaneId2link = unidirUpstreamLaneId2link;
		this.unidirDownstrLaneId2link = unidirDownstreamLaneId2link;
		this.loadedConnectionCnt = 0;
		this.ignoredConnectionCnt = 0;
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(turningMovesFileName, this);
	}

	// -------------------- GETTERS --------------------

	int getLoadedConnectionCnt() {
		return this.loadedConnectionCnt;
	}

	int getIgnoredConnectionCnt() {
		return this.ignoredConnectionCnt;
	}

	// -------------------- INTERNALS --------------------

	private void addLaneConnection(final String upstrLaneId,
			final String downstrLaneId, final double length) {
		/*
		 * The upstream lane is located downstream in its link, and the
		 * downstream lane is located upstream in its link.
		 */
		final TransmodelerLink upstrLink = this.unidirDownstrLaneId2link
				.get(upstrLaneId);
		final TransmodelerLink downstrLink = this.unidirUpstrLaneId2link
				.get(downstrLaneId);
		if ((upstrLink != null) && (downstrLink != null)
				&& (!upstrLink.equals(downstrLink))) {
			upstrLink.downstreamLink2turnLength.put(downstrLink, length);
			System.out.println("read connection from link " + upstrLink.getId()
					+ " to link " + downstrLink.getId());
			this.loadedConnectionCnt++;
		} else {
			this.ignoredConnectionCnt++;
		}
	}

	// --------------- OVERRIDING OF AbstractTabularFileHandler ---------------

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {
		final String upstrLaneId = row[this.index(this.upstreamLaneLabel)];
		final String downstrLaneId = row[this.index(this.downstreamLaneLabel)];
		final double length = parseDouble(row[this.index(this.lengthLabel)]);
		this.addLaneConnection(upstrLaneId, downstrLaneId, length);
	}
}
