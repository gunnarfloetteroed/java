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
package stockholm.ihop4.resampling;

import org.matsim.api.core.v01.population.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface Alternative {

	public double getSampersOnlyScore();

	public double getSampersTimeScore();

	public double getMATSimTimeScore();

	// TODO NEW
	// public void setMATSimTimeScore(double score);
	public void updateMATSimTimeScore(double score, double innovationWeight);
	
	public double getSampersChoiceProbability();

	public EpsilonDistribution getEpsilonDistribution();

	public double getSampersEpsilonRealization();
	
	public void setSampersEpsilonRealization(double eps);
	
	public Plan getMATSimPlan();
	
}
