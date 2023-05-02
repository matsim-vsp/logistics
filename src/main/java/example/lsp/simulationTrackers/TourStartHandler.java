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

package example.lsp.simulationTrackers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.contrib.freight.events.FreightTourStartEvent;
import org.matsim.contrib.freight.events.eventhandler.FreightTourStartEventHandler;

import java.util.Collection;

/*package-private*/ class TourStartHandler implements FreightTourStartEventHandler {

	private static final Logger log = LogManager.getLogger(TourStartHandler.class);
	private final Carriers carriers;
	private double vehicleFixedCosts;

	public TourStartHandler(Scenario scenario) {
		this.carriers = FreightUtils.addOrGetCarriers(scenario);
	}

	@Override
	public void reset(int iteration) {
		vehicleFixedCosts = 0;
	}

	@Override
	public void handleEvent(FreightTourStartEvent event) {
		log.warn("handling tour start event=" + event.toString());

		CarrierVehicle carrierVehicle = null;
		/*
		* This somehow a workaround, because the Vehicle can't get recieved from the (MATSim) allVehicle container.
		* At the TourStartEvent stage, the event.getVehicle is still not known ("null"), because it bases on ActivityEndEvent.
		* And since it is the first ActEndEvent of the person, it never entered a vehicle before -.-
		*
		* My preferred apporach would have been something like
		* final Vehicle vehicle = allVehicles.getVehicles().get(event.getVehicleId());
		*  kmt sep'22
		*/
		Carrier carrier = carriers.getCarriers().get(event.getCarrierId());
		Collection<ScheduledTour> scheduledTours = carrier.getSelectedPlan().getScheduledTours();
		for (ScheduledTour scheduledTour : scheduledTours) {
			if (scheduledTour.getTour().getId() == event.getTourId()) {
				carrierVehicle = scheduledTour.getVehicle();
				break;
			}
		}
		assert carrierVehicle != null;
		vehicleFixedCosts = vehicleFixedCosts + carrierVehicle.getType().getCostInformation().getFixedCosts();
	}

	/**
	 * ATTENTION:
	 * Does this really give back the costs of the current vehicle?
	 * Or is the value maybe overwritten if another event happens before calling the getFixedCosts function?
	 * kmt sep'22
	 *
	 * @return the fixedCosts
	 */
	public double getVehicleFixedCosts() {
		return vehicleFixedCosts;
	}

}
