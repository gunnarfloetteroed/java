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

import static stockholm.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.unquote;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * TODO Are there nodes that should be left out?
 * 
 * TODO Are there further node attributes that could be used?
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerNodesReader extends AbstractTabularFileHandlerWithHeaderLine {

	private final String nodeIdLabel = "ID";

	private final String longitudeLabel = "Longitude";

	private final String latitudeLabel = "Latitude";

	final Map<String, TransmodelerNode> id2node = new LinkedHashMap<String, TransmodelerNode>();

	TransmodelerNodesReader(final String nodesFileName) throws IOException {
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(nodesFileName, this);
	}

	@Override
	protected String preprocessColumnLabel(final String label) {
		return unquote(label);
	}

	@Override
	public void startDataRow(final String[] row) {
		final String nodeId = row[this.index(this.nodeIdLabel)];
		final TransmodelerNode node = new TransmodelerNode(nodeId,
				Double.parseDouble(row[this.index(this.longitudeLabel)]),
				Double.parseDouble(row[this.index(this.latitudeLabel)]));
		this.id2node.put(nodeId, node);
		System.out.println("read node: " + node);
	}
}
