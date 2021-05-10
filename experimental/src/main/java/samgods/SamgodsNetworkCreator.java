/*
 * Copyright 2021 Gunnar Flötteröd
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
package samgods;

import static java.lang.Math.max;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.Units;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SamgodsNetworkCreator {

	// public static final double MIN_CAP_VEH_H = SamgodsVehiclesCreator.AVERAGE_LENGTH_M / 7.5; // one train per hour
	
	private final Network network;

	public SamgodsNetworkCreator(final String capacityFile, final String nodesFile, final String linksFile,
			final boolean includeUncapacitated) {

		this.network = NetworkUtils.createNetwork();
		this.network.setCapacityPeriod(3600.0);

		final Map<Tuple<Id<Node>, Id<Node>>, Double> nodeIds2cap_veh_h = new LinkedHashMap<>();
		{
			System.out.println("loading " + capacityFile);
			final TabularFileHandler capHandler = new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startCurrentDataRow() {
					final Id<Node> nodeAId = Id.createNodeId(this.getStringValue("A"));
					final Id<Node> nodeBId = Id.createNodeId(this.getStringValue("B"));
					final double cap_veh_h = Math.max(this.getDoubleValue("ORIGCAP") / 24.0, 1.0);
					nodeIds2cap_veh_h.put(new Tuple<>(nodeAId, nodeBId), cap_veh_h);
				}
			};
			final TabularFileParser capParser = new TabularFileParser();
			capParser.setDelimiterTags(new String[] { ";" });
			capParser.setOmitEmptyColumns(false);
			try {
				capParser.parse(capacityFile, capHandler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		{
			System.out.println("loading " + nodesFile);
			final TabularFileHandler nodesHandler = new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startCurrentDataRow() {
					final Id<Node> id = Id.createNodeId(this.getStringValue("\"N\""));
					final double x = this.getDoubleValue("\"X\"");
					final double y = this.getDoubleValue("\"Y\"");
					NetworkUtils.createAndAddNode(network, id, new Coord(x, y));
				}
			};
			final TabularFileParser nodesParser = new TabularFileParser();
			nodesParser.setDelimiterTags(new String[] { ";" });
			nodesParser.setOmitEmptyColumns(false);
			try {
				nodesParser.parse(nodesFile, nodesHandler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		{
			System.out.println("loading " + linksFile);
			final TabularFileHandler linksHandler = new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startCurrentDataRow() {
					if (this.getDoubleValue("\"MODE_L\"").intValue() == 2) {
						final Node nodeA = network.getNodes().get(Id.createNodeId(this.getStringValue("\"A\"")));
						final Node nodeB = network.getNodes().get(Id.createNodeId(this.getStringValue("\"B\"")));
						final Double cap_veh_h = nodeIds2cap_veh_h.get(new Tuple<>(nodeA.getId(), nodeB.getId()));
						if ((cap_veh_h != null) || includeUncapacitated) {
							double speed_m_s = Units.M_S_PER_KM_H * this.getDoubleValue("\"SPEED_1\"");
							double length_m = Units.M_PER_KM * this.getDoubleValue("\"DIST_KM\"");
							NetworkUtils.createAndAddLink(network, Id.createLinkId(nodeA.getId() + "_" + nodeB.getId()),
									nodeA, nodeB, length_m, speed_m_s, (cap_veh_h != null) ? cap_veh_h : 1000.0, 1.0,
									null, null);
						}
					}
				}
			};
			final TabularFileParser linksParser = new TabularFileParser();
			linksParser.setDelimiterTags(new String[] { ";" });
			linksParser.setOmitEmptyColumns(false);
			try {
				linksParser.parse(linksFile, linksHandler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		System.out.println("nodes: " + this.network.getNodes().size());
		System.out.println("links: " + this.network.getLinks().size());
	}

	public Network getNetwork() {
		return this.network;
	}
}
