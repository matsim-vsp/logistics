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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;

import lsp.LogisticsSolutionElement;
import lsp.LSPResource;
import lsp.controler.LSPSimulationTracker;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * {@link LSPResource} bei der die geplanten Tätigkeiten NICHT am Verkehr teilnehmen.
 *
 * Thus, these activities are entered directly in the Schedule of the LSPShipments that pass through the ReloadingPoint.
 *
 * An entry is added to the schedule of the shipments that is an instance of
 * {@link lsp.shipment.ScheduledShipmentHandle}. There, the name of the Resource
 * and the client element are entered, so that the way that the {@link lsp.shipment.LSPShipment}
 * takes is specified. In addition, the planned start and end time of the handling
 * (i.e. crossdocking) of the shipment is entered. In the example, crossdocking
 * starts as soon as the considered LSPShipment arrives at the {@link ReloadingPoint}
 * and ends after a fixed and a size dependent amount of time.
 * <p/>
 * <ul>Discussion points:
 * <li>yyyy Ich fände TransshipmentHub als Name besser.  kai, may'22 </li></ul>
 */
/*package-private*/ class ReloadingPoint implements LSPResource {
	private final Attributes attributes = new Attributes();

	private final Id<LSPResource> id;
	private final Id<Link> locationLinkId;
	private final ReloadingPointScheduler reloadingScheduler;
	private final List<LogisticsSolutionElement> clientElements;
	private final List<EventHandler> eventHandlers;
	private final Collection<LSPSimulationTracker> trackers;

	ReloadingPoint(UsecaseUtils.ReloadingPointBuilder builder){
		this.id = builder.getId();
		this.locationLinkId = builder.getLocationLinkId();
		this.reloadingScheduler = builder.getReloadingScheduler();
		reloadingScheduler.setReloadingPoint(this);
		ReloadingPointTourEndEventHandler eventHandler = new ReloadingPointTourEndEventHandler(this);
		reloadingScheduler.setEventHandler(eventHandler);
		this.clientElements = builder.getClientElements();
		this.eventHandlers = new ArrayList<>();
		this.trackers = new ArrayList<>();
		eventHandlers.add(eventHandler);
	}
	
	@Override
	public Id<Link> getStartLinkId() {
		return locationLinkId;
	}

//	@Override
//	public Class<? extends ReloadingPoint> getClassOfResource() {
//		return this.getClass();
//	}
//
	@Override
	public Id<Link> getEndLinkId() {
		return locationLinkId;
	}

	@Override
	public Collection<LogisticsSolutionElement> getClientElements() {
		return clientElements;
	}

	@Override
	public Id<LSPResource> getId() {
		return id;
	}

	@Override
	public void schedule(int bufferTime) {
		reloadingScheduler.scheduleShipments(this, bufferTime);	
	}

	public double getCapacityNeedFixed(){
		return reloadingScheduler.getCapacityNeedFixed();
	}

	public double getCapacityNeedLinear(){
		return reloadingScheduler.getCapacityNeedLinear();
	}

	@Override public Collection <EventHandler> getEventHandlers(){
		return eventHandlers;
	}

//	@Override
//	public void addSimulationTracker( LSPSimulationTracker tracker ) {
//		this.trackers.add(tracker);
//		this.eventHandlers.addAll(tracker.getEventHandlers());
////		this.infos.addAll(tracker.getAttributes() );
//		for( Map.Entry<String, Object> entry : tracker.getAttributes().getAsMap().entrySet() ){
//			this.attributes.putAttribute( entry.getKey(), entry.getValue());
//		}
//	}
//
//	@Override
//	public Collection<LSPSimulationTracker> getSimulationTrackers() {
//		return trackers;
//	}
	@Override public Attributes getAttributes(){
		return attributes;
	}

//	@Override
//	public void setEventsManager(EventsManager eventsManager) {
//	}
}
