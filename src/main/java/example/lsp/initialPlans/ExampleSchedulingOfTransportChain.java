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

package example.lsp.initialPlans;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

/*A transport chain with five elements (collection-> reloading -> main run -> reloading -> delivery) is created and scheduled
 *
 */

/*package-private*/ class ExampleSchedulingOfTransportChain {

	private static LSP createInitialLSP(Scenario scenario) {

		Network network = scenario.getNetwork();

		//The Carrier for collection is created
		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50 / 3.6);
		VehicleType collectionType = vehicleTypeBuilder.build();

		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();

		Carrier collectionCarrier = CarrierUtils.createCarrier(collectionCarrierId);
		collectionCarrier.setCarrierCapabilities(capabilities);

		//The collection adapter i.e. the Resource is created
		LSPResource collectionResource= UsecaseUtils.CollectionCarrierResourceBuilder.newInstance(collectionCarrier, network)
				.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler())
				.setLocationLinkId(collectionLinkId)
				.build();

		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		LogisticChainElement collectionElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("CollectionElement", LogisticChainElement.class))
				.setResource(collectionResource)
				.build();


		//The first reloading adapter i.e. the Resource is created
		Id<LSPResource> firstTransshipmentHubId = Id.create("TranshipmentHub1", LSPResource.class);
		Id<Link> firstTransshipmentHub_LinkId = Id.createLinkId("(4 2) (4 3)");
		UsecaseUtils.TransshipmentHubBuilder firstTransshipmentHubBuilder = UsecaseUtils.TransshipmentHubBuilder.newInstance(firstTransshipmentHubId, firstTransshipmentHub_LinkId, scenario);

		//The scheduler for the first reloading point is created
		final LSPResourceScheduler firstHubScheduler = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance()
				.setCapacityNeedFixed(10)
				.setCapacityNeedLinear(1)
				.build();

		//The scheduler is added to the Resource and the Resource is created
		firstTransshipmentHubBuilder.setTransshipmentHubScheduler(firstHubScheduler);
		LSPResource firstTranshipmentHubResource = firstTransshipmentHubBuilder.build();

		//The SolutionElement for the first reloading point is created
		Id<LogisticChainElement> firstHubElementId = Id.create("FirstHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder firstHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(firstHubElementId);
		firstHubElementBuilder.setResource(firstTranshipmentHubResource);
		LogisticChainElement firstHubElement = firstHubElementBuilder.build();


		//The Carrier for the main run Resource is created
		Id<Carrier> mainRunCarrierId = Id.create("MainRunCarrier", Carrier.class);
		final VehicleType mainRunType = CarrierVehicleType.Builder.newInstance(Id.create("MainRunCarrierVehicleType", VehicleType.class))
				.setCapacity(30)
				.setCostPerDistanceUnit(0.0002)
				.setCostPerTimeUnit(0.38)
				.setFixCost(120)
				.setMaxVelocity(50 / 3.6)
				.build();


		Id<Link> fromLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId, mainRunType);

		CarrierCapabilities mainRunCapabilities = CarrierCapabilities.Builder.newInstance()
				.addType(mainRunType)
				.addVehicle(mainRunCarrierVehicle)
				.setFleetSize(FleetSize.INFINITE)
				.build();
		Carrier mainRunCarrier = CarrierUtils.createCarrier(mainRunCarrierId);
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);

		//The adapter i.e. the main run resource is created
		LSPResource mainRunResource = UsecaseUtils.MainRunCarrierResourceBuilder.newInstance(mainRunCarrier, network)
				.setFromLinkId(Id.createLinkId("(4 2) (4 3)"))
				.setToLinkId(Id.createLinkId("(14 2) (14 3)"))
				.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler())
				.build();

		//The LogisticsSolutionElement for the main run Resource is created
		LogisticChainElement mainRunElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("MainRunElement", LogisticChainElement.class))
				.setResource(mainRunResource)
				.build();


		//The second reloading adapter i.e. the Resource is created       
		Id<LSPResource> secondTransshipmentHubId = Id.create("TranshipmentHub2", LSPResource.class);
		Id<Link> secondTransshipmentHub_LinkId = Id.createLinkId("(14 2) (14 3)");

		//The scheduler for the second reloading point is created
		LSPResourceScheduler secondHubScheduler  = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance()
				.setCapacityNeedFixed(10)
				.setCapacityNeedLinear(1)
				.build();

		LSPResource secondTransshipmentHubResource = UsecaseUtils.TransshipmentHubBuilder.newInstance(secondTransshipmentHubId, secondTransshipmentHub_LinkId, scenario)
				.setTransshipmentHubScheduler(secondHubScheduler)
				.build();

		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticChainElement> secondHubElementId = Id.create("SecondHubElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder secondHubElementBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(secondHubElementId);
		secondHubElementBuilder.setResource(secondTransshipmentHubResource);
		LogisticChainElement secondHubElement = secondHubElementBuilder.build();


		//The Carrier for distribution is created
		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		VehicleType distributionType = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId)
				.setCapacity(10)
				.setCostPerDistanceUnit(0.0004)
				.setCostPerTimeUnit(0.38)
				.setFixCost(49)
				.setMaxVelocity(50 / 3.6)
				.build();

		Id<Link> distributionLinkId = Id.createLinkId("(14 2) (14 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, distributionType);

		CarrierCapabilities.Builder distributionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		distributionCapabilitiesBuilder.addType(distributionType);
		distributionCapabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		distributionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = distributionCapabilitiesBuilder.build();
		Carrier distributionCarrier = CarrierUtils.createCarrier(distributionCarrierId);
		distributionCarrier.setCarrierCapabilities(distributionCapabilities);

		//The distribution adapter i.e. the Resource is created
		UsecaseUtils.DistributionCarrierResourceBuilder distributionResourceBuilder = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(distributionCarrier, network);
		distributionResourceBuilder.setLocationLinkId(distributionLinkId);

		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		distributionResourceBuilder.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler());
		LSPResource distributionResource = distributionResourceBuilder.build();

		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticChainElement> distributionElementId = Id.create("DistributionElement", LogisticChainElement.class);
		LSPUtils.LogisticChainElementBuilder distributionBuilder = LSPUtils.LogisticChainElementBuilder.newInstance(distributionElementId);
		distributionBuilder.setResource(distributionResource);
		LogisticChainElement distributionElement = distributionBuilder.build();

		//The Order of the logisticsSolutionElements is now specified
		collectionElement.connectWithNextElement(firstHubElement);
		firstHubElement.connectWithNextElement(mainRunElement);
		mainRunElement.connectWithNextElement(secondHubElement);
		secondHubElement.connectWithNextElement(distributionElement);


		//The SolutionElements are now inserted into the only LogisticsSolution of the LSP
		Id<LogisticChain> solutionId = Id.create("SolutionId", LogisticChain.class);
		LSPUtils.LogisticChainBuilder completeSolutionBuilder = LSPUtils.LogisticChainBuilder.newInstance(solutionId);
		completeSolutionBuilder.addLogisticChainElement(collectionElement);
		completeSolutionBuilder.addLogisticChainElement(firstHubElement);
		completeSolutionBuilder.addLogisticChainElement(mainRunElement);
		completeSolutionBuilder.addLogisticChainElement(secondHubElement);
		completeSolutionBuilder.addLogisticChainElement(distributionElement);
		LogisticChain completeSolution = completeSolutionBuilder.build();


		//The initial plan of the lsp is generated and the assigner and the solution from above are added
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		ShipmentAssigner assigner = UsecaseUtils.createSingleLogisticChainShipmentAssigner();
		completePlan.setAssigner(assigner);
		completePlan.addLogisticChain(completeSolution);

		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		completeLSPBuilder.setInitialPlan(completePlan);

		//The exogenous list of Resources for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionResource);
		resourcesList.add(firstTranshipmentHubResource);
		resourcesList.add(mainRunResource);
		resourcesList.add(secondTransshipmentHubResource);
		resourcesList.add(distributionResource);

