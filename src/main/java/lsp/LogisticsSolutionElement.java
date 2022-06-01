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

import org.matsim.api.core.v01.Identifiable;

import org.matsim.utils.objectattributes.attributable.Attributable;


public interface LogisticsSolutionElement extends Identifiable<LogisticsSolutionElement>, HasBackpointer<LogisticsSolution>, /*HasEventHandlers,*/ /*HasSimulationTrackers,*/ Attributable{

	void connectWithNextElement(LogisticsSolutionElement element);

	/**
	 * The logistics solution element wraps around a resource.  Don't know why we need this wrapping.
	 *
	 * @return the resource
	 */
	LSPResource getResource();

	LogisticsSolutionElement getPreviousElement();

	LogisticsSolutionElement getNextElement();

	/**
	 * This collections stores LSPShipments that are waiting for their treatment in this element or more precisely the Resource that is in
	 *  charge of the actual physical handling.
	 *
	 * @return WaitingShipments
	 */
	WaitingShipments getIncomingShipments();

	/**
	 *  Shipments that have already been treated.
	 */
	WaitingShipments getOutgoingShipments();

}
