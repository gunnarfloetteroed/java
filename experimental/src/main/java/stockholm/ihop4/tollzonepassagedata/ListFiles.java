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
package stockholm.ihop4.tollzonepassagedata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import stockholm.ihop4.IhopConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class ListFiles {

	public static void main(String[] args) {

		final Config config = ConfigUtils.loadConfig(args[0]);
		final String pathStr = ConfigUtils.addOrGetModule(config, IhopConfigGroup.class).getTollZoneCountsFolder();

		List<String> files;
		try {
			files = Files.list(Paths.get(pathStr)).map(e -> e.toString()).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		for (String fileName : files) {
			System.out.println(fileName);
		}

	}

}
