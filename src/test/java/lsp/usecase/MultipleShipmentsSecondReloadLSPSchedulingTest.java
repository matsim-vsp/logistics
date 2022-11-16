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

package lsp.usecase;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

import static org.junit.Assert.*;

public class MultipleShipmentsSecondReloadLSPSchedulingTest {
	private LSP lsp;
	private LSPResource collectionResource;
	private LogisticsSolutionElement collectionElement;
	private LSPResource firstTranshipmentHubResource;
	private LogisticsSolutionElement firstHubElement;
	private LSPResource mainRunResource;
	private LogisticsSolutionElement mainRunElement;
	private LSPResource secondTranshipmentHubResource;
	private LogisticsSolutionElement secondHubElement;
	private Id<Link> toLinkId;

	@Before
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
		Carrier collectionCarrier = CarrierUtils.createCarrier(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(collectionCapabilities);


		Id<LSPResource> collectionResourceId = Id.create("CollectionCarrierResource", LSPResource.class);
		UsecaseUtils.CollectionCarrierResourceBuilder collectionResourceBuilder = UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(collectionResourceId, network);
		collectionResourceBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		collectionResourceBuilder.setCarrier(collectionCarrier);
		collectionResourceBuilder.setLocationLinkId(collectionLinkId);
		collectionResource = collectionResourceBuilder.build();

		Id<LogisticsSolutionElement> collectionElementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(collectionElementId);
		collectionBuilder.setResource(collectionResource);
		collectionElement = collectionBuilder.build();

		UsecaseUtils.TranshipmentHubSchedulerBuilder firstReloadingSchedulerBuilder = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
		firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
		Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");

		UsecaseUtils.TransshipmentHubBuilder firstTransshipmentHubBuilder = UsecaseUtils.TransshipmentHubBuilder.newInstance(firstTransshipmentHubId, firstTransshipmentHub_LinkId, scenario);
		firstTransshipmentHubBuilder.setTransshipmentHubScheduler(firstReloadingSchedulerBuilder.build());
		firstTranshipmentHubResource = firstTransshipmentHubBuilder.build();

		Id<LogisticsSolutionElement> firstHubElementId = Id.create("FirstHubElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder firstHubElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(firstHubElementId);
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
		Carrier mainRunCarrier = CarrierUtils.createCarrier(mainRunCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);


		Id<LSPResource> mainRunId = Id.create("MainRunResource", LSPResource.class);
		UsecaseUtils.MainRunCarrierResourceBuilder mainRunResourceBuilder = UsecaseUtils.MainRunCarrierResourceBuilder.newInstance(mainRunId, network);
		mainRunResourceBuilder.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler());
		mainRunResourceBuilder.setFromLinkId(fromLinkId);
		mainRunResourceBuilder.setToLinkId(Id.createLinkId(toLinkId));
		mainRunResourceBuilder.setCarrier(mainRunCarrier);
		mainRunResource = mainRunResourceBuilder.build();

		Id<LogisticsSolutionElement> mainRunElementId = Id.create("MainRunElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder mainRunBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(mainRunElementId);
		mainRunBuilder.setResource(mainRunResource);
		mainRunElement = mainRunBuilder.build();

		UsecaseUtils.TranshipmentHubSchedulerBuilder secondSchedulerBuilder = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance();
		secondSchedulerBuilder.setCapacityNeedFixed(10);
		secondSchedulerBuilder.setCapacityNeedLinear(1);


		Id<LSPResource> secondTransshipmentHubId = Id.create("TranshipmentHub2", LSPResource.class);
		Id<Link> secondTransshipmentHub_LinkId = Id.createLinkId("(14 2) (14 3)");

		UsecaseUtils.TransshipmentHubBuilder secondTransshipmentHubBuilder = UsecaseUtils.TransshipmentHubBuilder.newInstance(secondTransshipmentHubId, secondTransshipmentHub_LinkId, scenario);
		secondTransshipmentHubBuilder.setTransshipmentHubScheduler(secondSchedulerBuilder.build());
		secondTranshipmentHubResource = secondTransshipmentHubBuilder.build();

		Id<LogisticsSolutionElement> secondHubElementId = Id.create("SecondHubElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder secondHubElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(secondHubElementId);
		secondHubElementBuilder.setResource(secondTranshipmentHubResource);
		secondHubElement = secondHubElementBuilder.build();


		collectionElement.connectWithNextElement(firstHubElement);
		firstHubElement.connectWithNextElement(mainRunElement);
		mainRunElement.connectWithNextElement(secondHubElement);


		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder completeSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(solutionId);
		completeSolutionBuilder.addSolutionElement(collectionElement);
		completeSolutionBuilder.addSolutionElement(firstHubElement);
		completeSolutionBuilder.addSolutionElement(mainRunElement);
		completeSolutionBuilder.addSolutionElement(secondHubElement);
		LogisticsSolution completeSolution = completeSolutionBuilder.build();

		ShipmentAssigner assigner = UsecaseUtils.createSingleSolutionShipmentAssigner();
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		completePlan.setAssigner(assigner);
		completePlan.addSolution(completeSolution);

		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		completeLSPBuilder.setInitialPlan(completePlan);
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);
		resourcesList.add(firstTranshipmentHubResource);
		resourcesList.add(mainRunResource);
		resourcesList.add(secondTranshipmentHubResource);

		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		simpleScheduler.setBufferTime(300);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		lsp = completeLSPBuilder.build();

		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

		for (int i = 1; i < 100; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
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
		lsp.scheduleSolutions();

	}

	@Test
	public void testSecondReloadLSPScheduling() {
		
		/*for(LSPShipment shipment : lsp.getShipments()) {
			ArrayList<AbstractShipmentPlanElement> elementList = new ArrayList<AbstractShipmentPlanElement>(shipment.getSchedule().getPlanElements().values());
			Collections.sort(elementList, new AbstractShipmentPlanElementComparator());
			System.out.println();
			for(AbstractShipmentPlanElement element : elementList) {
				System.out.println(element.getSolutionElement().getId() + " " + element.getResourceId() + " " + element.getElementType() + " " + element.getStartTime() + " " + element.getEndTime());	
			}			
			System.out.println();
		}*/

		for (LSPShipment shipment : lsp.getShipments()) {
			assertEquals(8, shipment.getShipmentPlan().getPlanElements().size());
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			planElements.sort(ShipmentUtils.createShipmentPlanElementComparator());

			assertEquals("HANDLE", planElements.get(7).getElementType());
			assertTrue(planElements.get(7).getEndTime() >= (0));
			assertTrue(planElements.get(7).getEndTime() <= (24*3600));
			assertTrue(planElements.get(7).getStartTime() <= planElements.get(7).getEndTime());
			assertTrue(planElements.get(7).getStartTime() >= (0));
			assertTrue(planElements.get(7).getStartTime() <= (24*3600));
			assertSame(planElements.get(7).getResourceId(), secondTranshipmentHubResource.getId());
			assertSame(planElements.get(7).getSolutionElement(), secondHubElement);

			assertEquals(planElements.get(7).getStartTime(), (planElements.get(6).getEndTime() + 300), 0.0);

			assertEquals("UNLOAD", planElements.get(6).getElementType());
			assertTrue(planElements.get(6).getEndTime() >= (0));
			assertTrue(planElements.get(6).getEndTime() <= (24*3600));
			assertTrue(planElements.get(6).getStartTime() <= planElements.get(6).getEndTime());
			assertTrue(planElements.get(6).getStartTime() >= (0));
			assertTrue(planElements.get(6).getStartTime() <= (24*3600));
			assertSame(planElements.get(6).getResourceId(), mainRunResource.getId());
			assertSame(planElements.get(6).getSolutionElement(), mainRunElement);

			assertEquals(planElements.get(6).getStartTime(), planElements.get(5).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(5).getElementType());
			assertTrue(planElements.get(5).getEndTime() >= (0));
			assertTrue(planElements.get(5).getEndTime() <= (24*3600));
			assertTrue(planElements.get(5).getStartTime() <= planElements.get(5).getEndTime());
			assertTrue(planElements.get(5).getStartTime() >= (0));
			assertTrue(planElements.get(5).getStartTime() <= (24*3600));
			assertSame(planElements.get(5).getResourceId(), mainRunResource.getId());
			assertSame(planElements.get(5).getSolutionElement(), mainRunElement);

			assertEquals(planElements.get(5).getStartTime(), planElements.get(4).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(4).getElementType());
			assertTrue(planElements.get(4).getEndTime() >= (0));
			assertTrue(planElements.get(4).getEndTime() <= (24*3600));
			assertTrue(planElements.get(4).getStartTime() <= planElements.get(4).getEndTime());
			assertTrue(planElements.get(4).getStartTime() >= (0));
			assertTrue(planElements.get(4).getStartTime() <= (24*3600));
			assertSame(planElements.get(4).getResourceId(), mainRunResource.getId());
			assertSame(planElements.get(4).getSolutionElement(), mainRunElement);

			assertTrue(planElements.get(4).getStartTime() >= (planElements.get(3).getEndTime() / (1.0001)) + 300);

			assertEquals("HANDLE", planElements.get(3).getElementType());
			assertTrue(planElements.get(3).getEndTime() >= (0));
			assertTrue(planElements.get(3).getEndTime() <= (24*3600));
			assertTrue(planElements.get(3).getStartTime() <= planElements.get(3).getEndTime());
			assertTrue(planElements.get(3).getStartTime() >= (0));
			assertTrue(planElements.get(3).getStartTime() <= (24*3600));
			assertSame(planElements.get(3).getResourceId(), firstTranshipmentHubResource.getId());
			assertSame(planElements.get(3).getSolutionElement(), firstHubElement);

			assertEquals(planElements.get(3).getStartTime(), (planElements.get(2).getEndTime() + 300), 0.0);

			assertEquals("UNLOAD", planElements.get(2).getElementType());
			assertTrue(planElements.get(2).getEndTime() >= (0));
			assertTrue(planElements.get(2).getEndTime() <= (24*3600));
			assertTrue(planElements.get(2).getStartTime() <= planElements.get(2).getEndTime());
			assertTrue(planElements.get(2).getStartTime() >= (0));
			assertTrue(planElements.get(2).getStartTime() <= (24*3600));
			assertSame(planElements.get(2).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(2).getSolutionElement(), collectionElement);

			assertEquals(planElements.get(2).getStartTime(), planElements.get(1).getEndTime(), 0.0);

			assertEquals("TRANSPORT", planElements.get(1).getElementType());
			assertTrue(planElements.get(1).getEndTime() >= (0));
			assertTrue(planElements.get(1).getEndTime() <= (24*3600));
			assertTrue(planElements.get(1).getStartTime() <= planElements.get(1).getEndTime());
			assertTrue(planElements.get(1).getStartTime() >= (0));
			assertTrue(planElements.get(1).getStartTime() <= (24*3600));
			assertSame(planElements.get(1).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(1).getSolutionElement(), collectionElement);

			assertEquals(planElements.get(1).getStartTime(), planElements.get(0).getEndTime(), 0.0);

			assertEquals("LOAD", planElements.get(0).getElementType());
			assertTrue(planElements.get(0).getEndTime() >= (0));
			assertTrue(planElements.get(0).getEndTime() <= (24*3600));
			assertTrue(planElements.get(0).getStartTime() <= planElements.get(0).getEndTime());
			assertTrue(planElements.get(0).getStartTime() >= (0));
			assertTrue(planElements.get(0).getStartTime() <= (24*3600));
			assertSame(planElements.get(0).getResourceId(), collectionResource.getId());
			assertSame(planElements.get(0).getSolutionElement(), collectionElement);
		}

		assertEquals(1, firstTranshipmentHubResource.getSimulationTrackers().size());
		ArrayList<EventHandler> eventHandlers = new ArrayList<>(firstTranshipmentHubResource.getSimulationTrackers());
		assertTrue(eventHandlers.iterator().next() instanceof TransshipmentHubEventHandler);
		TransshipmentHubEventHandler reloadEventHandler = (TransshipmentHubEventHandler) eventHandlers.iterator().next();
		Iterator<Entry<CarrierService, TransshipmentHubEventHandler.TransshipmentHubEventHandlerPair>> iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();

		while (iter.hasNext()) {
			Entry<CarrierService, TransshipmentHubEventHandler.TransshipmentHubEventHandlerPair> entry = iter.next();
			CarrierService service = entry.getKey();
			LSPShipment shipment = entry.getValue().shipment;
			LogisticsSolutionElement element = entry.getValue().element;
			assertSame(service.getLocationLinkId(), shipment.getFrom());
			assertEquals(service.getCapacityDemand(), shipment.getSize());
			assertEquals(service.getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			boolean handledByTranshipmentHub = false;
			for (LogisticsSolutionElement clientElement : reloadEventHandler.getTranshipmentHub().getClientElements()) {
				if (clientElement == element) {
					handledByTranshipmentHub = true;
				}
			}
			assertTrue(handledByTranshipmentHub);

			assertFalse(element.getOutgoingShipments().getShipments().contains(shipment));
			assertFalse(element.getIncomingShipments().getShipments().contains(shipment));
		}

		assertEquals(1, secondTranshipmentHubResource.getSimulationTrackers().size());
		eventHandlers = new ArrayList<>(secondTranshipmentHubResource.getSimulationTrackers());
		assertTrue(eventHandlers.iterator().next() instanceof TransshipmentHubEventHandler);
		reloadEventHandler = (TransshipmentHubEventHandler) eventHandlers.iterator().next();
		iter = reloadEventHandler.getServicesWaitedFor().entrySet().iterator();

		while (iter.hasNext()) {
			Entry<CarrierService, TransshipmentHubEventHandler.TransshipmentHubEventHandlerPair> entry = iter.next();
			CarrierService service = entry.getKey();
			LSPShipment shipment = entry.getValue().shipment;
			LogisticsSolutionElement element = entry.getValue().element;
			assertSame(service.getLocationLinkId(), toLinkId);
			assertEquals(service.getCapacityDemand(), shipment.getSize());
			assertEquals(service.getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			boolean handledByTranshipmentHub = false;
			for (LogisticsSolutionElement clientElement : reloadEventHandler.getTranshipmentHub().getClientElements()) {
				if (clientElement == element) {
					handledByTranshipmentHub = true;
				}
			}
			assertTrue(handledByTranshipmentHub);

			assertFalse(element.getOutgoingShipments().getShipments().contains(shipment));
			assertFalse(element.getIncomingShipments().getShipments().contains(shipment));
		}

		for (LSPShipment shipment : lsp.getShipments()) {
			assertEquals(4, shipment.getSimulationTrackers().size());
			eventHandlers = new ArrayList<>(shipment.getSimulationTrackers());
			ArrayList<ShipmentPlanElement> planElements = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			planElements.sort(ShipmentUtils.createShipmentPlanElementComparator());
			ArrayList<LogisticsSolutionElement> solutionElements = new ArrayList<>(lsp.getSelectedPlan().getSolutions().iterator().next().getSolutionElements());
			ArrayList<LSPResource> resources = new ArrayList<>(lsp.getResources());

			assertTrue(eventHandlers.get(0) instanceof CollectionTourEndEventHandler);
			CollectionTourEndEventHandler collectionEndHandler = (CollectionTourEndEventHandler) eventHandlers.get(0);
			assertSame(collectionEndHandler.getCarrierService().getLocationLinkId(), shipment.getFrom());
			assertEquals(collectionEndHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(collectionEndHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertSame(collectionEndHandler.getCarrierService().getServiceStartTimeWindow(), shipment.getPickupTimeWindow());
			assertSame(collectionEndHandler.getElement(), planElements.get(0).getSolutionElement());
			assertSame(collectionEndHandler.getElement(), planElements.get(1).getSolutionElement());
			assertSame(collectionEndHandler.getElement(), planElements.get(2).getSolutionElement());
			assertSame(collectionEndHandler.getElement(), solutionElements.get(0));
			assertSame(collectionEndHandler.getLspShipment(), shipment);
			assertSame(collectionEndHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(collectionEndHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(collectionEndHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(collectionEndHandler.getResourceId(), resources.get(0).getId());

			assertTrue(eventHandlers.get(1) instanceof CollectionServiceEndEventHandler);
			CollectionServiceEndEventHandler collectionServiceHandler = (CollectionServiceEndEventHandler) eventHandlers.get(1);
			assertSame(collectionServiceHandler.getCarrierService().getLocationLinkId(), shipment.getFrom());
			assertEquals(collectionServiceHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(collectionServiceHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertSame(collectionServiceHandler.getCarrierService().getServiceStartTimeWindow(), shipment.getPickupTimeWindow());
			assertSame(collectionServiceHandler.getElement(), planElements.get(0).getSolutionElement());
			assertSame(collectionServiceHandler.getElement(), planElements.get(1).getSolutionElement());
			assertSame(collectionServiceHandler.getElement(), planElements.get(2).getSolutionElement());
			assertSame(collectionServiceHandler.getElement(), solutionElements.get(0));
			assertSame(collectionServiceHandler.getLspShipment(), shipment);
			assertSame(collectionServiceHandler.getResourceId(), planElements.get(0).getResourceId());
			assertSame(collectionServiceHandler.getResourceId(), planElements.get(1).getResourceId());
			assertSame(collectionServiceHandler.getResourceId(), planElements.get(2).getResourceId());
			assertSame(collectionServiceHandler.getResourceId(), resources.get(0).getId());

			assertTrue(eventHandlers.get(2) instanceof MainRunEventHandler);
			MainRunEventHandler mainRunStartHandler = (MainRunEventHandler) eventHandlers.get(2);
			assertSame(mainRunStartHandler.getCarrierService().getLocationLinkId(), toLinkId);
			assertEquals(mainRunStartHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(mainRunStartHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunStartHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(mainRunStartHandler.getSolutionElement(), planElements.get(4).getSolutionElement());
			assertSame(mainRunStartHandler.getSolutionElement(), planElements.get(5).getSolutionElement());
			assertSame(mainRunStartHandler.getSolutionElement(), planElements.get(6).getSolutionElement());
			assertSame(mainRunStartHandler.getSolutionElement(), solutionElements.get(2));
			assertSame(mainRunStartHandler.getLspShipment(), shipment);
			assertSame(mainRunStartHandler.getResource().getId(), planElements.get(4).getResourceId());
			assertSame(mainRunStartHandler.getResource().getId(), planElements.get(5).getResourceId());
			assertSame(mainRunStartHandler.getResource().getId(), planElements.get(6).getResourceId());
			assertSame(mainRunStartHandler.getResource().getId(), resources.get(2).getId());

			assertTrue(eventHandlers.get(3) instanceof MainRunEventHandler);
			MainRunEventHandler mainRunEndHandler = (MainRunEventHandler) eventHandlers.get(3);
			assertSame(mainRunEndHandler.getCarrierService().getLocationLinkId(), toLinkId);
			assertEquals(mainRunEndHandler.getCarrierService().getServiceDuration(), shipment.getDeliveryServiceTime(), 0.0);
			assertEquals(mainRunEndHandler.getCarrierService().getCapacityDemand(), shipment.getSize());
			assertEquals(0, mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getStart(), 0.0);
			assertEquals(Integer.MAX_VALUE, mainRunEndHandler.getCarrierService().getServiceStartTimeWindow().getEnd(), 0.0);
			assertSame(mainRunEndHandler.getSolutionElement(), planElements.get(4).getSolutionElement());
			assertSame(mainRunEndHandler.getSolutionElement(), planElements.get(5).getSolutionElement());
			assertSame(mainRunEndHandler.getSolutionElement(), planElements.get(6).getSolutionElement());
			assertSame(mainRunEndHandler.getSolutionElement(), solutionElements.get(2));
			assertSame(mainRunEndHandler.getLspShipment(), shipment);
			assertSame(mainRunEndHandler.getResource().getId(), planElements.get(4).getResourceId());
			assertSame(mainRunEndHandler.getResource().getId(), planElements.get(5).getResourceId());
			assertSame(mainRunEndHandler.getResource().getId(), planElements.get(6).getResourceId());
			assertSame(mainRunEndHandler.getResource().getId(), resources.get(2).getId());
		}

		for (LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
			for (LogisticsSolutionElement element : solution.getSolutionElements()) {
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
