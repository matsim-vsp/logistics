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

package org.matsim.freight.logistics.resourceImplementations;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.freight.logistics.LSPCarrierResource;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPSimulationTracker;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.events.HandlingInHubStartsEvent;
import org.matsim.freight.logistics.resourceImplementations.transshipmentHub.TransshipmentHubResource;
import org.matsim.freight.logistics.shipment.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.Tour.ServiceActivity;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.events.CarrierTourEndEvent;
import org.matsim.freight.carriers.events.eventhandler.CarrierTourEndEventHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LSPTourEndEventHandler implements AfterMobsimListener, LSPSimulationTracker<LSPResource>, CarrierTourEndEventHandler {
// Todo: I have made it (temporarily) public because of junit tests :( -- need to find another way to do the junit testing. kmt jun'23

	private Scenario scenario;
	private HashMap<CarrierService, TransshipmentHubEventHandlerPair> servicesWaitedFor;
	private TransshipmentHubResource transshipmentHubResource;
	private Id<LSPResource> resourceId;
//	private Id<Link> linkId;
	private EventsManager eventsManager;

	private CarrierService carrierService;
	private LogisticChainElement logisticChainElement;
	private LSPCarrierResource resource;
	private LSPShipment lspShipment;
//	private Tour tour;


	/**
	 * This is a TourEndEvent-Handler, doing some stuff regarding the {@link TransshipmentHubResource}.
	 *
	 * @param transshipmentHubResource hub
	 * @param scenario The scenario. Is used to get the Carrier(s).
	 */
	public LSPTourEndEventHandler( TransshipmentHubResource transshipmentHubResource, Scenario scenario ) {
		this.transshipmentHubResource = transshipmentHubResource;
//		this.linkId = transshipmentHubResource.getEndLinkId();
		this.resourceId = transshipmentHubResource.getId();
		this.scenario = scenario;
		this.servicesWaitedFor = new HashMap<>();
		this.transshipmentHubResource.addSimulationTracker(this);
	}

	public LSPTourEndEventHandler( LSPShipment lspShipment, CarrierService carrierService, LogisticChainElement logisticChainElement, LSPCarrierResource resource ) {
		this.lspShipment = lspShipment;
		this.carrierService = carrierService;
		this.logisticChainElement = logisticChainElement;
		this.resource = resource;
	}

	public LSPTourEndEventHandler( CarrierService carrierService, LSPShipment lspShipment, LogisticChainElement element, LSPCarrierResource resource ) {
		this.carrierService = carrierService;
		this.lspShipment = lspShipment;
		this.logisticChainElement = element;
		this.resource = resource;
	}


	@Override
	public void setEmbeddingContainer(LSPResource pointer) {
	}

	@Override public void setEventsManager( EventsManager eventsManager ) {
		this.eventsManager = eventsManager;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		servicesWaitedFor.clear(); // cleanup after Mobsim ends (instead of doing it in reset() = before Mobsim starts.) kmt oct'22
	}

	@Override
	public void reset(int iteration) {
		// not implemented; cleanup is done after Mobsim ends, because the internal state is (re)set before Mobsim starts.
		// --> cleaning up here is too late.
		// This is maybe not ideal, but works; kmt oct'22
	}

	public void addShipment(LSPShipment shipment, LogisticChainElement logisticChainElement, ShipmentPlan shipmentPlan) {
		TransshipmentHubEventHandlerPair pair = new TransshipmentHubEventHandlerPair(shipment, logisticChainElement);

		for (ShipmentPlanElement planElement : shipmentPlan.getPlanElements().values()) {
			if (planElement instanceof ShipmentLeg transport) {
				if (transport.getLogisticChainElement().getNextElement() == logisticChainElement) {
					servicesWaitedFor.put(transport.getCarrierService(), pair);
				}
			}
		}
	}


//	@Override
//	public void handleEvent(CarrierTourEndEvent event) {
//		if (event.getTourId().equals(tour.getId())) {
//			for (TourElement tourElement : tour.getTourElements()) {
//				if (tourElement instanceof ServiceActivity serviceActivity) {
//					if (serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
//						logUnload(event);
//						logTransport(event);
//					}
//				}
//			}
//		}
//	}

	@Override
	public void handleEvent(CarrierTourEndEvent event) {

		// find tour based on event:
		Tour tour = null;
		Carrier carrier = CarriersUtils.getCarriers(scenario).getCarriers().get(event.getCarrierId());
		Collection<ScheduledTour> scheduledTours = carrier.getSelectedPlan().getScheduledTours();
		for (ScheduledTour scheduledTour : scheduledTours) {
			if (scheduledTour.getTour().getId() == event.getTourId()) {
				tour = scheduledTour.getTour();
				break;
			}
		}

//		if (event.getTourId().equals(tour.getId())){
		// previously, the logit was to have one such event handler per tour, and to check here if the event corresponds to the tour.  Now this is ONE events handler for all tours.


			for (TourElement element : tour.getTourElements()) {
				if (element instanceof ServiceActivity serviceActivity) {
					if (serviceActivity.getService().getId() == carrierService.getId() && event.getCarrierId() == resource.getCarrier().getId()) {
						logTransport(event, tour);
						logUnload(event, tour);
					}
				}
			}
//		}


//		if ((event.getLinkId() == this.linkId)) {
//			assert tour != null;

			if (ResourceImplementationUtils.getCarrierType(carrier) == ResourceImplementationUtils.CARRIER_TYPE.mainRunCarrier) {
				if (allShipmentsOfTourEndInOnePoint(tour)) {
					for (TourElement tourElement : tour.getTourElements()) {
						if (tourElement instanceof ServiceActivity serviceActivity) {
							if (serviceActivity.getLocation() == transshipmentHubResource.getStartLinkId()
									&& allServicesAreInOnePoint(tour)
									&& (tour.getStartLinkId() != transshipmentHubResource.getStartLinkId())) {
								logHandlingInHub(serviceActivity.getService(), event.getTime(), event.getLinkId() );
							}
						}
					}
				}
			} else if ((ResourceImplementationUtils.getCarrierType(carrier) == ResourceImplementationUtils.CARRIER_TYPE.collectionCarrier)) {
				for (TourElement tourElement : tour.getTourElements()) {
					if (tourElement instanceof ServiceActivity serviceActivity ) {
						if (tour.getEndLinkId() == transshipmentHubResource.getStartLinkId())
							logHandlingInHub(serviceActivity.getService(), event.getTime(), event.getLinkId() );
					}
				}
			}
//		}
	}

	private boolean allShipmentsOfTourEndInOnePoint(Tour tour) {
		boolean allShipmentsOfTourEndInOnePoint = true;
		for (TourElement tourElement : tour.getTourElements()) {
			if (tourElement instanceof ServiceActivity serviceActivity) {
				if (!servicesWaitedFor.containsKey(serviceActivity.getService())) {
					return false;
				}
			}
		}
		return allShipmentsOfTourEndInOnePoint;
	}

	private void logHandlingInHub( CarrierService carrierService, double startTime, Id<Link> linkId ) {
		LSPShipment lspShipment = servicesWaitedFor.get(carrierService).shipment;

		double expHandlingDuration = transshipmentHubResource.getCapacityNeedFixed() + transshipmentHubResource.getCapacityNeedLinear() * lspShipment.getSize();
		double endTime = startTime + expHandlingDuration;

		{ //Old logging approach - will be removed at some point in time
			ShipmentPlanElement handle = ShipmentUtils.LoggedShipmentHandleBuilder.newInstance()
					.setLinkId(linkId)
					.setResourceId(resourceId)
					.setStartTime(startTime)
					.setEndTime(endTime)
					.setLogisticsChainElement(servicesWaitedFor.get(carrierService).element)
					.build();
			Id<ShipmentPlanElement> loadId = Id.create(handle.getResourceId() + String.valueOf(handle.getLogisticChainElement().getId()) + handle.getElementType(), ShipmentPlanElement.class);
			if (!lspShipment.getShipmentLog().getPlanElements().containsKey(loadId)) {
				lspShipment.getShipmentLog().addPlanElement(loadId, handle);
			}
		}
		{ // New event-based approach
			//  Könnte sein, dass das jetzt funktioniert.  kai, may'23
			eventsManager.processEvent(new HandlingInHubStartsEvent(startTime, linkId, lspShipment.getId(), resourceId, expHandlingDuration));
		}

	}

	private double getUnloadEndTime(Tour tour) {
		double unloadEndTime = 0;
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof ServiceActivity serviceActivity) {
				unloadEndTime = unloadEndTime + serviceActivity.getDuration();
			}
		}
		return unloadEndTime;
	}

	private boolean allServicesAreInOnePoint(Tour tour) {
		for (TourElement element : tour.getTourElements()) {
			if (element instanceof ServiceActivity activity) {
				if (activity.getLocation() != tour.getEndLinkId()) {
					return false;
				}
			}
		}
		return true;
	}

	public Map<CarrierService, TransshipmentHubEventHandlerPair> getServicesWaitedFor() {
		return servicesWaitedFor;
	}

	public TransshipmentHubResource getTranshipmentHub() {
		return transshipmentHubResource;
	}

	public Id<LSPResource> getResourceId() {
		return resourceId;
	}

