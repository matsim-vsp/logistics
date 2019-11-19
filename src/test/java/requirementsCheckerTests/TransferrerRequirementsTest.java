package requirementsCheckerTests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Random;

import lsp.*;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import demand.decoratedLSP.LSPDecorator;
import demand.decoratedLSP.LSPPlanDecorator;
import demand.decoratedLSP.LSPPlanWithOfferTransferrer;
import demand.decoratedLSP.LSPWithOffers;
import demand.decoratedLSP.LogisticsSolutionWithOffers;
import demand.demandObject.DemandObject;
import demand.demandObject.DemandObjectImpl;
import demand.offer.Offer;
import demand.offer.OfferFactoryImpl;
import demand.offer.OfferTransferrer;
import lsp.resources.Resource;
import lsp.shipment.Requirement;
import lsp.usecase.CollectionCarrierAdapter;
import lsp.usecase.CollectionCarrierScheduler;
import lsp.usecase.SimpleForwardSolutionScheduler;

public class TransferrerRequirementsTest {
	private Network network;
	private LogisticsSolutionWithOffers blueOfferSolution;
	private LogisticsSolutionWithOffers redOfferSolution;
	private OfferTransferrer transferrer;
	private LSPPlanDecorator collectionPlan;
	private LSPDecorator offerLSP;
	private ArrayList<DemandObject> demandObjects;

