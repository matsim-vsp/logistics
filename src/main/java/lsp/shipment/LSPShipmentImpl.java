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

package lsp.shipment;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.core.events.handler.EventHandler;

import lsp.LogisticsSolution;
import lsp.LSPInfo;

class LSPShipmentImpl implements LSPShipment {

	private final Id<LSPShipment> id;
	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;
	private final TimeWindow startTimeWindow;
	private final TimeWindow endTimeWindow;
	private final int capacityDemand;
	private final double deliveryServiceTime;
	private final double pickupServiceTime;
	private final ShipmentPlan schedule;
	private final ShipmentPlan log;
	private final ArrayList<EventHandler> eventHandlers;
	private final ArrayList<Requirement> requirements;
	private final ArrayList<LSPInfo> infos;
	private Id<LogisticsSolution> solutionId;

	LSPShipmentImpl( ShipmentUtils.LSPShipmentBuilder builder ){
		this.id = builder.id;
		this.fromLinkId = builder.fromLinkId;
		this.toLinkId = builder.toLinkId;
		this.startTimeWindow = builder.startTimeWindow;
		this.endTimeWindow = builder.endTimeWindow;
		this.capacityDemand = builder.capacityDemand;
		this.deliveryServiceTime = builder.deliveryServiceTime;
		this.pickupServiceTime = builder.pickupServiceTime;
		this.schedule = new ShipmentPlanImpl(this);
		this.log = new ShipmentPlanImpl(this);
		this.eventHandlers = new ArrayList<>();
		this.requirements = new ArrayList<>();
		this.requirements.addAll( builder.requirements );
		this.infos = new ArrayList<>();
		this.infos.addAll( builder.infos );
	}


	@Override
	public Id<LSPShipment> getId() {
		return id;
	}

	@Override
	public Id<Link> getFrom() {
		return fromLinkId;
	}

	@Override
	public Id<Link> getTo() {
		return toLinkId;
	}

	@Override
	public TimeWindow getPickupTimeWindow() {
		return startTimeWindow;
	}

	@Override
	public TimeWindow getDeliveryTimeWindow() {
		return endTimeWindow;
	}

	@Override
	public ShipmentPlan getShipmentPlan() {
		return schedule;
	}

	@Override
	public ShipmentPlan getLog() {
		return log;
	}

	@Override
	public int getSize() {
		return capacityDemand;
	}

	@Override
	public double getDeliveryServiceTime() {
		return deliveryServiceTime;
	}

	@Override
	public Collection<EventHandler> getEventHandlers() {
		return eventHandlers;
	}


	@Override
	public Collection<Requirement> getRequirements() {
		return requirements;
	}

	@Override
	public Collection<LSPInfo> getInfos() {
		return infos;
	}

	@Override public Id<LogisticsSolution> getSolutionId() {
		return solutionId;
	}

	@Override public double getPickupServiceTime(){
		return pickupServiceTime;
	}
	
}
