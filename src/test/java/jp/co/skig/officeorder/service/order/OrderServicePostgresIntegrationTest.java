package jp.co.skig.officeorder.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jp.co.skig.officeorder.model.cart.CartLineView;
import jp.co.skig.officeorder.model.cart.CartSummaryView;
import jp.co.skig.officeorder.model.cart.CartView;
import jp.co.skig.officeorder.model.coupon.CouponErrorCode;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.service.coupon.CouponValidationException;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(OrderServicePostgresIntegrationTest.IntegrationTestClockConfiguration.class)
@EnabledIfEnvironmentVariable(named = "FEAT002_IT_ENABLED", matches = "true")
class OrderServicePostgresIntegrationTest {
    private static final String SCHEMA_NAME = "feat002_order_service_it_20261023";
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Tokyo");

    @DynamicPropertySource
    static void configureIsolatedDatabase(DynamicPropertyRegistry registry) {
        if (!SCHEMA_NAME.equals(System.getenv("FEAT002_IT_SCHEMA"))) {
            throw new IllegalStateException("FEAT002_IT_SCHEMA must name the dedicated test schema");
        }
        String username = requiredEnvironment("FEAT002_IT_DB_USER");
        String password = requiredEnvironment("FEAT002_IT_DB_PASSWORD");
        if (!"office_order".equals(username)) {
            throw new IllegalStateException("Integration tests require the local office_order database user");
        }

        registry.add("spring.datasource.url",
                () -> "jdbc:postgresql://localhost:5432/office_order?currentSchema=" + SCHEMA_NAME
                        + "&ApplicationName=feat002-order-service-it");
        registry.add("spring.datasource.username", () -> username);
        registry.add("spring.datasource.password", () -> password);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "8");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.batch.jdbc.initialize-schema", () -> "never");
        registry.add("spring.batch.job.enabled", () -> "false");
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set to run isolated integration tests");
        }
        return value;
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private NotificationMailService notificationMailService;

    @Autowired
    private IntegrationTestClock appClock;

    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final List<String> testEmails = new ArrayList<>();
    private String couponCode;
    private String couponName;
    private String failureFunctionName;
    private String failureTriggerName;
    private String delayFunctionName;
    private String delayTriggerName;
    private long couponId;
    private long firstMemberId;
    private long secondMemberId;
    private long initialOrderCount;
    private long initialItemCount;
    private long initialCouponCount;
    private long initialMemberCount;
    private long initialUsageCount;

    @BeforeEach
    void setUp() {
        transactionTemplate.setTransactionManager(transactionManager);
        couponCode = "IT-" + UUID.randomUUID().toString().replace("-", "");
        couponName = "Order service PostgreSQL integration test";
        long[] baseline = tableCounts();
        initialOrderCount = baseline[0];
        initialItemCount = baseline[1];
        initialCouponCount = baseline[2];
        initialMemberCount = baseline[3];
        initialUsageCount = baseline[4];
        firstMemberId = insertMember();
        secondMemberId = insertMember();
        couponId = insertCoupon(10, 10);
        synchronizeOrderNumberCounter();
    }

    @AfterEach
    void tearDown() {
        dropTestTriggers();
        jdbcTemplate.update("DELETE FROM orders WHERE applied_coupon_code = ?", couponCode);
        jdbcTemplate.update(
                "DELETE FROM user_coupon_usages WHERE coupon_id = (SELECT id FROM coupons WHERE code = ?)",
                couponCode);
        jdbcTemplate.update("UPDATE coupons SET is_active = FALSE WHERE code = ?", couponCode);
        if (!testEmails.isEmpty()) {
            jdbcTemplate.update("DELETE FROM members WHERE email IN (?, ?)",
                    testEmails.getFirst(), testEmails.getLast());
        }

        assertThat(tableCounts()).containsExactly(
                initialOrderCount, initialItemCount, initialCouponCount + 1, initialMemberCount, initialUsageCount);
    }

    @Test
    void placeOrder_databaseFailureAtCommit_rollsBackHeaderItemsAndCouponUsage() {
        installDeferredOrderFailure();

        assertThatThrownBy(() -> transactionTemplate.execute(status ->
                orderService.placeOrder(firstMemberId, validForm(), testCart(), couponCode)))
                .isInstanceOf(RuntimeException.class);

        assertThat(count("SELECT COUNT(*) FROM orders WHERE applied_coupon_code = ?", couponCode)).isZero();
        assertThat(count("SELECT COUNT(*) FROM order_items WHERE product_code = ?", testProductCode())).isZero();
        assertThat(count("SELECT used_count_total FROM coupons WHERE id = ?", couponId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM user_coupon_usages WHERE coupon_id = ?", couponId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT 20 + 22", Integer.class)).isEqualTo(42);
    }

    @Test
    void placeOrder_parallelOrders_doNotExceedGlobalUsageLimit() throws Exception {
        jdbcTemplate.update("UPDATE coupons SET max_usage_total = 1, max_usage_per_user = 10 WHERE id = ?", couponId);
        installOrderItemDelay();

        List<OrderOutcome> outcomes = runConcurrentOrders(firstMemberId, secondMemberId);

        assertSingleSuccessAndLimitRejection(outcomes);
        assertThat(count("SELECT used_count_total FROM coupons WHERE id = ?", couponId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE applied_coupon_code = ?", couponCode)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM order_items WHERE product_code = ?", testProductCode())).isEqualTo(1);
        assertThat(count("SELECT COALESCE(SUM(usage_count), 0) FROM user_coupon_usages WHERE coupon_id = ?",
                couponId)).isEqualTo(1);
    }

    @Test
    void placeOrder_parallelOrders_doNotExceedMemberUsageLimit() throws Exception {
        jdbcTemplate.update("UPDATE coupons SET max_usage_total = 10, max_usage_per_user = 1 WHERE id = ?", couponId);
        installOrderItemDelay();

        List<OrderOutcome> outcomes = runConcurrentOrders(firstMemberId, firstMemberId);

        assertSingleSuccessAndLimitRejection(outcomes);
        assertThat(count("SELECT used_count_total FROM coupons WHERE id = ?", couponId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE applied_coupon_code = ?", couponCode)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM order_items WHERE product_code = ?", testProductCode())).isEqualTo(1);
        assertThat(count("SELECT COALESCE(SUM(usage_count), 0) FROM user_coupon_usages WHERE coupon_id = ?",
                couponId)).isEqualTo(1);
        assertThat(count("SELECT COALESCE(SUM(usage_count), 0) FROM user_coupon_usages WHERE member_id = ? AND coupon_id = ?",
                firstMemberId, couponId)).isEqualTo(1);
    }

    private List<OrderOutcome> runConcurrentOrders(long memberIdA, long memberIdB) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<OrderOutcome> first = executor.submit(
                    () -> placeOrderAfterGate(memberIdA, 0, ready, start));
            Future<OrderOutcome> second = executor.submit(
                    () -> placeOrderAfterGate(memberIdB, 1, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            AtomicBoolean lockWaitObserved = new AtomicBoolean();
            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            while (System.nanoTime() < deadline && !first.isDone() && !second.isDone()) {
                Integer waiting = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM pg_stat_activity
                        WHERE datname = current_database()
                          AND application_name = 'feat002-order-service-it'
                          AND pid <> pg_backend_pid()
                          AND state = 'active'
                          AND wait_event_type = 'Lock'
                          AND query ILIKE '%FROM coupons%'
                          AND query ILIKE '%FOR UPDATE%'
                        """, Integer.class);
                if (waiting != null && waiting > 0) {
                    lockWaitObserved.set(true);
                    break;
                }
                Thread.sleep(50);
            }

            List<OrderOutcome> outcomes = List.of(
                    first.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS));
            assertThat(lockWaitObserved)
                    .as("a competing service transaction must wait on the coupon row lock")
                    .isTrue();
            return outcomes;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private OrderOutcome placeOrderAfterGate(long memberId,
                                             long orderDateOffsetDays,
                                             CountDownLatch ready,
                                             CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent order start");
            }
            appClock.setOrderDateOffset(orderDateOffsetDays);
            String orderNumber = transactionTemplate.execute(status ->
                    orderService.placeOrder(memberId, validForm(), testCart(), couponCode));
            return new OrderOutcome(true, null, orderNumber);
        } catch (CouponValidationException ex) {
            return new OrderOutcome(false, ex.errorCode(), null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent order start", ex);
        } finally {
            appClock.clearOrderDateOverride();
        }
    }

    private void assertSingleSuccessAndLimitRejection(List<OrderOutcome> outcomes) {
        assertThat(outcomes).filteredOn(OrderOutcome::succeeded).hasSize(1);
        assertThat(outcomes).filteredOn(outcome ->
                outcome.errorCode() == CouponErrorCode.COUPON_USAGE_LIMIT_REACHED).hasSize(1);
    }

    private void installDeferredOrderFailure() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        failureFunctionName = "it_fail_order_commit_" + suffix;
        failureTriggerName = "it_fail_order_commit_trigger_" + suffix;
        jdbcTemplate.execute("""
                CREATE FUNCTION %s() RETURNS trigger
                LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.applied_coupon_code = '%s' THEN
                        RAISE EXCEPTION 'intentional integration-test commit failure';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """.formatted(failureFunctionName, couponCode));
        jdbcTemplate.execute("""
                CREATE CONSTRAINT TRIGGER %s
                AFTER INSERT ON orders
                DEFERRABLE INITIALLY DEFERRED
                FOR EACH ROW EXECUTE FUNCTION %s()
                """.formatted(failureTriggerName, failureFunctionName));
    }

    private void installOrderItemDelay() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        delayFunctionName = "it_delay_order_item_" + suffix;
        delayTriggerName = "it_delay_order_item_trigger_" + suffix;
        jdbcTemplate.execute("""
                CREATE FUNCTION %s() RETURNS trigger
                LANGUAGE plpgsql AS $$
                BEGIN
                    IF EXISTS (
                        SELECT 1 FROM orders
                        WHERE order_id = NEW.order_id AND applied_coupon_code = '%s'
                    ) THEN
                        PERFORM pg_sleep(3);
                    END IF;
                    RETURN NEW;
                END;
                $$
                """.formatted(delayFunctionName, couponCode));
        jdbcTemplate.execute("""
                CREATE TRIGGER %s
                AFTER INSERT ON order_items
                FOR EACH ROW EXECUTE FUNCTION %s()
                """.formatted(delayTriggerName, delayFunctionName));
    }

    private void dropTestTriggers() {
        if (failureTriggerName != null) {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + failureTriggerName + " ON orders");
        }
        if (delayTriggerName != null) {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + delayTriggerName + " ON order_items");
        }
        if (failureFunctionName != null) {
            jdbcTemplate.execute("DROP FUNCTION IF EXISTS " + failureFunctionName + "()");
        }
        if (delayFunctionName != null) {
            jdbcTemplate.execute("DROP FUNCTION IF EXISTS " + delayFunctionName + "()");
        }
    }

    private long insertMember() {
        String email = "feat002-it-" + UUID.randomUUID() + "@example.invalid";
        testEmails.add(email);
        return jdbcTemplate.queryForObject("""
                INSERT INTO members (
                    personal_or_corporate, last_name, first_name, last_name_kana, first_name_kana,
                    email, gender, anniversary_date, password_hash, postal_code, prefecture, city,
                    address_line, delivery_floor, has_elevator, daytime_phone
                )
                VALUES ('personal', 'Integration', 'Test', 'インテグレーション', 'テスト', ?, 'no_answer',
                        DATE '1990-01-01', 'integration-test-only', '1000001', 'Tokyo', 'Chiyoda',
                        'Test address', 1, FALSE, '0312345678')
                RETURNING member_id
                """, Long.class, email);
    }

    private long insertCoupon(long maxUsageTotal, long maxUsagePerUser) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO coupons (
                    code, name, discount_type, discount_value, min_purchase_amount,
                    valid_from, valid_to, target_conditions, max_usage_total,
                    max_usage_per_user, used_count_total, is_active
                )
                VALUES (?, ?, 'fixed_amount', 1, 0, CURRENT_TIMESTAMP - INTERVAL '10 days',
                        CURRENT_TIMESTAMP + INTERVAL '10 days',
                        '{"allowed_category_ids":["desk"],"allowed_product_ids":[],"target_user_type":"all"}'::jsonb,
                        ?, ?, 0, TRUE)
                RETURNING id
                """, Long.class, couponCode, couponName, maxUsageTotal, maxUsagePerUser);
    }

    private void synchronizeOrderNumberCounter() {
        LocalDate orderDate = LocalDate.now(APP_ZONE);
        String orderNumberPrefix = "ORD" + orderDate.toString().replace("-", "") + "-%";
        jdbcTemplate.update("""
                INSERT INTO order_number_counters (order_date, last_sequence, created_at, updated_at)
                VALUES (
                    ?,
                    COALESCE((
                        SELECT MAX(SUBSTRING(order_number FROM 13)::INTEGER)
                        FROM orders
                        WHERE order_number LIKE ?
                    ), 0),
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (order_date) DO UPDATE
                SET last_sequence = GREATEST(order_number_counters.last_sequence, EXCLUDED.last_sequence),
                    updated_at = CURRENT_TIMESTAMP
                """, orderDate, orderNumberPrefix);
    }

    private CheckoutInputForm validForm() {
        CheckoutInputForm form = new CheckoutInputForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("Integration");
        form.setFirstName("Test");
        form.setLastNameKana("インテグレーション");
        form.setFirstNameKana("テスト");
        form.setEmail(testEmails.getFirst());
        form.setDaytimePhone("0312345678");
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("Tokyo");
        form.setCity("Chiyoda");
        form.setAddressLine("Test address");
        form.setDeliveryFloor("1");
        form.setHasElevator(Boolean.FALSE);
        form.setPaymentMethod("bank_transfer");
        return form;
    }

    private CartView testCart() {
        CartLineView line = new CartLineView(
                1L, 1L, "Integration test product", testProductCode(), "desk", "black",
                new BigDecimal("100"), 99, false, BigDecimal.ZERO, false, 1, "/products/1");
        return new CartView(
                List.of(line),
                1,
                1,
                new CartSummaryView(
                        new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("10"), new BigDecimal("110")));
    }

    private String testProductCode() {
        return "FEAT002-ORDER-IT";
    }

    private long[] tableCounts() {
        return new long[]{
                count("SELECT COUNT(*) FROM orders"),
                count("SELECT COUNT(*) FROM order_items"),
                count("SELECT COUNT(*) FROM coupons"),
                count("SELECT COUNT(*) FROM members"),
                count("SELECT COUNT(*) FROM user_coupon_usages")
        };
    }

    private long count(String sql, Object... arguments) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        return result == null ? 0 : result;
    }

    private record OrderOutcome(boolean succeeded, CouponErrorCode errorCode, String orderNumber) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class IntegrationTestClockConfiguration {
        @Bean
        @Primary
        IntegrationTestClock integrationTestClock() {
            return new IntegrationTestClock();
        }
    }

    static final class IntegrationTestClock extends Clock {
        private final ThreadLocal<Instant> orderDateOverride = new ThreadLocal<>();

        void setOrderDateOffset(long days) {
            orderDateOverride.set(Instant.now().plus(Duration.ofDays(days)));
        }

        void clearOrderDateOverride() {
            orderDateOverride.remove();
        }

        @Override
        public ZoneId getZone() {
            return APP_ZONE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return APP_ZONE.equals(zone) ? this : Clock.system(zone);
        }

        @Override
        public Instant instant() {
            Instant override = orderDateOverride.get();
            return override == null ? Instant.now() : override;
        }
    }
}
