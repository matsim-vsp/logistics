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

import lsp.LSPUtils;
import lsp.LogisticsSolution;
import lsp.controler.LSPSimulationTracker;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.Collection;

/*package-private*/ class LinearCostTracker implements LSPSimulationTracker<LogisticsSolution> {

	private final Attributes attributes = new Attributes();
	private final Collection<EventHandler> eventHandlers;
//	private final Collection<LSPInfo> infos;
	private double distanceCosts;
	private double timeCosts;
	private double loadingCosts;
	private double vehicleFixedCosts;
	private int totalNumberOfShipments;
	private int totalWeightOfShipments;
	
	private double fixedUnitCosts;
	private double linearUnitCosts;
	
	private final double shareOfFixedCosts;
	private LogisticsSolution logisticsSolution;

	public LinearCostTracker(double shareOfFixedCosts) {
		this.shareOfFixedCosts = shareOfFixedCosts;
//		CostInfo costInfo = new CostInfo();
//		infos = new ArrayList<>();
//		infos.add(costInfo);
		this.eventHandlers = new ArrayList<>();
	}
	
	
	@Override
	public Collection<EventHandler> getEventHandlers() {
		return eventHandlers;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for(EventHandler handler : eventHandlers) {
			if(handler instanceof TourStartHandler) {
				TourStartHandler startHandler = (TourStartHandler) handler;
				this.vehicleFixedCosts = startHandler.getVehicleFixedCosts();
			}
			if(handler instanceof DistanceAndTimeHandler) {
				DistanceAndTimeHandler distanceHandler = (DistanceAndTimeHandler) handler;
				this.distanceCosts = distanceHandler.getDistanceCosts();
				this.timeCosts = distanceHandler.getTimeCosts();
			}
			if(handler instanceof CollectionServiceHandler) {
				CollectionServiceHandler collectionHandler = (CollectionServiceHandler) handler;
				totalNumberOfShipments = collectionHandler.getTotalNumberOfShipments();
				System.out.println(totalNumberOfShipments);
				totalWeightOfShipments = collectionHandler.getTotalWeightOfShipments();
				loadingCosts = collectionHandler.getTotalLoadingCosts();
			}
		}
		
		double totalCosts = distanceCosts + timeCosts + loadingCosts + vehicleFixedCosts;
		fixedUnitCosts = (totalCosts * shareOfFixedCosts)/totalNumberOfShipments;
		linearUnitCosts = (totalCosts * (1-shareOfFixedCosts))/totalWeightOfShipments;
		
//		CostInfo info = (CostInfo) infos.iterator().next();
//		for(LSPInfoFunctionValue value : info.getFunction().getValues()) {
//			if(value instanceof example.lsp.simulationTrackers.FixedCostFunctionValue) {
//				((example.lsp.simulationTrackers.FixedCostFunctionValue)value).setValue(fixedUnitCosts);
//			}
//			if(value instanceof example.lsp.simulationTrackers.LinearCostFunctionValue) {
//				((example.lsp.simulationTrackers.LinearCostFunctionValue)value).setValue(linearUnitCosts);
//			}
//		}
//		info.setFixedCost( fixedUnitCosts );
//		info.setVariableCost( linearUnitCosts );
		LSPUtils.setFixedCost( this.getEmbeddingContainer(), fixedUnitCosts );
		LSPUtils.setVariableCost( this.getEmbeddingContainer(), linearUnitCosts );
		
		
	}


	@Override
	public void reset() {
		distanceCosts = 0;
		timeCosts = 0;
		loadingCosts = 0;
		vehicleFixedCosts = 0;
		totalNumberOfShipments = 0;
		totalWeightOfShipments = 0;
		fixedUnitCosts = 0;
		linearUnitCosts = 0;
		
	}
	@Override public void setEmbeddingContainer( LogisticsSolution pointer ){
		this.logisticsSolution = pointer;
	}
	@Override public LogisticsSolution getEmbeddingContainer(){
		return logisticsSolution;
	}


//	@Override public Attributes getAttributes(){
//		return attributes;
//	}
}
