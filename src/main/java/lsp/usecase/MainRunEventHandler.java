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
import lsp.LSPSimulationTracker;
import lsp.LogisticsSolutionElement;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentLeg;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.ServiceActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.events.FreightServiceEndEvent;
import org.matsim.contrib.freight.events.FreightServiceStartEvent;
import org.matsim.contrib.freight.events.FreightTourStartEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightServiceEndEventHandler;
import org.matsim.contrib.freight.events.eventhandler.FreightServiceStartEventHandler;
import org.matsim.contrib.freight.events.eventhandler.FreightTourStartEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import java.util.HashMap;
import java.util.LinkedHashMap;

/*package-private*/ class MainRunEventHandler implements AfterMobsimListener, FreightTourStartEventHandler, FreightServiceStartEventHandler, FreightServiceEndEventHandler, LSPSimulationTracker<LSPShipment> {

	private final CarrierService carrierService;
	private final LogisticsSolutionElement solutionElement;
	private final LSPCarrierResource resource;
	private LSPShipment lspShipment;
	private final Tour tour;

	MainRunEventHandler(LSPShipment lspShipment, CarrierService carrierService, LogisticsSolutionElement solutionElement, LSPCarrierResource resource, Tour tour) {
		this.lspShipment = lspShipment;
		this.carrierService = carrierService;
		this.solutionElement = solutionElement;
		this.resource = resource;
		this.tour = tour;
	}


	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	HashMap<Id<Carrier>, HashMap > servicesOfCarrier = new LinkedHashMap<>();
	HashMap<Id<CarrierService>, Double> timeOfServiceStart = new LinkedHashMap<>();
	@Override public void handleEvent(FreightServiceStartEvent event) {
		timeOfServiceStart.put(event.getServiceId(), event.getTime());
		servicesOfCarrier.put(event.getCarrierId(), timeOfServiceStart);
	}

	@Override
	public void handleEvent(FreightServiceEndEvent event) {
			for (TourElement tourElement : tour.getTourElements()) {
				if (tourElement instanceof ServiceActivity serviceActivity) {
					if (serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
						logUnload(event);
						logTransport(event);
					}
				}
			}
	}

	@Override
	public void handleEvent(FreightTourStartEvent event) {
		if (event.getTourId().equals(tour.getId())) {
			for (TourElement tourElement : tour.getTourElements()) {
				if (tourElement instanceof ServiceActivity serviceActivity) {
					if (serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
						logLoad(event, tour);
						logTransport(event, tour);
					}
				}
			}
		}
	}


	//--- internal methods

	private void logLoad(FreightTourStartEvent event, Tour tour) {
		ShipmentUtils.LoggedShipmentLoadBuilder builder = ShipmentUtils.LoggedShipmentLoadBuilder.newInstance();
		builder.setCarrierId(event.getCarrierId());
		builder.setLinkId(event.getLinkId());
		builder.setStartTime(event.getTime() - getCumulatedLoadingTime(tour));
		builder.setEndTime(event.getTime());
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		ShipmentPlanElement loggedShipmentLoad = builder.build();
		String idString = loggedShipmentLoad.getResourceId() + "" + loggedShipmentLoad.getSolutionElement().getId() + "" + loggedShipmentLoad.getElementType();
		Id<ShipmentPlanElement> loadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().addPlanElement(loadId, loggedShipmentLoad);
	}


	private double getCumulatedLoadingTime(Tour tour) {
		double cumulatedLoadingTime = 0;
		for (TourElement tourElement : tour.getTourElements()) {
			if (tourElement instanceof ServiceActivity serviceActivity) {
				cumulatedLoadingTime = cumulatedLoadingTime + serviceActivity.getDuration();
			}
		}
		return cumulatedLoadingTime;
	}

	private void logTransport(FreightTourStartEvent event, Tour tour) {
		ShipmentUtils.LoggedShipmentTransportBuilder builder = ShipmentUtils.LoggedShipmentTransportBuilder.newInstance();
		builder.setCarrierId(event.getCarrierId());
		builder.setFromLinkId(event.getLinkId());
		builder.setToLinkId(tour.getEndLinkId());
		builder.setStartTime(event.getTime());
		builder.setLogisticsSolutionElement(solutionElement);
		builder.setResourceId(resource.getId());
		ShipmentLeg transport = builder.build();
		String idString = transport.getResourceId() + "" + transport.getSolutionElement().getId() + "" + transport.getElementType();
		Id<ShipmentPlanElement> transportId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getLog().addPlanElement(transportId, transport);
	}

	/// existing



	private void logUnload(FreightServiceEndEvent event) {
		if (servicesOfCarrier.containsKey(event.getCarrierId())) {
			double startTimeOfService = (double) servicesOfCarrier.get(event.getCarrierId()).get(event.getServiceId());
			ShipmentUtils.LoggedShipmentUnloadBuilder builder = ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
			builder.setStartTime(startTimeOfService);
			builder.setEndTime(event.getTime());
			builder.setLogisticsSolutionElement(solutionElement);
			builder.setResourceId(resource.getId());
			builder.setCarrierId(event.getCarrierId());
			ShipmentPlanElement unload = builder.build();
			String idString = unload.getResourceId() + "" + unload.getSolutionElement().getId() + "" + unload.getElementType();
			Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
			lspShipment.getLog().addPlanElement(unloadId, unload);
		}
	}

	private void logTransport(FreightServiceEndEvent event) {
		if (servicesOfCarrier.containsKey(event.getCarrierId())) {
			String idString = resource.getId() + "" + solutionElement.getId() + "" + "TRANSPORT";
			Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
			ShipmentPlanElement abstractPlanElement = lspShipment.getLog().getPlanElements().get(id);
			if (abstractPlanElement instanceof ShipmentLeg transport) {
				transport.setEndTime(event.getTime());
				transport.setToLinkId(event.getLinkId());
			}
		}
	}

	private double getTotalUnloadingTime(Tour tour) {
		double totalTime = 0;
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof ServiceActivity serviceActivity) {
				totalTime = totalTime + serviceActivity.getDuration();
			}
		}
		return totalTime;
	}


	public LSPShipment getLspShipment() {
		return lspShipment;
	}


	public CarrierService getCarrierService() {
		return carrierService;
	}


	public LogisticsSolutionElement getSolutionElement() {
		return solutionElement;
	}


	public LSPCarrierResource getResource() {
		return resource;
	}


	@Override public void setEmbeddingContainer( LSPShipment pointer ){
		this.lspShipment = pointer;
	}

	@Override public void notifyAfterMobsim( AfterMobsimEvent event ){
	}

}
