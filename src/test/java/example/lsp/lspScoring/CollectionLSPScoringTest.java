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

package example.lsp.lspScoring;

import lsp.*;
import lsp.LSPModule;
import lsp.shipment.ShipmentUtils;
import lsp.LSPResource;
import lsp.shipment.LSPShipment;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static lsp.usecase.UsecaseUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectionLSPScoringTest {

	private final int numberOfShipments = 25;
	private LSP collectionLSP;

	@Before
	public void initialize() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("scenarios/2regions/2regions-network.xml");

		var freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setTimeWindowHandling(FreightConfigGroup.TimeWindowHandling.ignore);


		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		VehicleType collectionVehicleType = CarrierVehicleType.Builder.newInstance(Id.create("CollectionCarrierVehicleType", VehicleType.class))
				.setCapacity(10).setCostPerDistanceUnit(0.0004).setCostPerTimeUnit(0.38).setFixCost(49).setMaxVelocity(50 / 3.6).build();

		Link collectionLink = network.getLinks().get(Id.createLinkId("(4 2) (4 3)")); // (make sure that the link exists)

		CarrierVehicle carrierVehicle = CarrierVehicle.newInstance(Id.createVehicleId("CollectionVehicle"), collectionLink.getId(), collectionVehicleType);

		Carrier carrier = CarrierUtils.createCarrier(Id.create("CollectionCarrier", Carrier.class));
		carrier.setCarrierCapabilities(CarrierCapabilities.Builder.newInstance().addType(collectionVehicleType).addVehicle(carrierVehicle).setFleetSize(FleetSize.INFINITE).build());

		LSPResource collectionAdapter = CollectionCarrierAdapterBuilder.newInstance(Id.create("CollectionCarrierAdapter", LSPResource.class), network)
				.setCollectionScheduler(createDefaultCollectionCarrierScheduler()).setCarrier(carrier).setLocationLinkId(collectionLink.getId()).build();

		LogisticsSolutionElement collectionElement = LSPUtils.LogisticsSolutionElementBuilder
				.newInstance(Id.create("CollectionElement", LogisticsSolutionElement.class)).setResource(collectionAdapter).build();

		LogisticsSolution collectionSolution = LSPUtils.LogisticsSolutionBuilder.newInstance(Id.create("CollectionSolution", LogisticsSolution.class))
				.addSolutionElement(collectionElement).build();

		collectionLSP = LSPUtils.LSPBuilder.getInstance(Id.create("CollectionLSP", LSP.class))
				.setInitialPlan(LSPUtils.createLSPPlan().setAssigner(createSingleSolutionShipmentAssigner()).addSolution(collectionSolution))
				.setSolutionScheduler(createDefaultSimpleForwardSolutionScheduler(Collections.singletonList(collectionAdapter)))
//				.setSolutionScorer(new ExampleLSPScoring.TipScorer())
				.build();

//		TipEventHandler handler = new TipEventHandler();
//		LSPAttribute<Double> value = LSPInfoFunctionUtils.createInfoFunctionValue("TIP IN EUR" );
//		LSPAttributes function = LSPInfoFunctionUtils.createDefaultInfoFunction();
//		function.getAttributes().add(value );
//		TipInfo info = new TipInfo();
//		TipScorer.TipSimulationTracker tipTracker = new TipScorer.TipSimulationTracker();
//		collectionAdapter.addSimulationTracker(tipTracker);
//		TipScorer tipScorer = new TipScorer();
//		collectionLSP.addSimulationTracker( tipScorer );
//		collectionLSP.setScorer(tipScorer);

		ArrayList<Link> linkList = new ArrayList<>(network.getLinks().values());

		for (int i = 1; i < (numberOfShipments + 1); i++) {
			Id<LSPShipment> id = Id.create(i, LSPShipment.class);
			ShipmentUtils.LSPShipmentBuilder builder = ShipmentUtils.LSPShipmentBuilder.newInstance(id);
			Random random = new Random(1);
			int capacityDemand = random.nextInt(10);
			builder.setCapacityDemand(capacityDemand);

			while (true) {
				Collections.shuffle(linkList, random);
				Link pendingFromLink = linkList.get(0);
				if (pendingFromLink.getFromNode().getCoord().getX() <= 4000
						&& pendingFromLink.getFromNode().getCoord().getY() <= 4000
						&& pendingFromLink.getToNode().getCoord().getX() <= 4000
						&& pendingFromLink.getToNode().getCoord().getY() <= 4000) {
					builder.setFromLinkId(pendingFromLink.getId());
					break;
				}
			}

			builder.setToLinkId(collectionLink.getId());
			TimeWindow endTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setEndTimeWindow(endTimeWindow);
			TimeWindow startTimeWindow = TimeWindow.newInstance(0, (24 * 3600));
			builder.setStartTimeWindow(startTimeWindow);
			builder.setDeliveryServiceTime(capacityDemand * 60);
			LSPShipment shipment = builder.build();
			collectionLSP.assignShipmentToLSP(shipment);
		}

		collectionLSP.scheduleSolutions();

		ArrayList<LSP> lspList = new ArrayList<>();
		lspList.add(collectionLSP);
		LSPs lsps = new LSPs(lspList);

		LSPUtils.addLSPs(scenario, lsps);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule( new LSPModule() );
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( LSPScorerFactory.class ).toInstance( ( lsp) -> new ExampleLSPScoring.TipScorer() );
			}
		});

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
//		config.network().setInputFile("scenarios/2regions/2regions-network.xml");
		controler.run();
	}

	@Test
	public void testCollectionLSPScoring() {
		System.out.println(collectionLSP.getSelectedPlan().getScore());
		assertEquals(numberOfShipments, collectionLSP.getShipments().size());
		assertEquals(numberOfShipments, collectionLSP.getSelectedPlan().getSolutions().iterator().next().getShipments()
				.size());
		assertTrue(collectionLSP.getSelectedPlan().getScore() > 0);
		assertTrue(collectionLSP.getSelectedPlan().getScore() <= (numberOfShipments * 5));
		/*noch zu testen
		 * tipTracker
		 * InfoFunction
		 * Info
		 * Scorer
		 */
	}

}
