
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

import java.util.ArrayList;
import java.util.Collection;

/* package-private */ class LSPPlanImpl implements LSPPlan {

	private final Collection<LogisticChain> logisticChains;
	private LSP lsp;
	private double score;
	private ShipmentAssigner assigner;

	LSPPlanImpl() {
		this.logisticChains = new ArrayList<>();
	}

	@Override
	public LSPPlan addLogisticChain(LogisticChain solution) {
		this.logisticChains.add(solution);
		solution.setLSP(this.lsp);
		return this;
	}

	@Override
	public Collection<LogisticChain> getLogisticChains() {
		return logisticChains;
	}

	@Override
	public ShipmentAssigner getAssigner() {
		return assigner;
	}

	@Override
	public LSPPlan setAssigner(ShipmentAssigner assigner) {
		this.assigner = assigner;
		this.assigner.setLSP(this.lsp);
		return this;
	}

	@Override
	public Double getScore() {
		return score;
	}

	@Override
	public void setScore(Double score) {
		this.score = score;
	}

	@Override
	public LSP getLSP() {
		return lsp;
	}

	@Override
	public void setLSP(LSP lsp) {
		this.lsp = lsp;
		if (assigner != null) {
			this.assigner.setLSP(lsp);
			// yy vom Design her wäre es vlt. einfacher und logischer, wenn der assigner einen backpointer auf den LSPPlan hätte. Dann
			// müsste man nicht (wie hier) hedgen gegen unterschiedliche Initialisierungssequenzen. kai, may'22
		}
		for (LogisticChain solution : logisticChains) {
			solution.setLSP(lsp);
		}
	}

	@Override public String toString() {
		StringBuilder strb = new StringBuilder();
			strb.append("[score=").append(this.score).append("]");
			for (LogisticChain solution : this.logisticChains) {
				strb.append(", [solutionId=").append(solution.getId()).append("], [No of SolutionElements=").append(solution.getLogisticChainElements().size()).append("] \n");
				if (!solution.getLogisticChainElements().isEmpty()){
					for (LogisticChainElement solutionElement : solution.getLogisticChainElements()) {
						strb.append("\t \t").append(solutionElement.toString()).append("\n");
					}
				}
			}
		return strb.toString();
	}

}