//		SolutionScheduler forwardSolutionScheduler = LSPUtils.createForwardSolutionScheduler(); //Ist der "nicht einfache" Scheduler. TODO braucht der keine RessourcenLsite oder ähnliches? --> Offenbar ja, weil Null Pointer. argh!
//		completeLSPBuilder.setSolutionScheduler(forwardSolutionScheduler);

		LogisticChainScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
		completeLSPBuilder.setLogisticChainScheduler(simpleScheduler);

		return completeLSPBuilder.build();

	}

	private static Collection<LSPShipment> createInitialLSPShipments(Network network) {
		ArrayList<LSPShipment> shipmentList = new ArrayList<>();
		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());
		Random rand = new Random(1);
		for (int i = 1; i < 6; i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			int capacityDemand = rand.nextInt(10);
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
			shipmentList.add(builder.build());
		}
		return shipmentList;
	}


	public static void main(String[] args) {

		//Set up required MATSim classes
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");

		//Create LSP and shipments
		LSP lsp = createInitialLSP(scenario);
		Collection<LSPShipment> shipments = createInitialLSPShipments(scenario.getNetwork());

		//assign the shipments to the LSP
		for (LSPShipment shipment : shipments) {
			lsp.assignShipmentToLSP(shipment);
		}

		//schedule the LSP with the shipments and according to the scheduler of the Resource
		lsp.scheduleLogisticChains();

		//print the schedules for the assigned LSPShipments
		for (LSPShipment shipment : lsp.getShipments()) {
			ArrayList<ShipmentPlanElement> elementList = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			elementList.sort(ShipmentUtils.createShipmentPlanElementComparator());
			System.out.println("Shipment: " + shipment.getId());
			for (ShipmentPlanElement element : elementList) {
				System.out.println(element.getLogisticChainElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime());
			}
			System.out.println();
		}


//		for (LSPResource lspResource : lsp.getResources()) {
//			if (lspResource instanceof Carrier ) {
//				((Carrier) lspResource).getShipments().toString();
//			}
//		}
		// the above cast keeps complaining when I refactor; in consequence, I am becoming doubtful if this condition can ever be satisfied.
		// also not sure what the code stub might be doing: It is converting to string, but not doing anything with it.  kai, may'22


	}

}
