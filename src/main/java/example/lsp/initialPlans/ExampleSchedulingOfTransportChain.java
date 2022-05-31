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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import lsp.*;
import lsp.shipment.*;
import lsp.usecase.*;
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

import lsp.LSPResource;

/*A transport chain with five elements (collection-> reloading -> main run -> reloading -> delivery) is created and scheduled
 * 
 */

/*package-private*/ class ExampleSchedulingOfTransportChain {

	private static LSP createInitialLSP(Network network) {
		
		//The Carrier for collection is created
		Id<Carrier> collectionCarrierId = Id.create("CollectionCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId, collectionType);

		CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		capabilitiesBuilder.addType(collectionType);
		capabilitiesBuilder.addVehicle(carrierVehicle);
		capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities capabilities = capabilitiesBuilder.build();

		Carrier collectionCarrier = CarrierUtils.createCarrier( collectionCarrierId );
		collectionCarrier.setCarrierCapabilities(capabilities);
		
		//The collection adapter i.e. the Resource is created
		Id<LSPResource> adapterId = Id.create("CollectionCarrierAdapter", LSPResource.class);
		UsecaseUtils.CollectionCarrierAdapterBuilder adapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(adapterId, network);
		
		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		adapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
		adapterBuilder.setCarrier(collectionCarrier);
		adapterBuilder.setLocationLinkId(collectionLinkId);
		LSPResource collectionAdapter = adapterBuilder.build();
		
		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
		collectionElementBuilder.setResource(collectionAdapter);
		LogisticsSolutionElement collectionElement = collectionElementBuilder.build();

		
		//The first reloading adapter i.e. the Resource is created
        Id<LSPResource> firstReloadingId = Id.create("ReloadingPoint1", LSPResource.class);
        Id<Link> firstReloadingLinkId = Id.createLinkId("(4 2) (4 3)");
        UsecaseUtils.ReloadingPointBuilder firstReloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(firstReloadingId, firstReloadingLinkId);
        
        //The scheduler for the first reloading point is created
    	UsecaseUtils.ReloadingPointSchedulerBuilder firstReloadingSchedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        firstReloadingSchedulerBuilder.setCapacityNeedFixed(10);
        firstReloadingSchedulerBuilder.setCapacityNeedLinear(1);
        
        //The scheduler is added to the Resource and the Resource is created
        firstReloadingPointBuilder.setReloadingScheduler(firstReloadingSchedulerBuilder.build());
        LSPResource firstReloadingPointAdapter = firstReloadingPointBuilder.build();
        
        //The SolutionElement for the first reloading point is created
        Id<LogisticsSolutionElement> firstReloadingElementId = Id.create("FirstReloadElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder firstReloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(firstReloadingElementId );
		firstReloadingElementBuilder.setResource(firstReloadingPointAdapter);
		LogisticsSolutionElement firstReloadElement = firstReloadingElementBuilder.build();
		
		
		//The Carrier for the main run Resource is created
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
		Id<Vehicle> mainRunVehicleId = Id.createVehicleId("MainRunVehicle");
		CarrierVehicle mainRunCarrierVehicle = CarrierVehicle.newInstance(mainRunVehicleId, fromLinkId, mainRunType);

		CarrierCapabilities.Builder mainRunCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		mainRunCapabilitiesBuilder.addType(mainRunType);
		mainRunCapabilitiesBuilder.addVehicle(mainRunCarrierVehicle);
		mainRunCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities mainRunCapabilities = mainRunCapabilitiesBuilder.build();
		Carrier mainRunCarrier = CarrierUtils.createCarrier( mainRunCarrierId );
		mainRunCarrier.setCarrierCapabilities(mainRunCapabilities);
        
		//The adapter i.e. the main run resource is created
		Id<LSPResource> mainRunId = Id.create("MainRunAdapter", LSPResource.class);
        UsecaseUtils.MainRunCarrierAdapterBuilder mainRunAdapterBuilder = UsecaseUtils.MainRunCarrierAdapterBuilder.newInstance(mainRunId, network);
        mainRunAdapterBuilder.setFromLinkId(Id.createLinkId("(4 2) (4 3)"));
        mainRunAdapterBuilder.setToLinkId(Id.createLinkId("(14 2) (14 3)"));
        mainRunAdapterBuilder.setCarrier(mainRunCarrier);
        
        //The scheduler for the main run Rescource is created and added to the Resource
		mainRunAdapterBuilder.setMainRunCarrierScheduler(UsecaseUtils.createDefaultMainRunCarrierScheduler());
        LSPResource mainRunAdapter = mainRunAdapterBuilder.build();

        //The LogisticsSolutionElement for the main run Resource is created
        Id<LogisticsSolutionElement> mainRunElementId = Id.create("MainRunElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder mainRunBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(mainRunElementId );
		mainRunBuilder.setResource(mainRunAdapter);
		LogisticsSolutionElement mainRunElement = mainRunBuilder.build();
		
		
		//The second reloading adapter i.e. the Resource is created       
        Id<LSPResource> secondReloadingId = Id.create("ReloadingPoint2", LSPResource.class);
        Id<Link> secondReloadingLinkId = Id.createLinkId("(14 2) (14 3)");
        UsecaseUtils.ReloadingPointBuilder secondReloadingPointBuilder = UsecaseUtils.ReloadingPointBuilder.newInstance(secondReloadingId, secondReloadingLinkId);
        
        //The scheduler for the second reloading point is created
        UsecaseUtils.ReloadingPointSchedulerBuilder secondSchedulerBuilder =  UsecaseUtils.ReloadingPointSchedulerBuilder.newInstance();
        secondSchedulerBuilder.setCapacityNeedFixed(10);
        secondSchedulerBuilder.setCapacityNeedLinear(1); 
       
        //The scheduler is added to the Resource and the Resource is created
        secondReloadingPointBuilder.setReloadingScheduler(secondSchedulerBuilder.build());
        LSPResource secondReloadingPointAdapter = secondReloadingPointBuilder.build();
        
        //The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
        Id<LogisticsSolutionElement> secondReloadingElementId = Id.create("SecondReloadElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder secondReloadingElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(secondReloadingElementId );
		secondReloadingElementBuilder.setResource(secondReloadingPointAdapter);
		LogisticsSolutionElement secondReloadElement = secondReloadingElementBuilder.build();
		
		
		//The Carrier for distribution is created
		Id<Carrier> distributionCarrierId = Id.create("DistributionCarrier", Carrier.class);
		Id<VehicleType> distributionVehicleTypeId = Id.create("DistributionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder dsitributionVehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(distributionVehicleTypeId);
		dsitributionVehicleTypeBuilder.setCapacity(10);
		dsitributionVehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		dsitributionVehicleTypeBuilder.setCostPerTimeUnit(0.38);
		dsitributionVehicleTypeBuilder.setFixCost(49);
		dsitributionVehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType distributionType = dsitributionVehicleTypeBuilder.build();
		
		Id<Link> distributionLinkId = Id.createLinkId("(14 2) (14 3)");
		Id<Vehicle> distributionVehicleId = Id.createVehicleId("DistributionVehicle");
		CarrierVehicle distributionCarrierVehicle = CarrierVehicle.newInstance(distributionVehicleId, distributionLinkId, distributionType);

		CarrierCapabilities.Builder distributionCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		distributionCapabilitiesBuilder.addType(distributionType);
		distributionCapabilitiesBuilder.addVehicle(distributionCarrierVehicle);
		distributionCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities distributionCapabilities = distributionCapabilitiesBuilder.build();
		Carrier distributionCarrier = CarrierUtils.createCarrier( distributionCarrierId );
		distributionCarrier.setCarrierCapabilities(distributionCapabilities);
		
		//The distribution adapter i.e. the Resource is created
		Id<LSPResource> distributionAdapterId = Id.create("DistributionCarrierAdapter", LSPResource.class);
		UsecaseUtils.DistributionCarrierResourceBuilder distributionAdapterBuilder = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(distributionAdapterId, network );
		distributionAdapterBuilder.setCarrier(distributionCarrier);
		distributionAdapterBuilder.setLocationLinkId(distributionLinkId);
		
		//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		distributionAdapterBuilder.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler());
		LSPResource distributionAdapter = distributionAdapterBuilder.build();
		
		//The adapter is now inserted into the corresponding LogisticsSolutionElement of the only LogisticsSolution of the LSP
		Id<LogisticsSolutionElement> distributionElementId = Id.create("DistributionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder distributionBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(distributionElementId );
		distributionBuilder.setResource(distributionAdapter);
		LogisticsSolutionElement distributionElement =    distributionBuilder.build();
		
		//The Order of the logisticsSolutionElements is now specified
		collectionElement.connectWithNextElement(firstReloadElement);
		firstReloadElement.connectWithNextElement(mainRunElement);
		mainRunElement.connectWithNextElement(secondReloadElement);
		secondReloadElement.connectWithNextElement(distributionElement);


		//The SolutionElements are now inserted into the only LogisticsSolution of the LSP
		Id<LogisticsSolution> solutionId = Id.create("SolutionId", LogisticsSolution.class);
		LSPUtils.LogisticsSolutionBuilder completeSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(solutionId );
		completeSolutionBuilder.addSolutionElement(collectionElement);
		completeSolutionBuilder.addSolutionElement(firstReloadElement);
		completeSolutionBuilder.addSolutionElement(mainRunElement);
		completeSolutionBuilder.addSolutionElement(secondReloadElement);
		completeSolutionBuilder.addSolutionElement(distributionElement);
		LogisticsSolution completeSolution = completeSolutionBuilder.build();
		
		
		//The initial plan of the lsp is generated and the assigner and the solution from above are added
		LSPPlan completePlan = LSPUtils.createLSPPlan();
		ShipmentAssigner assigner = UsecaseUtils.createDeterministicShipmentAssigner();
		completePlan.setAssigner(assigner);
		completePlan.addSolution(completeSolution);
		
		LSPUtils.LSPBuilder completeLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
		completeLSPBuilder.setInitialPlan(completePlan);

		//The exogenous list of Resources for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder
		ArrayList<LSPResource> resourcesList = new ArrayList<>();
		resourcesList.add(collectionAdapter);
		resourcesList.add(firstReloadingPointAdapter);
		resourcesList.add(mainRunAdapter);
		resourcesList.add(secondReloadingPointAdapter);
		resourcesList.add(distributionAdapter);

//		SolutionScheduler forwardSolutionScheduler = LSPUtils.createForwardSolutionScheduler(); //Ist der "nicht einfache" Scheduler. TODO braucht der keine RessourcenLsite oder ähnliches? --> Offenbar ja, weil Null Pointer. argh!
//		completeLSPBuilder.setSolutionScheduler(forwardSolutionScheduler);

		SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
		completeLSPBuilder.setSolutionScheduler(simpleScheduler);
		
		return completeLSPBuilder.build();
		
	}
	
	private static Collection<LSPShipment> createInitialLSPShipments(Network network){
		ArrayList<LSPShipment> shipmentList = new ArrayList<>();
		ArrayList <Link> linkList = new ArrayList<>(network.getLinks().values());
		Random rand = new Random(1);
		 for(int i = 1; i < 6; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
	        	int capacityDemand = rand.nextInt(10);
	        	builder.setCapacityDemand(capacityDemand);
	        	
	        	while(true) {
	        		Collections.shuffle(linkList, rand);
	        		Link pendingToLink = linkList.get(0);
	        		if((pendingToLink.getFromNode().getCoord().getX() <= 18000 &&
	        			pendingToLink.getFromNode().getCoord().getY() <= 4000 &&
	        			pendingToLink.getFromNode().getCoord().getX() >= 14000 &&       			
	        			pendingToLink.getToNode().getCoord().getX() <= 18000 &&
	        			pendingToLink.getToNode().getCoord().getY() <= 4000  &&
	        			pendingToLink.getToNode().getCoord().getX() >= 14000	)) {
	        		   builder.setToLinkId(pendingToLink.getId());
	        		   break;	
	        		}
	        	
	        	}
	        	
	        	while(true) {
	        		Collections.shuffle(linkList, rand);
	        		Link pendingFromLink = linkList.get(0);
	        		if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getY() <= 4000    ) {
	        		   builder.setFromLinkId(pendingFromLink.getId());
	        		   break;	
	        		}
	        	
	        	}
	        	
	        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setEndTimeWindow(endTimeWindow);
	        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setStartTimeWindow(startTimeWindow);
	        	builder.setDeliveryServiceTime(capacityDemand * 60 );
	        	LSPShipment shipment = builder.build();
        	shipmentList.add(builder.build());
		} 	
		return shipmentList;
	}


	public static void main (String [] args) {
		
		//Set up required MATSim classes
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();
		
		 //Create LSP and shipments
        LSP lsp = createInitialLSP(network);
        Collection<LSPShipment> shipments =  createInitialLSPShipments(network);
        
        //assign the shipments to the LSP
        for(LSPShipment shipment : shipments) {
        	lsp.assignShipmentToLSP(shipment);
        }
        
        //schedule the LSP with the shipments and according to the scheduler of the Resource
        lsp.scheduleSolutions();
        
      //print the schedules for the assigned LSPShipments
        for(LSPShipment shipment : lsp.getShipments()) {
			ArrayList<ShipmentPlanElement> elementList = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
			elementList.sort( ShipmentUtils.createShipmentPlanElementComparator() );
			System.out.println("Shipment: " + shipment.getId());
			for(ShipmentPlanElement element : elementList) {
				System.out.println(element.getSolutionElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime());	
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
