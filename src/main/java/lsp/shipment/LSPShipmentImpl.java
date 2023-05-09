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

import lsp.LSPDataObject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.TimeWindow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class LSPShipmentImpl extends LSPDataObject<LSPShipment> implements LSPShipment {

	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;
	private final TimeWindow startTimeWindow;
	private final TimeWindow endTimeWindow;
	private final int capacityDemand;
	private final double deliveryServiceTime;
	private final double pickupServiceTime;
	private final ShipmentPlan schedule;
	private final ShipmentPlan log;
	private final List<Requirement> requirements;

	LSPShipmentImpl(ShipmentUtils.LSPShipmentBuilder builder) {
		super(builder.id);
		this.fromLinkId = builder.fromLinkId;
		this.toLinkId = builder.toLinkId;
		this.startTimeWindow = builder.startTimeWindow;
		this.endTimeWindow = builder.endTimeWindow;
		this.capacityDemand = builder.capacityDemand;
		this.deliveryServiceTime = builder.deliveryServiceTime;
		this.pickupServiceTime = builder.pickupServiceTime;
		this.schedule = new ShipmentPlanImpl(this);
		this.log = new ShipmentPlanImpl(this);
		this.requirements = new ArrayList<>();
		this.requirements.addAll(builder.requirements);
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
	public Collection<Requirement> getRequirements() {
		return requirements;
	}

	@Override
	public double getPickupServiceTime() {
		return pickupServiceTime;
	}

	@Override public String toString() {
		return "LSPShipmentImpl{" +
				"Id=" + getId() +
				"\t fromLinkId=" + fromLinkId +
				"\t toLinkId=" + toLinkId +
				"\t capacityDemand=" + capacityDemand +
				"\t startTimeWindow=" + startTimeWindow +
				"\t endTimeWindow=" + endTimeWindow +
				"\t capacityDemand=" + capacityDemand +
				"\t deliveryServiceTime=" + deliveryServiceTime +
				"\t pickupServiceTime=" + pickupServiceTime +
//				"\t schedule=" + schedule +
//				"\t log=" + log +
//				"\t requirements=" + requirements +
				'}';
	}
}
