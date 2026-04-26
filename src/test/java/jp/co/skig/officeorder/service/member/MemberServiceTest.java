package jp.co.skig.officeorder.service.member;

import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressPage;
import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.model.member.MemberFavoritePage;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MemberService} の単体テスト。
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    MemberService sut;

    void setUp() {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("business.member.duplicateEmail", java.util.Locale.JAPAN, "メールアドレスは既に登録されています。");
        messageSource.addMessage("business.member.addressLimitExceeded", java.util.Locale.JAPAN, "追加お届け先の上限件数に達しています。");
        messageSource.addMessage("business.member.favoriteLimitExceeded", java.util.Locale.JAPAN, "お気に入りの上限件数に達しています。");
        sut = new MemberService(memberRepository, passwordEncoder, messageSource);
    }

    @BeforeEach
    void init() {
        setUp();
    }

    // ── findActiveCredentialByEmail ──────────────────────────────────────

    @Test
    void 有効なメールアドレスで認証情報が返ること() {
        var credential = new MemberCredential(1L, "test@example.com", "山田", "太郎", "hash");
        when(memberRepository.findActiveCredentialByEmail("test@example.com"))
                .thenReturn(Optional.of(credential));

        var result = sut.findActiveCredentialByEmail("test@example.com");

        assertThat(result).contains(credential);
    }

    @Test
    void nullメールアドレスを渡すとemptyが返ること() {
        var result = sut.findActiveCredentialByEmail(null);
        assertThat(result).isEmpty();
        verify(memberRepository, never()).findActiveCredentialByEmail(any());
    }

    @Test
    void 空文字メールアドレスを渡すとemptyが返ること() {
        var result = sut.findActiveCredentialByEmail("  ");
        assertThat(result).isEmpty();
        verify(memberRepository, never()).findActiveCredentialByEmail(any());
    }

    @Test
    void メールアドレスの前後空白はトリムされてリポジトリに渡ること() {
        when(memberRepository.findActiveCredentialByEmail("test@example.com")).thenReturn(Optional.empty());

        sut.findActiveCredentialByEmail("  test@example.com  ");

        verify(memberRepository).findActiveCredentialByEmail("test@example.com");
    }

    // ── existsByEmail ────────────────────────────────────────────────────

    @Test
    void 登録済みメールアドレスでtrueが返ること() {
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThat(sut.existsByEmail("dup@example.com")).isTrue();
    }

    @Test
    void 未登録メールアドレスでfalseが返ること() {
        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);

        assertThat(sut.existsByEmail("new@example.com")).isFalse();
    }

    @Test
    void nullを渡すとfalseが返りリポジトリを呼ばないこと() {
        assertThat(sut.existsByEmail(null)).isFalse();
        verify(memberRepository, never()).existsByEmail(any());
    }

    @Test
    void 空文字を渡すとfalseが返りリポジトリを呼ばないこと() {
        assertThat(sut.existsByEmail("")).isFalse();
        verify(memberRepository, never()).existsByEmail(any());
    }

    // ── register ─────────────────────────────────────────────────────────

    @Test
    void 正常な会員登録でセッションユーザーが返ること() {
        MemberRegisterForm form = buildRegisterForm("new@example.com");
        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(memberRepository.insertMember(any(), eq("$2a$10$hashed"))).thenReturn(42L);
        var sessionUser = new MemberSessionUser(42L, "new@example.com", "山田", "太郎");
        when(memberRepository.findActiveById(42L)).thenReturn(Optional.of(sessionUser));

        var result = sut.register(form);

        assertThat(result.memberId()).isEqualTo(42L);
        assertThat(result.email()).isEqualTo("new@example.com");
        verify(passwordEncoder).encode(form.getPassword());
    }

    @Test
    void メールアドレス重複時にDuplicateEmailExceptionをスローすること() {
        MemberRegisterForm form = buildRegisterForm("dup@example.com");
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DuplicateEmailException.class);
        verify(memberRepository, never()).insertMember(any(), any());
    }

    @Test
    void INSERT時のDataIntegrityViolationかつメール重複でDuplicateEmailExceptionをスローすること() {
        MemberRegisterForm form = buildRegisterForm("race@example.com");
        when(memberRepository.existsByEmail("race@example.com"))
                .thenReturn(false)  // 事前チェックはOK
                .thenReturn(true);  // INSERT失敗後の再チェックで重複を検出
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(memberRepository.insertMember(any(), any()))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void INSERT時のDataIntegrityViolationかつメール重複なしは例外を再スローすること() {
        MemberRegisterForm form = buildRegisterForm("other@example.com");
        when(memberRepository.existsByEmail("other@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(memberRepository.insertMember(any(), any()))
                .thenThrow(new DataIntegrityViolationException("other violation"));

        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── findAdditionalAddresses ──────────────────────────────────────────

    @Test
    void 正常なページ番号でリポジトリが呼ばれること() {
        var page = new MemberAdditionalAddressPage(List.of(), 0L, 1, 20);
        when(memberRepository.findAdditionalAddresses(1L, 1, MemberService.MYPAGE_PAGE_SIZE)).thenReturn(page);

        var result = sut.findAdditionalAddresses(1L, 1);

        assertThat(result).isEqualTo(page);
    }

    @Test
    void ページ番号0以下は1に補正されてリポジトリに渡ること() {
        var page = new MemberAdditionalAddressPage(List.of(), 0L, 1, 20);
        when(memberRepository.findAdditionalAddresses(1L, 1, MemberService.MYPAGE_PAGE_SIZE)).thenReturn(page);

        sut.findAdditionalAddresses(1L, 0);

        verify(memberRepository).findAdditionalAddresses(1L, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    // ── isAddressLimitReached ────────────────────────────────────────────

    @Test
    void 追加お届け先が上限未満の場合にfalseが返ること() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(19L);

        assertThat(sut.isAddressLimitReached(1L)).isFalse();
    }

    @Test
    void 追加お届け先が上限に達した場合にtrueが返ること() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L);

        assertThat(sut.isAddressLimitReached(1L)).isTrue();
    }

    // ── createAdditionalAddress ──────────────────────────────────────────

    @Test
    void 上限超過時にAddressLimitExceededExceptionをスローすること() {
        when(memberRepository.countAdditionalAddresses(1L))
                .thenReturn(Long.valueOf(MemberService.ADDITIONAL_ADDRESS_LIMIT));

        assertThatThrownBy(() -> sut.createAdditionalAddress(1L,
                new jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm()))
                .isInstanceOf(AddressLimitExceededException.class);
        verify(memberRepository, never()).insertAdditionalAddress(any(Long.class), any());
    }

    // ── withdraw ─────────────────────────────────────────────────────────

    @Test
    void 退会処理が成功するとtrueが返ること() {
        when(memberRepository.withdrawMember(1L)).thenReturn(true);

        assertThat(sut.withdraw(1L)).isTrue();
    }

    @Test
    void 退会処理に該当会員がいない場合はfalseが返ること() {
        when(memberRepository.withdrawMember(99L)).thenReturn(false);

        assertThat(sut.withdraw(99L)).isFalse();
    }

    // ── toggleFavorite ───────────────────────────────────────────────────

    @Test
    void お気に入り未登録状態でtoggleするとinsertが呼ばれること() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(0L);

        sut.toggleFavorite(1L, 10L);

        verify(memberRepository).insertFavorite(1L, 10L);
        verify(memberRepository, never()).deleteFavorite(any(Long.class), any(Long.class));
    }

    @Test
    void お気に入り登録済み状態でtoggleするとdeleteが呼ばれること() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(true);

        sut.toggleFavorite(1L, 10L);

        verify(memberRepository).deleteFavorite(1L, 10L);
        verify(memberRepository, never()).insertFavorite(any(Long.class), any(Long.class));
    }

    @Test
    void お気に入り上限到達時にFavoritesLimitExceededExceptionをスローすること() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L))
                .thenReturn(Long.valueOf(MemberService.FAVORITES_LIMIT));

        assertThatThrownBy(() -> sut.toggleFavorite(1L, 10L))
                .isInstanceOf(FavoritesLimitExceededException.class);
        verify(memberRepository, never()).insertFavorite(any(Long.class), any(Long.class));
    }

    // ── removeFavorite ───────────────────────────────────────────────────

    @Test
    void removeFavoriteでdeleteが呼ばれること() {
        sut.removeFavorite(1L, 10L);

        verify(memberRepository).deleteFavorite(1L, 10L);
    }

    // ── findFavorites ─────────────────────────────────────────────────────

    @Test
    void findFavoritesでページ0以下は1に補正されること() {
        var favPage = new MemberFavoritePage(List.of(), 0L, 1, 20);
        when(memberRepository.findFavorites(1L, 1, MemberService.MYPAGE_PAGE_SIZE)).thenReturn(favPage);

        sut.findFavorites(1L, 0);

        verify(memberRepository).findFavorites(1L, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private MemberRegisterForm buildRegisterForm(String email) {
        var form = new MemberRegisterForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail(email);
        form.setGender("male");
        form.setAnniversaryDate(java.time.LocalDate.of(1990, 1, 1));
        form.setPassword("TestPass1!");
        form.setNewsletterOptIn(Boolean.TRUE);
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("千代田1-1");
        form.setDeliveryFloor("1");
        form.setHasElevator(Boolean.TRUE);
        form.setDaytimePhone("0312345678");
        return form;
    }
}
