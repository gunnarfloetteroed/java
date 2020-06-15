/*
 * Copyright 2020 Gunnar Flötteröd
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
package lebudgeteur;

import static java.lang.Double.POSITIVE_INFINITY;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Example {

	public static void main(String[] args) {

		// Plan 5 years ahead.
		LeBudgeteur lb = new LeBudgeteur(5);

		// Create project 1.
		lb.createProject("Project 1");
		// Planned funding: 700 per year over two years.
		lb.setTargetFundings("Project 1", 700, 700, 0, 0, 0);
		// No minimum work requirement on the project.
		lb.setMinConsumptions("Project 1", 0, 0, 0, 0, 0);
		// We can defer as much funding as we want during the first year but must not
		// defer beyond year two.
		lb.setMaxDeferrals("Project 1", POSITIVE_INFINITY, 0, 0, 0, 0);

		// Create project 2.
		lb.createProject("Project 2");
		// Planned funding: 500 per year over four years.
		lb.setTargetFundings("Project 2", 500, 500, 500, 500, 0);
		// No minimum work requirement on the project.
		lb.setMinConsumptions("Project 2", 0, 0, 0, 0, 0);
		// We can defer as much funding as we want during the first three years but must
		// not defer beyond year four.
		lb.setMaxDeferrals("Project 2", POSITIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_INFINITY, 0, 0);

		// Annual working time cost (salary plus overhead etc) in arbitrary monetary
		// unit.
		double annualCost = 1000.0;
		// Annual salary growth factor.
		double growthFactor = 1.03;
		double[] costSeries = lb.newSeries(annualCost, growthFactor);


		lb.solve(costSeries, 2020);

		// RESULT:
		
		// Year	Project 1(target)	Project 1(used)	Project 1(deferred)	Project 2(target)	Project 2(used)	Project 2(deferred)	salary	deficit
		// 2020	700	500	200	500	500	0	1000	0	
		// 2021	700	900	0	500	130	370	1030	0	
		// 2022	0	0	0	500	870	0	1061	191	
		// 2023	0	0	0	500	500	0	1093	593	
		// 2024	0	0	0	0	0	0	1126	1126	

	}
}
