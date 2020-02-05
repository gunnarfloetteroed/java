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
package stockholm.wum;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.listener.ControlerListener;

import com.google.inject.Inject;

import modalsharecalibrator.ModeASCContainer;
import stockholm.StockholmConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WUMASCInstaller implements ControlerListener {

	@Inject
	WUMASCInstaller(final Config config, final ModeASCContainer ascs) {
		final StockholmConfigGroup sthlmConf = ConfigUtils.addOrGetModule(config, StockholmConfigGroup.class);
		ascs.setASC("car", sthlmConf.getCarASC());
		ascs.setASC("pt", sthlmConf.getPtASC());
		ascs.setASC("bike", sthlmConf.getBikeASC());
		ascs.setASC("walk", sthlmConf.getWalkASC());
	}
}
