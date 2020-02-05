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
package nonpropassignment;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Test {

	public static void main(String[] args) {

		System.out.println("STARTED ..");

		int linkCnt = 3;
		double[] point = new double[0];

		for (int basisSize : new int[] { 1, 2, 4, 8, 16 }) {

			int oldBasisSize = point.length / 2 / linkCnt;
			
			double[] newPoint = new double[point.length + 2 * (basisSize - oldBasisSize) * linkCnt];
			System.arraycopy(point, 0, newPoint, 0, point.length / 2);
			System.arraycopy(point, point.length / 2, newPoint, newPoint.length / 2, point.length / 2);
			for (int i = 0; i < (basisSize - oldBasisSize) * linkCnt; i++) {
				newPoint[point.length / 2 + i] = basisSize;
				newPoint[newPoint.length / 2 + point.length / 2 + i] = basisSize;
			}
			point = newPoint;

			String line = "";
			for (double val : newPoint) {
				line += ((int) val + " ");
			}
			try {
				FileUtils.writeStringToFile(new File("test.out"), line + "\n", true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				throw new RuntimeException(e);
			}

		}

		System.out.println(".. DONE");

	}

}
