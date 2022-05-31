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

package example.lsp.lspReplanning;

import lsp.*;
import lsp.controler.LSPModule;
import lsp.replanning.LSPReplanner;
import lsp.replanning.LSPReplanningModule;
import lsp.replanning.LSPReplanningModuleImpl;
import lsp.replanning.LSPReplanningUtils;
import lsp.scoring.LSPScoringListener;
import lsp.scoring.LSPScoringModuleDefaultImpl;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

/*package-private*/ class ExampleLSPReplanning {

	private static LSP createLSPWithReplanner(Network network) {
		//The Carrier for the resource of the sole LogisticsSolutionElement of the LSP is created
				Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
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

		Carrier carrier = CarrierUtils.createCarrier( carrierId );
				carrier.setCarrierCapabilities(capabilities);
				
				//The Adapter i.e. the Resource is created
				Id<LSPResource> adapterId = Id.create("CollectionCarrierAdapter", LSPResource.class);
				UsecaseUtils.CollectionCarrierAdapterBuilder adapterBuilder = UsecaseUtils.CollectionCarrierAdapterBuilder.newInstance(adapterId, network);
				
				//The scheduler for the Resource is created and added. This is where jsprit comes into play.
		adapterBuilder.setCollectionScheduler(UsecaseUtils.createDefaultCollectionCarrierScheduler());
				adapterBuilder.setCarrier(carrier);
				adapterBuilder.setLocationLinkId(collectionLinkId);
				LSPResource collectionAdapter = adapterBuilder.build();
				
				//The adapter is now inserted into the only LogisticsSolutionElement of the only LogisticsSolution of the LSP
				Id<LogisticsSolutionElement> elementId = Id.create("CollectionElement", LogisticsSolutionElement.class);
				LSPUtils.LogisticsSolutionElementBuilder collectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(elementId );
				collectionElementBuilder.setResource(collectionAdapter);
				LogisticsSolutionElement collectionElement = collectionElementBuilder.build();
				
				//The LogisticsSolutionElement is now inserted into the only LogisticsSolution of the LSP
				Id<LogisticsSolution> collectionSolutionId = Id.create("CollectionSolution", LogisticsSolution.class);
				LSPUtils.LogisticsSolutionBuilder collectionSolutionBuilder = LSPUtils.LogisticsSolutionBuilder.newInstance(collectionSolutionId );
				collectionSolutionBuilder.addSolutionElement(collectionElement);
				LogisticsSolution collectionSolution = collectionSolutionBuilder.build();
				
				//The initial plan of the lsp is generated and the assigner and the solution from above are added
				LSPPlan collectionPlan = LSPUtils.createLSPPlan();
				ShipmentAssigner assigner = UsecaseUtils.createDeterministicShipmentAssigner();
				collectionPlan.setAssigner(assigner);
				collectionPlan.addSolution(collectionSolution);
				
				LSPUtils.LSPBuilder collectionLSPBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
				collectionLSPBuilder.setInitialPlan(collectionPlan);
				
				//The exogenous list of Resoruces for the SolutionScheduler is compiled and the Scheduler is added to the LSPBuilder 
				ArrayList<LSPResource> resourcesList = new ArrayList<>();
				resourcesList.add(collectionAdapter);
				SolutionScheduler simpleScheduler = UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(resourcesList);
				collectionLSPBuilder.setSolutionScheduler(simpleScheduler);
		
		LSP lsp  = collectionLSPBuilder.build();
				
				//Create StrategyManager, insert it in Replanner and add it to the lsp;
				GenericStrategyManagerFactoryImpl factory = new GenericStrategyManagerFactoryImpl();
				// yyyy this feels quite odd.  The matsim GenericStrategyManager is heavyweight infrastructure, which exists
				// once in the system.  Does it really make sense to now have one per agent?  Maybe just program directly
				// what you want and need.  ??
				
				GenericStrategyManager<LSPPlan, LSP> manager = factory.createStrategyManager(lsp);
				LSPReplanner replanner = LSPReplanningUtils.createDefaultLSPReplanner(lsp);
				replanner.setStrategyManager(manager);
				lsp.setReplanner(replanner);
//		collectionLSPBuilder.setReplanner( replanner ) ;
		// yyyy set replanner in builder. kai, sep'18
		
		return lsp;
	}
	
	private static Collection<LSPShipment> createInitialLSPShipments(Network network){
		ArrayList<LSPShipment> shipmentList = new ArrayList<>();
		ArrayList <Link> linkList = new ArrayList<>(network.getLinks().values());

		//Create five LSPShipments that are located in the left half of the network.
		for(int i = 1; i < 6; i++) {
	        	Id<LSPShipment> id = Id.create(i, LSPShipment.class);
	        	ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id );
	        	Random random = new Random(1);
	        	int capacityDemand = random.nextInt(4);
	        	builder.setCapacityDemand(capacityDemand);
	        	
	        	while(true) {
	        		Collections.shuffle(linkList, random);
	        		Link pendingFromLink = linkList.get(0);
	        		if(pendingFromLink.getFromNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getFromNode().getCoord().getY() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getX() <= 4000 &&
	        		   pendingFromLink.getToNode().getCoord().getY() <= 4000) {
	        		   builder.setFromLinkId(pendingFromLink.getId());
	        		   break;	
	        		}	
	        	}
	        	
	        	builder.setToLinkId(Id.createLinkId("(4 2) (4 3)"));
	        	TimeWindow endTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setEndTimeWindow(endTimeWindow);
	        	TimeWindow startTimeWindow = TimeWindow.newInstance(0,(24*3600));
	        	builder.setStartTimeWindow(startTimeWindow);
	        	builder.setDeliveryServiceTime(capacityDemand * 60 );
	        	shipmentList.add(builder.build());
		 } 	
	    return shipmentList;
	}
	
	
	public static void main(String[]args) {
		//Set up required MATSim classes
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
		Network network = scenario.getNetwork();
			
		//Create LSP and shipments
		LSP lsp = createLSPWithReplanner(network);
		Collection<LSPShipment> shipments =  createInitialLSPShipments(network);
			
		//assign the shipments to the LSP
		for(LSPShipment shipment : shipments) {
		   	lsp.assignShipmentToLSP(shipment);
		}
		        
		
		//Prepare LSPModule and add the LSP
		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(lsp);
		LSPs lsps = new LSPs(lspList);
		LSPUtils.addLSPs( scenario, lsps );

		//Start the Mobsim two iterations are necessary for replanning
		Controler controler = new Controler(config);
		controler.addOverridingModule( new LSPModule() );
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.bind( LSPReplanningModule.class ).to( LSPReplanningModuleImpl.class );
				this.bind( LSPScoringListener.class ).to( LSPScoringModuleDefaultImpl.class );
			}
		} );
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(4);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile("scenarios/2regions/2regions-network.xml");
		controler.run();
		
		System.out.println("Shipments delivered today:");
		for(LSPShipment shipment : lsp.getSelectedPlan().getSolutions().iterator().next().getShipments()) {
			System.out.println(shipment.getId());
		}
		
		lsp.getShipments().removeAll(lsp.getSelectedPlan().getSolutions().iterator().next().getShipments());
		
		System.out.println("Shipments delivered tomorrow:");
		for(LSPShipment shipment : lsp.getShipments()) {
			System.out.println(shipment.getId());
		}
		
	}
	
	
}
