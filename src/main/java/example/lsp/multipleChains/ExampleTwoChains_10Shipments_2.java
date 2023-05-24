package example.lsp.multipleChains;

import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
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
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;

import java.util.*;

public class ExampleTwoChains_10Shipments_2 {

	private static final double TOLL_VALUE = 0;

	private static final Logger log = LogManager.getLogger(ExampleTwoChains_10Shipments_2.class);

	private static final Id<Link> DEPOT_SOUTH_LINK_ID = Id.createLinkId("i(1,0)");
	private static final Id<Link> DEPOT_NORTH_LINK_ID = Id.createLinkId("i(1,8)");

	private static final VehicleType VEH_TYPE_LARGE_50 = CarrierVehicleType.Builder.newInstance(Id.create("large50", VehicleType.class))
			.setCapacity(50)
			.setMaxVelocity(10)
			.setFixCost(100)
			.setCostPerDistanceUnit(0.01)
			.setCostPerTimeUnit(0.01)
			.build();

	private ExampleTwoChains_10Shipments_2() {
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
				carrierScorer.setToll(TOLL_VALUE);

				bind(CarrierScoringFunctionFactory.class).toInstance(carrierScorer);
				bind(CarrierStrategyManager.class).toProvider(() -> {
					CarrierStrategyManager strategyManager = FreightUtils.createDefaultCarrierStrategyManager();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind( LSPStrategyManager.class ).toProvider(() -> {
					LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
					strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
					return strategyManager;
				});
				bind(LSPScorerFactory.class).toInstance( () -> new MyLSPScorer());
			}
		});

		log.info("Run MATSim");

		//The VSP default settings are designed for person transport simulation. After talking to Kai, they will be set to WARN here. Kai MT may'23
		controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
		controler.run();

		log.info("Some results ....");

		for (LSP lsp : LSPUtils.getLSPs(controler.getScenario()).getLSPs().values()) {
			UsecaseUtils.printScores(controler.getControlerIO().getOutputPath(), lsp);
			UsecaseUtils.printShipmentsOfLSP(controler.getControlerIO().getOutputPath(), lsp);
			UsecaseUtils.printResults_shipmentPlan(controler.getControlerIO().getOutputPath(), lsp);
			UsecaseUtils.printResults_shipmentLog(controler.getControlerIO().getOutputPath(), lsp);
		}
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
			config.controler().setOutputDirectory("output/2chains10shipments_2");
			config.controler().setLastIteration(2);
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

		// A plan with two different logistic chains in the south and north, with respective carriers is created
		LSPPlan lspPlan_twoChains_1;
		{
			LogisticChainElement southCarrierElement1;
			{
				Carrier carrierSouth1 = CarrierUtils.createCarrier(Id.create("carrierSouth1", Carrier.class));
				carrierSouth1.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarrierUtils.addCarrierVehicle(carrierSouth1, CarrierVehicle.newInstance(Id.createVehicleId("directTruck1"), DEPOT_SOUTH_LINK_ID, VEH_TYPE_LARGE_50));
				LSPResource carrierSouthResource = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(carrierSouth1, network)
						.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
						.build();

				southCarrierElement1 = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("southCarrierElement1", LogisticChainElement.class))
						.setResource(carrierSouthResource)
						.build();
			}

