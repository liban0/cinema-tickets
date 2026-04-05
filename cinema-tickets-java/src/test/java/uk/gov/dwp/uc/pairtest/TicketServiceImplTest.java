package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

class TicketServiceImplTest {

    private static final long VALID_ACCOUNT = 1L;

    private SeatReservationService seatReservationService;
    private TicketPaymentService ticketPaymentService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        seatReservationService = mock(SeatReservationService.class);
        ticketPaymentService = mock(TicketPaymentService.class);
        ticketService = new TicketServiceImpl(seatReservationService, ticketPaymentService);
    }

    @Test
    void ticketTypeRequest_rejectsNullType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new TicketTypeRequest(null, 1));
        assertEquals("Ticket type cannot be null", ex.getMessage());
    }

    @Test
    void ticketServiceImpl_rejectsNullSeatReservationService() {
        assertThrows(NullPointerException.class,
                () -> new TicketServiceImpl(null, mock(TicketPaymentService.class)));
    }

    @Test
    void ticketServiceImpl_rejectsNullPaymentService() {
        assertThrows(NullPointerException.class,
                () -> new TicketServiceImpl(mock(SeatReservationService.class), null));
    }

    @Test
    void purchaseTickets_reservesSeatsThenTakesPayment() {
        ticketService.purchaseTickets(
                VALID_ACCOUNT,
                new TicketTypeRequest(Type.ADULT, 2),
                new TicketTypeRequest(Type.CHILD, 1),
                new TicketTypeRequest(Type.INFANT, 1));

        InOrder order = inOrder(seatReservationService, ticketPaymentService);
        order.verify(seatReservationService).reserveSeat(VALID_ACCOUNT, 3);
        order.verify(ticketPaymentService).makePayment(VALID_ACCOUNT, 2 * 25 + 15);
    }

    @Test
    void purchaseTickets_mergesMultipleLinesForSameType() {
        ticketService.purchaseTickets(
                VALID_ACCOUNT,
                new TicketTypeRequest(Type.ADULT, 1),
                new TicketTypeRequest(Type.ADULT, 1));

        verify(seatReservationService).reserveSeat(VALID_ACCOUNT, 2);
        verify(ticketPaymentService).makePayment(VALID_ACCOUNT, 50);
    }

    @Test
    void purchaseTickets_allowsTwentyFiveTickets() {
        ticketService.purchaseTickets(VALID_ACCOUNT, new TicketTypeRequest(Type.ADULT, 25));

        verify(seatReservationService).reserveSeat(VALID_ACCOUNT, 25);
        verify(ticketPaymentService).makePayment(VALID_ACCOUNT, 25 * 25);
    }

    @Test
    void purchaseTickets_infantsDoNotAddSeatsOrPrice() {
        ticketService.purchaseTickets(VALID_ACCOUNT, new TicketTypeRequest(Type.ADULT, 3),
                new TicketTypeRequest(Type.INFANT, 3));

        verify(seatReservationService).reserveSeat(VALID_ACCOUNT, 3);
        verify(ticketPaymentService).makePayment(VALID_ACCOUNT, 3 * 25);
    }

    @Test
    void purchaseTickets_rejectsNullAccountId() {
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(null, new TicketTypeRequest(Type.ADULT, 1)));
        assertEquals("Account id must be a positive number", ex.getMessage());
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_rejectsNonPositiveAccountId() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(0L, new TicketTypeRequest(Type.ADULT, 1)));
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(-1L, new TicketTypeRequest(Type.ADULT, 1)));
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_rejectsNoRequests() {
        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(VALID_ACCOUNT));
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_rejectsNullRequestElement() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(VALID_ACCOUNT, new TicketTypeRequest(Type.ADULT, 1), null));
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_rejectsNegativeTicketCount() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(VALID_ACCOUNT, new TicketTypeRequest(Type.ADULT, -1)));
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_rejectsZeroTotalTickets() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(VALID_ACCOUNT, new TicketTypeRequest(Type.ADULT, 0)));
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_rejectsMoreThanTwentyFiveTickets() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(VALID_ACCOUNT, new TicketTypeRequest(Type.ADULT, 26)));
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_rejectsChildWithoutAdult() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(VALID_ACCOUNT, new TicketTypeRequest(Type.CHILD, 1)));
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_rejectsInfantWithoutAdult() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(VALID_ACCOUNT, new TicketTypeRequest(Type.INFANT, 1)));
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_rejectsMoreInfantsThanAdults() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(VALID_ACCOUNT,
                        new TicketTypeRequest(Type.ADULT, 1),
                        new TicketTypeRequest(Type.INFANT, 2)));
        verifyNoInteractions(seatReservationService, ticketPaymentService);
    }

    @Test
    void purchaseTickets_allowsInfantsEqualToAdultCount() {
        ticketService.purchaseTickets(VALID_ACCOUNT,
                new TicketTypeRequest(Type.ADULT, 2),
                new TicketTypeRequest(Type.INFANT, 2));

        verify(seatReservationService).reserveSeat(VALID_ACCOUNT, 2);
        verify(ticketPaymentService).makePayment(VALID_ACCOUNT, 50);
    }

    @Test
    void purchaseTickets_nullVarargsTreatedAsEmpty() {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(VALID_ACCOUNT, (TicketTypeRequest[]) null));
        verify(seatReservationService, never()).reserveSeat(anyLong(), anyInt());
        verify(ticketPaymentService, never()).makePayment(anyLong(), anyInt());
    }
}