	@Before
	public void initialize() {
		Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/2regions/2regions-network.xml");
        this.network = scenario.getNetwork();
		
		CollectionCarrierScheduler redScheduler = new CollectionCarrierScheduler();
		Id<Carrier> redCarrierId = Id.create("RedCarrier", Carrier.class);
		Id<VehicleType> vehicleTypeId = Id.create("CollectionCarrierVehicleType", VehicleType.class);
		CarrierVehicleType.Builder vehicleTypeBuilder = CarrierVehicleType.Builder.newInstance(vehicleTypeId);
		vehicleTypeBuilder.setCapacity(10);
		vehicleTypeBuilder.setCostPerDistanceUnit(0.0004);
		vehicleTypeBuilder.setCostPerTimeUnit(0.38);
		vehicleTypeBuilder.setFixCost(49);
		vehicleTypeBuilder.setMaxVelocity(50/3.6);
		org.matsim.vehicles.VehicleType collectionType = vehicleTypeBuilder.build();
		
		Id<Link> collectionLinkId = Id.createLinkId("(4 2) (4 3)");
		Id<Vehicle> redVehicleId = Id.createVehicleId("RedVehicle");
		CarrierVehicle redVehicle = CarrierVehicle.newInstance(redVehicleId, collectionLinkId);
		redVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder redCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		redCapabilitiesBuilder.addType(collectionType);
		redCapabilitiesBuilder.addVehicle(redVehicle);
		redCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities redCapabilities = redCapabilitiesBuilder.build();
		Carrier redCarrier = CarrierImpl.newInstance(redCarrierId);
		redCarrier.setCarrierCapabilities(redCapabilities);
				
		Id<Resource> redAdapterId = Id.create("RedCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder redAdapterBuilder = CollectionCarrierAdapter.Builder.newInstance(redAdapterId, network);
		redAdapterBuilder.setCollectionScheduler(redScheduler);
		redAdapterBuilder.setCarrier(redCarrier);
		redAdapterBuilder.setLocationLinkId(collectionLinkId);
		Resource redCollectionAdapter = redAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> redElementId = Id.create("RedCollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder redCollectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(redElementId );
		redCollectionElementBuilder.setResource(redCollectionAdapter);
		LogisticsSolutionElement redCollectionElement = redCollectionElementBuilder.build();
		
		Id<LogisticsSolution> redCollectionSolutionId = Id.create("RedCollectionSolution", LogisticsSolution.class);
		LogisticsSolutionWithOffers.Builder redOfferSolutionBuilder = LogisticsSolutionWithOffers.Builder.newInstance(redCollectionSolutionId);
		redOfferSolutionBuilder.addSolutionElement(redCollectionElement);
		redOfferSolution = redOfferSolutionBuilder.build();
		redOfferSolution.getInfos().add(new RedInfo());
		OfferFactoryImpl redOfferFactory = new OfferFactoryImpl(redOfferSolution);
		redOfferFactory.addOffer(new NonsenseOffer());
		redOfferSolution.setOfferFactory(redOfferFactory);
		
		collectionPlan = new LSPPlanWithOfferTransferrer();
		collectionPlan.addSolution(redOfferSolution);
	
		CollectionCarrierScheduler blueScheduler = new CollectionCarrierScheduler();
		Id<Carrier> blueCarrierId = Id.create("BlueCarrier", Carrier.class);
		Id<Vehicle> blueVehicleId = Id.createVehicleId("BlueVehicle");
		CarrierVehicle blueVehicle = CarrierVehicle.newInstance(blueVehicleId, collectionLinkId);
		blueVehicle.setVehicleType(collectionType);
		
		CarrierCapabilities.Builder blueCapabilitiesBuilder = CarrierCapabilities.Builder.newInstance();
		blueCapabilitiesBuilder.addType(collectionType);
		blueCapabilitiesBuilder.addVehicle(blueVehicle);
		blueCapabilitiesBuilder.setFleetSize(FleetSize.INFINITE);
		CarrierCapabilities blueCapabilities = blueCapabilitiesBuilder.build();
		Carrier blueCarrier = CarrierImpl.newInstance(blueCarrierId);
		blueCarrier.setCarrierCapabilities(blueCapabilities);
				
		Id<Resource> blueAdapterId = Id.create("BlueCarrierAdapter", Resource.class);
		CollectionCarrierAdapter.Builder blueAdapterBuilder = CollectionCarrierAdapter.Builder.newInstance(blueAdapterId, network);
		blueAdapterBuilder.setCollectionScheduler(blueScheduler);
		blueAdapterBuilder.setCarrier(blueCarrier);
		blueAdapterBuilder.setLocationLinkId(collectionLinkId);
		Resource blueCollectionAdapter = blueAdapterBuilder.build();
		
		Id<LogisticsSolutionElement> blueElementId = Id.create("BlueCollectionElement", LogisticsSolutionElement.class);
		LSPUtils.LogisticsSolutionElementBuilder blueCollectionElementBuilder = LSPUtils.LogisticsSolutionElementBuilder.newInstance(blueElementId );
		blueCollectionElementBuilder.setResource(blueCollectionAdapter);
		LogisticsSolutionElement blueCollectionElement = blueCollectionElementBuilder.build();
		
		Id<LogisticsSolution> blueCollectionSolutionId = Id.create("BlueCollectionSolution", LogisticsSolution.class);
		LogisticsSolutionWithOffers.Builder blueOfferSolutionBuilder = LogisticsSolutionWithOffers.Builder.newInstance(blueCollectionSolutionId);
		blueOfferSolutionBuilder.addSolutionElement(blueCollectionElement);
		blueOfferSolution = blueOfferSolutionBuilder.build();
		blueOfferSolution.getInfos().add(new BlueInfo());
		OfferFactoryImpl blueOfferFactory = new OfferFactoryImpl(blueOfferSolution);
		blueOfferFactory.addOffer(new NonsenseOffer());
		blueOfferSolution.setOfferFactory(blueOfferFactory);
		collectionPlan.addSolution(blueOfferSolution);
		
		transferrer = new RequirementsTransferrer();
		collectionPlan.setOfferTransferrer(transferrer);
		
		LSPWithOffers.Builder offerLSPBuilder = LSPWithOffers.Builder.getInstance();
		offerLSPBuilder.setInitialPlan(collectionPlan);
		Id<LSP> collectionLSPId = Id.create("CollectionLSP", LSP.class);
		offerLSPBuilder.setId(collectionLSPId);
		ArrayList<Resource> resourcesList = new ArrayList<Resource>();
		resourcesList.add(redCollectionAdapter);
		resourcesList.add(blueCollectionAdapter);
			
		SolutionScheduler simpleScheduler = new SimpleForwardSolutionScheduler(resourcesList);
		offerLSPBuilder.setSolutionScheduler(simpleScheduler);
		offerLSP = offerLSPBuilder.build();
		LSPPlanDecorator decorator = (LSPPlanDecorator)offerLSP.getSelectedPlan();
		
		demandObjects = new ArrayList<DemandObject>();
	    
	    Random rand = new Random(1); 
	    
	    for(int i = 1; i < 11; i++) {
        	Id<DemandObject> id = Id.create(i, DemandObject.class);
        	DemandObjectImpl.Builder builder = DemandObjectImpl.Builder.newInstance();
        	
        	boolean blue = rand.nextBoolean();
        	if (blue == true) {
        		builder.addRequirement(new BlueRequirement());
        	}
        	else {
        		builder.addRequirement(new RedRequirement());
        	}
        	
        	DemandObject demandObject = builder.build();
        	demandObjects.add(demandObject);
	    }	
	}
	   
	@Test
	  public void testRequirementsTransferrer() {
	    	for(DemandObject demandObject : demandObjects) {
	    		Offer offer = offerLSP.getOffer(demandObject, "nonsense", null);
	    		for(Requirement requirement : demandObject.getRequirements()) {
	    			if(requirement instanceof RedRequirement) {
	    				assertTrue(offer.getSolution() == redOfferSolution);
	    			}
	    			if(requirement instanceof BlueRequirement) {
	    				assertTrue(offer.getSolution() == blueOfferSolution);
	    			}
	    		}
	    	}
	 }






}