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

package example.lsp.initialPlans;

import com.google.inject.Provider;
import lsp.*;
import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentUtils;
import lsp.usecase.UsecaseUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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

/**
 * This is an academic example for the 2-echelon problem.
 * It uses the 9x9-grid network from the matsim-examples.
 * <p>
 * The depot is located at the outer border of the network, while the jobs are located in the middle area.
 * The {@link lsp.LSP} has two different {@link lsp.LSPPlan}s:
 * 1) direct delivery from the depot
 * 2) Using a TransshipmentHub: All goods were brought from the depot to the hub, reloaded and then brought from the hub to the customers
 * <p>
 * The decision which of these plans is chosen should be made via the Score of the plans.
 * We will modify the costs of the vehicles and/or for using(having) the Transshipment hub. Depending on this setting,
 * the plan selection should be done accordingly.
 * <p>
 * Please note: This example is in part on existing examples, but I start from the scratch for a) see, if this works and b) have a "clean" class :)
 *
 * @author Kai Martins-Turner (kturner)
 */
final class ExampleTwoEchelonGrid {


	private static final Logger log = Logger.getLogger(ExampleTwoEchelonGrid.class);
	private static final Id<Link> DEPOT_LINK_ID = Id.createLinkId("i(5,0)");
	private static final Id<Link> HUB_LINK_ID = Id.createLinkId("j(5,3)");

	private static final VehicleType VEH_TYPE_LARGE_10 = CarrierVehicleType.Builder.newInstance(Id.create("large10", VehicleType.class))
			.setCapacity(10)
			.setMaxVelocity(10)
			.setFixCost(130)
			.setCostPerDistanceUnit(0.001)
			.setCostPerTimeUnit(0.01)
			.build();

	private static final VehicleType VEH_TYPE_SMALL_02 = CarrierVehicleType.Builder.newInstance(Id.create("small02", VehicleType.class))
			.setCapacity(2)
			.setMaxVelocity(10)
			.setFixCost(25)
			.setCostPerDistanceUnit(0.0005)
			.setCostPerTimeUnit(0.005)
			.build();

	private ExampleTwoEchelonGrid() {
	} // so it cannot be instantiated

