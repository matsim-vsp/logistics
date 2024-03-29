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

package org.matsim.freight.logistics.example.lsp.lspReplanning;

import jakarta.inject.Provider;
import java.util.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.GenericStrategyManagerImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/*package-private*/ class ExampleLSPReplanning {

  private static LSP createLSPWithReplanner(Network network) {
    // The Carrier for the resource of the sole LogisticsSolutionElement of the LSP is created
    Id<Carrier> carrierId = Id.create("CollectionCarrier", Carrier.class);
    Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
    CarrierVehicleType.Builder vehicleTypeBuilder =
        CarrierVehicleType.Builder.newInstance(vehicleTypeId);
    vehicleTypeBuilder.setCapacity(10);
    vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
    vehicleTypeBuilder.setCostPerTimeUnit(0.38);
    vehicleTypeBuilder.setFixCost(49);
    vehicleTypeBuilder.setMaxVelocity(50 / 3.6);
    org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();

    Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
    Id<Vehicle> vollectionVehicleId = Id.createVehicleId("CollectionVehicle");
    CarrierVehicle carrierVehicle =
        CarrierVehicle.newInstance(vollectionVehicleId, collectionLinkId, collectionType);

    CarrierCapabilities.Builder capabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
    capabilitiesBuilder.addType(collectionType);
    capabilitiesBuilder.addVehicle(carrierVehicle);
    capabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
    CarrierCapabilities capabilities = capabilitiesBuilder.build();

    Carrier carrier = CarriersUtils.createCarrier(carrierId);
    carrier.setCarrierCapabilities(capabilities);

    // The Resource i.e. the Resource is created
    LSPResource collectionResource =
        ResourceImplementationUtils.CollectionCarrierResourceBuilder.newInstance(carrier, network)
            .setCollectionScheduler(
                ResourceImplementationUtils.createDefaultCollectionCarrierScheduler())
            .setLocationLinkId(collectionLinkId)
            .build();

    // The adapter is now inserted into the only LogisticsSolutionElement of the only
    // LogisticsSolution of the LSP
    LogisticChainElement collectionElement =
        LSPUtils.LogisticChainElementBuilder.newInstance(
                Id.create("CollectionElement", LogisticChainElement.class))
            .setResource(collectionResource)
            .build();

    // The LogisticsSolutionElement is now inserted into the only LogisticsSolution of the LSP
    LogisticChain collectionSolution =
        LSPUtils.LogisticChainBuilder.newInstance(
                Id.create("CollectionSolution", LogisticChain.class))
            .addLogisticChainElement(collectionElement)
            .build();

    // The initial plan of the lsp is generated and the assigner and the solution from above are
    // added
    LSPPlan collectionPlan = LSPUtils.createLSPPlan();
    InitialShipmentAssigner assigner =
        ResourceImplementationUtils.createSingleLogisticChainShipmentAssigner();
    collectionPlan.setInitialShipmentAssigner(assigner);
    collectionPlan.addLogisticChain(collectionSolution);

    LSPUtils.LSPBuilder collectionLSPBuilder =
        LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class));
    collectionLSPBuilder.setInitialPlan(collectionPlan);

    // The exogenous list of Resoruces for the SolutionScheduler is compiled and the Scheduler is
    // added to the LSPBuilder
    ArrayList<LSPResource> resourcesList = new ArrayList<>();
    resourcesList.add(collectionResource);
    LogisticChainScheduler simpleScheduler =
        ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(resourcesList);
    collectionLSPBuilder.setLogisticChainScheduler(simpleScheduler);

    // Create StrategyManager, insert it in Replanner and add it to the lsp;
    //				TomorrowShipmentAssignerStrategyManagerFactoryImpl factory = new
    // TomorrowShipmentAssignerStrategyManagerFactoryImpl();
    // yyyy this feels quite odd.  The matsim GenericStrategyManager is heavyweight infrastructure,
    // which exists
    // once in the system.  Does it really make sense to now have one per agent?  Maybe just program
    // directly
    // what you want and need.  ??

    //		GenericStrategyManager<LSPPlan, LSP> strategyManager = new GenericStrategyManagerImpl<>();
    //
    //		ShipmentAssigner maybeTodayAssigner = new MaybeTodayAssigner();
    //		maybeTodayAssigner.setLSP(lsp);
    //
    //		strategyManager.addStrategy( new
    // TomorrowShipmentAssignerStrategyFactory(maybeTodayAssigner).createStrategy(), null, 1);
    //
    //		LSPReplanner replanner = LSPReplanningUtils.createDefaultLSPReplanner(strategyManager);
    //
    //				replanner.setStrategyManager(manager);
    //		lsp.setReplanner(replanner);
    //		collectionLSPBuilder.setReplanner( replanner ) ;
    // yyyy set replanner in builder. kai, sep'18

    return collectionLSPBuilder.build();
  }

  private static Collection<LSPShipment> createInitialLSPShipments(Network network) {
    List<LSPShipment> shipmentList = new ArrayList<>();
    ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

    // Create five LSPShipments that are located in the left half of the network.
    for (int i = 1; i < 6; i++) {
      Id<LSPShipment> id = Id.create(i, LSPShipment.class);
      ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
      Random random = new Random(1);
      int capacityDemand = random.nextInt(4);
      builder.setCapacityDemand(capacityDemand);

      while (true) {
        Collections.shuffle(linkList, random);
        Link pendingFromLink = linkList.get(0);
        if (pendingFromLink.getFromNode().getCoord().getX() <= 4000
            && pendingFromLink.getFromNode().getCoord().getY() <= 4000
            && pendingFromLink.getToNode().getCoord().getX() <= 4000
            && pendingFromLink.getToNode().getCoord().getY() <= 4000) {
          builder.setFromLinkId(pendingFromLink.getId());
          break;
        }
      }

      builder.setToLinkId(Id.createLinkId("(4 2) (4 3)"));
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
    // Set up required MATSim classes
    Config config = new Config();
    config.addCoreModules();
    Scenario scenario = ScenarioUtils.createScenario(config);
    new MatsimNetworkReader(scenario.getNetwork())
        .readFile("scenarios/2regions/2regions-network.xml");
    Network network = scenario.getNetwork();

    // Create LSP and shipments
    LSP lsp = createLSPWithReplanner(network);
    Collection<LSPShipment> shipments = createInitialLSPShipments(network);

    // assign the shipments to the LSP
    for (LSPShipment shipment : shipments) {
      lsp.assignShipmentToLSP(shipment);
    }

    // Prepare LSPModule and add the LSP
    ArrayList<LSP> lspList = new ArrayList<>();
    lspList.add(lsp);
    LSPs lsps = new LSPs(lspList);
    LSPUtils.addLSPs(scenario, lsps);

    // Start the Mobsim two iterations are necessary for replanning
    Controler controler = new Controler(config);
    controler.addOverridingModule(new LSPModule());
    controler.addOverridingModule(
        new AbstractModule() {
          @Override
          public void install() {
            bind(LSPStrategyManager.class)
                .toProvider(
                    new Provider<LSPStrategyManager>() {
                      @Override
                      public LSPStrategyManager get() {
                        LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
                        {
                          InitialShipmentAssigner maybeTodayAssigner = new MaybeTodayAssigner();
                          strategyManager.addStrategy(
                              new TomorrowShipmentAssignerStrategyFactory(maybeTodayAssigner)
                                  .createStrategy(),
                              null,
                              1);
                        }
                        return strategyManager;
                      }
                    });
          }
        });
    GenericStrategyManager<LSPPlan, LSP> strategyManager = new GenericStrategyManagerImpl<>();

    InitialShipmentAssigner maybeTodayAssigner = new MaybeTodayAssigner();

    strategyManager.addStrategy(
        new TomorrowShipmentAssignerStrategyFactory(maybeTodayAssigner).createStrategy(), null, 1);

    config.controller().setFirstIteration(0);
    config.controller().setLastIteration(4);
    config.controller().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
    config.network().setInputFile("scenarios/2regions/2regions-network.xml");
    // The VSP default settings are designed for person transport simulation. After talking to Kai,
    // they will be set to WARN here. Kai MT may'23
    controler
        .getConfig()
        .vspExperimental()
        .setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
    controler.run();

    System.out.println("Shipments delivered today:");
    for (Id<LSPShipment> lspShipmentId :
        lsp.getSelectedPlan().getLogisticChains().iterator().next().getShipmentIds()) {
      System.out.println(lspShipmentId);
    }

    lsp.getShipments()
        .removeAll(lsp.getSelectedPlan().getLogisticChains().iterator().next().getShipmentIds());

    System.out.println("Shipments delivered tomorrow:");
    for (LSPShipment shipment : lsp.getShipments()) {
      System.out.println(shipment.getId());
    }
  }
}
