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

package org.matsim.freight.logistics.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LogisticChainElement;

class ScheduledShipmentUnload implements ShipmentPlanElement {

  private final double startTime;
  private final double endTime;
  private final LogisticChainElement element;
  private final Id<LSPResource> resourceId;
  private final Id<Carrier> carrierId;
  private final CarrierService carrierService;

  ScheduledShipmentUnload(ShipmentUtils.ScheduledShipmentUnloadBuilder builder) {
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.element = builder.element;
    this.resourceId = builder.resourceId;
    this.carrierId = builder.carrierId;
    this.carrierService = builder.carrierService;
  }

  @Override
  public String getElementType() {
    return "UNLOAD";
  }

  @Override
  public double getStartTime() {
    return startTime;
  }

  @Override
  public double getEndTime() {
    return endTime;
  }

  @Override
  public LogisticChainElement getLogisticChainElement() {
    return element;
  }

  @Override
  public Id<LSPResource> getResourceId() {
    return resourceId;
  }

  public Id<Carrier> getCarrierId() {
    return carrierId;
  }

  public CarrierService getCarrierService() {
    return carrierService;
  }
}
