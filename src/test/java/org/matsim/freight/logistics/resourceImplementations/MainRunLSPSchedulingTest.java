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

package org.matsim.freight.logistics.resourceImplementations;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TranshipmentHubSchedulerBuilder;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils.TransshipmentHubBuilder;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

public class MainRunLSPSchedulingTest {
	private LSP lsp;
	private LSPResource collectionResource;
	private LogisticChainElement collectionElement;
	private LSPResource firstTranshipmentHubResource;
	private LogisticChainElement firstHubElement;
	private LSPResource mainRunResource;
	private LogisticChainElement mainRunElement;
	private Id<Link> toLinkId;

	@BeforeEach
	public void initialize() {
		Config config = new Config();
		config.addCoreModules();
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
		collectionVehicleTypeBuilder.setMaxVelocity(50/3.6);
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

		collectionResource = ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(collectionCarrier, network)
				.setCollectionScheduler(ResourceImplementationUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();

		collectionElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("CollectionElement", LogisticChainElement.class))
				.setResource(collectionResource)
				.build();

		TranshipmentHubSchedulerBuilder firstReloadingSchedulerBuilder = ResourceImplementationUtils.TranshipmentHubSchedulerBuilder.newInstance();
		firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
		firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);

		Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
		Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");

		TransshipmentHubBuilder firstTransshipmentHubBuilder = ResourceImplementationUtils.TransshipmentHubBuilder.newInstance(firstTransshipmentHubId, firstTransshipmentHub_LinkId, scenario);
		firstTransshipmentHubBuilder.setTransshipmentHubScheduler(firstReloadingSchedulerBuilder.build());
		firstTranshipmentHubResource = firstTransshipmentHubBuilder.build();

		Id<LogisticChainElement> firstHubElementId = Id.create("FirstHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder firstHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(firstHubElementId);
		firstHubElementBuilder.setResource(firstTranshipmentHubResource);
		firstHubElement = firstHubElementBuilder.build();

		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		Id<VehicleType> mainRunVehicleTypeId = Id.create("MainRunCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder mainRunVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(mainRunVehicleTypeId);
		mainRunVehicleTypeBuilder.setCapacity(30);
		mainRunVehicleTypeBuilder.setCostPerDistanceUnit(0.0002);
		mainRunVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		mainRunVehicleTypeBuilder.setFixCost(120);
		mainRunVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType mainRunType = mainRunVehicleTypeBuilder.build();

		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		toLinkId = Id.createLinkId("(14 2) (14 3)");

		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId, mainRunType);

		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarriersUtils.createCarrier(mainRunCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);

		mainRunResource = ResourceImplementationUtils.MainRunCarrierResourceBuilder.newInstance(mainRunCarrier, network)
				.setMainRunCarrierScheduler(ResourceImplementationUtils.createDefaultMainRunCarrierScheduler())
				.setFromLinkId(fromLinkId)
				.setToLinkId(toLinkId)
				.build();

		mainRunElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("MainRunElement", LogisticChainElement.class))
				.setResource(mainRunResource)
				.build();

		collectionElement.connectWithNextElement(firstHubElement);
		firstHubElement.connectWithNextElement(mainRunElement);

		Id<LogisticChain> solutionId = Id.create("SolutionId", LogisticChain.class);
		LSPUtils.LogisticChainBuilder completeSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(solutionId);
		completeSolutionBuilder.addLogisticChainElement(collectionElement);
		completeSolutionBuilder.addLogisticChainElement(firstHubElement);
		completeSolutionBuilder.addLogisticChainElement(mainRunElement);
		LogisticChain completeSolution = completeSolutionBuilder.build();

		InitialShipmentAssigner assigner = ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		completePlan.setInitialShipmentAssigner(assigner);
		completePlan.addLogisticChain(completeSolution);

		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		completeLSPBuilder.setInitialPlan(completePlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);
		resourcesList.add(firstTranshipmentHubResource);
		resourcesList.add(mainRunResource);

		LogisticChainScheduler simpleScheduler = ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setLogisticChainScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();

		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

