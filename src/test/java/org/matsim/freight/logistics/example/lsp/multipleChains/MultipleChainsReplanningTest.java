package org.matsim.freight.logistics.example.lsp.multipleChains;

import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.example.lsp.multipleChains.EventBasedCarrierScorer_MultipleChains;
import org.matsim.freight.logistics.example.lsp.multipleChains.MultipleChainsUtils;
import org.matsim.freight.logistics.example.lsp.multipleChains.MyLSPScorer;
import org.matsim.freight.logistics.example.lsp.multipleChains.RandomShiftingStrategyFactory;
import org.matsim.freight.logistics.resourceImplementations.ResourceImplementationUtils;
import org.matsim.freight.logistics.resourceImplementations.distributionCarrier.DistributionCarrierUtils;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentPlan;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.junit.Before;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controler.CarrierControlerUtils;
import org.matsim.freight.carriers.controler.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controler.CarrierStrategyManager;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MultipleChainsReplanningTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");

	private static final VehicleType VEH_TYPE_LARGE_50 = CarrierVehicleType.Builder.newInstance(Id.create("large50", VehicleType.class))
			.setCapacity(50)
			.setMaxVelocity(10)
			.setFixCost(150)
			.setCostPerDistanceUnit(0.01)
			.setCostPerTimeUnit(0.01)
			.build();

	int initialPlanCount;
	int initialPlanShipmentPlanCount;

	int updatedPlanCount;
	int innovatedPlanShipmentPlanCount;
	int innovatedPlanFirstLogisticChainShipmentCount;
	boolean innovatedPlanHasEmptyShipmentPlanElements = false;

	@Before
	public void initialize() {

		Config config = prepareConfig();

		Scenario scenario = prepareScenario(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					install(new LSPModule());
				}
			});

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				final EventBasedCarrierScorer_MultipleChains carrierScorer = new EventBasedCarrierScorer_MultipleChains();
				bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
				bind(LSPScorerFactory.class).toInstance(MyLSPScorer::new);
				bind(CarrierStrategyManager.class).toProvider(() -> {
					CarrierStrategyManager strategyManager = CarrierControlerUtils.createDefaultCarrierStrategyManager();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind(LSPStrategyManager.class).toProvider(() -> {
					LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
					strategyManager.addStrategy( RandomShiftingStrategyFactory.createStrategy(), null, 1);
					return strategyManager;
				});
			}
		});

		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

		LSP lsp = LSPUtils.getLSPs(controler.getScenario()).getLSPs().values().iterator().next();

		initialPlanCount = lsp.getPlans().size();
		initialPlanShipmentPlanCount = lsp.getPlans().get(0).getShipmentPlans().size();

		controler.run();

		updatedPlanCount = lsp.getPlans().size();
		innovatedPlanShipmentPlanCount = lsp.getPlans().get(1).getShipmentPlans().size();

		innovatedPlanFirstLogisticChainShipmentCount = lsp.getPlans().get(1).getLogisticChains().iterator().next().getShipmentIds().size();
		for (ShipmentPlan shipmentPlan : lsp.getPlans().get(1).getShipmentPlans()) {
			if (shipmentPlan.getPlanElements().isEmpty()) {
				innovatedPlanHasEmptyShipmentPlanElements = true;
			}
		}


	}

	private Config prepareConfig() {
		Config config = ConfigUtils.createConfig();

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(1);

		config.network().setInputFile(String.valueOf(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "grid9x9.xml")));
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setWriteEventsInterval(1);

		FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightCarriersConfigGroup.TimeWindowHandling.ignore);

		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setFreespeed(30 / 3.6);
			link.setCapacity(1000);
		}

		LSPUtils.addLSPs(scenario, new LSPs(Collections.singletonList(createLSP(scenario))));

		return scenario;
	}

	private static LSP createLSP(Scenario scenario) {
		Network network = scenario.getNetwork();

		// A plan with two different logistic chains on the left and right, with respective carriers is created
		LSPPlan multipleOneEchelonChainsPlan;
		{
			LogisticChainElement leftCarrierElement;
			{
				Carrier carrierLeft = CarriersUtils.createCarrier(Id.create("carrierLeft", Carrier.class));
				carrierLeft.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarriersUtils.addCarrierVehicle(carrierLeft, CarrierVehicle.newInstance(Id.createVehicleId("veh_small"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
				LSPResource carrierLeftResource = DistributionCarrierUtils.DistributionCarrierResourceBuilder.newInstance(carrierLeft, network)
						.setDistributionScheduler(DistributionCarrierUtils.createDefaultDistributionCarrierScheduler())
						.build();

				leftCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("leftCarrierElement", LogisticChainElement.class))
						.setResource(carrierLeftResource)
						.build();
			}

			LogisticChainElement rightCarrierElement;
			{
				Carrier carrierRight = CarriersUtils.createCarrier(Id.create("carrierRight", Carrier.class));
				carrierRight.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarriersUtils.addCarrierVehicle(carrierRight, CarrierVehicle.newInstance(Id.createVehicleId("veh_small"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
				LSPResource carrierRightResource = DistributionCarrierUtils.DistributionCarrierResourceBuilder.newInstance(carrierRight, network)
						.setDistributionScheduler(DistributionCarrierUtils.createDefaultDistributionCarrierScheduler())
						.build();

				rightCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("rightCarrierElement", LogisticChainElement.class))
						.setResource(carrierRightResource)
						.build();
			}

			LogisticChain leftChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("leftChain", LogisticChain.class))
					.addLogisticChainElement(leftCarrierElement)
					.build();

			LogisticChain rightChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("rightChain", LogisticChain.class))
					.addLogisticChainElement(rightCarrierElement)
					.build();

			final ShipmentAssigner shipmentAssigner = MultipleChainsUtils.createRoundRobinLogisticChainShipmentAssigner();
			multipleOneEchelonChainsPlan = LSPUtils.createLSPPlan()
					.addLogisticChain(leftChain)
					.addLogisticChain(rightChain)
					.setAssigner(shipmentAssigner);

			multipleOneEchelonChainsPlan.setType(MultipleChainsUtils.LspPlanTypes.MULTIPLE_ONE_ECHELON_CHAINS.toString());
		}

		List<LSPPlan> lspPlans = new ArrayList<>();
		lspPlans.add(multipleOneEchelonChainsPlan);

		LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
				.setInitialPlan(multipleOneEchelonChainsPlan)
				.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(lspPlans)))
				.build();

		for (LSPShipment shipment : createInitialLSPShipments()) {
			lsp.assignShipmentToLSP(shipment);
		}

		lsp.scheduleLogisticChains();

		return lsp;
	}

	private static Collection<LSPShipment> createInitialLSPShipments() {
		List<LSPShipment> shipmentList = new ArrayList<>();
		int capacityDemand = 1;

		for (int i = 1; i <= 10; i++) {
			if (i % 2 != 0) {
				Id<LSPShipment> id = Id.create("ShipmentLeft_" + i, LSPShipment.class);
				ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);

				builder.setCapacityDemand(capacityDemand);
				builder.setFromLinkId(DEPOT_LINK_ID);
				final Id<Link> shipmentLeftLinkId = Id.createLinkId("i(1,9)R");
				builder.setToLinkId(shipmentLeftLinkId);

				builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
				builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
				builder.setDeliveryServiceTime(capacityDemand * 60);

				shipmentList.add(builder.build());
			} else {
				Id<LSPShipment> id = Id.create("ShipmentRight_" + i, LSPShipment.class);
				ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);

				builder.setCapacityDemand(capacityDemand);
				builder.setFromLinkId(DEPOT_LINK_ID);
				final Id<Link> shipmentRightLinkId = Id.createLinkId("j(9,9)");
				builder.setToLinkId(shipmentRightLinkId);

				builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
				builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
				builder.setDeliveryServiceTime(capacityDemand * 60);

				shipmentList.add(builder.build());
			}
		}
		return shipmentList;
	}

	private static List<LSPResource> createResourcesListFromLSPPlans(List<LSPPlan> lspPlans) {
		List<LSPResource> resourceList = new ArrayList<>();
		for (LSPPlan lspPlan : lspPlans) {
			for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
				for (LogisticChainElement logisticChainElement : logisticChain.getLogisticChainElements()) {
					resourceList.add(logisticChainElement.getResource());
				}
			}
		}
		return resourceList;
	}


	@Test
	public void testGeneratedInnovatedPlan() {
		// a new innovative plan should have been added
		assertEquals(initialPlanCount + 1, updatedPlanCount);

		// number of shipmentPlans should remain the same
		assertEquals(initialPlanShipmentPlanCount, innovatedPlanShipmentPlanCount);
	}

	@Test
	public void testShipmentDistributionChanged() {
		// starting from 5 shipments, exactly one shipment should have shifted the logistic chain
		assertTrue(innovatedPlanFirstLogisticChainShipmentCount == 4 || innovatedPlanFirstLogisticChainShipmentCount == 6);
	}

	@Test
	public void testScheduledLogisticChains() {
		assertFalse(innovatedPlanHasEmptyShipmentPlanElements);
	}
}