	public static void main(String[] args) {
		log.info("Prepare Config");
		Config config = prepareConfig(args);

		log.info("Prepare scenario");
		Scenario scenario = prepareScenario(config);

		log.info("Prepare Controler");
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new LSPModule());
			}
		});
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( LSPStrategyManager.class ).toProvider( new Provider<LSPStrategyManager>(){
					@Override public LSPStrategyManager get(){
						LSPStrategyManager strategyManager = new LSPStrategyManagerImpl();
						strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
						return strategyManager;
					}
				} );
			}
		} );

		log.info("Run MATSim");
		controler.run();

		log.info("Some results ....");
		printScores(LSPUtils.getLSPs(controler.getScenario()).getLSPs().values());
		for (LSP lsp : LSPUtils.getLSPs(controler.getScenario()).getLSPs().values()) {
			UsecaseUtils.printResults(controler.getControlerIO().getOutputPath(), lsp);
		}
		log.info("Done.");
	}

	private static Config prepareConfig(String[] args) {
		Config config = ConfigUtils.createConfig();
		if (args.length != 0) {
			for (String arg : args) {
				log.warn(arg);
			}
			ConfigUtils.applyCommandline(config, args);

			CommandLine cmd = ConfigUtils.getCommandLine(args);
		} else {
			config.controler().setOutputDirectory("output/2echelon/");
			config.controler().setLastIteration(10);
		}

		config.network().setInputFile(String.valueOf(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9"), "grid9x9.xml")));
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.ignore);

		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Change speed on all links to 30 km/h (8.33333 m/s) for easier computation --> Freeflow TT per link is 2min
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setFreespeed(30 / 3.6);
		}

		log.info("Add LSP to the scenario");
		LSPUtils.addLSPs(scenario, new LSPs(Collections.singletonList(createLSP(scenario.getNetwork()))));

		return scenario;
	}

	private static LSP createLSP(Network network) {
		log.info("create LSP");

		LSPPlan lspPlan_direct;
		{
			log.info("Create lspPlan for direct delivery");

			Carrier directCarrier = CarrierUtils.createCarrier(Id.create("directCarrier", Carrier.class));
			directCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			CarrierUtils.addCarrierVehicle(directCarrier, CarrierVehicle.newInstance(Id.createVehicleId("directTruck"), DEPOT_LINK_ID, VEH_TYPE_LARGE_10));
			LSPResource directCarrierRessource = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(Id.create("directCarrierRes", LSPResource.class), network)
					.setCarrier(directCarrier)
					.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
					.build();

			LogisticsSolutionElement directCarrierElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(Id.create("directCarrierLSE", LogisticsSolutionElement.class))
					.setResource(directCarrierRessource)
					.build();


			LogisticsSolution solution_direct = LSPUtils.LogisticsSolutionBuilder.newInstance(Id.create("directSolution", LogisticsSolution.class))
					.addSolutionElement(directCarrierElement)
					.build();

			final ShipmentAssigner singlesolutionShipmentAssigner = UsecaseUtils.createSingleSolutionShipmentAssigner();
			lspPlan_direct = LSPUtils.createLSPPlan()
					.addSolution(solution_direct)
					.setAssigner(singlesolutionShipmentAssigner);
		}

		LSPPlan lspPlan_withHub;
		{
			log.info("Create lspPlan with Hub");

			Carrier mainCarrier = CarrierUtils.createCarrier(Id.create("mainCarrier", Carrier.class));
			mainCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			//TODO: Funktioniert mit DistributionCarrier ... aber nicht mit main Carrier. --> Ansehen! --> Main ben??tigt eventuell vorher ein Depot?
			CarrierUtils.addCarrierVehicle(mainCarrier, CarrierVehicle.newInstance(Id.createVehicleId("mainTruck"), DEPOT_LINK_ID, VEH_TYPE_LARGE_10));
			LSPResource mainCarrierRessource = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(Id.create("mainCarrierRes", LSPResource.class), network)
					.setCarrier(mainCarrier)
					.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
					.build();

			LogisticsSolutionElement mainCarrierLSE = LSPUtils.LogisticsSolutionElementBuilder.newInstance(Id.create("mainCarrierLSE", LogisticsSolutionElement.class))
					.setResource(mainCarrierRessource)
					.build();

			//The scheduler for the first reloading point is created --> this will be the depot in this use case
			LSPResourceScheduler hubScheduler = UsecaseUtils.TranshipmentHubSchedulerBuilder.newInstance()
					.setCapacityNeedFixed(10) //Time needed, fixed (for Scheduler)
					.setCapacityNeedLinear(1) //additional time needed per shipmentSize (for Scheduler)
					.build();

			//The scheduler is added to the Resource and the Resource is created
			LSPResource hubResource = UsecaseUtils.TransshipmentHubBuilder.newInstance(Id.create("Hub", LSPResource.class), HUB_LINK_ID)
					.setTransshipmentHubScheduler(hubScheduler)
					.build();

			LogisticsSolutionElement hubLSE = LSPUtils.LogisticsSolutionElementBuilder.newInstance(Id.create("HubLSE", LogisticsSolutionElement.class))
					.setResource(hubResource)
					.build(); //Nicht unbedingt n??tig, aber nehme den alten Hub nun als Depot. Waren werden dann dort "Zusammengestellt".

			Carrier distributionCarrier = CarrierUtils.createCarrier(Id.create("distributionCarrier", Carrier.class));
			distributionCarrier.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);

			CarrierUtils.addCarrierVehicle(distributionCarrier, CarrierVehicle.newInstance(Id.createVehicleId("distributionTruck"), HUB_LINK_ID, VEH_TYPE_SMALL_02));
			LSPResource distributionCarrierRessource = UsecaseUtils.DistributionCarrierAdapterBuilder.newInstance(Id.create("distributionCarrierRes", LSPResource.class), network)
					.setCarrier(distributionCarrier)
					.setDistributionScheduler(UsecaseUtils.createDefaultDistributionCarrierScheduler())
					.build();

			LogisticsSolutionElement distributionCarrierElement = LSPUtils.LogisticsSolutionElementBuilder.newInstance(Id.create("distributionCarrierLSE", LogisticsSolutionElement.class))
					.setResource(distributionCarrierRessource)
					.build();

			//Kettenbildung per hand, damit dann klar ist, wie das Scheduling ablaufen soll. TODO: Vielleicht bekommt man das noch eleganter hin.
			// z.B. in der Reihenfolge in der die solutionsElements der LogisticsSolution zugeordnet werden: ".addSolutionElement(..)"
			mainCarrierLSE.connectWithNextElement(hubLSE);
			hubLSE.connectWithNextElement(distributionCarrierElement);

			LogisticsSolution solution_direct = LSPUtils.LogisticsSolutionBuilder.newInstance(Id.create("hubSolution", LogisticsSolution.class))
					.addSolutionElement(mainCarrierLSE)
					.addSolutionElement(hubLSE)
					.addSolutionElement(distributionCarrierElement)
					.build();


			lspPlan_withHub = LSPUtils.createLSPPlan()
					.addSolution(solution_direct)
					.setAssigner(UsecaseUtils.createSingleSolutionShipmentAssigner());

		}

		//Todo: Auch das ist wirr: Muss hier alle sommeln, damit man die dann im LSPBuilder dem SolutionSceduler mitgeben kann. Im Nachgang packt man dann aber erst den zweiten Plan dazu ... urgs KMT'Jul22
		List<LSPPlan> lspPlans = new ArrayList<>();
		lspPlans.add(lspPlan_withHub);
		lspPlans.add(lspPlan_direct);

		LSPUtils.LSPBuilder lspBuilder = LSPUtils.LSPBuilder.getInstance(Id.create("myLSP", LSP.class))
