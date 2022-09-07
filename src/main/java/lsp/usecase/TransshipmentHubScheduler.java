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

import java.util.ArrayList;

import lsp.shipment.ShipmentUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;

import lsp.LogisticsSolutionElement;
import lsp.ShipmentWithTime;
import lsp.LSPResource;
import lsp.LSPResourceScheduler;
import lsp.shipment.ShipmentPlanElement;

/*package-private*/ class TransshipmentHubScheduler extends LSPResourceScheduler {

	final Logger log = LogManager.getLogger(TransshipmentHubScheduler.class);
	private final double capacityNeedLinear;
	private final double capacityNeedFixed;
	private TransshipmentHub transshipmentHub;
	private TranshipmentHubTourEndEventHandler eventHandler;

	TransshipmentHubScheduler(UsecaseUtils.TranshipmentHubSchedulerBuilder builder) {
		this.shipments = new ArrayList<>();
		this.capacityNeedLinear = builder.getCapacityNeedLinear();
		this.capacityNeedFixed = builder.getCapacityNeedFixed();

	}
	
	@Override protected void initializeValues( LSPResource resource ) {
//		if(resource.getClass() == TranshipmentHub.class){
		this.transshipmentHub = (TransshipmentHub) resource;
//		}
	}
	
	@Override protected void scheduleResource() {
		for( ShipmentWithTime tupleToBeAssigned: shipments){
			handleWaitingShipment(tupleToBeAssigned);
		}
	}

	@Override @Deprecated //TODO Method has no content, KMT Jul'20
	protected void updateShipments() {
		log.error("This method is not implemented. Nothing will happen here. ");
	}


	private void handleWaitingShipment(ShipmentWithTime tupleToBeAssigned) {
		updateSchedule(tupleToBeAssigned);
		addShipmentToEventHandler(tupleToBeAssigned);
	}

	private void updateSchedule(ShipmentWithTime tuple) {
		ShipmentUtils.ScheduledShipmentHandleBuilder builder = ShipmentUtils.ScheduledShipmentHandleBuilder.newInstance();
		builder.setStartTime(tuple.getTime());
		builder.setEndTime(tuple.getTime() + capacityNeedFixed + capacityNeedLinear * tuple.getShipment().getSize());
		builder.setResourceId(transshipmentHub.getId());
		for (LogisticsSolutionElement element : transshipmentHub.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				builder.setLogisticsSolutionElement(element);
			}
		}
		builder.setLinkId(transshipmentHub.getStartLinkId());
		ShipmentPlanElement handle = builder.build();
		String idString = handle.getResourceId() + "" + handle.getSolutionElement().getId() + "" + handle.getElementType();
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		tuple.getShipment().getShipmentPlan().addPlanElement(id, handle);
	}

	private void addShipmentToEventHandler(ShipmentWithTime tuple) {
		for (LogisticsSolutionElement element : transshipmentHub.getClientElements()) {
			if (element.getIncomingShipments().getShipments().contains(tuple)) {
				eventHandler.addShipment(tuple.getShipment(), element);
				break;
			}
		}
	}

	public double getCapacityNeedLinear() {
		return capacityNeedLinear;
	}


	public double getCapacityNeedFixed() {
		return capacityNeedFixed;
	}


	public void setTranshipmentHub(TransshipmentHub transshipmentHub) {
		this.transshipmentHub = transshipmentHub;
	}

	public void setEventHandler(TranshipmentHubTourEndEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

}
