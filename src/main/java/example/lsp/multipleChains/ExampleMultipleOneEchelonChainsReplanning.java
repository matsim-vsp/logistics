package example.lsp.multipleChains;

import lsp.*;
import lsp.resourceImplementations.ResourceImplementationUtils;
import lsp.resourceImplementations.distributionCarrier.DistributionCarrierUtils;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.controler.CarrierStrategyManager;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;

import java.util.*;

public class ExampleMultipleOneEchelonChainsReplanning {

	private static final Logger log = LogManager.getLogger(ExampleMultipleOneEchelonChainsReplanning.class);

	private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");

	private static final VehicleType VEH_TYPE_SMALL_05 = CarrierVehicleType.Builder.newInstance(Id.create("small05", VehicleType.class))
			.setCapacity(5)
			.setMaxVelocity(10)
			.setFixCost(5)
			.setCostPerDistanceUnit(0.001)
			.setCostPerTimeUnit(0.01)
			.build();

	private static final VehicleType VEH_TYPE_LARGE_50 = CarrierVehicleType.Builder.newInstance(Id.create("large50", VehicleType.class))
			.setCapacity(50)
			.setMaxVelocity(10)
			.setFixCost(150)
			.setCostPerDistanceUnit(0.01)
			.setCostPerTimeUnit(0.01)
			.build();

	private ExampleMultipleOneEchelonChainsReplanning() {
	}

