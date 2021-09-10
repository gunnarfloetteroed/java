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
package vienna.teaching;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TwoRoutes {

	public static void main(String[] args) {

		int numberOfDays = 100;
		double adjustmentRate;
		
		double capacity1_veh = 1000.0; // vehicles
		double capacity2_veh = 1000.0;

		double totalFlow = 2000;
		double flow1_veh = totalFlow;
		double flow2_veh = 0.0;

		double delay1_min;
		double delay2_min;

		System.out.println("flow1 flow2 delay1 delay2 adjustmentRate");

		for (int day = 0; day < numberOfDays; day++) {

			// MSA step size control
			adjustmentRate = 1.0 / (day + 1);
			
			// Give flows on each route, compute the resulting delay.
			delay1_min = flow1_veh / capacity1_veh;
			delay2_min = flow2_veh / capacity2_veh;

			// Given the delay on each route, travelers choose the better one.
			double todaysBestFlow1_veh;
			double todaysBestFlow2_veh;
			
			if (delay1_min < delay2_min) {
				todaysBestFlow1_veh = totalFlow;
				todaysBestFlow2_veh = 0;
			} else {
				todaysBestFlow1_veh = 0;
				todaysBestFlow2_veh = totalFlow;
			}
			
			// "learning rate"
			flow1_veh = adjustmentRate * todaysBestFlow1_veh + (1.0 - adjustmentRate) * flow1_veh;
			flow2_veh = adjustmentRate * todaysBestFlow2_veh + (1.0 - adjustmentRate) * flow2_veh;			

			// output
			System.out.println((flow1_veh + " " + flow2_veh + " " + delay1_min + " " + delay2_min + " " + adjustmentRate).replace('.', ','));
		}
	}
}
