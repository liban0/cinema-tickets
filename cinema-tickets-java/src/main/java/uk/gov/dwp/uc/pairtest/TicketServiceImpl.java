package uk.gov.dwp.uc.pairtest;

import java.util.Objects;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

/**
 * Checks the request, books seats for adults and children only, then charges the account in whole pounds.
 * Invalid requests are rejected before calling payment or seat booking.
 * See {@code README.md} in this module for rules (including the infant lap limit).
 */
public class TicketServiceImpl implements TicketService {

    private static final int MAX_TICKETS_PER_PURCHASE = 25;
    private static final int ADULT_PRICE_GBP = 25;
    private static final int CHILD_PRICE_GBP = 15;
    private static final int INFANT_PRICE_GBP = 0; // Infants do not pay as per rule

    private final SeatReservationService seatReservationService;
    private final TicketPaymentService ticketPaymentService;

    public TicketServiceImpl(SeatReservationService seatReservationService, TicketPaymentService ticketPaymentService) {
        this.seatReservationService = Objects.requireNonNull(seatReservationService, "seatReservationService");
        this.ticketPaymentService = Objects.requireNonNull(ticketPaymentService, "ticketPaymentService");
    }

    /**
     * Should only have private methods other than the one below.
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccount(accountId);
        TicketTypeRequest[] requests = ticketTypeRequests != null ? ticketTypeRequests : new TicketTypeRequest[0];
        TicketTotals totals = validateAndAggregate(requests);

        long account = accountId;
        seatReservationService.reserveSeat(account, totals.seatsToReserve());
        ticketPaymentService.makePayment(account, totals.totalPriceGbp());
    }

    private static void validateAccount(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account id must be a positive number");
        }
    }

    private static TicketTotals validateAndAggregate(TicketTypeRequest[] requests) {
        if (requests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket request is required");
        }

        int adults = 0;
        int children = 0;
        int infants = 0;

        for (TicketTypeRequest request : requests) {
            if (request == null) {
                throw new InvalidPurchaseException("Ticket request cannot be null");
            }
            int count = request.getNoOfTickets();
            if (count < 0) {
                throw new InvalidPurchaseException("Number of tickets cannot be negative");
            }
            switch (request.getTicketType()) {
                case ADULT -> adults += count;
                case CHILD -> children += count;
                case INFANT -> infants += count;
            }
        }

        int totalTickets = adults + children + infants;
        if (totalTickets == 0) {
            throw new InvalidPurchaseException("Total number of tickets must be at least one");
        }
        if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException("Cannot purchase more than " + MAX_TICKETS_PER_PURCHASE + " tickets");
        }
        if ((children > 0 || infants > 0) && adults == 0) {
            throw new InvalidPurchaseException("Child and infant tickets require at least one adult ticket");
        }
        if (infants > adults) {
            throw new InvalidPurchaseException("Cannot have more infant tickets than adult tickets (lap rule)");
        }

        return new TicketTotals(adults, children, infants);
    }

    private record TicketTotals(int adults, int children, int infants) {

        int seatsToReserve() {
            return adults + children;
        }

        int totalPriceGbp() {
            return adults * ADULT_PRICE_GBP + children * CHILD_PRICE_GBP + infants * INFANT_PRICE_GBP;
        }
    }

}
