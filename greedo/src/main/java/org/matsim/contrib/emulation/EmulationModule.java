/*
 * Greedo -- Equilibrium approximation for general-purpose multi-agent simulations.
 *
 * Copyright 2022 Gunnar Flötteröd
 * 
 *
 * This file is part of Greedo.
 *
 * Greedo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Greedo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Greedo.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@gmail.com
 *
 */
package org.matsim.contrib.emulation;

import java.util.Map;

import org.matsim.contrib.emulation.emulators.AgentEmulator;
import org.matsim.contrib.emulation.emulators.LegDecomposer;
import org.matsim.contrib.emulation.emulators.LegEmulator;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class EmulationModule extends AbstractModule {

	private final Map<String, Class<? extends LegEmulator>> mode2emulator;

	private final Map<String, Class<? extends LegDecomposer>> mode2decomposer;

	public EmulationModule(final Map<String, Class<? extends LegEmulator>> mode2emulator,
			final Map<String, Class<? extends LegDecomposer>> mode2decomposer) {
		this.mode2emulator = mode2emulator;
		this.mode2decomposer = mode2decomposer;
	}

	private <C> MapBinder<String, C> createMapBinder(Map<String, Class<C>> type2class) {
		final MapBinder<String, C> mapBinder = MapBinder.newMapBinder(super.binder(), new TypeLiteral<String>() {
		}, new TypeLiteral<C>() {
		});
		for (Map.Entry<String, Class<C>> entry : type2class.entrySet()) {
			mapBinder.addBinding(entry.getKey()).to(entry.getValue());
		}
		return mapBinder;
	}

	@Override
	public void install() {
		bind(AgentEmulator.class);
		{
			final MapBinder<String, LegEmulator> mapBinder = MapBinder.newMapBinder(super.binder(),
					new TypeLiteral<String>() {
					}, new TypeLiteral<LegEmulator>() {
					});
			for (Map.Entry<String, Class<? extends LegEmulator>> entry : this.mode2emulator.entrySet()) {
				mapBinder.addBinding(entry.getKey()).to(entry.getValue());
			}
		}
		// Did not find a way to do this generically, hence duplicating code...
		{
			final MapBinder<String, LegDecomposer> mapBinder = MapBinder.newMapBinder(super.binder(),
					new TypeLiteral<String>() {
					}, new TypeLiteral<LegDecomposer>() {
					});
			for (Map.Entry<String, Class<? extends LegDecomposer>> entry : this.mode2decomposer.entrySet()) {
				mapBinder.addBinding(entry.getKey()).to(entry.getValue());
			}

		}
	}
}
