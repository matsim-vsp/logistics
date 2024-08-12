/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2022 by the members listed in the COPYING,       *
  *                   LICENSE and WARRANTY file.                            *
  * email           : info at matsim dot org                                *
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  *   This program is free software; you can redistribute it and/or modify  *
  *   it under the terms of the GNU General Public License as published by  *
  *   the Free Software Foundation; either version 2 of the License, or     *
  *   (at your option) any later version.                                   *
  *   See also COPYING, LICENSE and WARRANTY file                           *
  *                                                                         *
  * ***********************************************************************
 */

package org.matsim.freight.logistics.resourceImplementations;

import java.util.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.Tour.Leg;
import org.matsim.freight.carriers.Tour.TourElement;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.matsim.vehicles.VehicleType;

/**
 * In the case of the MainRunResource, the incoming LSPShipments are bundled together until their
 * total weight exceeds the capacity of the deployed vehicle type. Then, this bundle of LSPShipments
 * is converted to a scheduled tour from the freight contrib of MATSim. The start of this tour is
 * located at the first TranshipmentHub and the end at the second one. All LSPShipments are
 * converted to services that take place at the end point of the tour.
 *
 * <p>Tour is routed by MATSim Network Router.
 *
 * <p>* The tour starts after the last shipment * has arrived and the time necessary for loading all
 * shipments into the vehicle * has passed.
 */
