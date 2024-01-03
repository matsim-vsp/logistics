/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.adapterTests;

import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.resourceImplementations.mainRunCarrier.MainRunCarrierUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;


public class MainRunResourceTest {

	private org.matsim.vehicles.VehicleType mainRunType;
	private Id<Link> fromLinkId;
	private Id<Link> toLinkId;
	private CarrierVehicle carrierVehicle;
	private CarrierCapabilities capabilities;
	private Carrier carrier;
	private LSPCarrierResource mainRunResource;

	@BeforeEach
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();


		Id<Carrier> carrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(30);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0008);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(120);
		vehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		mainRunType = vehicleTypeBuilder.build();

		toLinkId = Id.createLinkId("(14 2) (14 3)");
		fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("MainRunVehicle");
		carrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, fromLinkId, mainRunType);


		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(mainRunType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		capabilities = capabilitiesBuilder.build();
		carrier = CarriersUtils.createCarrier(carrierId);
		carrier.setCarrierCapabilities(capabilities);

		mainRunResource = MainRunCarrierUtils.MainRunCarrierResourceBuilder.newInstance(carrier, network)
				.setMainRunCarrierScheduler(MainRunCarrierUtils.createDefaultMainRunCarrierScheduler())
				.setFromLinkId(Id.createLinkId("(4 2) (4 3)")).setToLinkId(Id.createLinkId("(14 2) (14 3)"))
				.build();

	}

	@Test
	public void testMainRunResource() {
		assertNotNull(mainRunResource.getClientElements());
		assertTrue(mainRunResource.getClientElements().isEmpty());
		assertTrue(LSPCarrierResource.class.isAssignableFrom(mainRunResource.getClass()));
		if (LSPCarrierResource.class.isAssignableFrom(mainRunResource.getClass())) {
//			assertTrue(Carrier.class.isAssignableFrom(mainRunResource.getClassOfResource()));
			assertSame(mainRunResource.getCarrier(), carrier);
		}
		assertSame(mainRunResource.getEndLinkId(), toLinkId);
		assertSame(mainRunResource.getStartLinkId(), fromLinkId);
		assertNotNull(mainRunResource.getSimulationTrackers());
		assertTrue(mainRunResource.getSimulationTrackers().isEmpty());
		assertNotNull(mainRunResource.getAttributes());
		assertTrue(mainRunResource.getAttributes().isEmpty());
		if (mainRunResource.getCarrier() == carrier) {
			assertSame(carrier.getCarrierCapabilities(), capabilities);
			assertTrue(Carrier.class.isAssignableFrom(carrier.getClass()));
			assertTrue(carrier.getPlans().isEmpty());
			assertNull(carrier.getSelectedPlan());
			assertTrue(carrier.getServices().isEmpty());
			assertTrue(carrier.getShipments().isEmpty());
			if (carrier.getCarrierCapabilities() == capabilities) {
				assertSame(capabilities.getFleetSize(), FleetSize.INFINITE);
				assertFalse(capabilities.getVehicleTypes().isEmpty());
				ArrayList<VehicleType> types = new ArrayList<>(capabilities.getVehicleTypes());
				if (types.size() == 1) {
					assertSame(types.get(0), mainRunType);
					assertEquals(30, mainRunType.getCapacity().getOther().intValue());
					assertEquals(0.0008, mainRunType.getCostInformation().getCostsPerMeter(), 0.0);
					assertEquals(0.38, mainRunType.getCostInformation().getCostsPerSecond(), 0.0);
					assertEquals(120, mainRunType.getCostInformation().getFixedCosts(), 0.0);
					assertEquals((50 / 3.6), mainRunType.getMaximumVelocity(), 0.0);
				}
				ArrayList<CarrierVehicle> vehicles = new ArrayList<>(capabilities.getCarrierVehicles().values());
				if (vehicles.size() == 1) {
					assertSame(vehicles.get(0), carrierVehicle);
					assertSame(carrierVehicle.getType(), mainRunType);
					assertSame(carrierVehicle.getLinkId(), fromLinkId);
				}
			}
		}


	}
}
