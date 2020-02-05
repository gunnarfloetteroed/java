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
package nonpropassignment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import floetteroed.utilities.Tuple;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SymmetricLinkPairIndexer {

	// -------------------- MEMBERS --------------------

	private Map<Id<Link>, Integer> linkId2Index = new LinkedHashMap<>();

	private TupleIndexer<Id<Link>> linkIdPairIndexer = new TupleIndexer<>();

	// -------------------- CONSTRUCTION --------------------

	public SymmetricLinkPairIndexer(final Network net) {
		// Establish a unique link ordering.
		for (Link link : net.getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				this.linkId2Index.put(link.getId(), this.linkId2Index.size());
			}
		}
		// Index link pairs, indifferently to within-pair ordering.
		for (Map.Entry<Id<Link>, Integer> entry : this.linkId2Index.entrySet()) {
			final Link link = net.getLinks().get(entry.getKey());
			final int linkIndex = entry.getValue();
			for (Link adjLink : adjacentCarLinks(link)) {
				final int adjLinkIndex = this.linkId2Index.get(adjLink.getId());
				this.linkIdPairIndexer.add(this.newKey(link.getId(), linkIndex, adjLink.getId(), adjLinkIndex));
			}
		}
	}

	// -------------------- INTERNALS --------------------

	private static Set<Link> adjacentCarLinks(final Link link) {
		final Set<Link> result = new LinkedHashSet<>();
		for (Node node : new Node[] { link.getFromNode(), link.getToNode() }) {
			result.addAll(node.getInLinks().values());
			result.addAll(node.getOutLinks().values());
		}
		result.remove(link);
		return result;
	}
	
	private Tuple<Id<Link>, Id<Link>> newKey(final Id<Link> linkId1, final int link1Index, final Id<Link> linkId2,
			final int link2Index) {
		if (link1Index < link2Index) {
			return new Tuple<>(linkId1, linkId2);
		} else {
			return new Tuple<>(linkId2, linkId1);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	// SINGLE LINKS

	public int getNumberOfLinks() {
		return this.linkId2Index.size();
	}

	public Map<Id<Link>, Integer> getLink2IndexView() {
		return Collections.unmodifiableMap(this.linkId2Index);
	}

	public Integer getLinkIndex(final Id<Link> linkId) {
		return this.linkId2Index.get(linkId);
	}

	// LINK PAIRS

	public int getNumberOfLinkPairs() {
		return this.linkIdPairIndexer.size();
	}

	public Map<Tuple<Id<Link>, Id<Link>>, Integer> getLinkPair2IndexView() {
		return this.linkIdPairIndexer.getTuple2IndexView();
	}

	public Integer getLinkPairIndex(final Id<Link> fromLinkId, final int fromLinkIndex, final Id<Link> toLinkId,
			final int toLinkIndex) {
		return this.linkIdPairIndexer.getIndex(this.newKey(fromLinkId, fromLinkIndex, toLinkId, toLinkIndex));
	}

	public Integer getLinkPairIndex(final Id<Link> fromLinkId, final Id<Link> toLinkId) {
		return this.getLinkPairIndex(fromLinkId, this.linkId2Index.get(fromLinkId), toLinkId,
				this.linkId2Index.get(toLinkId));
	}

	public Integer getLinkPairIndex(final Link fromLink, final Link toLink) {
		return this.getLinkPairIndex(fromLink.getId(), this.linkId2Index.get(fromLink.getId()), toLink.getId(),
				this.linkId2Index.get(toLink.getId()));
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Map.Entry<Id<Link>, Integer> entry : this.linkId2Index.entrySet()) {
			result.append(entry.getKey() + " <-> " + entry.getValue() + "\n");
		}
		result.append(this.linkIdPairIndexer.toString());
		return result.toString();
	}
}
