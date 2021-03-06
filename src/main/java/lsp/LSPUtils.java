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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.ArrayList;
import java.util.Collection;

public final class LSPUtils {
	private static final String lspsString = "lsps";

	private LSPUtils() {
	} // do not instantiate

	public static LSPPlan createLSPPlan() {
		return new LSPPlanImpl();
	}

	public static SolutionScheduler createForwardSolutionScheduler() {
		return new ForwardSolutionSchedulerImpl();
	}

	public static WaitingShipments createWaitingShipments() {
		return new WaitingShipmentsImpl();
	}

	public static void addLSPs(Scenario scenario, LSPs lsps) {
		FreightUtils.addOrGetCarriers( scenario );
		scenario.addScenarioElement(lspsString, lsps);
	}

	public static LSPs getLSPs(Scenario scenario) {
		Object result = scenario.getScenarioElement(lspsString);
		if (result == null) {
			throw new RuntimeException("there is no scenario element of type " + lspsString +
					".  You will need something like LSPUtils.addLSPs( scenario, lsps) somewhere.");
		}
		return (LSPs) result;
	}

	public static Double getVariableCost(Attributable attributable) {
		return (Double) attributable.getAttributes().getAttribute("variableCost");
	}

	public static void setVariableCost(Attributable attributable, Double variableCost) {
		attributable.getAttributes().putAttribute("variableCost", variableCost);
	}

	public static Double getFixedCost(Attributable attributable) {
		return (Double) attributable.getAttributes().getAttribute("fixedCost");
	}
	//	The following would be closer to how we have done it elsewhere (scenario containers are mutable).  kai, may'22'
//	public static LSPs createOrGetLPSs( Scenario scenario ){
//		Object result = scenario.getScenarioElement( lspsString );
//		LSPs lsps;
//		if ( result != null ) {
//			lsps = (LSPs) result;
//		} else {
//			lsps = new LSPs(  );
//			scenario.addScenarioElement( lspsString, lsps );
//		}
//		return lsps;
//	}

	public static void setFixedCost(Attributable attributable, Double fixedCost) {
		attributable.getAttributes().putAttribute("fixedCost", fixedCost);
	}

	public static final class LSPBuilder {
		final Collection<LSPResource> resources;
		Id<LSP> id;
		SolutionScheduler solutionScheduler;
		LSPPlan initialPlan;
//		LSPScorer scorer;
//		LSPReplanner replanner;


		private LSPBuilder(Id<LSP> id) {
			this.id = id; // this line was not there until today.  kai, may'22
			this.resources = new ArrayList<>();
		}

		public static LSPBuilder getInstance(Id<LSP> id) {
			return new LSPBuilder(id);
		}

		public LSPBuilder setSolutionScheduler(SolutionScheduler solutionScheduler) {
			this.solutionScheduler = solutionScheduler;
			return this;
		}

//		/**
//		 * @deprecated -- It feels attractive to attach this to the "agent".  A big disadvantage with this approach, however, is that
//		 * 		we cannot use injection ... since we cannot inject as many scorers as we have agents.  (At least this is what I think.) Which means
//		 * 		that the approach in matsim core and in carriers to have XxxScoringFunctionFactory is better for what we are doing here.  yyyyyy So
//		 * 		this needs to be changed.  kai, jul'22
//		 */
//		public LSPBuilder setSolutionScorer(LSPScorer scorer) {
//			this.scorer = scorer;
//			return this;
//		}

//		/**
//		 * @deprecated -- It feels attractive to attach this to the "agent".  A big disadvantage with this approach, however, is that
//		 * 		we cannot use injection ... since we cannot inject as many replanners as we have agents.  (At least this is what I think.)  yyyyyy So
//		 * 		this needs to be changed.  kai, jul'22
//		 */
//		public LSPBuilder setReplanner(LSPReplanner replanner) {
//			this.replanner = replanner;
//			return this;
//		}
		// never used.  Thus disabling it.  kai, jul'22


		public LSPBuilder setInitialPlan(LSPPlan plan) {
			this.initialPlan = plan;
			for (LogisticsSolution solution : plan.getSolutions()) {
				for (LogisticsSolutionElement element : solution.getSolutionElements()) {
					if (!resources.contains(element.getResource())) {
						resources.add(element.getResource());
					}
				}
			}
			return this;
		}


		public LSP build() {
			return new LSPImpl(this);
		}
	}

	public static final class LogisticsSolutionBuilder {
		final Id<LogisticsSolution> id;
		final Collection<LogisticsSolutionElement> elements;
		//		final Collection<EventHandler> eventHandlers;
		final Collection<LSPSimulationTracker<LogisticsSolution>> trackers;

		private LogisticsSolutionBuilder(Id<LogisticsSolution> id) {
			this.elements = new ArrayList<>();
//			this.eventHandlers = new ArrayList<>();
			this.trackers = new ArrayList<LSPSimulationTracker<LogisticsSolution>>();
			this.id = id;
		}

		public static LogisticsSolutionBuilder newInstance(Id<LogisticsSolution> id) {
			return new LogisticsSolutionBuilder(id);
		}

		public LogisticsSolutionBuilder addSolutionElement(LogisticsSolutionElement element) {
			elements.add(element);
			return this;
		}

//		public LogisticsSolutionBuilder addEventHandler( EventHandler handler ) {
//			eventHandlers.add(handler);
//			return this;
//		}

		public LogisticsSolutionBuilder addTracker(LSPSimulationTracker<LogisticsSolution> tracker) {
			trackers.add(tracker);
			return this;
		}

		public LogisticsSolution build() {
			return new LogisticsSolutionImpl(this);
		}
	}

	public static final class LogisticsSolutionElementBuilder {
		final Id<LogisticsSolutionElement> id;
		final WaitingShipments incomingShipments;
		final WaitingShipments outgoingShipments;
		LSPResource resource;

		private LogisticsSolutionElementBuilder(Id<LogisticsSolutionElement> id) {
			this.id = id;
			this.incomingShipments = createWaitingShipments();
			this.outgoingShipments = createWaitingShipments();
		}

		public static LogisticsSolutionElementBuilder newInstance(Id<LogisticsSolutionElement> id) {
			return new LogisticsSolutionElementBuilder(id);
		}

		public LogisticsSolutionElementBuilder setResource(LSPResource resource) {
			this.resource = resource;
			return this;
		}

		public LogisticsSolutionElement build() {
			return new LogisticsSolutionElementImpl(this);
		}
	}

}