	public static void main(String[] args) {
		log.info("Prepare config");
		Config config = prepareConfig(args);

		log.info("Prepare scenario");
		Scenario scenario = prepareScenario(config);

		log.info("Prepare controler");
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
				final MyEventBasedCarrierScorer carrierScorer = new MyEventBasedCarrierScorer();
				bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
				bind(LSPScorerFactory.class).toInstance(MyLSPScorer::new);
				bind(CarrierStrategyManager.class).toProvider(() -> {
					CarrierStrategyManager strategyManager = FreightUtils.createDefaultCarrierStrategyManager();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind(LSPStrategyManager.class).toProvider(() -> {
					LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new ExpBetaPlanSelector<>(new PlanCalcScoreConfigGroup())), null, 1);
//					strategyManager.addStrategy(new RoundRobinDistributionAllShipmentsStrategyFactory().createStrategy(), null, 1);
//					strategyManager.addStrategy(new RandomDistributionAllShipmentsStrategyFactory().createStrategy(), null, 1);
					strategyManager.addStrategy(new RebalancingShipmentsStrategyFactory().createStrategy(), null, 2);
//					strategyManager.addStrategy(new RandomShiftingStrategyFactory().createStrategy(), null, 1);
//					strategyManager.addStrategy(new ProximityStrategyFactory(scenario.getNetwork()).createStrategy(), null, 1);
					strategyManager.setMaxPlansPerAgent(5);
					strategyManager.setPlanSelectorForRemoval(new WorstPlanForRemovalSelector());
					return strategyManager;
				});
			}
		});

		//TODO: Innovation switch not working
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		log.info("Run MATSim");

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();

		log.info("Done.");
	}

	private static Config prepareConfig(String[] args) {
		Config config = ConfigUtils.createConfig();
		if (args.length != 0) {
			for (String arg : args) {
				log.warn(arg);
			}
			ConfigUtils.applyCommandline(config,args);
		} else {
			config.controler().setOutputDirectory("output/multipleOneEchelonChainsReplanning");
			config.controler().setLastIteration(50);
		}
		config.network().setInputFile(String.valueOf(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "grid9x9.xml")));
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWriteEventsInterval(1);

		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.ignore);

		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setFreespeed(30 / 3.6);
			link.setCapacity(1000);
		}

		log.info("Add LSP to the scenario");
		LSPUtils.addLSPs(scenario, new LSPs(Collections.singletonList(createLSP(scenario))));

		return scenario;
	}

	private static LSP createLSP(Scenario scenario) {
		log.info("create LSP");
		Network network = scenario.getNetwork();

		// A plan with one logistic chain, containing a single carrier is created
		LSPPlan singleOneEchelonChainPlan;
		{
			Carrier singleCarrier = CarrierUtils.createCarrier(Id.create("singleCarrier", Carrier.class));
			singleCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			CarrierUtils.addCarrierVehicle(singleCarrier, CarrierVehicle.newInstance(Id.createVehicleId("veh_large"), DEPOT_LINK_ID, VEH_TYPE_LARGE_50));
			LSPResource singleCarrierResource = DistributionCarrierUtils.DistributionCarrierResourceBuilder.newInstance(singleCarrier, network)
					.setDistributionScheduler(DistributionCarrierUtils.createDefaultDistributionCarrierScheduler())
					.build();

			LogisticChainElement singleCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("singleCarrierElement", LogisticChainElement.class))
					.setResource(singleCarrierResource)
					.build();

			LogisticChain singleChain = LSPUtils.LogisticChainBuilder.newInstance(Id.create("singleChain", LogisticChain.class))
					.addLogisticChainElement(singleCarrierElement)
					.build();

			final ShipmentAssigner singleSolutionShipmentAssigner = Utils.createPrimaryLogisticChainShipmentAssigner();
			singleOneEchelonChainPlan = LSPUtils.createLSPPlan()
					.addLogisticChain(singleChain)
					.setAssigner(singleSolutionShipmentAssigner);

			singleOneEchelonChainPlan.setType(Utils.LspPlanTypes.SINGLE_ONE_ECHELON_CHAIN.toString());
		}

		// A plan with two different logistic chains on the left and right, with respective carriers is created
		LSPPlan multipleOneEchelonChainsPlan;
		{
			LogisticChainElement leftCarrierElement;
			{
				Carrier carrierLeft = CarrierUtils.createCarrier(Id.create("carrierLeft", Carrier.class));
				carrierLeft.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarrierUtils.addCarrierVehicle(carrierLeft, CarrierVehicle.newInstance(Id.createVehicleId("veh_small"), DEPOT_LINK_ID, VEH_TYPE_SMALL_05));
				LSPResource carrierLeftResource = DistributionCarrierUtils.DistributionCarrierResourceBuilder.newInstance(carrierLeft, network)
						.setDistributionScheduler(DistributionCarrierUtils.createDefaultDistributionCarrierScheduler())
						.build();

				leftCarrierElement = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("leftCarrierElement", LogisticChainElement.class))
						.setResource(carrierLeftResource)
						.build();
			}

			LogisticChainElement rightCarrierElement;
			{
				Carrier carrierRight = CarrierUtils.createCarrier(Id.create("carrierRight", Carrier.class));
				carrierRight.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarrierUtils.addCarrierVehicle(carrierRight, CarrierVehicle.newInstance(Id.createVehicleId("veh_small"), DEPOT_LINK_ID, VEH_TYPE_SMALL_05));
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

			final ShipmentAssigner shipmentAssigner = Utils.createRandomLogisticChainShipmentAssigner();
			multipleOneEchelonChainsPlan = LSPUtils.createLSPPlan()
					.addLogisticChain(leftChain)
					.addLogisticChain(rightChain)
					.setAssigner(shipmentAssigner);

			multipleOneEchelonChainsPlan.setType(Utils.LspPlanTypes.MULTIPLE_ONE_ECHELON_CHAINS.toString());
		}

		List<LSPPlan> lspPlans = new ArrayList<>();
		lspPlans.add(singleOneEchelonChainPlan);
		lspPlans.add(multipleOneEchelonChainsPlan);

		LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
				.setInitialPlan(singleOneEchelonChainPlan)
				.setLogisticChainScheduler(ResourceImplementationUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(lspPlans)))
				.build();
		lsp.addPlan(multipleOneEchelonChainsPlan);

		log.info("create initial LSPShipments");
		log.info("assign the shipments to the LSP");
		for (LSPShipment shipment : createInitialLSPShipments()) {
			lsp.assignShipmentToLSP(shipment);
		}

		log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
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
		log.info("Collecting all LSPResources from the LSPPlans");
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

}