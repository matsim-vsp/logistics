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


import lsp.shipment.LSPShipment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.events.handler.EventHandler;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


class LSPControlerListener implements BeforeMobsimListener, AfterMobsimListener, ScoringListener,
							  ReplanningListener, IterationStartsListener{
	private static final Logger log = LogManager.getLogger( LSPControlerListener.class );
	private final Scenario scenario;

	private final List<EventHandler> registeredHandlers = new ArrayList<>();
//	private CarrierAgentTracker carrierResourceTracker;

	@Inject private EventsManager eventsManager;
	@Inject private MatsimServices matsimServices;
	@Inject private LSPScorerFactory lspScoringFunctionFactory;
	@Inject @Nullable private LSPStrategyManager strategyManager;

	@Inject LSPControlerListener( Scenario scenario ) {
		this.scenario = scenario;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		LSPs lsps = LSPUtils.getLSPs(scenario);

		LSPRescheduler.notifyBeforeMobsim(lsps, event);

		for (LSP lsp : lsps.getLSPs().values()) {
			((LSPImpl) lsp).setScorer( lspScoringFunctionFactory.createScoringFunction( lsp ) );

			// simulation trackers of lsp:
			registerSimulationTrackers(lsp );

			// simulation trackers of shipments:
			for (LSPShipment shipment : lsp.getShipments()) {
				registerSimulationTrackers(shipment );
			}

			// simulation trackers of solutions:
			for (LogisticsSolution solution : lsp.getSelectedPlan().getSolutions()) {
				registerSimulationTrackers(solution );

				// simulation trackers of solution elements:
				for (LogisticsSolutionElement element : solution.getSolutionElements()) {
					registerSimulationTrackers(element );

					// simulation trackers of resources:
					registerSimulationTrackers(element.getResource() );

				}
			}
		}
	}

	private void registerSimulationTrackers( HasSimulationTrackers<?> lsp ) {
		// get all simulation trackers ...
		for (LSPSimulationTracker<?> simulationTracker : lsp.getSimulationTrackers()) {
			// ... register them ...
			if (!registeredHandlers.contains(simulationTracker)) {
				log.warn("adding eventsHandler: " + simulationTracker);
				eventsManager.addHandler(simulationTracker);
				registeredHandlers.add(simulationTracker);
				matsimServices.addControlerListener(simulationTracker);
			} else {
				log.warn("not adding eventsHandler since already added: " + simulationTracker);
			}
		}
	}


	//Hier muss noch die Moeglichkeit reinkommen, dass nicht alle LSPs nach jeder Iteration neu planen, sondern nur ein Teil von denen
	//Das kann durch ein entsprechendes replanningModule erreicht werden. Hier muss man dann nix aendern. kmt
	// das geht jetzt nicht mehr.  kai, jun'22
	@Override
	public void notifyReplanning(ReplanningEvent event) {
		if ( strategyManager==null ) {
			throw new RuntimeException( "You need to set LSPStrategyManager to something meaningful to run iterations." );
		}
		final Collection<LSP> lsps = LSPUtils.getLSPs( scenario ).getLSPs().values();
		strategyManager.run( lsps, event.getIteration(), event.getReplanningContext() );
		for( LSP lsp : lsps ){
			lsp.getSelectedPlan().getAssigner().setLSP(lsp);//TODO: Feels weird, but getting NullPointer because of missing lsp inside the assigner
			//TODO: Do we need to do it for each plan, if it gets selected???
			// yyyyyy Means IMO that something is incomplete with the plans copying.  kai, jul'22
		}
	}

	@Override
	public void notifyScoring(ScoringEvent scoringEvent) {
		for (LSP lsp : LSPUtils.getLSPs(scenario).getLSPs().values()) {
			lsp.scoreSelectedPlan(scoringEvent);
		}
		// yyyyyy might make more sense to register the lsps directly as scoring controler listener (??)
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
//		eventsManager.removeHandler(carrierResourceTracker);
	}


	Carriers getCarriers() {
		LSPs lsps = LSPUtils.getLSPs(scenario);

		Carriers carriers = new Carriers();
		for (LSP lsp : lsps.getLSPs().values()) {
			LSPPlan selectedPlan = lsp.getSelectedPlan();
			for (LogisticsSolution solution : selectedPlan.getSolutions()) {
				for (LogisticsSolutionElement element : solution.getSolutionElements()) {
					if( element.getResource() instanceof LSPCarrierResource carrierResource ) {
						Carrier carrier = carrierResource.getCarrier();
						if (!carriers.getCarriers().containsKey(carrier.getId())) {
							carriers.addCarrier(carrier);
						}
					}
				}
			}
		}
		return carriers;
	}

//	public CarrierAgentTracker getCarrierResourceTracker() {
//		return carrierResourceTracker;
//	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
	}
}
