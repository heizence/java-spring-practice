package com.example.bookstore.shipping;

/**
 * The courier company's API. Each call goes over the network to another company's servers: it can take seconds, and
 * it can fail.
 */
public interface CourierClient {

    /**
     * Books a pickup: the courier collects the parcel at the warehouse.
     *
     * @return the tracking number of the parcel
     * @throws RuntimeException if the courier can't be reached or refuses the request
     */
    String requestPickup(PickupRequest request);
}
