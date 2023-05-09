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

import lsp.LSPCarrierResource;
import lsp.LSPResource;
import lsp.LSPSimulationTracker;
import lsp.LogisticChainElement;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentLeg;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.events.FreightServiceEndEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightServiceEndEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

class CollectionServiceEndEventHandler implements AfterMobsimListener, FreightServiceEndEventHandler, LSPSimulationTracker<LSPShipment> {

	private final CarrierService carrierService;
	private final LogisticChainElement logisticChainElement;
	private final LSPCarrierResource resource;
	private LSPShipment lspShipment;

	public CollectionServiceEndEventHandler(CarrierService carrierService, LSPShipment lspShipment, LogisticChainElement element, LSPCarrierResource resource) {
		this.carrierService = carrierService;
		this.lspShipment = lspShipment;
		this.logisticChainElement = element;
		this.resource = resource;
	}


	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(FreightServiceEndEvent event) {
		if (event.getServiceId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
			logLoad(event);
			logTransport_beginOfTransport(event);
		}
	}

	private void logLoad(FreightServiceEndEvent event) {
		ShipmentUtils.LoggedShipmentLoadBuilder builder = ShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
		builder.setStartTime(event.getTime() - event.getServiceDuration());
		builder.setEndTime(event.getTime());
		builder.setLogisticsChainElement(logisticChainElement);
		builder.setResourceId(resource.getId());
		builder.setLinkId(event.getLinkId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentPlanElement loggedShipmentLoad = builder.build();
		String idString = loggedShipmentLoad.getResourceId() + "" + loggedShipmentLoad.getLogisticChainElement().getId() + "" + loggedShipmentLoad.getElementType();
		Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().addPlanElement(loadId, loggedShipmentLoad);
	}

	/**
	 * The information of the transportations end, will be added in {@link CollectionTourEndEventHandler}
	 * <p>
	 * This goes, because for collection the goods are brought to the hub/depot (Service-based approach)
	 * --> The service-activity is the collection of the good (loading in)
	 * <p>
	 * <b>Note:</b> This can be totally different, once we are using Shipments instead inside the carrier.
	 * --> As soon we are doing that, we should have pickup and drop-off activities, and therefore the corresponding events.
	 *
	 * @param event
	 */
	private void logTransport_beginOfTransport(FreightServiceEndEvent event) {
		ShipmentUtils.LoggedShipmentTransportBuilder builder = ShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
		builder.setStartTime(event.getTime());
		builder.setLogisticChainElement(logisticChainElement);
		builder.setResourceId(resource.getId());
		builder.setFromLinkId(event.getLinkId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentLeg transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getLogisticChainElement().getId() + "" + transport.getElementType();
		Id<ShipmentPlanElement> transportId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().addPlanElement(transportId, transport);
	}


	public CarrierService getCarrierService() {
		return carrierService;
	}


	public LSPShipment getLspShipment() {
		return lspShipment;
	}


	public LogisticChainElement getElement() {
		return logisticChainElement;
	}


	public Id<LSPResource> getResourceId() {
		return resource.getId();
	}


	@Override
	public void setEmbeddingContainer(LSPShipment pointer) {
		this.lspShipment = pointer;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
	}
}
