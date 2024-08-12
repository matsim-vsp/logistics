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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.matsim.freight.logistics.shipment.LSPShipment;

/* package-private */ class WaitingShipmentsImpl implements WaitingShipments {

  private final List<LspShipmentWithTime> shipments;

  WaitingShipmentsImpl() {
    this.shipments = new ArrayList<>();
  }

  @Override
  public void addShipment(double time, LSPShipment lspShipment) {
    LspShipmentWithTime tuple = new LspShipmentWithTime(time, lspShipment);
    this.shipments.add(tuple);
    shipments.sort(Comparator.comparingDouble(LspShipmentWithTime::getTime));
  }

  @Override
  public Collection<LspShipmentWithTime> getSortedLspShipments() {
    shipments.sort(Comparator.comparingDouble(LspShipmentWithTime::getTime));
    return shipments;
  }

  public void clear() {
    shipments.clear();
  }

  @Override
  public Collection<LspShipmentWithTime> getLspShipmentsWTime() {
    return shipments;
  }

  @Override
  public String toString() {
    StringBuilder strb = new StringBuilder();
    strb.append("WaitingShipmentsImpl{").append("No of Shipments= ").append(shipments.size());
    if (!shipments.isEmpty()) {
      strb.append("; ShipmentIds=");
      for (LspShipmentWithTime shipment : getSortedLspShipments()) {
        strb.append("[").append(shipment.getLspShipment().getId()).append("]");
      }
    }
    strb.append('}');
    return strb.toString();
  }
}
