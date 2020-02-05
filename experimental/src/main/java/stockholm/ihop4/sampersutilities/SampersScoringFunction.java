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
package stockholm.ihop4.sampersutilities;

import java.util.LinkedList;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class SampersScoringFunction implements ScoringFunction {

	// -------------------- CONSTANTS --------------------

	private final Person person;

	protected /* Vanilla PT implementation is in a subclass. */ final SampersTourUtilityFunction utlFct;

	// -------------------- MEMBERS --------------------

	private final LinkedList<Activity> homeActivities = new LinkedList<>();

	private final LinkedList<SampersTour> tours = new LinkedList<>();

	private double score = 0.0;

	protected boolean stuck = false;

	private double money_SEK = 0.0;

	// -------------------- CONSTRUCTION --------------------

	public SampersScoringFunction(final Person person, final SampersTourUtilityFunction utlFct) {
		this.person = person;
		this.utlFct = utlFct;
	}

	// -------------------- INTERNALS --------------------

	private Attributes getActivityAttributes(final String type) {
		for (PlanElement pe : this.person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (type.equals(act.getType())) {
					return act.getAttributes();
				}
			}
		}
		return null;
	}

	// --------------- IMPLEMENTATION OF SampersUtilityFunction ---------------

	@Override
	public void handleActivity(final Activity activity) {

		if (this.homeActivities.size() == 0) {
			// first activity of the day
			this.homeActivities.add(activity);
		} else if (this.tours.size() > 0) {
			if (!this.tours.getLast().isComplete()) {
				// tour activity
				this.tours.getLast().addActivity(activity, this.getActivityAttributes(activity.getType()));
			} else if (this.homeActivities.size() == this.tours.size()) {
				// intermediate home activity
				this.homeActivities.add(activity);
			} else {
				throw new RuntimeException("Cannot add activity.");
			}
		} else {
			throw new RuntimeException("Cannot add activity.");
		}
	}

	@Override
	public void handleLeg(final Leg leg) {
		if (this.homeActivities.size() > 0) {
			if ((this.tours.size() == 0) || this.tours.getLast().isComplete()) {
				this.tours.add(new SampersTour());
			}
			this.tours.getLast().addLeg(leg);
			this.tours.getLast().addMoney_SEK(this.money_SEK);
			this.money_SEK = 0;
		} else {
			throw new RuntimeException("Cannot add leg.");
		}
	}

	@Override
	public void agentStuck(double time) {
		this.stuck = true;
	}

	@Override
	public void addMoney(double amount) {
		this.money_SEK += amount;
	}

	@Override
	public void handleEvent(Event event) {
	}

	@Override
	public void finish() {

		if (this.money_SEK > 0) {
			throw new RuntimeException("Unprocessed money events of value " + this.money_SEK + " SEK.");
		}

		if (this.stuck) {
			this.score = this.utlFct.getStuckScore(this.person);
		} else {
			this.score = 0.0;
			for (SampersTour tour : this.tours) {
				this.score += this.utlFct.getUtility(tour, this.person);
			}
		}
	}

	@Override
	public double getScore() {
		return this.score;
	}
}