//				.setInitialPlan(lspPlan_direct)
				.setInitialPlan(lspPlan_withHub)
//				.setSolutionScheduler(LSPUtils.createForwardSolutionScheduler())  //Does not work, because of "null" pointer in predecessor.. TODO: Have a look into it later... kmt jul22
				.setSolutionScheduler(UsecaseUtils.createDefaultSimpleForwardSolutionScheduler(createResourcesListFromLSPPlans(lspPlans))) //Hier m??ssen irgendwie die Ressourcen beider Pl??ne rein, oder? - Habe ich jetzt gemacht. kmt ' jul22
//				.setSolutionScorer(new MyLSPScorer())
				;


		LSP lsp = lspBuilder.build();
//		lsp.addPlan(lspPlan_withHub); //add the second Plan to the lsp
		lsp.addPlan(lspPlan_direct); //add the second Plan to the lsp

		//Todo: ZZZZZZZZZ Trying to enable choosing of other plan... first try: use a RandomPlanSelector, KMT Jul22
//		GenericPlanStrategy<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<>(new RandomPlanSelector<>());
//		GenericStrategyManager<LSPPlan, LSP> strategyManager = new GenericStrategyManagerImpl<>();
//		strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 1);
//		LSPReplanner replanner = LSPReplanningUtils.createDefaultLSPReplanner(strategyManager);
//		replanner.setEmbeddingContainer(lsp);

//		lsp.setReplanner(replanner);

		log.info("create initial LSPShipments");
		log.info("assign the shipments to the LSP");
		for (LSPShipment shipment : createInitialLSPShipments()) {
			lsp.assignShipmentToLSP(shipment);
		}

		log.info("schedule the LSP with the shipments and according to the scheduler of the Resource");
		lsp.scheduleSolutions();

		return lsp;
	}

	private static Collection<LSPShipment> createInitialLSPShipments() {
		List<LSPShipment> shipmentList = new ArrayList<>();

		Random rand = MatsimRandom.getRandom();
		int i = 1;
//		for(int i = 1; i < 6; i++) {
		Id<LSPShipment> id = Id.create("Shipment_" + i, LSPShipment.class);
		ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
//		int capacityDemand = rand.nextInt(10);
		int capacityDemand = rand.nextInt(5);
		builder.setCapacityDemand(capacityDemand);

		builder.setFromLinkId(DEPOT_LINK_ID);
		builder.setToLinkId(Id.createLinkId("i(5,5)R"));

		builder.setEndTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
		builder.setStartTimeWindow(TimeWindow.newInstance(0, (24 * 3600)));
		builder.setDeliveryServiceTime(capacityDemand * 60);

		shipmentList.add(builder.build());
//		}
		return shipmentList;
	}

	//TODO: This is maybe something that can go into a utils class ... KMT jul22

	private static List<LSPResource> createResourcesListFromLSPPlans(List<LSPPlan> lspPlans) {
		log.info("Collecting all LSPResources from the LSPPlans");
		List<LSPResource> resourcesList = new ArrayList<>();            //TODO: Mache daraus ein Set, damit jede Resource nur einmal drin ist? kmt Feb22
		for (LSPPlan lspPlan : lspPlans) {
			for (LogisticsSolution solution : lspPlan.getSolutions()) {
				for (LogisticsSolutionElement solutionElement : solution.getSolutionElements()) {
					resourcesList.add(solutionElement.getResource());
				}
			}
		}
		return resourcesList;
	}


	private static void printScores(Collection<LSP> lsps) {
		for (LSP lsp : lsps) {
			log.info("The LSP `` " + lsp.getId() + " ???? has the following number of plans: " + lsp.getPlans().size());
			log.info("The scores are");
			for (LSPPlan plan : lsp.getPlans()) {
				log.info(plan.getScore());
			}
			log.info("The selected plan has the score: " + lsp.getSelectedPlan().getScore());
			log.info("###");
		}
	}
}
