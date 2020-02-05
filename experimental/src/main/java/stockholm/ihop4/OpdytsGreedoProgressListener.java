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
package stockholm.ihop4;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.matsim.contrib.opdyts.OpdytsProgressListener;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class OpdytsGreedoProgressListener implements OpdytsProgressListener {

	private final String fileName;

	// To store possibly unwritten lines in case of an IOException for later writing.
	private List<String> lines = new ArrayList<>();
	
	private int opdytsStage = 0;

	public OpdytsGreedoProgressListener(final String fileName) {
		this.fileName = fileName;
		final File oldFile = new File(fileName);
		if (oldFile.exists()) {
			oldFile.delete();
		}
	}

	private synchronized void msg(final String who, final Integer iteration, final String txt, final int indentationLevel) {
		this.msg(who + (iteration != null ? " (it." + iteration + ")" : "") + ": " + txt, indentationLevel);
	}

	private synchronized void msg(final String txt, final int indentationLevel) {
		StringBuffer lineBuffer = new StringBuffer();
		for (int i = 0; i < indentationLevel; i++) {
			lineBuffer.append("  ");
		}
		lineBuffer.append(txt);
		this.lines.add(lineBuffer.toString());
		try {
			FileUtils.writeLines(new File(this.fileName), Arrays.asList(lineBuffer.toString()), true);
			this.lines.clear();
		} catch (IOException e) {
			this.lines.add("[IOException will trying to write the previous line. Will try again ...");
		}
	}

	// GREEDO

	// @Override
	public void callToNotifyStartup_greedo(StartupEvent event) {
		msg("GREEDO", null, "call to notifyStartup", 1);
	}

	// @Override
	public void callToReset_greedo(int iteration) {
		msg("GREEDO", iteration, "reset", 1);
	}

	// @Override
	public void callToNotifyIterationEnds_greedo(IterationEndsEvent event) {
		msg("GREEDO", event.getIteration(), "call to notifyIterationEnds", 1);
	}

	// @Override
	public void setWeightOfHypotheticalReplanning(double weight) {
		msg("GREEDO", null, "setWeightOfHypotheticalReplanning(" + weight + ")", 2);
	}

	// @Override
	public void extractedLastPhysicalPopulationState(int iteration) {
		msg("GREEDO", iteration, "extracted last physical population state", 2);
	}

	// @Override
	public void observedLastPSimIterationWithinABlock(int iteration) {
		msg("GREEDO", iteration, "observed last psim iteration within a block", 2);
	}

	// @Override
	public void madeReplanningDecisions(int iteration) {
		msg("GREEDO", iteration, "made replanning decisions", 2);
	}

	// OPDYTS

	@Override
	public void callToNotifyStartup_opdyts(StartupEvent event) {
		msg("OPDYTS", null, "===== callToNotifyStartup, opdyts stage is (presumably) " + (this.opdytsStage++) + " =====", 0);
	}

	@Override
	public void callToNotifyBeforeMobsim_opdyts(BeforeMobsimEvent event) {
		msg("OPDYTS", event.getIteration(), "call to notifyBeforeMobsim", 1);
	}

	@Override
	public void callToNotifyAfterMobsim_opdyts(AfterMobsimEvent event) {
		msg("OPDYTS", event.getIteration(), "call to notifyAfterMobsim", 1);
	}

	@Override
	public void expectToBeBeforePhysicalMobsimRun(int iteration) {
		msg("OPDYTS", iteration, "expecting to be before physical mobsim", 2);
	}

	@Override
	public void beforeVeryFirstPhysicalMobsimRun(int iteration) {
		msg("OPDYTS", iteration, "before very first physical mobsim", 2);
	}

	@Override
	public void beforeOtherThanVeryFirstPhysicalMobsimRun(int iteration) {
		msg("OPDYTS", iteration, "before other than very first physical mobsim", 2);
	}

	@Override
	public void extractedStateAndCalledTrajectorySampler(int iteration) {
		msg("OPDYTS", iteration, "extracted state and called trajectory sampler", 2);
	}

	@Override
	public void clearedAndAddedMacroStateAnalyzers(int iteration) {
		msg("OPDYTS", iteration, "cleared and added macro state analyzers", 2);
	}

	@Override
	public void expectToBeAfterAPhysicalMobsimRun(int iteration) {
		msg("OPDYTS", iteration, "expecting to be after a physical mobsim run", 2);
	}

	@Override
	public void removedButDidNotClearMacroStateAnalyzers(int iteration) {
		msg("OPDYTS", iteration, "removed but did not clear macro state analyzers", 2);
	}
}
