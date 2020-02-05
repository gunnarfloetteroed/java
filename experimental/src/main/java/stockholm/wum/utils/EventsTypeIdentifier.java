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
package stockholm.wum.utils;

import java.util.LinkedHashSet;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class EventsTypeIdentifier implements EventHandler {

	private final LinkedHashSet<Class<?>> eventClasses = new LinkedHashSet<>();

	
	
	public static void main(String[] args) {
		
		
		EventsManager manager = EventsUtils.createEventsManager();
		EventsUtils.readEvents(manager, "output_events.xml.gz");

	}

}
