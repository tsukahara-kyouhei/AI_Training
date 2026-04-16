package jp.co.skig.officeorder.service.member;

import java.util.Locale;
import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressPage;
import jp.co.skig.officeorder.model.member.MemberFavoritePage;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceTest {

    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private MessageSource messageSource;
    private MemberService sut;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("error");
        sut = new MemberService(memberRepository, passwordEncoder, messageSource);
    }

    // --- findActiveCredentialByEmail ---

    @Test
    @DisplayName("空メールアドレスのとき empty を返す（リポジトリは呼ばない）")
    void findActiveCredentialByEmail_blankEmail_returnsEmpty() {
        var result = sut.findActiveCredentialByEmail("   ");

        assertThat(result).isEmpty();
        verify(memberRepository, never()).findActiveCredentialByEmail(anyString());
    }

    @Test
    @DisplayName("有効なメールアドレスのときリポジトリに trim したメールを渡す")
    void findActiveCredentialByEmail_validEmail_delegatesToRepository() {
        when(memberRepository.findActiveCredentialByEmail("test@example.com")).thenReturn(Optional.empty());

        sut.findActiveCredentialByEmail("  test@example.com  ");

        verify(memberRepository).findActiveCredentialByEmail("test@example.com");
    }

    // --- existsByEmail ---

    @Test
    @DisplayName("空メールアドレスのとき false を返す")
    void existsByEmail_blankEmail_returnsFalse() {
        assertThat(sut.existsByEmail("")).isFalse();
        verify(memberRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("有効なメールアドレスのときリポジトリに委譲する")
    void existsByEmail_validEmail_delegatesToRepository() {
        when(memberRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThat(sut.existsByEmail("test@example.com")).isTrue();
    }

    // --- register ---

    @Test
    @DisplayName("メールアドレスが重複している場合は DuplicateEmailException をスローする（insert 前に検知）")
    void register_duplicateEmail_beforeInsert_throwsDuplicateEmailException() {
        MemberRegisterForm form = buildRegisterForm("dup@example.com");
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DuplicateEmailException.class);

        verify(memberRepository, never()).insertMember(any(), anyString());
    }

    @Test
    @DisplayName("正常登録時は MemberSessionUser を返す")
    void register_success_returnsMemberSessionUser() {
        MemberRegisterForm form = buildRegisterForm("new@example.com");
        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(memberRepository.insertMember(any(), eq("hashed"))).thenReturn(1L);
        MemberSessionUser sessionUser = new MemberSessionUser(1L, "new@example.com", "山田", "太郎");
        when(memberRepository.findActiveById(1L)).thenReturn(Optional.of(sessionUser));

        MemberSessionUser result = sut.register(form);

        assertThat(result.email()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("DataIntegrityViolation かつメール重複のとき DuplicateEmailException をスロー（楽観的ロック）")
    void register_dataIntegrityViolation_emailExists_throwsDuplicateEmailException() {
        MemberRegisterForm form = buildRegisterForm("race@example.com");
        when(memberRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(memberRepository.insertMember(any(), anyString())).thenThrow(new DataIntegrityViolationException("dup"));
        when(memberRepository.existsByEmail("race@example.com")).thenReturn(true);

        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // --- findAdditionalAddresses ---

    @Test
    @DisplayName("page が 0 以下のとき 1 に正規化してリポジトリに渡す")
    void findAdditionalAddresses_pageZero_normalizesToOne() {
        when(memberRepository.findAdditionalAddresses(1L, 1, MemberService.MYPAGE_PAGE_SIZE))
                .thenReturn(mock(MemberAdditionalAddressPage.class));

        sut.findAdditionalAddresses(1L, 0);

        verify(memberRepository).findAdditionalAddresses(1L, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    @Test
    @DisplayName("page が負のとき 1 に正規化してリポジトリに渡す")
    void findAdditionalAddresses_pageNegative_normalizesToOne() {
        when(memberRepository.findAdditionalAddresses(1L, 1, MemberService.MYPAGE_PAGE_SIZE))
                .thenReturn(mock(MemberAdditionalAddressPage.class));

        sut.findAdditionalAddresses(1L, -5);

        verify(memberRepository).findAdditionalAddresses(1L, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    // --- isAddressLimitReached ---

    @Test
    @DisplayName("追加お届け先件数が 20 未満のとき false を返す")
    void isAddressLimitReached_countBelow20_returnsFalse() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(19L);

        assertThat(sut.isAddressLimitReached(1L)).isFalse();
    }

    @Test
    @DisplayName("追加お届け先件数がちょうど 20 のとき true を返す")
    void isAddressLimitReached_countEquals20_returnsTrue() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L);

        assertThat(sut.isAddressLimitReached(1L)).isTrue();
    }

    // --- createAdditionalAddress ---

    @Test
    @DisplayName("上限到達時は AddressLimitExceededException をスローする")
    void createAdditionalAddress_limitReached_throwsAddressLimitExceededException() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L);

        assertThatThrownBy(() -> sut.createAdditionalAddress(1L, mock(MemberAdditionalAddressForm.class)))
                .isInstanceOf(AddressLimitExceededException.class);

        verify(memberRepository, never()).insertAdditionalAddress(eq(1L), any());
    }

    @Test
    @DisplayName("上限未満のとき正規化したフォームを挿入する")
    void createAdditionalAddress_belowLimit_insertsAddress() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(5L);
        MemberAdditionalAddressForm form = mock(MemberAdditionalAddressForm.class);
        MemberAdditionalAddressForm normalized = mock(MemberAdditionalAddressForm.class);
        when(form.normalize()).thenReturn(normalized);

        sut.createAdditionalAddress(1L, form);

        verify(memberRepository).insertAdditionalAddress(1L, normalized);
    }

    // --- updateProfile ---

    @Test
    @DisplayName("他会員とメールが重複した場合は DuplicateEmailException をスローする")
    void updateProfile_duplicateEmail_throwsDuplicateEmailException() {
        MemberProfileEditForm form = mock(MemberProfileEditForm.class);
        MemberProfileEditForm normalized = mock(MemberProfileEditForm.class);
        when(form.normalize()).thenReturn(normalized);
        when(normalized.getEmail()).thenReturn("dup@example.com");
        when(memberRepository.existsByEmailForOtherMember("dup@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> sut.updateProfile(1L, form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("対象会員が見つからない場合は IllegalStateException をスローする")
    void updateProfile_memberNotFound_throwsIllegalStateException() {
        MemberProfileEditForm form = mock(MemberProfileEditForm.class);
        MemberProfileEditForm normalized = mock(MemberProfileEditForm.class);
        when(form.normalize()).thenReturn(normalized);
        when(normalized.getEmail()).thenReturn("ok@example.com");
        when(memberRepository.existsByEmailForOtherMember("ok@example.com", 1L)).thenReturn(false);
        when(memberRepository.updateProfile(1L, normalized)).thenReturn(false);

        assertThatThrownBy(() -> sut.updateProfile(1L, form))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- withdraw ---

    @Test
    @DisplayName("退会成功のとき true を返す")
    void withdraw_success_returnsTrue() {
        when(memberRepository.withdrawMember(1L)).thenReturn(true);

        assertThat(sut.withdraw(1L)).isTrue();
    }

    @Test
    @DisplayName("退会対象が見つからないとき false を返す")
    void withdraw_notFound_returnsFalse() {
        when(memberRepository.withdrawMember(1L)).thenReturn(false);

        assertThat(sut.withdraw(1L)).isFalse();
    }

    // --- findFavorites ---

    @Test
    @DisplayName("page が 0 のとき 1 に正規化してリポジトリに渡す")
    void findFavorites_pageZero_normalizesToOne() {
        when(memberRepository.findFavorites(1L, 1, MemberService.MYPAGE_PAGE_SIZE))
                .thenReturn(mock(MemberFavoritePage.class));

        sut.findFavorites(1L, 0);

        verify(memberRepository).findFavorites(1L, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    // --- toggleFavorite ---

    @Test
    @DisplayName("既にお気に入りの場合は削除する")
    void toggleFavorite_alreadyFavorite_removes() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(true);

        sut.toggleFavorite(1L, 10L);

        verify(memberRepository).deleteFavorite(1L, 10L);
        verify(memberRepository, never()).insertFavorite(eq(1L), eq(10L));
    }

    @Test
    @DisplayName("お気に入り件数が上限に達している場合は FavoritesLimitExceededException をスローする")
    void toggleFavorite_limitReached_throwsFavoritesLimitExceededException() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn((long) MemberService.FAVORITES_LIMIT);

        assertThatThrownBy(() -> sut.toggleFavorite(1L, 10L))
                .isInstanceOf(FavoritesLimitExceededException.class);

        verify(memberRepository, never()).insertFavorite(eq(1L), eq(10L));
    }

    @Test
    @DisplayName("未お気に入りかつ上限未満のとき追加する")
    void toggleFavorite_notFavorite_insertsNewEntry() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(5L);

        sut.toggleFavorite(1L, 10L);

        verify(memberRepository).insertFavorite(1L, 10L);
    }

    // --- ヘルパ ---

    private MemberRegisterForm buildRegisterForm(String email) {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail(email);
        form.setPassword("Password1!");
        return form;
    }
}
