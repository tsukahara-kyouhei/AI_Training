package jp.co.skig.officeorder.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.order.CheckoutInputForm;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import jp.co.skig.officeorder.model.order.OrderCompleteView;
import jp.co.skig.officeorder.model.order.OrderReorderItem;
import jp.co.skig.officeorder.model.member.MemberOrderHistoryPage;
import jp.co.skig.officeorder.model.member.MemberOrderDetailView;
import jp.co.skig.officeorder.repository.OrderRepository;
import jp.co.skig.officeorder.service.mail.NotificationMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OrderService} の単体テスト（createInitialForm 以降の公開メソッド）。
 *
 * <p>generateOrderNumber の採番テストは {@link OrderServiceGenerateOrderNumberTest} に委ねる。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationMailService notificationMailService;

    @Mock
    private MessageSource messageSource;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-03-10T00:12:00Z"), ZoneOffset.UTC);

    private OrderService sut;

    @BeforeEach
    void setUp() {
        sut = new OrderService(orderRepository, new ObjectMapper(),
                notificationMailService, FIXED_CLOCK, messageSource);
    }

    // =========================================================
    // createInitialForm
    // =========================================================

    // ---- IF-01: ゲスト（member empty）→ 空フォームを返す ----
    @Test
    @DisplayName("ゲストには空の初期フォームを返す")
    void if01_guestMember_returnsEmptyForm() {
        CheckoutInputForm result = sut.createInitialForm(Optional.empty());

        assertThat(result).isNotNull();
        assertThat(result.getLastName()).isNull();
    }

    // ---- IF-02: ログイン会員・Prefill が取得できない → 空フォームを返す ----
    @Test
    @DisplayName("会員情報プリフィルが取得できない場合は空フォームを返す")
    void if02_memberNoPrefill_returnsEmptyForm() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.empty());

        CheckoutInputForm result = sut.createInitialForm(Optional.of(member));

        assertThat(result.getLastName()).isNull();
    }

    // ---- IF-03: ログイン会員・Prefill あり → 会員情報が反映される ----
    @Test
    @DisplayName("会員情報プリフィルが取得できる場合はフォームに反映される")
    void if03_memberWithPrefill_formPopulated() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "test@example.com", "0312345678", null,
                "1000001", "東京都", "千代田区", "丸の内1-1-1", 3, true
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        CheckoutInputForm result = sut.createInitialForm(Optional.of(member));

        assertThat(result.getLastName()).isEqualTo("山田");
        assertThat(result.getFirstName()).isEqualTo("太郎");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    // ---- IF-04: Prefill の郵便番号が7桁なら前3桁・後4桁に分割される ----
    @Test
    @DisplayName("7桁郵便番号はフォームで前3桁・後4桁に分割される")
    void if04_7digitPostalCode_splitCorrectly() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "test@example.com", "0312345678", null,
                "1500001", "東京都", "渋谷区", "道玄坂1-1-1", 2, false
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        CheckoutInputForm result = sut.createInitialForm(Optional.of(member));

        assertThat(result.getPostalCodePart1()).isEqualTo("150");
        assertThat(result.getPostalCodePart2()).isEqualTo("0001");
    }

    // ---- IF-05: Prefill の郵便番号が7桁でない → 郵便番号は設定されない ----
    @Test
    @DisplayName("郵便番号が7桁でない場合はフォームに郵便番号を設定しない")
    void if05_nonStandardPostalCode_notSet() {
        MemberSessionUser member = new MemberSessionUser(1L, "test@example.com", "山田", "太郎");
        CheckoutMemberPrefill prefill = new CheckoutMemberPrefill(
                "personal", "山田", "太郎", "ヤマダ", "タロウ",
                null, null, "test@example.com", "0312345678", null,
                "123", "東京都", "千代田区", "丸の内1-1-1", 1, true
        );
        when(orderRepository.findCheckoutMemberPrefill(1L)).thenReturn(Optional.of(prefill));

        CheckoutInputForm result = sut.createInitialForm(Optional.of(member));

        assertThat(result.getPostalCodePart1()).isNull();
        assertThat(result.getPostalCodePart2()).isNull();
    }

    // =========================================================
    // findOrderCompleteView
    // =========================================================

    // ---- OC-01: 空文字の注文番号 → empty ----
    @Test
    @DisplayName("空文字の注文番号では empty を返す")
    void oc01_blankOrderNumber_returnsEmpty() {
        assertThat(sut.findOrderCompleteView("")).isEmpty();
    }

    // ---- OC-02: null の注文番号 → empty ----
    @Test
    @DisplayName("null 注文番号では empty を返す")
    void oc02_nullOrderNumber_returnsEmpty() {
        assertThat(sut.findOrderCompleteView(null)).isEmpty();
    }

    // ---- OC-03: 有効な注文番号 → リポジトリの結果を返す ----
    @Test
    @DisplayName("有効な注文番号ではリポジトリの結果を返す")
    void oc03_validOrderNumber_returnsRepositoryResult() {
        when(orderRepository.findOrderCompleteByOrderNumber("ORD20260310-000001"))
                .thenReturn(Optional.empty());

        Optional<OrderCompleteView> result = sut.findOrderCompleteView("ORD20260310-000001");

        verify(orderRepository).findOrderCompleteByOrderNumber("ORD20260310-000001");
        assertThat(result).isEmpty();
    }

    // ---- OC-04: 注文番号の前後空白はtrimされる ----
    @Test
    @DisplayName("注文番号の前後空白はtrimされてリポジトリに渡される")
    void oc04_orderNumberWithWhitespace_trimmed() {
        when(orderRepository.findOrderCompleteByOrderNumber("ORD20260310-000001"))
                .thenReturn(Optional.empty());

        sut.findOrderCompleteView("  ORD20260310-000001  ");

        verify(orderRepository).findOrderCompleteByOrderNumber("ORD20260310-000001");
    }

    // =========================================================
    // findMemberOrderHistories
    // =========================================================

    // ---- OH-01: page < 1 のとき 1 に正規化される ----
    @Test
    @DisplayName("page が 0 以下のときは 1 に正規化してリポジトリに渡す")
    void oh01_pageZero_normalizedToOne() {
        MemberOrderHistoryPage page = new MemberOrderHistoryPage(List.of(), 0, 1, 10);
        when(orderRepository.findMemberOrders(1L, 1, 10)).thenReturn(page);

        sut.findMemberOrderHistories(1L, 0);

        verify(orderRepository).findMemberOrders(1L, 1, 10);
    }

    // ---- OH-02: page = 2 → そのままリポジトリに渡す ----
    @Test
    @DisplayName("page が正の値のときはそのままリポジトリに渡す")
    void oh02_validPage_passedThrough() {
        MemberOrderHistoryPage page = new MemberOrderHistoryPage(List.of(), 0, 2, 10);
        when(orderRepository.findMemberOrders(1L, 2, 10)).thenReturn(page);

        sut.findMemberOrderHistories(1L, 2);

        verify(orderRepository).findMemberOrders(1L, 2, 10);
    }

    // =========================================================
    // findMemberOrderDetail
    // =========================================================

    // ---- OD-01: 空文字の注文番号 → empty ----
    @Test
    @DisplayName("空文字の注文番号では empty を返す")
    void od01_blankOrderNumber_returnsEmpty() {
        assertThat(sut.findMemberOrderDetail(1L, "")).isEmpty();
    }

    // ---- OD-02: null の注文番号 → empty ----
    @Test
    @DisplayName("null 注文番号では empty を返す")
    void od02_nullOrderNumber_returnsEmpty() {
        assertThat(sut.findMemberOrderDetail(1L, null)).isEmpty();
    }

    // ---- OD-03: 有効な注文番号 → リポジトリに渡される ----
    @Test
    @DisplayName("有効な注文番号はtrimされてリポジトリに渡される")
    void od03_validOrderNumber_passedToRepository() {
        when(orderRepository.findMemberOrderDetail(1L, "ORD20260310-000001"))
                .thenReturn(Optional.empty());

        sut.findMemberOrderDetail(1L, "  ORD20260310-000001  ");

        verify(orderRepository).findMemberOrderDetail(1L, "ORD20260310-000001");
    }

    // =========================================================
    // findReorderItems
    // =========================================================

    // ---- RO-01: 空文字の注文番号 → 空リスト ----
    @Test
    @DisplayName("空文字の注文番号では空リストを返す")
    void ro01_blankOrderNumber_returnsEmptyList() {
        assertThat(sut.findReorderItems(1L, "")).isEmpty();
    }

    // ---- RO-02: null の注文番号 → 空リスト ----
    @Test
    @DisplayName("null 注文番号では空リストを返す")
    void ro02_nullOrderNumber_returnsEmptyList() {
        assertThat(sut.findReorderItems(1L, null)).isEmpty();
    }

    // ---- RO-03: 有効な注文番号 → リポジトリの結果を返す ----
    @Test
    @DisplayName("有効な注文番号はtrimされてリポジトリに渡し結果を返す")
    void ro03_validOrderNumber_returnsRepositoryResult() {
        List<OrderReorderItem> items = List.of();
        when(orderRepository.findReorderItems(1L, "ORD20260310-000001")).thenReturn(items);

        List<OrderReorderItem> result = sut.findReorderItems(1L, "  ORD20260310-000001  ");

        verify(orderRepository).findReorderItems(1L, "ORD20260310-000001");
        assertThat(result).isSameAs(items);
    }
}
