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
package org.matsim.contrib.greedo.analysis;

import static floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader.MATSIM_NETWORK_TYPE;
import static floetteroed.utilities.networks.containerloaders.OpenStreetMapNetworkContainerLoader.OPENSTREETMAP_NETWORK_TYPE;
import static floetteroed.utilities.networks.containerloaders.SUMONetworkContainerLoader.SUMO_NETWORK_TYPE;

import java.awt.FileDialog;
import java.awt.Frame;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.ErrorMsgPrinter;
import floetteroed.utilities.config.Config;
import floetteroed.utilities.config.ConfigReader;
import floetteroed.utilities.networks.construction.NetworkContainer;
import floetteroed.utilities.networks.construction.NetworkPostprocessor;
import floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader;
import floetteroed.utilities.networks.containerloaders.OpenStreetMapNetworkContainerLoader;
import floetteroed.utilities.networks.containerloaders.SUMONetworkContainerLoader;
import floetteroed.utilities.visualization.LinkDataIO;
import floetteroed.utilities.visualization.MATSim2VisNetwork;
import floetteroed.utilities.visualization.NetVis;
import floetteroed.utilities.visualization.OpenStreetMap2VisNetwork;
import floetteroed.utilities.visualization.RenderableDynamicData;
import floetteroed.utilities.visualization.SUMO2VisNetwork;
import floetteroed.utilities.visualization.VisConfig;
import floetteroed.utilities.visualization.VisLink;
import floetteroed.utilities.visualization.VisNetwork;
import floetteroed.utilities.visualization.VisNetworkFactory;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class NetworkDisplay {

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		try {

			final NetworkContainer container = (new MATSimNetworkContainerLoader()).load(
					"/Users/GunnarF/NoBackup/data-workspace/ihop4/production-scenario/network.xml");
			final VisNetworkFactory factory = new VisNetworkFactory();
			factory.setNetworkPostprocessor(new MATSim2VisNetwork());
			final VisNetwork net = factory.newNetwork(container);

			final VisConfig visConfig = new VisConfig();
			final NetVis vis = new NetVis(visConfig, net, null);
			vis.run();

		} catch (Exception e) {
			ErrorMsgPrinter.toStdOut(e);
			ErrorMsgPrinter.toErrOut(e);
		}

		
		System.out.println("... DONE.");
	}
	
}

