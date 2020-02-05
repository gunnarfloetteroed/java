package org.matsim.contrib.opdyts.macrostate;

/**
 * Computes recursively the average value of an integer counting process over a
 * time dimension.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RecursiveCountAverage {

	// -------------------- MEMBERS --------------------

	private double initialTime;

	private double lastTime;

	private int lastValue;

	private double averageValue;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @param initialTime
	 *            the time from which on one wishes to average
	 */
	public RecursiveCountAverage(final double initialTime) {
		this.initialTime = initialTime;
		this.lastTime = initialTime;
		this.lastValue = 0;
		this.averageValue = 0.0;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void resetTime(final double time) {
		this.initialTime = time;
		this.lastTime = time;
		this.averageValue = this.lastValue;
	}

	public void advanceTo(final double time) {
		if (time < this.lastTime) {
			throw new IllegalArgumentException("current time " + time + " is before last time " + this.lastTime);
		}
		final double innoWeight = (time - this.lastTime) / Math.max(1e-8, time - this.initialTime);
		this.averageValue = (1.0 - innoWeight) * this.averageValue + innoWeight * this.lastValue;
		this.lastTime = time;
	}

	public void inc(final double time) {
		this.advanceTo(time);
		this.lastValue++;
	}

	public void dec(final double time) {
		if (this.lastValue == 0) {
			throw new RuntimeException("Cannot decrease a zero counting value further.");
		}
		this.advanceTo(time);
		this.lastValue--;
	}

	public double getAverage() {
		return this.averageValue;
	}

	public double getInitialTime() {
		return this.initialTime;
	}

	public double getFinalTime() {
		return this.lastTime;
	}

}
