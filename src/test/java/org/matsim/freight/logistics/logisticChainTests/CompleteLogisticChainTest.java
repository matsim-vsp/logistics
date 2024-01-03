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

package org.matsim.freight.logistics.logisticChainTests;

import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPUtils;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.resourceImplementations.collectionCarrier.CollectionCarrierUtils;
import org.matsim.freight.logistics.resourceImplementations.distributionCarrier.DistributionCarrierUtils;
import org.matsim.freight.logistics.resourceImplementations.mainRunCarrier.MainRunCarrierUtils;
import org.matsim.freight.logistics.resourceImplementations.transshipmentHub.TranshipmentHubUtils;
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

public class CompleteLogisticChainTest {

	private LogisticChainElement collectionElement;
	private LogisticChainElement firstHubElement;
	private LogisticChainElement mainRunElement;
	private LogisticChainElement secondHubElement;
	private LogisticChainElement distributionElement;
	private LogisticChain logisticChain;

	@BeforeEach
	public void initialize() {

		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> collectionVehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder collectionVehicleTypeBuilder = CarrierVehicleType.Builder
				.newInstance(collectionVehicleTypeId);
		collectionVehicleTypeBuilder.setCapacity(10);
		collectionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		collectionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		collectionVehicleTypeBuilder.setFixCost(49);
		collectionVehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		VehicleType collectionType = collectionVehicleTypeBuilder.build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> collectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle collectionCarrierVehicle = CarrierVehicle.newInstance(collectionVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities.Builder collectionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		collectionCapabilitiesBuilder.addType(collectionType);
		collectionCapabilitiesBuilder.addVehicle(collectionCarrierVehicle);
		collectionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities collectionCapabilities = collectionCapabilitiesBuilder.build();
		Carrier collectionCarrier = CarriersUtils.createCarrier(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);

		CollectionCarrierUtils.CollectionCarrierResourceBuilder collectionResourceBuilder = CollectionCarrierUtils.CollectionCarrierResourceBuilder
				.newInstance(collectionCarrier, network);
		collectionResourceBuilder.setCollectionScheduler(CollectionCarrierUtils.createDefaultCollectionCarrierScheduler());
		collectionResourceBuilder.setLocationLinkId(collectionLinkId);

		Id<LogisticChainElement> collectionElementId = Id.create("CollectionElement",
				LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder collectionBuilder = LSPUtils.LogisticChainElementBuilder
				.newInstance(collectionElementId);
		collectionBuilder.setResource(collectionResourceBuilder.build());
		collectionElement = collectionBuilder.build();

		TranshipmentHubUtils.TranshipmentHubSchedulerBuilder firstReloadingSchedulerBuilder = TranshipmentHubUtils.TranshipmentHubSchedulerBuilder.newInstance();
		firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
		firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);

		Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
		Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");

		TranshipmentHubUtils.TransshipmentHubBuilder firstTransshipmentHubBuilder = TranshipmentHubUtils.TransshipmentHubBuilder.newInstance(firstTransshipmentHubId,
				firstTransshipmentHub_LinkId, scenario)
				.setTransshipmentHubScheduler(firstReloadingSchedulerBuilder.build());

		Id<LogisticChainElement> firstHubElementId = Id.create("FirstHubElement", LogisticChainElement.class);
		firstHubElement = LSPUtils.LogisticChainElementBuilder
				.newInstance(firstHubElementId)
				.setResource(firstTransshipmentHubBuilder.build())
				.build();

		VehicleType mainRunType  = CarrierVehicleType.Builder
				.newInstance(collectionVehicleTypeId)
				.setCapacity(30)
				.setCostPerDistanceUnit(0.0002)
				.setCostPerTimeUnit(0.38)
				.setFixCost(120).setMaxVelocity(50 / 3.6)
				.build();

		final Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("MainRunVehicle"), fromLinkId, mainRunType);

		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = collectionCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarriersUtils.createCarrier(collectionCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);

		LSPResource mainRunResource  = MainRunCarrierUtils.MainRunCarrierResourceBuilder.newInstance(mainRunCarrier, network)
				.setMainRunCarrierScheduler(MainRunCarrierUtils.createDefaultMainRunCarrierScheduler())
				.setFromLinkId(Id.createLinkId("(4 2) (4 3)"))
				.setToLinkId(Id.createLinkId("(14 2) (14 3)"))
				.build();

		Id<LogisticChainElement> mainRunElementId = Id.create("MainRunElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder mainRunBuilder = LSPUtils.LogisticChainElementBuilder
				.newInstance(mainRunElementId);
		mainRunBuilder.setResource(mainRunResource);
		mainRunElement = mainRunBuilder.build();

		TranshipmentHubUtils.TranshipmentHubSchedulerBuilder secondSchedulerBuilder = TranshipmentHubUtils.TranshipmentHubSchedulerBuilder.newInstance();
		secondSchedulerBuilder.setCapacityNeedFixed(10);
		secondSchedulerBuilder.setCapacityNeedLinear(1);

		Id<LSPResource> secondTransshipmentHubId = Id.create("TranshipmentHub2", LSPResource.class);
		Id<Link> secondTransshipmentHub_LinkId = Id.createLinkId("(14 2) (14 3)");

		TranshipmentHubUtils.TransshipmentHubBuilder secondTransshipmentHubBuilder = TranshipmentHubUtils.TransshipmentHubBuilder.newInstance(secondTransshipmentHubId,
				secondTransshipmentHub_LinkId, scenario);
		secondTransshipmentHubBuilder.setTransshipmentHubScheduler(secondSchedulerBuilder.build());

		Id<LogisticChainElement> secondHubElementId = Id.create("SecondHubElement",
				LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder secondHubElementBuilder = LSPUtils.LogisticChainElementBuilder
				.newInstance(secondHubElementId);
		secondHubElementBuilder.setResource(secondTransshipmentHubBuilder.build());
		secondHubElement = secondHubElementBuilder.build();

		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder dsitributionVehicleTypeBuilder = CarrierVehicleType.Builder
				.newInstance(distributionVehicleTypeId);
		dsitributionVehicleTypeBuilder.setCapacity(10);
		dsitributionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		dsitributionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		dsitributionVehicleTypeBuilder.setFixCost(49);
		dsitributionVehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		VehicleType distributionType = dsitributionVehicleTypeBuilder.build();

		Id<Link> distributionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, distributionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(distributionType);
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = capabilitiesBuilder.build();
		Carrier carrier = CarriersUtils.createCarrier(distributionCarrierId);
		carrier.setCarrierCapabilities(distributionCapabilities);

		final LSPResource distributionCarrierResource = DistributionCarrierUtils.DistributionCarrierResourceBuilder
				.newInstance(carrier, network)
				.setDistributionScheduler(DistributionCarrierUtils.createDefaultDistributionCarrierScheduler())
				.setLocationLinkId(distributionLinkId)
				.build();

		Id<LogisticChainElement> distributionElementId = Id.create("DistributionElement", LogisticChainElement.class);
		distributionElement = LSPUtils.LogisticChainElementBuilder
				.newInstance(distributionElementId)
				.setResource(distributionCarrierResource)
				.build();

		collectionElement.connectWithNextElement(firstHubElement);
		firstHubElement.connectWithNextElement(mainRunElement);
		mainRunElement.connectWithNextElement(secondHubElement);
		secondHubElement.connectWithNextElement(distributionElement);

		Id<LogisticChain> solutionId = Id.create("SolutionId", LogisticChain.class);
		LSPUtils.LogisticChainBuilder completeSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(solutionId);
		completeSolutionBuilder.addLogisticChainElement(collectionElement);
		completeSolutionBuilder.addLogisticChainElement(firstHubElement);
		completeSolutionBuilder.addLogisticChainElement(mainRunElement);
		completeSolutionBuilder.addLogisticChainElement(secondHubElement);
		completeSolutionBuilder.addLogisticChainElement(distributionElement);
		logisticChain = completeSolutionBuilder.build();

	}

	@Test
	public void testCompleteLogisticChain() {
		assertNotNull(logisticChain.getSimulationTrackers());
		assertTrue(logisticChain.getSimulationTrackers().isEmpty());
		assertNotNull(logisticChain.getAttributes());
		assertTrue(logisticChain.getAttributes().isEmpty());
		assertNull(logisticChain.getLSP());
		assertNotNull(logisticChain.getShipmentIds());
		assertTrue(logisticChain.getShipmentIds().isEmpty());
		assertEquals(5, logisticChain.getLogisticChainElements().size());
		ArrayList<LogisticChainElement> elements = new ArrayList<>(logisticChain.getLogisticChainElements());
		for (LogisticChainElement element : elements) {
			if (elements.indexOf(element) == 0) {
				assertNull(element.getPreviousElement());
			}
			if (elements.indexOf(element) == (elements.size() - 1)) {
				assertNull(element.getNextElement());
			}
//				assertSame(element.getEmbeddingContainer(), solution );
		}
		assertNull(collectionElement.getPreviousElement());
		assertSame(collectionElement.getNextElement(), firstHubElement);
		assertSame(firstHubElement.getPreviousElement(), collectionElement);
		assertSame(firstHubElement.getNextElement(), mainRunElement);
		assertSame(mainRunElement.getPreviousElement(), firstHubElement);
		assertSame(mainRunElement.getNextElement(), secondHubElement);
		assertSame(secondHubElement.getPreviousElement(), mainRunElement);
		assertSame(secondHubElement.getNextElement(), distributionElement);
		assertSame(distributionElement.getPreviousElement(), secondHubElement);
		assertNull(distributionElement.getNextElement());
	}


}
