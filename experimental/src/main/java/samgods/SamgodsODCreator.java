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

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SamgodsODCreator {

	private final Map<String, Set<Id<Node>>> zone2nodeIds;

	private final Map<Tuple<Id<Node>, Id<Node>>, Double> nodeIds2tonsPerDay;

	public SamgodsODCreator(final String nodeKeyFile, final String odFolder) {

		this.zone2nodeIds = new LinkedHashMap<>();
		{
			System.out.println("loading " + nodeKeyFile);
			final TabularFileHandler nodeKeyHandler = new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startCurrentDataRow() {
					final Id<Node> nodeId = Id.createNodeId(this.getStringValue("N"));
					final String zoneStr = this.getStringValue("ZONEID");
					final Set<Id<Node>> allNodesInZone = zone2nodeIds.getOrDefault(zoneStr, new LinkedHashSet<>());
					allNodesInZone.add(nodeId);
					zone2nodeIds.put(zoneStr, allNodesInZone);
				}
			};
			final TabularFileParser nodeKeyParser = new TabularFileParser();
			nodeKeyParser.setDelimiterTags(new String[] { ";" });
			nodeKeyParser.setOmitEmptyColumns(false);
			try {
				nodeKeyParser.parse(nodeKeyFile, nodeKeyHandler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		this.zone2nodeIds.entrySet().forEach(e -> {
			final String zoneStr = e.getKey();
			final int nodeCnt = e.getValue().size();
			if (nodeCnt != 1) {
				throw new RuntimeException(nodeCnt + " nodes in zone " + zoneStr);
			}
		});

		this.nodeIds2tonsPerDay = new LinkedHashMap<>();
		final File dir = new File(odFolder);
		for (String fileName : dir.list()) {
			if (fileName.contains("STD") && fileName.contains("Tonnes")) {
				final String fullFileName = new File(dir, fileName).getAbsolutePath();
				System.out.println("loading " + fullFileName);
				final TabularFileHandler tonsHandler = new TabularFileHandler() {
					@Override
					public void startDocument() {
					}

					@Override
					public String preprocess(String line) {
						return line;
					}

					@Override
					public void startRow(String[] row) {
						final Id<Node> fromNodeId = zone2nodeIds.get(row[0]).iterator().next();
						final Id<Node> toNodeId = zone2nodeIds.get(row[1]).iterator().next();
						final Tuple<Id<Node>, Id<Node>> nodeIds = new Tuple<>(fromNodeId, toNodeId);
						final double val_ton_day = Double.parseDouble(row[2]) / 365.0;
						nodeIds2tonsPerDay.put(nodeIds, val_ton_day + nodeIds2tonsPerDay.getOrDefault(nodeIds, 0.0));
						if ("1377".equals(fromNodeId.toString()) && "460".equals(toNodeId.toString())) {
							System.out.println("CRITICAL: " + val_ton_day);
						}
					
					
					}

					@Override
					public void endDocument() {
					}
				};
				final TabularFileParser tonsParser = new TabularFileParser();
				tonsParser.setDelimiterTags(new String[] { " ", ":" });
				tonsParser.setOmitEmptyColumns(false);
				try {
					tonsParser.parse(fullFileName, tonsHandler);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		System.out.println("number of loaded od pairs: " + nodeIds2tonsPerDay.size());
		System.out.println("Total loaded tons: " + nodeIds2tonsPerDay.values().stream().mapToDouble(t -> t).sum());
	}

	public final Map<String, Set<Id<Node>>> getZone2nodeIds() {
		return this.zone2nodeIds;
	};
	
	public Map<Tuple<Id<Node>, Id<Node>>, Double> getNodeIds2tonsPerDay() {
		return this.nodeIds2tonsPerDay;
	}
}
