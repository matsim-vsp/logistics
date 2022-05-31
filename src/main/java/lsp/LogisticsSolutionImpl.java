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

package lsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;

import lsp.shipment.LSPShipment;
import lsp.controler.LSPSimulationTracker;
import org.matsim.utils.objectattributes.attributable.Attributes;

/* package-private */ class LogisticsSolutionImpl implements LogisticsSolution {

	private final Attributes attributes = new Attributes();
	private final Id<LogisticsSolution> id;
	private LSP lsp;
	private final Collection<LogisticsSolutionElement> solutionElements;
	private final Collection<LSPShipment> shipments;
	private final Collection<EventHandler> eventHandlers;
	private final Collection<LSPSimulationTracker>trackers;


	LogisticsSolutionImpl( LSPUtils.LogisticsSolutionBuilder builder ){
		this.id = builder.id;
		this.solutionElements = builder.elements;
		for(LogisticsSolutionElement element : this.solutionElements) {
			element.setEmbeddingContainer(this );
		}
		this.shipments = new ArrayList<>();
		this.eventHandlers = builder.eventHandlers;
		this.trackers = builder.trackers;
	}
	
	
	@Override
	public Id<LogisticsSolution> getId() {
		return id;
	}

	@Override
	public void setLSP( LSP lsp ) {
		this.lsp = lsp;
	}

	@Override
	public LSP getLSP() {
		return lsp;
	}

	@Override
	public Collection<LogisticsSolutionElement> getSolutionElements() {
		return  solutionElements;
	}

	@Override
	public Collection<LSPShipment> getShipments() {
		return shipments;
	}

	@Override
	public void assignShipment(LSPShipment shipment) {
		shipments.add(shipment);	
	}
	
//	@Override
//	public Collection<EventHandler> getEventHandlers() {
//		return eventHandlers;
//	}


	@Override
	public void addSimulationTracker( LSPSimulationTracker tracker ) {
		this.trackers.add(tracker);
		this.eventHandlers.addAll(tracker.getEventHandlers());
//		this.solutionInfos.addAll(tracker.getAttributes() );
		for( Map.Entry<String, Object> entry : tracker.getAttributes().getAsMap().entrySet() ){
			this.attributes.putAttribute( entry.getKey(), entry.getValue());
		}

	}


	@Override
	public Collection<LSPSimulationTracker> getSimulationTrackers() {
		return trackers;
	}

	@Override public Attributes getAttributes(){
		for( LSPSimulationTracker tracker : this.trackers ){
			for( Map.Entry<String, Object> entry : tracker.getAttributes().getAsMap().entrySet() ){
				this.attributes.putAttribute( entry.getKey(), entry.getValue());
			}
		}
		return attributes;
	}

//	@Override
//	public void setEventsManager(EventsManager eventsManager) {
//	}

}	