//	public Id<Link> getLinkId() {
//		return linkId;
//	}

	public static class TransshipmentHubEventHandlerPair {
		public final LSPShipment shipment;
		public final LogisticChainElement element;

		public TransshipmentHubEventHandlerPair(LSPShipment shipment, LogisticChainElement element) {
			this.shipment = shipment;
			this.element = element;
		}
	}


//	private void logUnload(CarrierTourEndEvent event) {
//		ShipmentUtils.LoggedShipmentUnloadBuilder builder = ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
//		builder.setStartTime(event.getTime() - getTotalUnloadingTime(tour));
//		builder.setEndTime(event.getTime());
//		builder.setLogisticChainElement(logisticChainElement);
//		builder.setResourceId(resource.getId());
//		builder.setCarrierId(event.getCarrierId());
//		ShipmentPlanElement unload = builder.build();
//		String idString = unload.getResourceId() + "" + unload.getLogisticChainElement().getId() + "" + unload.getElementType();
//		Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
//		lspShipment.getShipmentLog().addPlanElement(unloadId, unload);
//	}

//	private void logTransport(CarrierTourEndEvent event) {
//		String idString = resource.getId() + "" + logisticChainElement.getId() + "" + "TRANSPORT";
//		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
//		ShipmentPlanElement abstractPlanElement = lspShipment.getShipmentLog().getPlanElements().get(id);
//		if (abstractPlanElement instanceof ShipmentLeg transport) {
//			transport.setEndTime(event.getTime() - getTotalUnloadingTime(tour));
//			transport.setToLinkId(event.getLinkId());
//		}
//	}

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


	public LogisticChainElement getLogisticChainElement() {
		return logisticChainElement;
	}


	public LSPCarrierResource getResource() {
		return resource;
	}



	private void logUnload(CarrierTourEndEvent event, Tour tour) {
		ShipmentUtils.LoggedShipmentUnloadBuilder builder = ShipmentUtils.LoggedShipmentUnloadBuilder.newInstance();
		builder.setStartTime(event.getTime());
		builder.setEndTime(event.getTime() + getTotalUnloadingTime(tour));
		builder.setLogisticChainElement(logisticChainElement);
		builder.setResourceId(resource.getId());
		builder.setCarrierId(event.getCarrierId());
		ShipmentPlanElement unload = builder.build();
		String idString = unload.getResourceId() + "" + unload.getLogisticChainElement().getId() + "" + unload.getElementType();
		Id<ShipmentPlanElement> unloadId = Id.create(idString, ShipmentPlanElement.class);
		lspShipment.getShipmentLog().addPlanElement(unloadId, unload);
	}

	private void logTransport(CarrierTourEndEvent event, Tour tour) {
		String idString = resource.getId() + "" + logisticChainElement.getId() + "" + "TRANSPORT";
		Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
		ShipmentPlanElement abstractPlanElement = lspShipment.getShipmentLog().getPlanElements().get(id);
		if (abstractPlanElement instanceof ShipmentLeg transport) {
			transport.setEndTime(event.getTime());
			transport.setToLinkId(tour.getEndLinkId());
		}
	}

	public CarrierService getCarrierService() {
		return carrierService;
	}


	public LogisticChainElement getElement() {
		return logisticChainElement;
	}


}
