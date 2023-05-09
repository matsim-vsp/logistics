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

package adapterTests;

import lsp.LSPCarrierResource;
import lsp.usecase.UsecaseUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;

import static org.junit.Assert.*;


public class CollectionResourceTest {

	//die Trackers sind ja erst ein Bestandteil des Scheduling bzw. Replanning und kommen hier noch nicht rein.
	//Man kann sie deshalb ja extra außerhalb des Builders einsetzen.

	private org.matsim.vehicles.VehicleType collectionType;
	private CarrierVehicle collectionCarrierVehicle;
	private Carrier collectionCarrier;
	private LSPCarrierResource carrierResource;
	private Id<Link> collectionLinkId;
	private CarrierCapabilities capabilities;

	@Before
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

		Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		collectionType = vehicleTypeBuilder.build();

		collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		collectionCarrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		capabilities = capabilitiesBuilder.build();
		collectionCarrier = CarrierUtils.createCarrier(carrierId);
		collectionCarrier.setCarrierCapabilities(capabilities);


		carrierResource = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(collectionCarrier, network)
				.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();
	}


	@Test
	public void testCollectionResource() {
		assertNotNull(carrierResource.getClientElements());
		assertTrue(carrierResource.getClientElements().isEmpty());
		assertTrue(LSPCarrierResource.class.isAssignableFrom(carrierResource.getClass()));
		assertSame(carrierResource.getCarrier(), collectionCarrier);
		assertSame(carrierResource.getEndLinkId(), collectionLinkId);
		assertSame(carrierResource.getStartLinkId(), collectionLinkId);
		assertNotNull(carrierResource.getSimulationTrackers());
		assertTrue(carrierResource.getSimulationTrackers().isEmpty());
		assertNotNull(carrierResource.getAttributes());
		assertTrue(carrierResource.getAttributes().isEmpty());
		assertSame(carrierResource.getStartLinkId(), collectionLinkId);
		if (carrierResource.getCarrier() == collectionCarrier) {
			assertSame(collectionCarrier.getCarrierCapabilities(), capabilities);
			assertTrue(Carrier.class.isAssignableFrom(collectionCarrier.getClass()));
			assertTrue(collectionCarrier.getPlans().isEmpty());
			assertNull(collectionCarrier.getSelectedPlan());
			assertTrue(collectionCarrier.getServices().isEmpty());
			assertTrue(collectionCarrier.getShipments().isEmpty());
			if (collectionCarrier.getCarrierCapabilities() == capabilities) {
				assertSame(capabilities.getFleetSize(), FleetSize.INFINITE);
				assertFalse(capabilities.getVehicleTypes().isEmpty());
				ArrayList<VehicleType> types = new ArrayList<>(capabilities.getVehicleTypes());
				if (types.size() == 1) {
					assertSame(types.get(0), collectionType);
					assertEquals(10, collectionType.getCapacity().getOther().intValue());
					assertEquals(0.0004, collectionType.getCostInformation().getCostsPerMeter(), 0.0);
					assertEquals(0.38, collectionType.getCostInformation().getCostsPerSecond(), 0.0);
					assertEquals(49, collectionType.getCostInformation().getFixedCosts(), 0.0);
					assertEquals((50 / 3.6), collectionType.getMaximumVelocity(), 0.0);

				}
				ArrayList<CarrierVehicle> vehicles = new ArrayList<>(capabilities.getCarrierVehicles().values());
				if (vehicles.size() == 1) {
					assertSame(vehicles.get(0), collectionCarrierVehicle);
					assertSame(collectionCarrierVehicle.getType(), collectionType);
					assertSame(collectionCarrierVehicle.getLinkId(), collectionLinkId);
				}
			}
		}
	}
}