			LogisticChainElement northCarrierElement1;
			{
				Carrier carrierNorth1 = CarrierUtils.createCarrier(Id.create("CarrierNorth1", Carrier.class));
				carrierNorth1.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarrierUtils.addCarrierVehicle(carrierNorth1, CarrierVehicle.newInstance(Id.createVehicleId("directTruck1"), DEPOT_NORTH_LINK_ID, VEH_TYPE_LARGE_50));
				LSPResource carrierNorthResource = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(carrierNorth1, network)
						.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
						.build();

				northCarrierElement1 = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("northCarrierElement1", LogisticChainElement.class))
						.setResource(carrierNorthResource)
						.build();
			}

			LogisticChain southChain1 = LSPUtils.LogisticChainBuilder.newInstance(Id.create("southChain1", LogisticChain.class))
					.addLogisticChainElement(southCarrierElement1)
					.build();

			LogisticChain northChain1 = LSPUtils.LogisticChainBuilder.newInstance(Id.create("northChain1", LogisticChain.class))
					.addLogisticChainElement(northCarrierElement1)
					.build();

			final ShipmentAssigner shipmentAssigner = Utils.createConsecutiveLogisticChainShipmentAssigner();
			lspPlan_twoChains_1 = LSPUtils.createLSPPlan()
					.addLogisticChain(southChain1)
					.addLogisticChain(northChain1)
					.setAssigner(shipmentAssigner);
		}

		LSPPlan lspPlan_twoChains_2;
		{
			LogisticChainElement southCarrierElement2;
			{
				Carrier carrierSouth2 = CarrierUtils.createCarrier(Id.create("carrierSouth2", Carrier.class));
				carrierSouth2.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarrierUtils.addCarrierVehicle(carrierSouth2, CarrierVehicle.newInstance(Id.createVehicleId("directTruck2"), DEPOT_SOUTH_LINK_ID, VEH_TYPE_LARGE_50));
				LSPResource carrierSouthResource2 = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(carrierSouth2, network)
						.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
						.build();

				southCarrierElement2 = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("southCarrierElement2", LogisticChainElement.class))
						.setResource(carrierSouthResource2)
						.build();
			}

			LogisticChainElement northCarrierElement2;
			{
				Carrier carrierNorth2 = CarrierUtils.createCarrier(Id.create("CarrierNorth2", Carrier.class));
				carrierNorth2.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

				CarrierUtils.addCarrierVehicle(carrierNorth2, CarrierVehicle.newInstance(Id.createVehicleId("directTruck2"), DEPOT_NORTH_LINK_ID, VEH_TYPE_LARGE_50));
				LSPResource carrierNorthResource2 = UsecaseUtils.DistributionCarrierResourceBuilder.newInstance(carrierNorth2, network)
						.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
						.build();

				northCarrierElement2 = LSPUtils.LogisticChainElementBuilder.newInstance(Id.create("northCarrierElement2", LogisticChainElement.class))
						.setResource(carrierNorthResource2)
						.build();
			}

			LogisticChain southChain2 = LSPUtils.LogisticChainBuilder.newInstance(Id.create("southChain2", LogisticChain.class))
					.addLogisticChainElement(southCarrierElement2)
					.build();

			LogisticChain northChain2 = LSPUtils.LogisticChainBuilder.newInstance(Id.create("northChain2", LogisticChain.class))
					.addLogisticChainElement(northCarrierElement2)
					.build();

			final ShipmentAssigner shipmentAssigner = Utils.createConsecutiveLogisticChainShipmentAssigner();
			lspPlan_twoChains_2 = LSPUtils.createLSPPlan()
					.addLogisticChain(southChain2)
					.addLogisticChain(northChain2)
					.setAssigner(shipmentAssigner);
		}

		List<LSPPlan> lspPlans = new ArrayList<>();
		lspPlans.add(lspPlan_twoChains_1);
		lspPlans.add(lspPlan_twoChains_2);

		LSP lsp = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
				.setInitialPlan(lspPlan_twoChains_1)
				.setLogisticChainScheduler(UsecaseUtils.createDefaultSimpleForwardLogisticChainScheduler(createResourcesListFromLSPPlans(lspPlans)))
				.build();
		lsp.addPlan(lspPlan_twoChains_2);

		log.info("create initial LSPShipments");
		log.info("assign the shipments to the LSP");
		for (LSPShipment shipment : createInitialLSPShipments(network)) {
			lsp.assignShipmentToLSP(shipment);
		}

		log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
		lsp.scheduleLogisticChains();

		return lsp;
	}

	private static Collection<LSPShipment> createInitialLSPShipments(Network network) {
		List<LSPShipment> shipmentList = new ArrayList<>();

		Random rand2 = MatsimRandom.getLocalInstance();


		List<String> zoneLinkList = Arrays.asList("i(4,4)", "i(5,4)", "i(6,4)", "i(4,6)", "i(5,6)", "i(6,6)",
				"j(3,5)", "j(3,6)", "j(3,7)", "j(5,5)", "j(5,6)", "j(5,7)",
				"i(4,5)R", "i(5,5)R", "i(6,5)R", "i(4,7)R", "i(5,7)R", "i(6,7)R",
				"j(4,5)R", "j(4,6)R", "j(4,7)R", "j(6,5)R", "j(6,6)R", "j(6,7)R");
		for (String linkIdString : zoneLinkList) {
			if (!network.getLinks().containsKey( Id.createLinkId(linkIdString))) {
				throw new RuntimeException("Link is not in Network!");
			}
		}

		for(int i = 1; i <= 10; i++) {
			Id<LSPShipment> id = Id.create("Shipment_" + i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);

			int capacityDemand = 1;
			builder.setCapacityDemand(capacityDemand);

			builder.setFromLinkId(DEPOT_SOUTH_LINK_ID);
			final Id<Link> toLinkId = Id.createLinkId(zoneLinkList.get(rand2.nextInt(zoneLinkList.size()-1)));
			builder.setToLinkId(toLinkId);

			builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
			builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
			builder.setDeliveryServiceTime(capacityDemand * 60);

			shipmentList.add(builder.build());
		}
		return shipmentList;
	}

	private static List<LSPResource> createResourcesListFromLSPPlans(List<LSPPlan> lspPlans) {
		log.info("Collecting all LSPResources from the LSPPlans");
		List<LSPResource> resourceList = new ArrayList<>();
		for (LSPPlan lspPlan : lspPlans) {
			for (LogisticChain solution : lspPlan.getLogisticChains()) {
				for (LogisticChainElement solutionElement : solution.getLogisticChainElements()) {
					resourceList.add(solutionElement.getResource());
				}
			}
		}
		return resourceList;
	}

}
