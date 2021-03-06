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

import lsp.LSP;
import lsp.LogisticsSolutionElement;
import lsp.LSPResource;
import lsp.LSPResourceScheduler;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
import lsp.shipment.ShipmentUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class UsecaseUtils {

	public static CollectionCarrierScheduler createDefaultCollectionCarrierScheduler() {
		return new CollectionCarrierScheduler();
	}

	public static DistributionCarrierScheduler createDefaultDistributionCarrierScheduler() {
		return new DistributionCarrierScheduler();
	}

	public static MainRunCarrierScheduler createDefaultMainRunCarrierScheduler() {
		return new MainRunCarrierScheduler();
	}

	public static SimpleForwardSolutionScheduler createDefaultSimpleForwardSolutionScheduler(List<LSPResource> resources) {
		return new SimpleForwardSolutionScheduler(resources);
	}

	public static SingleSolutionShipmentAssigner createSingleSolutionShipmentAssigner() {
		return new SingleSolutionShipmentAssigner();
	}

	/**
	 * Collects all the vehicleTyps from the different Vehicle of the carrier.
	 * This is needed since we do not use carrier.getCarrierCapabilities().getVehicleTypes() any more as second field to safe vehicleTypes ...
	 * TODO: Maybe move to CarrierUtils in MATSim-libs / freigth contrib.
	 * <p>
	 * KMT/Jul22
	 *
	 * @param carrier
	 * @return Collection of VehicleTypes
	 */
	static Collection<VehicleType> getVehicleTypeCollection(Carrier carrier) {
		Set<VehicleType> vehicleTypeCollection = new HashSet<>();
		for (CarrierVehicle carrierVehicle : carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
			vehicleTypeCollection.add(carrierVehicle.getType());
		}
		return vehicleTypeCollection;
	}

	public static void printResults(String outputDir, LSP lsp) {
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_schedules.tsv")) {
			final String str0 = "LSP: " + lsp.getId();
			System.out.println(str0);
			writer.write(str0 + "\n");
			for (LSPShipment shipment : lsp.getShipments()) {
				ArrayList<ShipmentPlanElement> elementList = new ArrayList<>(shipment.getShipmentPlan().getPlanElements().values());
				elementList.sort(ShipmentUtils.createShipmentPlanElementComparator());
				final String str1 = "Shipment: " + shipment.getId();
				System.out.println(str1);
				writer.write(str1 + "\n");
				for (ShipmentPlanElement element : elementList) {
					final String str2 = element.getSolutionElement().getId() + "\t\t" + element.getResourceId() + "\t\t" + element.getElementType() + "\t\t" + element.getStartTime() + "\t\t" + element.getEndTime();
					System.out.println(str2);
					writer.write(str2 + "\n");
				}
				System.out.println();
				writer.write("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class CollectionCarrierAdapterBuilder {

		final Id<LSPResource> id;
		final ArrayList<LogisticsSolutionElement> clientElements;
		final Network network;
		Carrier carrier;
		Id<Link> locationLinkId;
		CollectionCarrierScheduler collectionScheduler;

		private CollectionCarrierAdapterBuilder(Id<LSPResource> id, Network network) {
			this.id = id;
			this.clientElements = new ArrayList<>();
			this.network = network;
		}

		public static CollectionCarrierAdapterBuilder newInstance(Id<LSPResource> id, Network network) {
			return new CollectionCarrierAdapterBuilder(id, network);
		}

		public CollectionCarrierAdapterBuilder setLocationLinkId(Id<Link> locationLinkId) {
			this.locationLinkId = locationLinkId;
			return this;
		}

		public CollectionCarrierAdapterBuilder setCarrier(Carrier carrier) {
			this.carrier = carrier;
			return this;
		}


		public CollectionCarrierAdapterBuilder setCollectionScheduler(CollectionCarrierScheduler collectionCarrierScheduler) {
			this.collectionScheduler = collectionCarrierScheduler;
			return this;
		}

		public CollectionCarrierResource build() {
			return new CollectionCarrierResource(this);
		}

	}


	public static class DistributionCarrierAdapterBuilder {

		final Id<LSPResource> id;
		final ArrayList<LogisticsSolutionElement> clientElements;
		final Network network;
		Carrier carrier;
		Id<Link> locationLinkId;
		DistributionCarrierScheduler distributionHandler;

		private DistributionCarrierAdapterBuilder(Id<LSPResource> id, Network network) {
			this.id = id;
			this.clientElements = new ArrayList<>();
			this.network = network;
		}

		public static DistributionCarrierAdapterBuilder newInstance(Id<LSPResource> id, Network network) {
			return new DistributionCarrierAdapterBuilder(id, network);
		}

		public DistributionCarrierAdapterBuilder setLocationLinkId(Id<Link> locationLinkId) {
			this.locationLinkId = locationLinkId;
			return this;
		}

		public DistributionCarrierAdapterBuilder setCarrier(Carrier carrier) {
			this.carrier = carrier;
			return this;
		}


		public DistributionCarrierAdapterBuilder setDistributionScheduler(DistributionCarrierScheduler distributionCarrierScheduler) {
			this.distributionHandler = distributionCarrierScheduler;
			return this;
		}

		public DistributionCarrierResource build() {
			return new DistributionCarrierResource(this);
		}

	}

	public static class MainRunCarrierAdapterBuilder {

		private final Id<LSPResource> id;
		private final ArrayList<LogisticsSolutionElement> clientElements;
		private final Network network;
		private Carrier carrier;
		private Id<Link> fromLinkId;
		private Id<Link> toLinkId;
		private MainRunCarrierScheduler mainRunScheduler;

		private MainRunCarrierAdapterBuilder(Id<LSPResource> id, Network network) {
			this.id = id;
			this.clientElements = new ArrayList<>();
			this.network = network;
		}

		public static MainRunCarrierAdapterBuilder newInstance(Id<LSPResource> id, Network network) {
			return new MainRunCarrierAdapterBuilder(id, network);
		}

		public MainRunCarrierAdapterBuilder setCarrier(Carrier carrier) {
			this.carrier = carrier;
			return this;
		}

		public MainRunCarrierAdapterBuilder setFromLinkId(Id<Link> fromLinkId) {
			this.fromLinkId = fromLinkId;
			return this;
		}

		public MainRunCarrierAdapterBuilder setToLinkId(Id<Link> toLinkId) {
			this.toLinkId = toLinkId;
			return this;
		}
		public MainRunCarrierAdapterBuilder setMainRunCarrierScheduler(MainRunCarrierScheduler mainRunScheduler) {
			this.mainRunScheduler = mainRunScheduler;
			return this;
		}

		public MainRunCarrierResource build() {
			return new MainRunCarrierResource(this);
		}

		//--- Getter ---

		Id<LSPResource> getId() {
			return id;
		}

		Carrier getCarrier() {
			return carrier;
		}

		Id<Link> getFromLinkId() {
			return fromLinkId;
		}

		Id<Link> getToLinkId() {
			return toLinkId;
		}

		ArrayList<LogisticsSolutionElement> getClientElements() {
			return clientElements;
		}

		MainRunCarrierScheduler getMainRunScheduler() {
			return mainRunScheduler;
		}

		Network getNetwork() {
			return network;
		}
	}

	public static class TranshipmentHubSchedulerBuilder {
		private double capacityNeedLinear;
		private double capacityNeedFixed;

		private TranshipmentHubSchedulerBuilder() {
		}

		public static TranshipmentHubSchedulerBuilder newInstance() {
			return new TranshipmentHubSchedulerBuilder();
		}

		public TranshipmentHubSchedulerBuilder setCapacityNeedLinear(double capacityNeedLinear) {
			this.capacityNeedLinear = capacityNeedLinear;
			return this;
		}

		public TranshipmentHubSchedulerBuilder setCapacityNeedFixed(double capacityNeedFixed) {
			this.capacityNeedFixed = capacityNeedFixed;
			return this;
		}
		public TransshipmentHubScheduler build() {
			return new TransshipmentHubScheduler(this);
		}

		//--- Getters ---

		double getCapacityNeedLinear() {
			return capacityNeedLinear;
		}

		double getCapacityNeedFixed() {
			return capacityNeedFixed;
		}
	}

	public static final class TransshipmentHubBuilder {

		private final Id<LSPResource> id;
		private final Id<Link> locationLinkId;
		private final ArrayList<LogisticsSolutionElement> clientElements;
		private TransshipmentHubScheduler transshipmentHubScheduler;

		private TransshipmentHubBuilder(Id<LSPResource> id, Id<Link> locationLinkId) {
			this.id = id;
			this.clientElements = new ArrayList<>();
			this.locationLinkId = locationLinkId;
		}

		public static TransshipmentHubBuilder newInstance(Id<LSPResource> id, Id<Link> locationLinkId) {
			return new TransshipmentHubBuilder(id, locationLinkId);
		}

		public TransshipmentHubBuilder setTransshipmentHubScheduler(LSPResourceScheduler TranshipmentHubScheduler) {
			this.transshipmentHubScheduler = (TransshipmentHubScheduler) TranshipmentHubScheduler;
			return this;
		}

		public TransshipmentHub build() {
			return new TransshipmentHub(this);
		}
		//--- Getters ---

		Id<LSPResource> getId() {
			return id;
		}

		Id<Link> getLocationLinkId() {
			return locationLinkId;
		}

		TransshipmentHubScheduler getTransshipmentHubScheduler() {
			return transshipmentHubScheduler;
		}

		ArrayList<LogisticsSolutionElement> getClientElements() {
			return clientElements;
		}

	}


}
