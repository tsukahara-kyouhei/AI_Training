package jp.co.skig.officeorder.service.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private NotificationMailService notificationMailService;
    private MessageSource messageSource;
    private OrderService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        notificationMailService = Mockito.mock(NotificationMailService.class);
        messageSource = Mockito.mock(MessageSource.class);
        clock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneId.of("UTC"));
        when(messageSource.getMessage(any(String.class), any(Object[].class), any(java.util.Locale.class)))
                .thenReturn("message");
        service = new OrderService(orderRepository, new tools.jackson.databind.ObjectMapper(),
                notificationMailService, clock, messageSource);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createInitialForm_whenMemberHasPrefill_populatesFields() {
        MemberSessionUser member = new MemberSessionUser(1L, "user@example.com", "Yamada", "Taro");
        when(orderRepository.findCheckoutMemberPrefill(1L))
                .thenReturn(Optional.of(new jp.co.skig.officeorder.model.order.CheckoutMemberPrefill(
                        "corporate", "Yamada", "Taro", "ヤマダ", "タロウ", "Company Ltd.", "Sales", "user@example.com",
                        "0312345678", "05012345678", "1234567", "Tokyo", "Chiyoda", "1-1", 2, true)));

        CheckoutInputForm form = service.createInitialForm(Optional.of(member));

        assertEquals("corporate", form.getPersonalOrCorporate());
        assertEquals("Yamada", form.getLastName());
        assertEquals("Taro", form.getFirstName());
        assertEquals("Company Ltd.", form.getCompanyName());
        assertEquals("Chiyoda", form.getCity());
    }

    @Test
    void placeOrder_whenCartIsEmpty_throwsIllegalArgumentException() {
        CartView emptyCart = new CartView(List.of(), 0, 0, new CartSummaryView(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, BigDecimal.ZERO, null // ← この3つを追加
        ));
        CheckoutInputForm form = new CheckoutInputForm();
        form.setDeliveryFloor("1");
        form.setEmail("user@example.com");

        assertThrows(IllegalArgumentException.class, () -> service.placeOrder(null, form, emptyCart));
    }

    @Test
    void placeOrder_whenPaymentMethodConvenienceStore_generatesPaymentInstructionJsonAndSchedulesMail() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            CartLineView line = new CartLineView(1L, 10L, "Desk", "D001", "Black", BigDecimal.valueOf(1000), 10, false,
                    BigDecimal.valueOf(0), false, 1, "/products/10");
            CartSummaryView summary = new CartSummaryView(
                    BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(100),
                    BigDecimal.valueOf(1100),
                    null, BigDecimal.ZERO, null // ← この3つを追加
            );
            CartView cart = new CartView(List.of(line), 1, 1, summary);
            CheckoutInputForm form = new CheckoutInputForm();
            form.setDeliveryFloor("2");
            form.setEmail("user@example.com");
            form.setPaymentMethod("convenience_store");
            when(orderRepository.findCurrentTaxRatePercent()).thenReturn(BigDecimal.TEN);
            when(orderRepository.nextOrderSequence(java.time.LocalDate.of(2025, 1, 1))).thenReturn(1);
            when(orderRepository.insertOrder(any(Map.class))).thenReturn(100L);
            when(orderRepository.findCheckoutMemberPrefill(any(Long.class))).thenReturn(Optional.empty());

            String orderNumber = service.placeOrder(1L, form, cart);

            assertEquals("ORD20250101-000001", orderNumber);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(notificationMailService)
                    .sendOrderCompleteMail(any(jp.co.skig.officeorder.model.mail.OrderCompleteMailPayload.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void legacyConstructor_whenUsingOriginalFiveArguments_setsAdditionalFieldsToDefaults() {
        LegacyCheckoutInputForm form = new LegacyCheckoutInputForm("corporate", "Yamada", "Taro", "Company Ltd.",
                "Chiyoda");

        assertEquals("corporate", form.getPersonalOrCorporate());
        assertEquals("Yamada", form.getLastName());
        assertEquals("Taro", form.getFirstName());
        assertEquals("Company Ltd.", form.getCompanyName());
        assertEquals("Chiyoda", form.getCity());
        assertEquals(null, form.getCouponCode());
        assertEquals(BigDecimal.ZERO, form.getDiscountAmount());
        assertEquals(null, form.getErrorMessage());
    }

    @Test
    void findOrderCompleteView_whenOrderNumberBlank_returnsEmpty() {
        assertEquals(Optional.empty(), service.findOrderCompleteView("  "));
    }

    static class LegacyCheckoutInputForm {
        private final String personalOrCorporate;
        private final String lastName;
        private final String firstName;
        private final String companyName;
        private final String city;
        private final String couponCode;
        private final BigDecimal discountAmount;
        private final String errorMessage;

        LegacyCheckoutInputForm(String personalOrCorporate, String lastName, String firstName, String companyName,
                String city) {
            this(personalOrCorporate, lastName, firstName, companyName, city, null, BigDecimal.ZERO, null);
        }

        LegacyCheckoutInputForm(String personalOrCorporate, String lastName, String firstName, String companyName,
                String city, String couponCode, BigDecimal discountAmount, String errorMessage) {
            this.personalOrCorporate = personalOrCorporate;
            this.lastName = lastName;
            this.firstName = firstName;
            this.companyName = companyName;
            this.city = city;
            this.couponCode = couponCode;
            this.discountAmount = discountAmount;
            this.errorMessage = errorMessage;
        }

        public String getPersonalOrCorporate() {
            return personalOrCorporate;
        }

        public String getLastName() {
            return lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getCompanyName() {
            return companyName;
        }

        public String getCity() {
            return city;
        }

        public String getCouponCode() {
            return couponCode;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
