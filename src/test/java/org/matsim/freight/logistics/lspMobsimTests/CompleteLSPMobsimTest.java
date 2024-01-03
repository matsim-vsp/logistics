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

package org.matsim.freight.logistics.lspMobsimTests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.resourceImplementations.collectionCarrier.CollectionCarrierUtils;
import org.matsim.freight.logistics.resourceImplementations.distributionCarrier.DistributionCarrierUtils;
import org.matsim.freight.logistics.resourceImplementations.mainRunCarrier.MainRunCarrierUtils;
import org.matsim.freight.logistics.resourceImplementations.transshipmentHub.TranshipmentHubUtils;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.junit.jupiter.api.BeforeEach;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CompleteLSPMobsimTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();
	private LSP completeLSP;

	@BeforeEach
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);


		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();


		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> collectionVehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder collectionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(collectionVehicleTypeId);
		collectionVehicleTypeBuilder.setCapacity(10);
		collectionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		collectionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		collectionVehicleTypeBuilder.setFixCost(49);
		collectionVehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		org.matsim.vehicles.VehicleType collectionType = collectionVehicleTypeBuilder.build();

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


		LSPResource collectionResource = CollectionCarrierUtils.CollectionCarrierResourceBuilder.newInstance(collectionCarrier, network)
				.setCollectionScheduler(CollectionCarrierUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();

		LogisticChainElement collectionElement  = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("CollectionElement", LogisticChainElement.class))
				.setResource(collectionResource)
				.build();

		TranshipmentHubUtils.TranshipmentHubSchedulerBuilder firstReloadingSchedulerBuilder = TranshipmentHubUtils.TranshipmentHubSchedulerBuilder.newInstance();
		firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
		firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
		Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");

		TranshipmentHubUtils.TransshipmentHubBuilder firstTransshipmentHubBuilder = TranshipmentHubUtils.TransshipmentHubBuilder.newInstance(firstTransshipmentHubId, firstTransshipmentHub_LinkId, scenario);
		firstTransshipmentHubBuilder.setTransshipmentHubScheduler(firstReloadingSchedulerBuilder.build());
		LSPResource firstTranshipmentHubResource = firstTransshipmentHubBuilder.build();

		Id<LogisticChainElement> firstHubElementId = Id.create("FirstHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder firstHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(firstHubElementId);
		firstHubElementBuilder.setResource(firstTranshipmentHubResource);
		LogisticChainElement firstHubElement = firstHubElementBuilder.build();

		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(mainRunVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		org.matsim.vehicles.VehicleType mainRunType = mainRunVehicleTypeBuilder.build();

		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId, mainRunType);

		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarriersUtils.createCarrier(mainRunCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);


		LSPResource mainRunResource  = MainRunCarrierUtils.MainRunCarrierResourceBuilder.newInstance(mainRunCarrier, network)
				.setMainRunCarrierScheduler(MainRunCarrierUtils.createDefaultMainRunCarrierScheduler())
				.setFromLinkId(Id.createLinkId("(4 2) (4 3)"))
				.setToLinkId(Id.createLinkId("(14 2) (14 3)"))
				.build();

		LogisticChainElement mainRunElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("MainRunElement", LogisticChainElement.class))
				.setResource(mainRunResource)
				.build();

		TranshipmentHubUtils.TranshipmentHubSchedulerBuilder secondSchedulerBuilder = TranshipmentHubUtils.TranshipmentHubSchedulerBuilder.newInstance();
		secondSchedulerBuilder.setCapacityNeedFixed(10);
		secondSchedulerBuilder.setCapacityNeedLinear(1);

		Id<LSPResource> secondTransshipmentHubId = Id.create("TranshipmentHub2", LSPResource.class);
		Id<Link> secondTransshipmentHub_LinkId = Id.createLinkId("(14 2) (14 3)");

		TranshipmentHubUtils.TransshipmentHubBuilder secondTransshipmentHubBuilder = TranshipmentHubUtils.TransshipmentHubBuilder.newInstance(secondTransshipmentHubId, secondTransshipmentHub_LinkId, scenario);

		secondTransshipmentHubBuilder.setTransshipmentHubScheduler(secondSchedulerBuilder.build());
		LSPResource secondTranshipmentHubResource = secondTransshipmentHubBuilder.build();

		Id<LogisticChainElement> secondHubElementId = Id.create("SecondHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder secondHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(secondHubElementId);
		secondHubElementBuilder.setResource(secondTranshipmentHubResource);
		LogisticChainElement secondHubElement = secondHubElementBuilder.build();

		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder dsitributionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId);
		dsitributionVehicleTypeBuilder.setCapacity(10);
		dsitributionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		dsitributionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		dsitributionVehicleTypeBuilder.setFixCost(49);
		dsitributionVehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		org.matsim.vehicles.VehicleType distributionType = dsitributionVehicleTypeBuilder.build();

		Id<Link> distributionLinkId = Id.createLinkId("(14 2) (14 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, distributionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(distributionType);
		capabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = capabilitiesBuilder.build();
		Carrier carrier = CarriersUtils.createCarrier(distributionCarrierId);
		carrier.setCarrierCapabilities(distributionCapabilities);

		LSPResource distributionResource = DistributionCarrierUtils.DistributionCarrierResourceBuilder.newInstance(carrier, network)
				.setDistributionScheduler(DistributionCarrierUtils.createDefaultDistributionCarrierScheduler())
				.setLocationLinkId(distributionLinkId)
				.build();

		Id<LogisticChainElement> distributionElementId = Id.create("DistributionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder distributionBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(distributionElementId);
		distributionBuilder.setResource(distributionResource);
		LogisticChainElement distributionElement = distributionBuilder.build();

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
		LogisticChain completeSolution = completeSolutionBuilder.build();

		ShipmentAssigner assigner = ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);
		completePlan.addLogisticChain(completeSolution);

		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		completeLSPBuilder.setInitialPlan(completePlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);
		resourcesList.add(firstTranshipmentHubResource);
		resourcesList.add(mainRunResource);
		resourcesList.add(secondTranshipmentHubResource);
		resourcesList.add(distributionResource);


		LogisticChainScheduler simpleScheduler = ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		completeLSP = completeLSPBuilder.build();

		List<Link> linkList = new LinkedList<>(network.getLinks().values());
		Random rand = new Random(1);
		for (int i = 1; i < 2; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			int capacityDemand = 1 + rand.nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, rand);
				Link pendingToLink = linkList.get(0);
				if ((pendingToLink.getFromNode().getCoord().getX() <= 18000 &&
						pendingToLink.getFromNode().getCoord().getY() <= 4000 &&
						pendingToLink.getFromNode().getCoord().getX() >= 14000 &&
						pendingToLink.getToNode().getCoord().getX() <= 18000 &&
						pendingToLink.getToNode().getCoord().getY() <= 4000 &&
						pendingToLink.getToNode().getCoord().getX() >= 14000)) {
					builder.setToLinkId(pendingToLink.getId());
					break;
				}

			}

			while (true) {
				Collections.shuffle(linkList, rand);
				Link pendingFromLink = linkList.get(0);
				if (pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
						pendingFromLink.getToNode().getCoord().getY() <= 4000) {
					builder.setFromLinkId(pendingFromLink.getId());
					break;
				}

			}

			TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60);
			LSPShipment shipment = builder.build();
			completeLSP.assignShipmentToLSP(shipment);
		}
		completeLSP.scheduleLogisticChains();

		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(completeLSP);
		LSPs lsps = new LSPs(lspList);

		LSPUtils.addLSPs(scenario, lsps);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LSPModule());
			}
		});


		config.controller().setFirstIteration(0);
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();
	}

	@Test
	public void testFirstReloadLSPMobsim() {
		for (LSPShipment shipment : completeLSP.getShipments()) {
			assertFalse(shipment.getShipmentLog().getPlanElements().isEmpty());
			ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<>(ShipmentUtils.getOrCreateShipmentPlan(completeLSP.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			scheduleElements.sort(ShipmentUtils.createShipmentPlanElementComparator());
			ArrayList<ShipmentPlanElement> logElements = new ArrayList<>(shipment.getShipmentLog().getPlanElements().values());
			logElements.sort(ShipmentUtils.createShipmentPlanElementComparator());

			for (ShipmentPlanElement scheduleElement : scheduleElements) {
				ShipmentPlanElement logElement = logElements.get(scheduleElements.indexOf(scheduleElement));
				assertEquals(scheduleElement.getElementType(), logElement.getElementType());
				assertSame(scheduleElement.getResourceId(), logElement.getResourceId());
				assertSame(scheduleElement.getLogisticChainElement(), logElement.getLogisticChainElement());
				assertEquals(scheduleElement.getStartTime(), logElement.getStartTime(), 300);
			}
		}
	}

	@Test
	public void compareEvents(){
		MatsimTestUtils.assertEqualEventsFiles(utils.getClassInputDirectory() + "output_events.xml.gz", utils.getOutputDirectory() + "output_events.xml.gz" );
	}
}

