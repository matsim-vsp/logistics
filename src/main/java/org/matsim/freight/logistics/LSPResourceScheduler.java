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

package org.matsim.freight.logistics;

import java.util.ArrayList;
import java.util.Comparator;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentUtils;

/**
 * Resources are scheduled separately by calling their individual scheduling algorithm.
 *
 * <p>Within this algorithm, some methods are abstract, whereas others have a default implementation
 * for forward scheduling. The former ones are specified in a suitable way by the corresponding
 * Resource whereas the latter are only specified in the abstract parent class in order to
 * coordinate the way in which the LSPShipments are handed over between subsequent Resources. The
 * abstract methods deal with aspects that are specific to the Resource which contains the
 * implementation of the ResourceScheduler.
 *
 * <p>Forwarding of LSPShipments is done by the two methods presortIncomingShipments() and
 * switchHandledShipments(int bufferTime).
 */
public abstract class LSPResourceScheduler {

  protected LSPResource resource;
  protected ArrayList<LspShipmentWithTime> lspShipmentsWithTime;

  protected LSPPlan lspPlan;

  public final void scheduleShipments(LSPPlan lspPlan, LSPResource resource, int bufferTime) {
    this.lspPlan = lspPlan;
    this.resource = resource;
    this.lspShipmentsWithTime = new ArrayList<>();
    initializeValues(resource);
    presortIncomingShipments();
    scheduleResource();
    updateShipments();
    switchHandledShipments(bufferTime);
    lspShipmentsWithTime.clear();
  }

  /**
   * Is in charge of the initialization of the actual scheduling process for the concerned Resource.
   * Depending on the concrete shape of this process, there are mainly values to be deleted that are
   * still stored from the previous iteration or the infrastructure for the used algorithm has to be
   * set up.
   *
   * @param resource The LSPRessource
   */
  protected abstract void initializeValues(LSPResource resource);

  /** Controls the actual scheduling process that depends on the shape and task of the Resource. */
  protected abstract void scheduleResource();

  /**
   * Endows the involved {@link LSPShipment}s with information that resulted from the scheduling in
   * a narrow sense in scheduleResource(). The information can be divided into two main components.
   * 1.) the schedule of the {@link LSPShipment}s is updated if necessary 2.) the information for a
   * later logging of the is added.
   */
  protected abstract void updateShipments();

  private void presortIncomingShipments() {
    this.lspShipmentsWithTime = new ArrayList<>();
    for (LogisticChainElement element : resource.getClientElements()) {
      lspShipmentsWithTime.addAll(element.getIncomingShipments().getLspShipmentsWTime());
    }
    lspShipmentsWithTime.sort(Comparator.comparingDouble(LspShipmentWithTime::getTime));
  }

  private void switchHandledShipments(int bufferTime) {
    for (LspShipmentWithTime lspShipmentWithTime : lspShipmentsWithTime) {
      var shipmentPlan =
          ShipmentUtils.getOrCreateShipmentPlan(lspPlan, lspShipmentWithTime.getLspShipment().getId());
      double endOfTransportTime = shipmentPlan.getMostRecentEntry().getEndTime() + bufferTime;
      LspShipmentWithTime outgoingTuple =
          new LspShipmentWithTime(endOfTransportTime, lspShipmentWithTime.getLspShipment());
      for (LogisticChainElement element : resource.getClientElements()) {
        if (element.getIncomingShipments().getLspShipmentsWTime().contains(lspShipmentWithTime)) {
          element.getOutgoingShipments().getLspShipmentsWTime().add(outgoingTuple);
          element.getIncomingShipments().getLspShipmentsWTime().remove(lspShipmentWithTime);
          if (element.getNextElement() != null) {
            element.getNextElement().getIncomingShipments().getLspShipmentsWTime().add(outgoingTuple);
            element.getOutgoingShipments().getLspShipmentsWTime().remove(outgoingTuple);
          }
        }
      }
    }
  }
}
