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

//import demand.UtilityFunction;

import lsp.LSPResource;
import lsp.LogisticChainElement;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.TimeWindow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ShipmentUtils {
	private ShipmentUtils() {
	} // do not instantiate

	public static Comparator<ShipmentPlanElement> createShipmentPlanElementComparator() {
		return new ShipmentPlanElementComparator();
	}

	public static final class LoggedShipmentHandleBuilder {
		private double startTime;
		private double endTime;
		private LogisticChainElement element;
		private Id<LSPResource> resourceId;
		private Id<Link> linkId;

		private LoggedShipmentHandleBuilder() {
		}

		public static LoggedShipmentHandleBuilder newInstance() {
			return new LoggedShipmentHandleBuilder();
		}

		public void setStartTime(double startTime){
			this.startTime = startTime;
		}

		public void setEndTime(double endTime){
			this.endTime = endTime;
		}

		public void setLogisticsChainElement(LogisticChainElement element){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId){
			this.resourceId = resourceId;
		}

		public void setLinkId(Id<Link> linkId){
			this.linkId = linkId;
		}

		public ShipmentPlanElement build(){
			return new LoggedShipmentHandle(this);
		}

		// --- Getters --- //

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}

		public LogisticChainElement getElement() {
			return element;
		}

		public Id<LSPResource> getResourceId() {
			return resourceId;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}
	}

	public static final class LoggedShipmentLoadBuilder {
		private double startTime;
		private double endTime;
		private LogisticChainElement element;
		private Id<LSPResource> resourceId;
		private Id<Carrier> carrierId;
		private Id<Link> linkId;

		private LoggedShipmentLoadBuilder(){
		}

		public static LoggedShipmentLoadBuilder newInstance(){
			return new LoggedShipmentLoadBuilder();
		}

		public void setStartTime(double startTime){
			this.startTime = startTime;
		}

		public void setEndTime(double endTime){
			this.endTime = endTime;
		}

		public void setLogisticsChainElement(LogisticChainElement element){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId){
			this.resourceId = resourceId;
		}

		public void setLinkId(Id<Link> linkId){
			this.linkId = linkId;
		}

		public void setCarrierId(Id<Carrier> carrierId){
			this.carrierId = carrierId;
		}

		public LoggedShipmentLoad build(){
			return new LoggedShipmentLoad(this);
		}

		// --- Getters --- //

		public double getStartTime() {
			return startTime;
		}

		public double getEndTime() {
			return endTime;
		}

		public LogisticChainElement getElement() {
			return element;
		}

		public Id<LSPResource> getResourceId() {
			return resourceId;
		}

		public Id<Carrier> getCarrierId() {
			return carrierId;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}
	}

	public static final class LoggedShipmentTransportBuilder {
		private double startTime;
		private LogisticChainElement element;
		private Id<LSPResource> resourceId;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private Id<Carrier> carrierId;

		private LoggedShipmentTransportBuilder(){
		}

		public static LoggedShipmentTransportBuilder newInstance(){
			return new LoggedShipmentTransportBuilder();
		}

		public void setStartTime(double startTime){
			this.startTime = startTime;
		}

		public void setLogisticChainElement(LogisticChainElement element){
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId){
			this.resourceId = resourceId;
		}

		public void setFromLinkId(Id<Link> fromLinkId){
			this.fromLinkId = fromLinkId;
		}

		public void setToLinkId(Id<Link> toLinkId){
			this.toLinkId = toLinkId;
		}

		public void setCarrierId(Id<Carrier> carrierId){
			this.carrierId = carrierId;
		}

		public LoggedShipmentTransport build(){
			return new LoggedShipmentTransport(this);
		}

		// --- Getters --- //
		public double getStartTime() {
			return startTime;
		}

		public LogisticChainElement getElement() {
			return element;
		}

		public Id<LSPResource> getResourceId() {
			return resourceId;
		}

		public Id<Link> getFromLinkId() {
			return fromLinkId;
		}

		public Id<Link> getToLinkId() {
			return toLinkId;
		}

		public Id<Carrier> getCarrierId() {
			return carrierId;
		}
	}

	public static final class ScheduledShipmentUnloadBuilder {
		double startTime;
		double endTime;
		LogisticChainElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> linkId;
		CarrierService carrierService;

		private ScheduledShipmentUnloadBuilder() {
		}

		public static ScheduledShipmentUnloadBuilder newInstance() {
			return new ScheduledShipmentUnloadBuilder();
		}

		public void setStartTime(double startTime) {
			this.startTime = startTime;
		}

		public void setEndTime(double endTime) {
			this.endTime = endTime;
		}

		public void setLogisticsChainElement(LogisticChainElement element) {
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId) {
			this.resourceId = resourceId;
		}

		public void setCarrierId(Id<Carrier> carrierId) {
			this.carrierId = carrierId;
		}

		public void setLinkId(Id<Link> linkId) {
			this.linkId = linkId;
		}

		public void setCarrierService(CarrierService carrierService) {
			this.carrierService = carrierService;
		}

		public ScheduledShipmentUnload build() {
			return new ScheduledShipmentUnload(this);
		}
	}

	public static final class ScheduledShipmentTransportBuilder {
		double startTime;
		double endTime;
		LogisticChainElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> fromLinkId;
		Id<Link> toLinkId;
		CarrierService carrierService;

		private ScheduledShipmentTransportBuilder() {
		}

		public static ScheduledShipmentTransportBuilder newInstance() {
			return new ScheduledShipmentTransportBuilder();
		}

		public void setStartTime(double startTime) {
			this.startTime = startTime;
		}

		public void setEndTime(double endTime) {
			this.endTime = endTime;
		}

		public void setLogisticChainElement(LogisticChainElement element) {
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId) {
			this.resourceId = resourceId;
		}

		public void setCarrierId(Id<Carrier> carrierId) {
			this.carrierId = carrierId;
		}

		public void setFromLinkId(Id<Link> fromLinkId) {
			this.fromLinkId = fromLinkId;
		}

		public void setToLinkId(Id<Link> toLinkId) {
			this.toLinkId = toLinkId;
		}

		public void setCarrierService(CarrierService carrierService) {
			this.carrierService = carrierService;
		}

		public ScheduledShipmentTransport build() {
			return new ScheduledShipmentTransport(this);
		}
	}

	public static final class ScheduledShipmentLoadBuilder {
		double startTime;
		double endTime;
		LogisticChainElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> linkId;
		CarrierService carrierService;

		private ScheduledShipmentLoadBuilder() {
		}

		public static ScheduledShipmentLoadBuilder newInstance() {
			return new ScheduledShipmentLoadBuilder();
		}

		public void setStartTime(double startTime) {
			this.startTime = startTime;
		}

		public void setEndTime(double endTime) {
			this.endTime = endTime;
		}

		public void setLogisticChainElement(LogisticChainElement element) {
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId) {
			this.resourceId = resourceId;
		}

		public void setLinkId(Id<Link> linkId) {
			this.linkId = linkId;
		}

		public void setCarrierId(Id<Carrier> carrierId) {
			this.carrierId = carrierId;
		}

		public void setCarrierService(CarrierService carrierService) {
			this.carrierService = carrierService;
		}

		public ScheduledShipmentLoad build() {
			return new ScheduledShipmentLoad(this);
		}
	}

	public static final class ScheduledShipmentHandleBuilder {
		double startTime;
		double endTime;
		LogisticChainElement element;
		Id<LSPResource> resourceId;

		private ScheduledShipmentHandleBuilder() {
		}

		public static ScheduledShipmentHandleBuilder newInstance() {
			return new ScheduledShipmentHandleBuilder();
		}

		public void setStartTime(double startTime) {
			this.startTime = startTime;
		}

		public void setEndTime(double endTime) {
			this.endTime = endTime;
		}

		public void setLogisticsChainElement(LogisticChainElement element) {
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId) {
			this.resourceId = resourceId;
		}

		public ScheduledShipmentHandle build() {
			return new ScheduledShipmentHandle(this);
		}
	}

	public static final class LoggedShipmentUnloadBuilder {
		double startTime;
		double endTime;
		LogisticChainElement element;
		Id<LSPResource> resourceId;
		Id<Carrier> carrierId;
		Id<Link> linkId;

		private LoggedShipmentUnloadBuilder() {
		}

		public static LoggedShipmentUnloadBuilder newInstance() {
			return new LoggedShipmentUnloadBuilder();
		}

		public void setStartTime(double startTime) {
			this.startTime = startTime;
		}

		public void setEndTime(double endTime) {
			this.endTime = endTime;
		}

		public void setLogisticChainElement(LogisticChainElement element) {
			this.element = element;
		}

		public void setResourceId(Id<LSPResource> resourceId) {
			this.resourceId = resourceId;
		}

		public void setLinkId(Id<Link> linkId) {
			this.linkId = linkId;
		}

		public void setCarrierId(Id<Carrier> carrierId) {
			this.carrierId = carrierId;
		}

		public LoggedShipmentUnload build() {
			return new LoggedShipmentUnload(this);
		}
	}

	public static final class LSPShipmentBuilder {
		final Id<LSPShipment> id;
		Id<Link> fromLinkId;
		Id<Link> toLinkId;
		TimeWindow startTimeWindow;
		TimeWindow endTimeWindow;
		int capacityDemand;
		double deliveryServiceTime;
		double pickupServiceTime;
		final List<Requirement> requirements;

		public static LSPShipmentBuilder newInstance( Id<LSPShipment> id ){
			return new LSPShipmentBuilder(id);
		}

		private LSPShipmentBuilder( Id<LSPShipment> id ){
			this.requirements = new ArrayList<>();
			this.id = id;
		}

		public void setFromLinkId(Id<Link> fromLinkId ){
			this.fromLinkId = fromLinkId;
		}

		public void setToLinkId(Id<Link> toLinkId) {
			this.toLinkId = toLinkId;
		}

		public void setStartTimeWindow(TimeWindow startTimeWindow) {
			this.startTimeWindow = startTimeWindow;
		}

		public void setEndTimeWindow(TimeWindow endTimeWindow) {
			this.endTimeWindow = endTimeWindow;
		}

		public void setCapacityDemand(int capacityDemand) {
			this.capacityDemand = capacityDemand;
		}

		public void setDeliveryServiceTime(double serviceTime) {
			this.deliveryServiceTime = serviceTime;
		}

		public LSPShipmentBuilder setPickupServiceTime(double serviceTime) {
			this.pickupServiceTime = serviceTime;
			return this;
		}

		public void addRequirement(Requirement requirement) {
			requirements.add(requirement);
		}

		public LSPShipment build() {
			return new LSPShipmentImpl(this);
		}
	}
}
