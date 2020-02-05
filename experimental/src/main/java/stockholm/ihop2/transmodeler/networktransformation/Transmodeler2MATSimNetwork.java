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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.LanesWriter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import floetteroed.utilities.Units;
import stockholm.saleem.StockholmTransformationFactory;

/**
 * Turns a mesoscopic Transmodeler network (defined through a set of files in
 * csv format) into a MATSim network (in xml format). Also creates MATSim lane
 * information that reflects the allowed turning moves in Transmodeler, and a
 * roadpricing input file.
 *
 * TODO: Detailed lane information in MATSim is missing (everything is still
 * single-lane); the only information taken over from Transmodeler are the
 * turning moves. Makes probably a difference if MATSim also does the network
 * loading.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Transmodeler2MATSimNetwork {

	public static final String TMPATHID_ATTR = "TMPathID";

	public static final String TMFROMNODEID_ATTR = "TMFromNodeID";

	public static final String TMLINKDIRPREFIX_ATTR = "TMLinkDirPrefix";

	// -------------------- STATIC PACKAGE HELPERS --------------------

	static String unquote(final String original) {
		String result = original;
		if ((result.length() > 0) && "\"".equals(result.substring(0, 1))) {
			result = result.substring(1, result.length());
		}
		if ((result.length() > 0) && "\"".equals(result.substring(result.length() - 1, result.length()))) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	static enum DIR {
		AB, BA
	};

	static String newUnidirectionalId(final String bidirectionalId, final DIR dir) {
		return (bidirectionalId + "_" + dir);
	}

	static String newUnidirectionalLinkId(final String bidirectionalId, final DIR dir, final String abDir,
			final String baDir) {
		if (DIR.AB.equals(dir)) {
			return (bidirectionalId + "_" + abDir);
		} else if (DIR.BA.equals(dir)) {
			return (bidirectionalId + "_" + baDir);
		} else {
			throw new RuntimeException("unknown direction " + dir + " for link " + bidirectionalId);
		}
	}

	// -------------------- MEMBERS --------------------

	private final String tmNodesFileName;

	private final String tmLinksFileName;

	private final String tmSegmentsFileName;

	private final String tmLanesFileName;

	private final String tmLaneConnectorsFileName;

	private final String matsimPlainNetworkFileName;

	private final String matsimFullFileName;

	private final String linkAttributesFileName;

	private final String matsimLanesFile;

	private final String matsimRoadPricingFileName;

	// -------------------- CONSTRUCTION --------------------

	private double totalNetworkArea(final Network network) {
		double result = 0;
		for (Link link : network.getLinks().values()) {
			result += link.getLength() * link.getNumberOfLanes();
		}
		return result;
	}

	public Transmodeler2MATSimNetwork(final String tmNodesFileName, final String tmLinksFileName,
			final String tmSegmentsFileName, final String tmLanesFileName, final String tmLaneConnectorsFileName,
			final String matsimPlainNetworkFileName, final String matsimFullFileName,
			final String linkAttributesFileName, final String matsimLanesFile, final String matsimRoadPricingFileName) {
		this.tmNodesFileName = tmNodesFileName;
		this.tmLinksFileName = tmLinksFileName;
		this.tmSegmentsFileName = tmSegmentsFileName;
		this.tmLanesFileName = tmLanesFileName;
		this.tmLaneConnectorsFileName = tmLaneConnectorsFileName;
		this.matsimPlainNetworkFileName = matsimPlainNetworkFileName;
		this.matsimFullFileName = matsimFullFileName;
		this.linkAttributesFileName = linkAttributesFileName;
		this.matsimLanesFile = matsimLanesFile;
		this.matsimRoadPricingFileName = matsimRoadPricingFileName;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run() throws IOException {

		final SegmentAnalyzer segmentAnalyzer = new SegmentAnalyzer();
		// Danvikstull infart 1 22626
		segmentAnalyzer.addRelevantSegment("22626", "Danvikstull infart 1");
		// Danvikstull utfart 2 110210
		segmentAnalyzer.addRelevantSegment("110210", "Danvikstull utfart 2");
		// Skansbron infart 3 6114
		segmentAnalyzer.addRelevantSegment("6114", "Skansbron infart 3");
		// Skansbron utfart 4 6114
		segmentAnalyzer.addRelevantSegment("6114", "Skansbron utfart 4");
		// Skanstullbron infart 5 80449
		segmentAnalyzer.addRelevantSegment("80449", "Skanstullbron infart 5");
		// Skanstullbron utfart 6 28480
		segmentAnalyzer.addRelevantSegment("28480", "Skanstullbron utfart 6");
		// Johanneshovbron infart 7 9216
		segmentAnalyzer.addRelevantSegment("9216", "Johanneshovbron infart 7");
		// Johanneshovbron utfart 8 18170
		segmentAnalyzer.addRelevantSegment("18170", "Johanneshovbron utfart 8");
		// Liljeholmsbron infart 9 25292
		segmentAnalyzer.addRelevantSegment("25292", "Liljeholmsbron infart 9");
		// Liljeholmsbron utfart 10 2453
		segmentAnalyzer.addRelevantSegment("2453", "Liljeholmsbron utfart 10");
		// Stora Essingen infart 11 74188
		segmentAnalyzer.addRelevantSegment("74188", "Stora Essingen infart 11");
		// Stora Essingen utfart 12 53064
		segmentAnalyzer.addRelevantSegment("53064", "Stora Essingen utfart 12");
		// Stora Essingen infart 13 51930
		segmentAnalyzer.addRelevantSegment("51930", "Stora Essingen infart 13");
		// Lilla Essingen utfart 14 44566
		segmentAnalyzer.addRelevantSegment("44566", "Lilla Essingen utfart 14");
		// Drottningholmsvägen infart från Bromma 15 92866
		segmentAnalyzer.addRelevantSegment("92866", "Drottningholmsvägen infart från Bromma 15");
		// Drottningholmsvägen utfart mot Bromma 16 77626
		segmentAnalyzer.addRelevantSegment("77626", "Drottningholmsvägen utfart mot Bromma 16");
		// Drottningholmsvägen infart från EL 17 127169
		segmentAnalyzer.addRelevantSegment("127169", "Drottningholmsvägen infart från EL 17");
		// Drottningholmsvägen utfart mot EL 18 124799
		segmentAnalyzer.addRelevantSegment("124799", "Drottningholmsvägen utfart mot EL 18");
		// Essingeleden södergående från Bromma 19 119586
		segmentAnalyzer.addRelevantSegment("119586", "Essingeleden södergående från Bromma 19");
		// Essingleden norrgående mot Bromma 20 120732
		segmentAnalyzer.addRelevantSegment("120732", "Essingleden norrgående mot Bromma 20");
		// Essingeleden södergående 21 74191
		segmentAnalyzer.addRelevantSegment("74191", "Essingeleden södergående 21");
		// Essingeleden norrgående 22 52300
		segmentAnalyzer.addRelevantSegment("52300", "Essingeleden norrgående 22");
		// Kristineberg Avfart Essingeleden S 23 95249
		segmentAnalyzer.addRelevantSegment("95249", "Kristineberg Avfart Essingeleden S 23");
		// Kristineberg Påfart Essingeleden N 24 52301
		segmentAnalyzer.addRelevantSegment("52301", "Kristineberg Påfart Essingeleden N 24");
		// Kristineberg Avfart Essingeleden N 25 128236
		segmentAnalyzer.addRelevantSegment("128236", "Kristineberg Avfart Essingeleden N 25");
		// Kristineberg Påfart Essingeleden S 26 128234
		segmentAnalyzer.addRelevantSegment("128234", "Kristineberg Påfart Essingeleden S 26");
		// Klarastrandsleden infart 27 122017
		segmentAnalyzer.addRelevantSegment("122017", "Klarastrandsleden infart 27");
		// Klarastrandsleden utfart 28 122017
		segmentAnalyzer.addRelevantSegment("122017", "Klarastrandsleden utfart 28");
		// Ekelundsbron infart 29 121908
		segmentAnalyzer.addRelevantSegment("121908", "Ekelundsbron infart 29");
		// Ekelundsbron utfart 30 121908
		segmentAnalyzer.addRelevantSegment("121908", "Ekelundsbron utfart 30");
		// Tomtebodavägen infart NY 31 52416
		segmentAnalyzer.addRelevantSegment("52416", "Tomtebodavägen infart NY 31");
		// Tomtebodavägen utfart NY 32 52416
		segmentAnalyzer.addRelevantSegment("52416", "Tomtebodavägen utfart NY 32");
		// Solnavägen infart 33 108353
		segmentAnalyzer.addRelevantSegment("108353", "Solnavägen infart 33");
		// Solnavägen utfart 34 113763
		segmentAnalyzer.addRelevantSegment("113763", "Solnavägen utfart 34");
		// Norrtull Sveavägen utfart 36 101350
		segmentAnalyzer.addRelevantSegment("101350", "Norrtull Sveavägen utfart 36");
		// Norrtull tillfällig väg 38 128229
		segmentAnalyzer.addRelevantSegment("128229", "Norrtull tillfällig väg 38");
		// Norra stationsgatan utfart 39 34743
		segmentAnalyzer.addRelevantSegment("34743", "Norra stationsgatan utfart 39");
		// Ekhagen Avfart E18S 40 54215
		segmentAnalyzer.addRelevantSegment("54215", "Ekhagen Avfart E18S 40");
		// Ekhagen Påfart E18N 41 116809
		segmentAnalyzer.addRelevantSegment("116809", "Ekhagen Påfart E18N 41");
		// Ekhagen Avfart E18N 42 74955
		segmentAnalyzer.addRelevantSegment("74955", "Ekhagen Avfart E18N 42");
		// Ekhagen Påfart E18S 43 35466
		segmentAnalyzer.addRelevantSegment("35466", "Ekhagen Påfart E18S 43");
		// Frescati infart 44 56348
		segmentAnalyzer.addRelevantSegment("56348", "Frescati infart 44");
		// Frescati utfart 45 56348
		segmentAnalyzer.addRelevantSegment("56348", "Frescati utfart 45");
		// Universitetet infart 46 127555
		segmentAnalyzer.addRelevantSegment("127555", "Universitetet infart 46");
		// Universitetet utfart 47 42557
		segmentAnalyzer.addRelevantSegment("42557", "Universitetet utfart 47");
		// Roslagstull infart 48 129132
		segmentAnalyzer.addRelevantSegment("129132", "Roslagstull infart 48");
		// Roslagstull utfart 49 127146
		segmentAnalyzer.addRelevantSegment("127146", "Roslagstull utfart 49");
		// Värtan - från E20/Hjorthagsv Öst mot Tpl Värtan In 50 128187
		segmentAnalyzer.addRelevantSegment("128187", "Värtan - från E20/Hjorthagsv Öst mot Tpl Värtan In 50");
		// Värtan - till E20/Hjorthagsv Väst från Tpl Värtan Ut 51 128192
		segmentAnalyzer.addRelevantSegment("128192", "Värtan - till E20/Hjorthagsv Väst från Tpl Värtan Ut 51");
		// Värtan - från E20/Hjorthagsv/Lidingöv mot S. Hamnv In 52 128204
		segmentAnalyzer.addRelevantSegment("128204", "Värtan - från E20/Hjorthagsv/Lidingöv mot S. Hamnv In 52");
		// Värtan - till E20/Hjorthagsv/Lidingöv fr. S. Hamnv Ut 53 128219
		segmentAnalyzer.addRelevantSegment("128219", "Värtan - till E20/Hjorthagsv/Lidingöv fr. S. Hamnv Ut 53");
		// Värtan - från E20/Hjorthagsv Öst mot Södra Hamnv In 54 128215
		segmentAnalyzer.addRelevantSegment("128215", "Värtan - från E20/Hjorthagsv Öst mot Södra Hamnv In 54");
		// Ropsten Infart till Norra Hamnvägen 55 23370
		segmentAnalyzer.addRelevantSegment("23370", "Ropsten Infart till Norra Hamnvägen 55");
		// Ropsten Utfart från Norra Hamnvägen 56 43117
		segmentAnalyzer.addRelevantSegment("43117", "Ropsten Utfart från Norra Hamnvägen 56");
		// Ropsten Infart mot Hjorthagen 57 125185
		segmentAnalyzer.addRelevantSegment("125185", "Ropsten Infart mot Hjorthagen 57");
		// Ropsten Utfart från Hjorthagen 58 58297
		segmentAnalyzer.addRelevantSegment("58297", "Ropsten Utfart från Hjorthagen 58");

		/*
		 * (1) Read all Transmodeler data.
		 */

		final TransmodelerNodesReader nodesReader = new TransmodelerNodesReader(this.tmNodesFileName);
		final TransmodelerLinksReader linksReader = new TransmodelerLinksReader(this.tmLinksFileName,
				nodesReader.id2node);
		final TransmodelerSegmentsReader segmentsReader = new TransmodelerSegmentsReader(this.tmSegmentsFileName,
				linksReader.id2link, segmentAnalyzer);
		final TransmodelerLaneReader lanesReader = new TransmodelerLaneReader(this.tmLanesFileName,
				segmentsReader.unidirSegmentId2link);
		final TransmodelerLaneConnectorReader connectorsReader = new TransmodelerLaneConnectorReader(
				this.tmLaneConnectorsFileName, lanesReader.upstrLaneId2link, lanesReader.downstrLaneId2link);

		System.out.println();
		System.out.println("------------------------------------------------------------");
		System.out.println("TRANSMODELER FILES SUMMARY");
		System.out.println("Loaded " + nodesReader.id2node.size() + " nodes.");
		System.out.println("Loaded " + linksReader.id2link.size() + " links; the number of "
				+ (linksReader.ignoreCircularLinks ? "ignored" : "included") + " circular links is "
				+ linksReader.getCircularLinksCnt() + ".");
		System.out.println("Loaded " + segmentsReader.unidirSegmentId2link.size() + " segments; ignored "
				+ segmentsReader.getIgnoredSegmentCnt() + " segments.");
		System.out.println("Loaded " + (lanesReader.upstrLaneId2link.size() + lanesReader.downstrLaneId2link.size())
				+ " lanes; ignored " + lanesReader.getIgnoredLaneCnt() + " lanes.");
		System.out.println("Loaded " + (connectorsReader.getLoadedConnectionCnt()) + " lane connections; ignored "
				+ connectorsReader.getIgnoredConnectionCnt() + " connections.");
		System.out.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (2a) Create a MATSim network and additional object attributes.
		 */

		final Network matsimNetwork = NetworkUtils.createNetwork();
		final NetworkFactory matsimNetworkFactory = matsimNetwork.getFactory();
		final ObjectAttributes linkAttributes = new ObjectAttributes();

		/*
		 * (2b) Create and add all MATSim nodes.
		 */

		final CoordinateTransformation coordinateTransform = StockholmTransformationFactory.getCoordinateTransformation(
				StockholmTransformationFactory.WGS84, StockholmTransformationFactory.WGS84_SWEREF99);

		for (TransmodelerNode transmodelerNode : nodesReader.id2node.values()) {

			final Coord coord = coordinateTransform.transform(
					new Coord(1e-6 * transmodelerNode.getLongitude(), 1e-6 * transmodelerNode.getLatitude()));

			final Node matsimNode = matsimNetworkFactory.createNode(Id.create(transmodelerNode.getId(), Node.class),
					coord);
			matsimNetwork.addNode(matsimNode);
		}

		/*
		 * (2c) Create and add all MATSim links.
		 */

		// final Set<String> unknownLinkTypes = new LinkedHashSet<String>();
		// final Set<String> linksWithUnknownTypes = new
		// LinkedHashSet<String>();

		for (TransmodelerLink transmodelerLink : linksReader.id2link.values()) {

			final Node matsimFromNode = matsimNetwork.getNodes()
					.get(Id.create(transmodelerLink.getFromNode().getId(), Node.class));
			final Node matsimToNode = matsimNetwork.getNodes()
					.get(Id.create(transmodelerLink.getToNode().getId(), Node.class));

			final Link matsimLink = matsimNetworkFactory.createLink(Id.create(transmodelerLink.getId(), Link.class),
					matsimFromNode, matsimToNode);

			LinkTypeParameters parameters = LinkTypeParameters.TYPE2PARAMS.get(transmodelerLink.getType());
			if (parameters == null) {
				parameters = LinkTypeParameters.TYPE2PARAMS.get(LinkTypeParameters.UNDEFINED);
			}

			// if (parameters != null) {
			final SortedSet<TransmodelerSegment> segments = transmodelerLink.segments;
			double lanes = 0.0;
			double length = 0.0;
			for (TransmodelerSegment segment : segments) {
				lanes += segment.getLanes() * segment.getLength();
				length += segment.getLength();
			}
			lanes /= length;
			matsimLink.setNumberOfLanes(lanes);
			matsimLink.setLength(length * Units.M_PER_KM);
			matsimLink.setCapacity(parameters.flowCapacity_veh_hLane * lanes);
			matsimLink.setFreespeed(parameters.maxSpeed_km_h * Units.M_S_PER_KM_H);
			matsimNetwork.addLink(matsimLink);
			// } else {
			// unknownLinkTypes.add(transmodelerLink.getType());
			// linksWithUnknownTypes.add(transmodelerLink.getId());
			// }

			linkAttributes.putAttribute(matsimLink.getId().toString(), TMPATHID_ATTR,
					transmodelerLink.getBidirectionalId());
			linkAttributes.putAttribute(matsimLink.getId().toString(), TMFROMNODEID_ATTR,
					transmodelerLink.getFromNode().getId());
			linkAttributes.putAttribute(matsimLink.getId().toString(), TMLINKDIRPREFIX_ATTR,
					DIR.AB.equals(transmodelerLink.getDirection()) ? "" : "-");
		}

		NetworkWriter networkWriter = new NetworkWriter(matsimNetwork);
		networkWriter.write(this.matsimFullFileName);

		System.out.println();
		System.out.println("------------------------------------------------------------");
		System.out.println("RAW MATSIM NETWORK STATISTICS");
		System.out.println("(This network is saved as " + this.matsimFullFileName + ".)");
		System.out.println("Number of nodes: " + matsimNetwork.getNodes().size());
		System.out.println("Number of links: " + matsimNetwork.getLinks().size());
		// System.out.println("Unknown (and ignored) link types: "
		// + unknownLinkTypes);
		// System.out.println("Ignored links with unknown types: "
		// + linksWithUnknownTypes);
		System.out.println("Total network area (link lengths times lanes): " + totalNetworkArea(matsimNetwork));
		System.out.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (2d) Clean up the network and save it to file.
		 */

		NetworkCleaner cleaner = new NetworkCleaner();
		cleaner.run(matsimNetwork);

		System.out.println();
		System.out.println("------------------------------------------------------------");
		System.out.println("MATSIM NETWORK STATISTICS AFTER NETWORK CLEANING");
		System.out.println("(This network is not saved to file.)");
		System.out.println("Number of nodes: " + matsimNetwork.getNodes().size());
		System.out.println("Number of links: " + matsimNetwork.getLinks().size());
		System.out.println("Total network area (link lengths times lanes): " + totalNetworkArea(matsimNetwork));
		System.out.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (2e) Identify the largest connected component given the turning moves.
		 */

		final Set<Link> removedLinks = new LinkedHashSet<Link>(matsimNetwork.getLinks().values());
		removedLinks.removeAll(ConnectedLinks.connectedLinks(matsimNetwork, linksReader.id2link));
		for (Link removedLink : removedLinks) {
			matsimNetwork.removeLink(removedLink.getId());
		}

		/*
		 * (2f) Clean the network once again and save it to file.
		 */

		cleaner = new NetworkCleaner();
		cleaner.run(matsimNetwork);

		networkWriter = new NetworkWriter(matsimNetwork);
		networkWriter.write(this.matsimPlainNetworkFileName);

		final ObjectAttributesXmlWriter linkAttributesWriter = new ObjectAttributesXmlWriter(linkAttributes);
		linkAttributesWriter.writeFile(this.linkAttributesFileName);

		System.out.println();
		System.out.println("------------------------------------------------------------");
		System.out.println("MATSIM NETWORK STATISTICS AFTER DEADEND REMOVAL AND REPEATED CLEANING");
		System.out.println("(This network is saved as " + this.matsimPlainNetworkFileName + ".)");
		System.out.println("Number of nodes: " + matsimNetwork.getNodes().size());
		System.out.println("Number of links: " + matsimNetwork.getLinks().size());
		System.out.println("Total network area (link lengths times lanes): " + totalNetworkArea(matsimNetwork));
		System.out.println("------------------------------------------------------------");
		System.out.println();

		/*
		 * (2d) Write out lanes.
		 */
		final Lanes lanedefs = LanesUtils.createLanesContainer();
		LanesFactory lanesFactory = lanedefs.getFactory();

		for (Node node : matsimNetwork.getNodes().values()) {

			for (Link matsimInLink : node.getInLinks().values()) {
				final TransmodelerLink tmInLink = linksReader.id2link.get(matsimInLink.getId().toString());

				final Lane lane = lanesFactory
						.createLane(Id.create(matsimInLink.getId().toString() + "-singleLane", Lane.class));
				lane.setNumberOfRepresentedLanes(matsimInLink.getNumberOfLanes());
				lane.setStartsAtMeterFromLinkEnd(matsimInLink.getLength() / 2.0);
				for (TransmodelerLink tmOutLink : tmInLink.downstreamLink2turnLength.keySet()) {
					final Id<Link> outLinkId = Id.create(tmOutLink.getId(), Link.class);
					if (node.getOutLinks().containsKey(outLinkId)) {
						lane.addToLinkId(outLinkId);
					}
				}

				if ((lane.getToLinkIds() != null) && (!lane.getToLinkIds().isEmpty())) {
					final LanesToLinkAssignment lanesToLink = lanesFactory
							.createLanesToLinkAssignment(matsimInLink.getId());
					lanesToLink.addLane(lane);
					lanedefs.addLanesToLinkAssignment(lanesToLink);
				} else {
					throw new RuntimeException("impossible state after preprocessing ...");
				}
			}
		}
		LanesUtils.createOriginalLanesAndSetLaneCapacities(matsimNetwork, lanedefs);

		final LanesWriter laneWriter = new LanesWriter(lanedefs);
		laneWriter.write(this.matsimLanesFile);

		/*
		 * Write out road pricing file.
		 * 
		 * TODO There exists a writer for this.
		 */
		final PrintWriter tollWriter = new PrintWriter(this.matsimRoadPricingFileName);

		tollWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		tollWriter.println("<!DOCTYPE roadpricing SYSTEM " + "\"http://www.matsim.org/files/dtd/roadpricing_v1.dtd\">");

		tollWriter.println("<roadpricing type=\"link\" name=\"abc\">");

		tollWriter.println("\t<links>");
		for (Link link : matsimNetwork.getLinks().values()) {
			if (linksReader.id2tollLink.containsKey(link.getId().toString())) {
				tollWriter.println("\t\t<link id=\"" + link.getId() + "\">");
				tollWriter.println("\t\t\t<cost start_time=\"06:30\" " + "end_time=\"07:00\" " + "amount=\"10.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"07:00\" " + "end_time=\"07:30\" " + "amount=\"15.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"07:30\" " + "end_time=\"08:30\" " + "amount=\"20.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"08:30\" " + "end_time=\"09:00\" " + "amount=\"15.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"09:00\" " + "end_time=\"15:30\" " + "amount=\"10.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"15:30\" " + "end_time=\"16:00\" " + "amount=\"15.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"16:00\" " + "end_time=\"17:30\" " + "amount=\"20.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"17:30\" " + "end_time=\"18:00\" " + "amount=\"15.00\"/>");
				tollWriter.println("\t\t\t<cost start_time=\"18:00\" " + "end_time=\"18:30\" " + "amount=\"10.00\"/>");
				tollWriter.println("\t\t</link>");
			}
		}
		tollWriter.println("\t</links>");

		tollWriter.println("</roadpricing>");

		tollWriter.flush();
		tollWriter.close();
		
		if (segmentAnalyzer != null) {
			System.out.println("");
			System.out.println("TOLL STATIONS:");
			System.out.println("");
			System.out.println(segmentAnalyzer.getSummary(matsimNetwork));			
		}
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) throws IOException {

		// final String inputPath = "./ihop2/network-input/";
		final String inputPath = "/Users/GunnarF/OneDrive - VTI/My Data/ihop2/ihop2-data/network-input/";
		final String nodesFile = inputPath + "Nodes.csv";
		final String segmentsFile = inputPath + "Segments.csv";
		final String lanesFile = inputPath + "Lanes.csv";
		final String laneConnectorsFile = inputPath + "Lane Connectors.csv";
		final String linksFile = inputPath + "Links.csv";

		// final String outputPath = "./ihop2/network-output/";
		final String outputPath = "/Users/GunnarF/NoBackup/data-workspace/ihop4/network-output/";
		final String matsimPlainFile = outputPath + "network.xml";
		final String matsimFullFile = outputPath + "network-raw.xml";
		final String linkAttributesFile = outputPath + "link-attributes.xml";
		final String matsimLanesFile = outputPath + "lanes.xml";
		final String matsimRoadPricingFile = outputPath + "toll.xml";

		System.out.println("STARTED ...");

		final Transmodeler2MATSimNetwork tm2MATSim = new Transmodeler2MATSimNetwork(nodesFile, linksFile, segmentsFile,
				lanesFile, laneConnectorsFile, matsimPlainFile, matsimFullFile, linkAttributesFile, matsimLanesFile,
				matsimRoadPricingFile);
		tm2MATSim.run();

		System.out.println("... DONE");
	}
}
