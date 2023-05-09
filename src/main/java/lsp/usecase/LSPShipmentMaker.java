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

import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * This class seems to be only used by TM.
 * Looks like it will create some random Shipment.
 * As input a network is need.
 */
class LSPShipmentMaker {

	public static void main(String[] args) {

		Network network = NetworkUtils.createNetwork();
		MatsimNetworkReader reader = new MatsimNetworkReader(network);
		reader.readFile("D:/Working_Copies_Dissertation/Code_Dissertation/logistics/scenarios/2regions/2regions-network.xml");
		ArrayList<LSPShipment> shipments = new ArrayList<>();

		for (int i = 0; i < 8; i++) {
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(Id.create("Shipment " + i, LSPShipment.class));
			builder.setDeliveryServiceTime(180);
			builder.setCapacityDemand(1);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, Double.MAX_VALUE);
			builder.setStartTimeWindow(startTimeWindow);
			TimeWindow endTimeWindow = TimeWindow.newInstance(0, Double.MAX_VALUE);
			builder.setEndTimeWindow(endTimeWindow);
			Id<Link> fromLinkId = null;
			Id<Link> toLinkId = null;
			while (fromLinkId == null || toLinkId == null) {
				List<Link> linkList = new ArrayList<>(network.getLinks().values());
				Collections.shuffle(linkList);
				Link link = linkList.get(0);

				if (link.getCoord().getX() < 4) {
					fromLinkId = link.getId();
					builder.setFromLinkId(fromLinkId);
				}
				if (link.getCoord().getX() > 14) {
					toLinkId = link.getId();
					builder.setToLinkId(toLinkId);
				}
			}
			shipments.add(builder.build());
		}

		for (LSPShipment shipment : shipments) {
			System.out.println(shipment.getFrom() + " " + shipment.getTo());
		}
	}
}