/*package-private*/ class MainRunCarrierScheduler extends LSPResourceScheduler {

  private Carrier carrier;
  private MainRunCarrierResource resource;
  private ArrayList<LSPShipmentCarrierServicePair> pairs;
  private int tourIdIndex = 1; // Have unique TourIds for the MainRun.

  /*package-private*/ MainRunCarrierScheduler() {
    this.pairs = new ArrayList<>();
  }

  @Override
  protected void initializeValues(LSPResource resource) {
    this.pairs = new ArrayList<>();
    if (resource.getClass() == MainRunCarrierResource.class) {
      this.resource = (MainRunCarrierResource) resource;
      this.carrier = this.resource.getCarrier();
      this.carrier.getServices().clear();
      this.carrier.getShipments().clear();
      this.carrier.getPlans().clear();
    }
  }

  @Override
  protected void scheduleResource() {
    int load = 0;
    List<LspShipmentWithTime> copyOfAssignedShipments = new ArrayList<>(lspShipmentsWithTime);
    copyOfAssignedShipments.sort(Comparator.comparingDouble(LspShipmentWithTime::getTime));
    ArrayList<LspShipmentWithTime> shipmentsInCurrentTour = new ArrayList<>();
    //		ArrayList<ScheduledTour> scheduledTours = new ArrayList<>();
    List<CarrierPlan> scheduledPlans = new LinkedList<>();

    for (LspShipmentWithTime tuple : copyOfAssignedShipments) {
      // Add job as "services" to the carrier. So the carrier has this available
      CarrierService carrierService = convertToCarrierService(tuple);
      carrier.getServices().put(carrierService.getId(), carrierService);

      VehicleType vehicleType =
          ResourceImplementationUtils.getVehicleTypeCollection(carrier).iterator().next();
      if ((load + tuple.getLspShipment().getSize())
          > vehicleType.getCapacity().getOther().intValue()) {
        load = 0;
        CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
        scheduledPlans.add(plan);
        shipmentsInCurrentTour.clear();
      }
      shipmentsInCurrentTour.add(tuple);
      load = load + tuple.getLspShipment().getSize();
    }
    if (!shipmentsInCurrentTour.isEmpty()) {
      CarrierPlan plan = createPlan(carrier, shipmentsInCurrentTour);
      scheduledPlans.add(plan);
      shipmentsInCurrentTour.clear();
    }

    List<ScheduledTour> scheduledTours = new LinkedList<>();
    for (CarrierPlan scheduledPlan : scheduledPlans) {
      scheduledTours.addAll(scheduledPlan.getScheduledTours());
    }
    CarrierPlan plan = new CarrierPlan(carrier, scheduledTours);
    plan.setScore(CarrierSchedulerUtils.sumUpScore(scheduledPlans));
    carrier.setSelectedPlan(plan);
  }

  private CarrierPlan createPlan(Carrier carrier, List<LspShipmentWithTime> tuples) {

    // TODO: Allgemein: Hier ist alles manuell zusammen gesetzt; es findet KEINE Tourenplanung
    // statt!
    NetworkBasedTransportCosts.Builder tpcostsBuilder =
        NetworkBasedTransportCosts.Builder.newInstance(
            resource.getNetwork(),
            ResourceImplementationUtils.getVehicleTypeCollection(resource.getCarrier()));
    NetworkBasedTransportCosts netbasedTransportcosts = tpcostsBuilder.build();
    Collection<ScheduledTour> tours = new ArrayList<>();

    Tour.Builder tourBuilder = Tour.Builder.newInstance(Id.create(tourIdIndex, Tour.class));
    tourIdIndex++;
    tourBuilder.scheduleStart(Id.create(resource.getStartLinkId(), Link.class));

    double totalLoadingTime = 0;
    double latestTupleTime = 0;

    for (LspShipmentWithTime tuple : tuples) {
      totalLoadingTime = totalLoadingTime + tuple.getLspShipment().getDeliveryServiceTime();
      if (tuple.getTime() > latestTupleTime) {
        latestTupleTime = tuple.getTime();
      }
      tourBuilder.addLeg(new Leg());
      CarrierService carrierService = convertToCarrierService(tuple);
      pairs.add(new LSPShipmentCarrierServicePair(tuple, carrierService));
      tourBuilder.scheduleService(carrierService);
    }

    tourBuilder.addLeg(new Leg());
    switch (resource.getVehicleReturn()) {
      case returnToFromLink -> // The more "urban" behaviour: The vehicle returns to its origin
                               // (startLink).
      tourBuilder.scheduleEnd(Id.create(resource.getStartLinkId(), Link.class));
      case endAtToLink -> // The more "long-distance" behaviour: The vehicle ends at its destination
                          // (toLink).
      tourBuilder.scheduleEnd(Id.create(resource.getEndLinkId(), Link.class));
      default -> throw new IllegalStateException(
          "Unexpected value: " + resource.getVehicleReturn());
    }
    Tour vehicleTour = tourBuilder.build();
    CarrierVehicle vehicle =
        carrier.getCarrierCapabilities().getCarrierVehicles().values().iterator().next();
    double tourStartTime = latestTupleTime + totalLoadingTime;
    ScheduledTour sTour = ScheduledTour.newInstance(vehicleTour, vehicle, tourStartTime);

    tours.add(sTour);
    CarrierPlan plan = new CarrierPlan(carrier, tours);
    NetworkRouter.routePlan(plan, netbasedTransportcosts);
    plan.setScore(scorePlanManually(plan));
    return plan;
  }

  /**
   * For the main run, there is currently (nov'22) no jsprit planning. The plan is instead
   * constructed manually. As a consequence, there is no score (from jsprit) for this plan
   * available. To avoid issues in later scoring of the LSP, we would like to hava also a score for
   * the MainRunCarrier. This is calculated here manually
   *
   * <p>It bases on the - vehicle's fixed costs - distance dependent costs - (expected) travel time
   * dependent costs NOT included is the calculation of activity times,... But this is currently
   * also missing e.g. in the distributionCarrier, where the VRP setup does not include this :(
   *
   * @param plan The carrierPlan, that should get scored.
   * @return the calculated score
   */
  private double scorePlanManually(CarrierPlan plan) {
    // score plan // Note: Activities are not scored, but they are also NOT scored for the
    // Distribution carrier (as the VRP is currently set up) kmt nov'22
    double score = 0.;
    for (ScheduledTour scheduledTour : plan.getScheduledTours()) {
      // vehicle fixed costs
      score = score + scheduledTour.getVehicle().getType().getCostInformation().getFixedCosts();

      // distance
      double distance = 0.0;
      double time = 0.0;
      for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {
        if (tourElement instanceof Leg leg) {
          // distance
          NetworkRoute route = (NetworkRoute) leg.getRoute();
          for (Id<Link> linkId : route.getLinkIds()) {
            distance = distance + resource.getNetwork().getLinks().get(linkId).getLength();
          }
          if (route.getEndLinkId()
              != route
                  .getStartLinkId()) { // Do not calculate any distance, if start and endpoint are
                                       // identical
            distance =
                distance + resource.getNetwork().getLinks().get(route.getEndLinkId()).getLength();
          }

          // travel time (exp.)
          time = time + leg.getExpectedTransportTime();
        }
      }
      score =
          score
              + scheduledTour.getVehicle().getType().getCostInformation().getCostsPerMeter()
                  * distance;
      score =
          score
              + scheduledTour.getVehicle().getType().getCostInformation().getCostsPerSecond()
                  * time;
    }
    return (-score); // negative, because we are looking at "costs" instead of "utility"
  }

  private CarrierService convertToCarrierService(LspShipmentWithTime tuple) {
    Id<CarrierService> serviceId =
        Id.create(tuple.getLspShipment().getId().toString(), CarrierService.class);
    CarrierService.Builder builder =
        CarrierService.Builder.newInstance(serviceId, resource.getEndLinkId());
    builder.setCapacityDemand(tuple.getLspShipment().getSize());
    builder.setServiceDuration(tuple.getLspShipment().getDeliveryServiceTime());
    return builder.build();
  }

  @Override
  protected void updateShipments() {
    for (LspShipmentWithTime lspShipmentWithTime : lspShipmentsWithTime) {
      for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
        Tour tour = scheduledTour.getTour();
        for (TourElement element : tour.getTourElements()) {
          if (element instanceof Tour.ServiceActivity serviceActivity) {
            LSPShipmentCarrierServicePair carrierPair =
                new LSPShipmentCarrierServicePair(
                    lspShipmentWithTime, serviceActivity.getService());
            for (LSPShipmentCarrierServicePair pair : pairs) {
              if (pair.tuple == carrierPair.tuple
                  && pair.carrierService.getId() == carrierPair.carrierService.getId()) {
                addShipmentLoadElement(lspShipmentWithTime, tour);
                addShipmentTransportElement(lspShipmentWithTime, tour, serviceActivity);
                addShipmentUnloadElement(lspShipmentWithTime, tour, serviceActivity);
                addMainTourRunStartEventHandler(pair.carrierService, lspShipmentWithTime, resource, tour);
                addMainRunTourEndEventHandler(pair.carrierService, lspShipmentWithTime, resource, tour);
              }
            }
          }
        }
      }
    }
  }

  private void addShipmentLoadElement(
      LspShipmentWithTime tuple, Tour tour) {
    ShipmentUtils.ScheduledShipmentLoadBuilder builder =
        ShipmentUtils.ScheduledShipmentLoadBuilder.newInstance();
    builder.setResourceId(resource.getId());
    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        builder.setLogisticChainElement(element);
      }
    }
    int startIndex =
        tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
    Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
    double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
    double cumulatedLoadingTime = 0;
    for (TourElement element : tour.getTourElements()) {
      if (element instanceof Tour.ServiceActivity activity) {
        cumulatedLoadingTime = cumulatedLoadingTime + activity.getDuration();
      }
    }
    builder.setStartTime(startTimeOfTransport - cumulatedLoadingTime);
    builder.setEndTime(startTimeOfTransport);

    ShipmentPlanElement load = builder.build();
    String idString =
        load.getResourceId()
            + String.valueOf(load.getLogisticChainElement().getId())
            + load.getElementType();
    Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
    ShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getLspShipment().getId())
        .addPlanElement(id, load);
  }

  private void addShipmentTransportElement(
      LspShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
    ShipmentUtils.ScheduledShipmentTransportBuilder builder =
        ShipmentUtils.ScheduledShipmentTransportBuilder.newInstance();
    builder.setResourceId(resource.getId());
    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        builder.setLogisticChainElement(element);
      }
    }
    int startIndex =
        tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
    Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
    double startTimeOfTransport = legAfterStart.getExpectedDepartureTime();
    builder.setStartTime(startTimeOfTransport);
    builder.setEndTime(legAfterStart.getExpectedTransportTime() + startTimeOfTransport);
    builder.setCarrierId(carrier.getId());
    builder.setFromLinkId(tour.getStartLinkId());
    builder.setToLinkId(tour.getEndLinkId());
    builder.setCarrierService(serviceActivity.getService());
    ShipmentPlanElement transport = builder.build();
    String idString =
        transport.getResourceId()
            + String.valueOf(transport.getLogisticChainElement().getId())
            + transport.getElementType();
    Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
    ShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getLspShipment().getId())
        .addPlanElement(id, transport);
  }

  private void addShipmentUnloadElement(
      LspShipmentWithTime tuple, Tour tour, Tour.ServiceActivity serviceActivity) {
    ShipmentUtils.ScheduledShipmentUnloadBuilder builder =
        ShipmentUtils.ScheduledShipmentUnloadBuilder.newInstance();
    builder.setResourceId(resource.getId());
    for (LogisticChainElement element : resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        builder.setLogisticsChainElement(element);
      }
    }
    double cumulatedLoadingTime = 0;
    for (TourElement element : tour.getTourElements()) {
      if (element instanceof Tour.ServiceActivity activity) {
        cumulatedLoadingTime = cumulatedLoadingTime + activity.getDuration();
      }
    }
    int startIndex =
        tour.getTourElements().indexOf(tour.getTourElements().indexOf(tour.getStart()));
    Leg legAfterStart = (Leg) tour.getTourElements().get(startIndex + 1);
    builder.setStartTime(
        legAfterStart.getExpectedDepartureTime() + legAfterStart.getExpectedTransportTime());
    builder.setEndTime(
        legAfterStart.getExpectedDepartureTime()
            + legAfterStart.getExpectedTransportTime()
            + cumulatedLoadingTime);

    ShipmentPlanElement unload = builder.build();
    String idString =
        unload.getResourceId()
            + String.valueOf(unload.getLogisticChainElement().getId())
            + unload.getElementType();
    Id<ShipmentPlanElement> id = Id.create(idString, ShipmentPlanElement.class);
    ShipmentUtils.getOrCreateShipmentPlan(super.lspPlan, tuple.getLspShipment().getId())
        .addPlanElement(id, unload);
  }

  private void addMainTourRunStartEventHandler(
      CarrierService carrierService,
      LspShipmentWithTime tuple,
      LSPCarrierResource resource,
      Tour tour) {
    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        LSPTourStartEventHandler handler =
            new LSPTourStartEventHandler(
                tuple.getLspShipment(), carrierService, element, resource, tour);
        tuple.getLspShipment().addSimulationTracker(handler);
        break;
      }
    }
  }

  private void addMainRunTourEndEventHandler(
      CarrierService carrierService,
      LspShipmentWithTime tuple,
      LSPCarrierResource resource,
      Tour tour) {
    for (LogisticChainElement element : this.resource.getClientElements()) {
      if (element.getIncomingShipments().getLspShipmentsWTime().contains(tuple)) {
        LSPTourEndEventHandler handler =
            new LSPTourEndEventHandler(
					 tuple.getLspShipment(),carrierService, element, resource, tour);
        tuple.getLspShipment().addSimulationTracker(handler);
        break;
      }
    }
  }

  private record LSPShipmentCarrierServicePair(LspShipmentWithTime tuple, CarrierService carrierService) {}
}
