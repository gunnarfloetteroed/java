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
package stockholm.ihop2.integration;

// import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class PlainRunner {

	public static void main(String[] args) {

		String configFileName = "./input/matsim-config.xml";
		Config config = ConfigUtils.loadConfig(configFileName);

		// TODO Gunnar had to change this, otherwise it infers u-turns that should not be!
//		LaneDefinitonsV11ToV20Converter.main(new String[] {
//				"./input/lanes.xml", "./input/lanes20.xml",
//				"./input/network-plain.xml" });
		// please use the 'new' lanes format directly. look e.g. into Transmodeler2MATSimNetwork for how to create it. tthunig, oct'17

		config.network().setLaneDefinitionsFile("./input/lanes20.xml");
		config.qsim().setUseLanes(true);
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		config.controler().setLinkToLinkRoutingEnabled(true);
		config.controler()
				.setOverwriteFileSetting(
						OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

//		Controler controler = new Controler(config);
//		controler.addOverridingModule(new SignalsModule()); // TODO NEEDED?
//
//		controler.run();

	}

}