		for (int i = 1; i < 2; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			//Random random = new Random(1);
			int capacityDemand = MatsimRandom.getRandom().nextInt(4);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList);
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
				Collections.shuffle(linkList);
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

	}

	@Test
	public void testMainRunLSPScheduling() {

		for (LSPShipment shipment : lsp.getShipments()) {
			ArrayList<ShipmentPlanElement> scheduleElements = new ArrayList<>(ShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			scheduleElements.sort(ShipmentUtils.createShipmentPlanElementComparator());

			System.out.println();
			for (int i = 0; i < ShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().size(); i++) {
				System.out.println("Scheduled: " + scheduleElements.get(i).getLogisticChainElement().getId() + "  " + scheduleElements.get(i).getResourceId() + "  " + scheduleElements.get(i).getElementType() + " Start: " + scheduleElements.get(i).getStartTime() + " End: " + scheduleElements.get(i).getEndTime());
			}
			System.out.println();
		}


		for (LSPShipment shipment : lsp.getShipments()) {
			assertEquals(7, ShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().size());
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<>(ShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			planElements.sort(ShipmentUtils.createShipmentPlanElementComparator());
			assertEquals("UNLOAD", planElements.get(6).getElementType());
			assertTrue(planElements.get(6).getEndTime() >= (0));
			assertTrue(planElements.get(6).getEndTime() <= (24*3600));
			assertTrue(planElements.get(6).getStartTime() <= planElements.get(6).getEndTime());
			assertTrue(planElements.get(6).getStartTime() >= (0));
			assertTrue(planElements.get(6).getStartTime() <= (24*3600));
			assertSame(planElements.get(6).getResourceId(), mainRunResource.getId());
			assertSame(planElements.get(6).getLogisticChainElement(), mainRunElement);

			assertEquals(planElements.get(6).getStartTime(), planElements.get(5).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(5).getElementType());
			assertTrue(planElements.get(5).getEndTime() >= (0));
			assertTrue(planElements.get(5).getEndTime() <= (24*3600));
			assertTrue(planElements.get(5).getStartTime() <= planElements.get(5).getEndTime());
			assertTrue(planElements.get(5).getStartTime() >= (0));
			assertTrue(planElements.get(5).getStartTime() <= (24*3600));
			assertSame(planElements.get(5).getResourceId(), mainRunResource.getId());
			assertSame(planElements.get(5).getLogisticChainElement(), mainRunElement);

			assertEquals(planElements.get(5).getStartTime(), planElements.get(4).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(4).getElementType());
			assertTrue(planElements.get(4).getEndTime() >= (0));
			assertTrue(planElements.get(4).getEndTime() <= (24*3600));
			assertTrue(planElements.get(4).getStartTime() <= planElements.get(4).getEndTime());
			assertTrue(planElements.get(4).getStartTime() >= (0));
			assertTrue(planElements.get(4).getStartTime() <= (24*3600));
			assertSame(planElements.get(4).getResourceId(), mainRunResource.getId());
			assertSame(planElements.get(4).getLogisticChainElement(), mainRunElement);

			assertTrue(planElements.get(4).getStartTime() >= (planElements.get(3).getEndTime() / (1.0001)) + 300);

			assertEquals("HANDLE", planElements.get(3).getElementType());
			assertTrue(planElements.get(3).getEndTime() >= (0));
			assertTrue(planElements.get(3).getEndTime() <= (24*3600));
			assertTrue(planElements.get(3).getStartTime() <= planElements.get(3).getEndTime());
			assertTrue(planElements.get(3).getStartTime() >= (0));
			assertTrue(planElements.get(3).getStartTime() <= (24*3600));
			assertSame(planElements.get(3).getResourceId(), firstTranshipmentHubResource.getId());
			assertSame(planElements.get(3).getLogisticChainElement(), firstHubElement);

			assertEquals(planElements.get(3).getStartTime(), (planElements.get(2).getEndTime() + 300), 0.0);

			assertEquals("UNLOAD", planElements.get(2).getElementType());
			assertTrue(planElements.get(2).getEndTime() >= (0));
			assertTrue(planElements.get(2).getEndTime() <= (24*3600));
			assertTrue(planElements.get(2).getStartTime() <= planElements.get(2).getEndTime());
			assertTrue(planElements.get(2).getStartTime() >= (0));
			assertTrue(planElements.get(2).getStartTime() <= (24*3600));
			assertSame(planElements.get(2).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(2).getLogisticChainElement(), collectionElement);

			assertEquals(planElements.get(2).getStartTime(), planElements.get(1).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(1).getElementType());
			assertTrue(planElements.get(1).getEndTime() >= (0));
			assertTrue(planElements.get(1).getEndTime() <= (24*3600));
			assertTrue(planElements.get(1).getStartTime() <= planElements.get(1).getEndTime());
			assertTrue(planElements.get(1).getStartTime() >= (0));
			assertTrue(planElements.get(1).getStartTime() <= (24*3600));
			assertSame(planElements.get(1).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(1).getLogisticChainElement(), collectionElement);

			assertEquals(planElements.get(1).getStartTime(), planElements.get(0).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(0).getElementType());
			assertTrue(planElements.get(0).getEndTime() >= (0));
			assertTrue(planElements.get(0).getEndTime() <= (24*3600));
			assertTrue(planElements.get(0).getStartTime() <= planElements.get(0).getEndTime());
			assertTrue(planElements.get(0).getStartTime() >= (0));
			assertTrue(planElements.get(0).getStartTime() <= (24*3600));
			assertSame(planElements.get(0).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(0).getLogisticChainElement(), collectionElement);
		}

		assertEquals(1, firstTranshipmentHubResource.getSimulationTrackers().size());
		ArrayList<EventHandler> eventHandlers = new ArrayList<>(firstTranshipmentHubResource.getSimulationTrackers());
		assertTrue(eventHandlers.iterator().next() instanceof TransshipmentHubTourEndEventHandler);
		TransshipmentHubTourEndEventHandler reloadEventHandler = (TransshipmentHubTourEndEventHandler) eventHandlers.iterator().next();

		for (Entry<CarrierService, TransshipmentHubTourEndEventHandler.TransshipmentHubEventHandlerPair> entry : reloadEventHandler.getServicesWaitedFor().entrySet()) {
			CarrierService service = entry.getKey();
			LSPShipment shipment = entry.getValue().shipment;
			LogisticChainElement element = entry.getValue().element;
			assertSame(service.getLocationLinkId(), shipment.getFrom());
			assertEquals(service.getCapacityDemand(), shipment.getSize());
			assertEquals(service.getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			boolean handledByTranshipmentHub = false;
			for (LogisticChainElement clientElement : reloadEventHandler.getTranshipmentHub().getClientElements()) {
				if (clientElement == element) {
					handledByTranshipmentHub = true;
					break;
				}
			}
			assertTrue(handledByTranshipmentHub);

			assertFalse(element.getOutgoingShipments().getShipments().contains(shipment));
			assertFalse(element.getIncomingShipments().getShipments().contains(shipment));
		}

		for (LSPShipment shipment : lsp.getShipments()) {
			assertEquals(4, shipment.getSimulationTrackers().size());
			eventHandlers = new ArrayList<>(shipment.getSimulationTrackers());
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<>(ShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId()).getPlanElements().values());
			planElements.sort(ShipmentUtils.createShipmentPlanElementComparator());
			ArrayList<LogisticChainElement> solutionElements = new ArrayList<>(lsp.getSelectedPlan().getLogisticChains().iterator().next().getLogisticChainElements());
			ArrayList<LSPResource> resources = new ArrayList<>(lsp.getResources());

			assertTrue(eventHandlers.get(0) instanceof LSPTourEndEventHandler);
			LSPTourEndEventHandler endHandler = (LSPTourEndEventHandler) eventHandlers.get(0);
			assertSame(endHandler.getCarrierService().getLocationLinkId(), shipment.getFrom());
			assertEquals(endHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(endHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertSame(endHandler.getCarrierService().getServiceStartTimeWindow(), shipment.getPickupTimeWindow());
			assertSame(endHandler.getLogisticChainElement(), planElements.get(0).getLogisticChainElement());
			assertSame(endHandler.getLogisticChainElement(), planElements.get(1).getLogisticChainElement());
			assertSame(endHandler.getLogisticChainElement(), planElements.get(2).getLogisticChainElement());
			assertSame(endHandler.getLogisticChainElement(), solutionElements.get(0));
			assertSame(endHandler.getLspShipment(), shipment);
			assertSame(endHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(endHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(endHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(endHandler.getResourceId(), resources.get(0).getId());

			//CollectionServiceEnd
			assertTrue(eventHandlers.get(1) instanceof CollectionServiceEndEventHandler);
			CollectionServiceEndEventHandler serviceHandler = (CollectionServiceEndEventHandler) eventHandlers.get(1);
			assertSame(serviceHandler.getCarrierService().getLocationLinkId(), shipment.getFrom());
			assertEquals(serviceHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(serviceHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertSame(serviceHandler.getCarrierService().getServiceStartTimeWindow(), shipment.getPickupTimeWindow());
			assertSame(serviceHandler.getElement(), planElements.get(0).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), planElements.get(1).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), planElements.get(2).getLogisticChainElement());
			assertSame(serviceHandler.getElement(), solutionElements.get(0));
			assertSame(serviceHandler.getLspShipment(), shipment);
			assertSame(serviceHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(serviceHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(serviceHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(serviceHandler.getResourceId(), resources.get(0).getId());

			//MainRunTourStart
			assertTrue(eventHandlers.get(2) instanceof LSPTourStartEventHandler);
			LSPTourStartEventHandler mainRunStartHandler = (LSPTourStartEventHandler) eventHandlers.get(2);
			assertSame(mainRunStartHandler.getCarrierService().getLocationLinkId(), toLinkId);
			assertEquals(mainRunStartHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(mainRunStartHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(mainRunStartHandler.getLogisticChainElement(), planElements.get(4).getLogisticChainElement());
			assertSame(mainRunStartHandler.getLogisticChainElement(), planElements.get(5).getLogisticChainElement());
			assertSame(mainRunStartHandler.getLogisticChainElement(), planElements.get(6).getLogisticChainElement());
			assertSame(mainRunStartHandler.getLogisticChainElement(), solutionElements.get(2));
			assertSame(mainRunStartHandler.getLspShipment(), shipment);
			assertSame(mainRunStartHandler.getResourceId(), planElements.get(4).getResourceId());
			assertSame(mainRunStartHandler.getResourceId(), planElements.get(5).getResourceId());
			assertSame(mainRunStartHandler.getResourceId(), planElements.get(6).getResourceId());
			assertSame(mainRunStartHandler.getResourceId(), resources.get(2).getId());

			//MainRunTourEnd
			assertTrue(eventHandlers.get(3) instanceof LSPTourEndEventHandler);
			LSPTourEndEventHandler mainRunEndHandler = (LSPTourEndEventHandler) eventHandlers.get(3);
			assertSame(mainRunEndHandler.getCarrierService().getLocationLinkId(), toLinkId);
			assertEquals(mainRunEndHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(mainRunEndHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(mainRunEndHandler.getLogisticChainElement(), planElements.get(4).getLogisticChainElement());
			assertSame(mainRunEndHandler.getLogisticChainElement(), planElements.get(5).getLogisticChainElement());
			assertSame(mainRunEndHandler.getLogisticChainElement(), planElements.get(6).getLogisticChainElement());
			assertSame(mainRunEndHandler.getLogisticChainElement(), solutionElements.get(2));
			assertSame(mainRunEndHandler.getLspShipment(), shipment);
			assertSame(mainRunEndHandler.getResourceId(), planElements.get(4).getResourceId());
			assertSame(mainRunEndHandler.getResourceId(), planElements.get(5).getResourceId());
			assertSame(mainRunEndHandler.getResourceId(), planElements.get(6).getResourceId());
			assertSame(mainRunEndHandler.getResourceId(), resources.get(2).getId());
		}

		for (LogisticChain solution : lsp.getSelectedPlan().getLogisticChains()) {
			assertEquals(1, solution.getShipmentIds().size());
			for (LogisticChainElement element : solution.getLogisticChainElements()) {
				assertTrue(element.getIncomingShipments().getShipments().isEmpty());
				if (element.getNextElement() != null) {
					assertTrue(element.getOutgoingShipments().getShipments().isEmpty());
				} else {
					assertFalse(element.getOutgoingShipments().getShipments().isEmpty());
				}
			}
		}

	}
}
