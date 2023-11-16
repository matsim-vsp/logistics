package org.matsim.freight.logistics.example.lsp.multipleChains;

import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LogisticChain;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class RandomShiftingStrategyFactory {

	private RandomShiftingStrategyFactory() {} // class contains only static methods; do not instantiate.

	static GenericPlanStrategy<LSPPlan, LSP> createStrategy() {

		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new ScoringConfigGroup()));
		GenericPlanStrategyModule<LSPPlan> randomModule = new GenericPlanStrategyModule<>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
			}

			@Override
			public void handlePlan(LSPPlan lspPlan) {

				// Shifting shipments only makes sense for multiple chains
				if (lspPlan.getLogisticChains().size() < 2) return;

				LSP lsp = lspPlan.getLSP();

				// Make a new list of shipments and pick a random shipment from it
				List<LSPShipment> shipments = new ArrayList<>(lsp.getShipments());
				int shipmentIndex = MatsimRandom.getRandom().nextInt(lsp.getShipments().size());
				LSPShipment shipment = shipments.get(shipmentIndex);

				// Find and remove the random shipment from its current logistic chain
				LogisticChain sourceLogisticChain = null;
				for (LogisticChain logisticChain : lsp.getSelectedPlan().getLogisticChains()) {
					if (logisticChain.getShipmentIds().remove(shipment.getId())) {
						sourceLogisticChain = logisticChain;
						break;
					}
				}

				// Find a new logistic chain for the shipment
				// Ensure that the chain selected is not the same as the one it was removed from
				int chainIndex;
				LogisticChain targetLogisticChain = null;
				do {
					chainIndex = MatsimRandom.getRandom().nextInt(lsp.getSelectedPlan().getLogisticChains().size());
					Iterator<LogisticChain> iterator = lsp.getSelectedPlan().getLogisticChains().iterator();
					for (int i = 0; iterator.hasNext(); i++) {
						targetLogisticChain = iterator.next();
						if (i == chainIndex) {
							break;
						}
					}
				} while (targetLogisticChain == sourceLogisticChain);

				// Add the shipment to the new logistic chain
				targetLogisticChain.addShipmentToChain(shipment);
			}

			@Override
			public void finishReplanning() {
			}

		};

		strategy.addStrategyModule(randomModule);
		return strategy;
	}

}
