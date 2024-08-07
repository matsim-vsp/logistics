/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2024 by the members listed in the COPYING,       *
  *                   LICENSE and WARRANTY file.                            *
  * email           : info at matsim dot org                                *
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  *   This program is free software; you can redistribute it and/or modify  *
  *   it under the terms of the GNU General Public License as published by  *
  *   the Free Software Foundation; either version 2 of the License, or     *
  *   (at your option) any later version.                                   *
  *   See also COPYING, LICENSE and WARRANTY file                           *
  *                                                                         *
  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.multipleChains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.LSPShipment;

/**
 *  This strategy removes **one** randomly selected shipment from logistic chain with the most shipments and reassign it to the chain with the closest chain.
 *  The distance is measured as the Euclidean distance between the shipment's destination and the resource's start link.
 *  This strategy allows to slowly change the plans and therefore follow the iterative learning process.
 *
 *  @author nrichter (during his master thesis @VSP)
 */
final class ProximityStrategyFactory {
  //This is ok so as long as it is **non-public**.
  //Before making it public, it should be configurable either via config or Injection.
  //KMT, KN (Jan'24)

  // yyyy This factory class contains a long anonymous class.  It seems that it should be the other way round: The anonymous class should be a proper
  // class, and the factory method (or maybe just normal constructor) should be contained in the class.  At some point, try to exchange.  kmt & kai, mar'24

  // @formatter:off

  private ProximityStrategyFactory() {} // class contains only static methods; do not instantiate

  static GenericPlanStrategy<LSPPlan, LSP> createStrategy(Network network) {

    GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup()));
    GenericPlanStrategyModule<LSPPlan> randomModule = new GenericPlanStrategyModule<>() {

          @Override
          public void prepareReplanning(ReplanningContext replanningContext) {}

          @Override
          public void handlePlan(LSPPlan lspPlan) {

            // Shifting shipments only makes sense for multiple chains
            if (lspPlan.getLogisticChains().size() < 2) return;

            LSP lsp = lspPlan.getLSP();
            double minDistance = Double.MAX_VALUE;
            LSPResource minDistanceResource = null;

            // get all shipments assigned to the LSP
            Map<Id<LSPShipment>, LSPShipment> shipmentById = new HashMap<>();
            for (LSPShipment lspShipment : lsp.getShipments()) {
              shipmentById.put(lspShipment.getId(), lspShipment);
            }

            // Retrieve all shipments in the logistic chains of the plan
            // These should be all shipments of the lsp, but not necessarily if shipments got lost
            ArrayList<LSPShipment> shipments = new ArrayList<>();
            for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
              for (Id<LSPShipment> id : logisticChain.getShipmentIds()) {
                LSPShipment shipment = shipmentById.get(id);
                if (shipment != null) {
                  shipments.add(shipment);
                }
              }
            }

            // pick a random shipment from the shipments contained in the plan
            int shipmentIndex = MatsimRandom.getRandom().nextInt(shipments.size());
            LSPShipment shipment = shipments.get(shipmentIndex);

            // Collect all resources of the logistic chains of the LSP plan
            ArrayList<LSPResource> resources = new ArrayList<>();
            for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
              for (LogisticChainElement logisticChainElement :
                  logisticChain.getLogisticChainElements()) {
                resources.add(logisticChainElement.getResource());
              }
            }

            // get the resource with the smallest distance to the shipment
            for (LSPResource resource : resources) {
              Link shipmentLink = network.getLinks().get(shipment.getTo());
              Link resourceLink = network.getLinks().get(resource.getStartLinkId());
              double distance =
                  NetworkUtils.getEuclideanDistance(
                      shipmentLink.getFromNode().getCoord(), resourceLink.getFromNode().getCoord());
              if (distance < minDistance) {
                minDistance = distance;
                minDistanceResource = resource;
              }
            }

            // add randomly picked shipment to chain with resource of the smallest distance
            for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
              for (LogisticChainElement logisticChainElement :
                  logisticChain.getLogisticChainElements()) {
                if (logisticChainElement.getResource().equals(minDistanceResource)) {
                  logisticChain.getShipmentIds().add(shipment.getId());
                }
              }
            }

            // remove the shipment from the previous logistic chain, can be the same as the new
            // logistic chain
            for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
              if (logisticChain.getShipmentIds().contains(shipment.getId())) {
                logisticChain.getShipmentIds().remove(shipment.getId());
                break;
              }
            }
          }

          @Override
          public void finishReplanning() {}
        };

    strategy.addStrategyModule(randomModule);
    return strategy;
  }
}
