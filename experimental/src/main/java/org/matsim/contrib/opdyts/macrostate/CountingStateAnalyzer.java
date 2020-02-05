package org.matsim.contrib.opdyts.macrostate;

import static java.lang.Math.min;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;

/**
 * Keeps track of a (part of a) state vector that is composed of counts (for
 * instance, vehicles on a road or passengers waiting at a stop).
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CountingStateAnalyzer<L extends Object> {

	// -------------------- MEMBERS --------------------

	/*
	 * Initial state is "not locked". When locked, no writing with
	 * registerIncrease(..), registerDecrease(..) is allowed.
	 * 
	 * Gets locked when data is accessed through getCount(..).
	 * 
	 * Gets again unlocked when reset(..) is called.
	 */
	private boolean locked = false;

	private final DynamicData<L> counts;

	private final Map<L, RecursiveCountAverage> location2avgCnt = new LinkedHashMap<>();

	private int lastCompletedBin = -1;

	// -------------------- CONSTRUCTION --------------------

	public CountingStateAnalyzer(final int startTime_s, final int binSize_s, final int binCnt) {
		this.counts = new DynamicData<>(startTime_s, binSize_s, binCnt);
		// this.reset(-1);
		this.reset();
	}

	public CountingStateAnalyzer(final TimeDiscretization timeDiscr) {
		this(timeDiscr.getStartTime_s(), timeDiscr.getBinSize_s(), timeDiscr.getBinCnt());
	}

	// -------------------- INTERNALS --------------------

	private void checkNotLocked() {
		if (this.locked) {
			throw new RuntimeException(this.getClass().getSimpleName() + " is locked and cannot accept more data.");
		}
	}

	private int lastCompletedBinEndTime() {
		return this.counts.getStartTime_s() + (this.lastCompletedBin + 1) * this.counts.getBinSize_s();
	}

	private void completeBins(final int lastBinToComplete) {
		while (this.lastCompletedBin < lastBinToComplete) {
			this.lastCompletedBin++; // is now zero or larger
			final int lastCompletedBinEndTime = this.lastCompletedBinEndTime();
			for (Map.Entry<L, RecursiveCountAverage> link2avgEntry : this.location2avgCnt.entrySet()) {
				link2avgEntry.getValue().advanceTo(lastCompletedBinEndTime);
				this.counts.put(link2avgEntry.getKey(), this.lastCompletedBin, link2avgEntry.getValue().getAverage());
				link2avgEntry.getValue().resetTime(lastCompletedBinEndTime);
			}
		}
	}

	private void completeBinsUntilTime(final int time_s) {
		final int lastBinToComplete = this.counts.bin(time_s) - 1;
		this.completeBins(min(lastBinToComplete, this.counts.getBinCnt() - 1));
	}

	private RecursiveCountAverage avg(final L link) {
		RecursiveCountAverage avg = this.location2avgCnt.get(link);
		if (avg == null) {
			avg = new RecursiveCountAverage(this.lastCompletedBinEndTime());
			this.location2avgCnt.put(link, avg);
		}
		return avg;
	}

	// -------------------- SETTERS --------------------

	public void reset() {
		this.locked = false;
		this.counts.clear();
		this.location2avgCnt.clear();
		this.lastCompletedBin = -1;
	}

	public void registerIncrease(final L location, final int time_s) {
		this.checkNotLocked();
		this.completeBinsUntilTime(time_s);
		this.avg(location).inc(time_s);
	}

	public void registerDecrease(final L location, final int time_s) {
		this.checkNotLocked();
		this.completeBinsUntilTime(time_s);
		this.avg(location).dec(time_s);
	}

	public void finalizeAndLock() {
		this.locked = true;
		this.completeBins(this.counts.getBinCnt() - 1);
	}

	// -------------------- GETTERS --------------------

	public Set<L> observedLinkSetView() {
		this.finalizeAndLock();
		return Collections.unmodifiableSet(this.counts.keySet());
	}

	public double getCount(final L link, final int bin) {
		this.finalizeAndLock();
		return this.counts.getBinValue(link, bin);
	}
}
