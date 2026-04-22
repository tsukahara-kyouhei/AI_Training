package jp.co.skig.officeorder.service.member;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MemberService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MessageSource messageSource;

    private MemberService sut;

    @BeforeEach
    void setUp() {
        sut = new MemberService(memberRepository, passwordEncoder, messageSource);
        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("error message");
    }

    // =========================================================
    // findActiveCredentialByEmail
    // =========================================================

    // ---- FC-01: 空文字 → empty ----
    @Test
    @DisplayName("空メールアドレスで findActiveCredentialByEmail は empty を返す")
    void fc01_blankEmail_returnsEmpty() {
        Optional<MemberCredential> result = sut.findActiveCredentialByEmail("");
        assertThat(result).isEmpty();
    }

    // ---- FC-02: null → empty ----
    @Test
    @DisplayName("null メールアドレスで findActiveCredentialByEmail は empty を返す")
    void fc02_nullEmail_returnsEmpty() {
        Optional<MemberCredential> result = sut.findActiveCredentialByEmail(null);
        assertThat(result).isEmpty();
    }

    // ---- FC-03: 有効なメールアドレス → リポジトリの結果を返す ----
    @Test
    @DisplayName("有効なメールアドレスでリポジトリの結果を返す")
    void fc03_validEmail_returnsRepositoryResult() {
        MemberCredential credential = new MemberCredential(1L, "test@example.com", "山田", "太郎", "hashed");
        when(memberRepository.findActiveCredentialByEmail("test@example.com"))
                .thenReturn(Optional.of(credential));

        Optional<MemberCredential> result = sut.findActiveCredentialByEmail("test@example.com");

        assertThat(result).contains(credential);
    }

    // ---- FC-04: メールアドレスの前後空白はtrimされる ----
    @Test
    @DisplayName("メールアドレスの前後空白はtrimされてリポジトリに渡される")
    void fc04_emailWithWhitespace_trimmedBeforeQuery() {
        when(memberRepository.findActiveCredentialByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        sut.findActiveCredentialByEmail("  test@example.com  ");

        verify(memberRepository).findActiveCredentialByEmail("test@example.com");
    }

    // =========================================================
    // existsByEmail
    // =========================================================

    // ---- EB-01: 空文字 → false ----
    @Test
    @DisplayName("空メールアドレスで existsByEmail は false を返す")
    void eb01_blankEmail_returnsFalse() {
        assertThat(sut.existsByEmail("")).isFalse();
    }

    // ---- EB-02: null → false ----
    @Test
    @DisplayName("null メールアドレスで existsByEmail は false を返す")
    void eb02_nullEmail_returnsFalse() {
        assertThat(sut.existsByEmail(null)).isFalse();
    }

    // ---- EB-03: 登録済みメールアドレス → true ----
    @Test
    @DisplayName("登録済みメールアドレスで existsByEmail は true を返す")
    void eb03_existingEmail_returnsTrue() {
        when(memberRepository.existsByEmail("exist@example.com")).thenReturn(true);
        assertThat(sut.existsByEmail("exist@example.com")).isTrue();
    }

    // ---- EB-04: 未登録メールアドレス → false ----
    @Test
    @DisplayName("未登録メールアドレスで existsByEmail は false を返す")
    void eb04_newEmail_returnsFalse() {
        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);
        assertThat(sut.existsByEmail("new@example.com")).isFalse();
    }

    // =========================================================
    // register
    // =========================================================

    // ---- RE-01: メール重複 → DuplicateEmailException ----
    @Test
    @DisplayName("登録済みメールアドレスで register は DuplicateEmailException をスローする")
    void re01_duplicateEmail_throwsDuplicateEmailException() {
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> sut.register(makeRegisterForm("dup@example.com")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // ---- RE-02: 正常登録 → パスワードハッシュ化してリポジトリに保存 ----
    @Test
    @DisplayName("正常な会員登録はパスワードをハッシュ化してリポジトリに保存する")
    void re02_validRegistration_encodesPasswordAndSavesToRepository() {
        MemberRegisterForm form = makeRegisterForm("new@example.com");
        MemberSessionUser sessionUser = new MemberSessionUser(1L, "new@example.com", "山田", "太郎");
        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(memberRepository.insertMember(any(), any())).thenReturn(1L);
        when(memberRepository.findActiveById(1L)).thenReturn(Optional.of(sessionUser));

        MemberSessionUser result = sut.register(form);

        assertThat(result).isEqualTo(sessionUser);
        verify(passwordEncoder).encode("password123");
        verify(memberRepository).insertMember(any(), any());
    }

    // ---- RE-03: DataIntegrityViolationException + メール重複 → DuplicateEmailException ----
    @Test
    @DisplayName("レース状態でのメール重複は DuplicateEmailException をスローする")
    void re03_raceDuplicateEmail_throwsDuplicateEmailException() {
        MemberRegisterForm form = makeRegisterForm("race@example.com");
        // 初回: false, 事後チェック: true
        when(memberRepository.existsByEmail("race@example.com")).thenReturn(false, true);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(memberRepository.insertMember(any(), any()))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // ---- RE-04: DataIntegrityViolationException + メール重複でない → 再スロー ----
    @Test
    @DisplayName("DataIntegrityViolationException でメール重複でない場合はそのまま再スローする")
    void re04_dataIntegrityOtherCause_rethrows() {
        MemberRegisterForm form = makeRegisterForm("other@example.com");
        when(memberRepository.existsByEmail("other@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("other constraint");
        when(memberRepository.insertMember(any(), any())).thenThrow(ex);
        when(memberRepository.existsByEmail("other@example.com")).thenReturn(false);

        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // =========================================================
    // isAddressLimitReached
    // =========================================================

    // ---- AL-01: 件数 < 20 → false ----
    @Test
    @DisplayName("追加お届け先が 19 件の場合は上限未達 (false)")
    void al01_below20_returnsFalse() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(19L);
        assertThat(sut.isAddressLimitReached(1L)).isFalse();
    }

    // ---- AL-02: 件数 = 20 → true ----
    @Test
    @DisplayName("追加お届け先が 20 件の場合は上限到達 (true)")
    void al02_exactly20_returnsTrue() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L);
        assertThat(sut.isAddressLimitReached(1L)).isTrue();
    }

    // ---- AL-03: 件数 > 20 → true ----
    @Test
    @DisplayName("追加お届け先が 21 件以上の場合は上限到達 (true)")
    void al03_above20_returnsTrue() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(21L);
        assertThat(sut.isAddressLimitReached(1L)).isTrue();
    }

    // =========================================================
    // createAdditionalAddress
    // =========================================================

    // ---- CA-01: 上限到達時 → AddressLimitExceededException ----
    @Test
    @DisplayName("追加お届け先が上限に達している場合は AddressLimitExceededException をスローする")
    void ca01_atLimit_throwsAddressLimitExceededException() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L);

        assertThatThrownBy(() -> sut.createAdditionalAddress(1L, makeAddressForm()))
                .isInstanceOf(AddressLimitExceededException.class);
    }

    // ---- CA-02: 上限未達 → リポジトリに登録される ----
    @Test
    @DisplayName("追加お届け先が上限未達の場合は正常に登録される")
    void ca02_belowLimit_insertsToRepository() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(5L);

        sut.createAdditionalAddress(1L, makeAddressForm());

        verify(memberRepository).insertAdditionalAddress(any(Long.class), any());
    }

    // =========================================================
    // toggleFavorite
    // =========================================================

    // ---- TF-01: お気に入り済み → 削除する ----
    @Test
    @DisplayName("お気に入り済み商品のトグルはお気に入りを削除する")
    void tf01_alreadyFavorite_deleteFavorite() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(true);

        sut.toggleFavorite(1L, 10L);

        verify(memberRepository).deleteFavorite(1L, 10L);
        verify(memberRepository, never()).insertFavorite(any(Long.class), any(Long.class));
    }

    // ---- TF-02: 未お気に入り・上限未達 → 追加する ----
    @Test
    @DisplayName("未お気に入りで上限未達の場合はお気に入りに追加する")
    void tf02_notFavorite_belowLimit_insertFavorite() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(50L);

        sut.toggleFavorite(1L, 10L);

        verify(memberRepository).insertFavorite(1L, 10L);
    }

    // ---- TF-03: 未お気に入り・上限到達 → FavoritesLimitExceededException ----
    @Test
    @DisplayName("未お気に入りで上限100件に達している場合は FavoritesLimitExceededException をスローする")
    void tf03_notFavorite_atLimit_throwsException() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(100L);

        assertThatThrownBy(() -> sut.toggleFavorite(1L, 10L))
                .isInstanceOf(FavoritesLimitExceededException.class);
    }

    // ---- TF-04: DataIntegrityViolationException + お気に入り済み → 正常終了 (レース条件) ----
    @Test
    @DisplayName("レース状態でのお気に入り重複は例外なく終了する")
    void tf04_raceConditionFavoriteExists_returnsNormally() {
        // 初回: false, 事後チェック: true
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false, true);
        when(memberRepository.countFavorites(1L)).thenReturn(1L);
        doThrow(new DataIntegrityViolationException("unique constraint"))
                .when(memberRepository).insertFavorite(1L, 10L);

        // 例外がスローされないことを確認
        sut.toggleFavorite(1L, 10L);
    }

    // ---- TF-05: DataIntegrityViolationException + お気に入りなし → 再スロー ----
    @Test
    @DisplayName("DataIntegrityViolationException でお気に入りが存在しない場合は再スローする")
    void tf05_raceConditionFavoriteNotExists_rethrows() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(1L);
        DataIntegrityViolationException ex = new DataIntegrityViolationException("other");
        doThrow(ex).when(memberRepository).insertFavorite(1L, 10L);
        // 事後チェックでも存在しないので thenReturn(false) のみで充分

        assertThatThrownBy(() -> sut.toggleFavorite(1L, 10L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // =========================================================
    // withdraw
    // =========================================================

    // ---- WD-01: 退会成功 → true を返す ----
    @Test
    @DisplayName("退会成功時は true を返す")
    void wd01_withdrawSuccess_returnsTrue() {
        when(memberRepository.withdrawMember(1L)).thenReturn(true);
        assertThat(sut.withdraw(1L)).isTrue();
    }

    // ---- WD-02: 退会対象なし → false を返す ----
    @Test
    @DisplayName("退会対象が見つからない場合は false を返す")
    void wd02_withdrawNotFound_returnsFalse() {
        when(memberRepository.withdrawMember(1L)).thenReturn(false);
        assertThat(sut.withdraw(1L)).isFalse();
    }

    // =========================================================
    // findFavorites
    // =========================================================

    // ---- FF-01: page < 1 のときは 1 に正規化される ----
    @Test
    @DisplayName("page が 0 以下の場合は 1 に正規化してリポジトリに渡す")
    void ff01_pageZero_normalizedToOne() {
        sut.findFavorites(1L, 0);
        verify(memberRepository).findFavorites(1L, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    // ---- FF-02: page = 2 → そのままリポジトリに渡す ----
    @Test
    @DisplayName("page が正の値の場合はそのままリポジトリに渡す")
    void ff02_validPage_passedThrough() {
        sut.findFavorites(1L, 2);
        verify(memberRepository).findFavorites(1L, 2, MemberService.MYPAGE_PAGE_SIZE);
    }

    // =========================================================
    // helpers
    // =========================================================

    private static MemberRegisterForm makeRegisterForm(String email) {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail(email);
        form.setGender("male");
        form.setAnniversaryDate(LocalDate.of(2000, 1, 1));
        form.setPassword("password123");
        form.setNewsletterOptIn(true);
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("丸の内1-1-1");
        form.setDeliveryFloor("1");
        form.setHasElevator(true);
        form.setDaytimePhone("0312345678");
        return form;
    }

    private static MemberAdditionalAddressForm makeAddressForm() {
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        form.setLastName("鈴木");
        form.setFirstName("花子");
        form.setLastNameKana("スズキ");
        form.setFirstNameKana("ハナコ");
        form.setPostalCodePart1("150");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("渋谷区");
        form.setAddressLine("道玄坂1-1-1");
        form.setDeliveryFloor("3");
        form.setHasElevator(true);
        return form;
    }
}
