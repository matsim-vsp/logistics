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

import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.resourceImplementations.mainRunCarrier.MainRunCarrierUtils;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.junit.Before;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.Test;
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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MainRunOnlyLSPMobsimTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();
	private LSP lsp;

	@Before
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);


		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();

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


		Id<LogisticChain> solutionId = Id.create("SolutionId", LogisticChain.class);
		LSPUtils.LogisticChainBuilder completeSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(solutionId);

		completeSolutionBuilder.addLogisticChainElement(mainRunElement);
		LogisticChain completeSolution = completeSolutionBuilder.build();

		ShipmentAssigner assigner = ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);
		completePlan.addLogisticChain(completeSolution);

		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		completeLSPBuilder.setInitialPlan(completePlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();

		resourcesList.add(mainRunResource);

		LogisticChainScheduler simpleScheduler = ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();

		List<Link> linkList = new LinkedList<>(network.getLinks().values());

		for (int i = 1; i < 2; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			int capacityDemand = 1 + MatsimRandom.getRandom().nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, MatsimRandom.getRandom());
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
				Collections.shuffle(linkList, MatsimRandom.getRandom());
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
			lsp.assignShipmentToLSP(shipment);
		}
		lsp.scheduleLogisticChains();

		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(lsp);
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
		for (LSPShipment shipment : lsp.getShipments()) {
			assertFalse(shipment.getShipmentLog().getPlanElements().isEmpty());
			assertEquals(ShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().size(), shipment.getShipmentLog().getPlanElements().size());
			ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<>(ShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().values());
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
