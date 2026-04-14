package jp.co.skig.officeorder.service.member;

import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressView;
import jp.co.skig.officeorder.model.member.MemberFavoritePage;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberType;
import org.springframework.dao.DataIntegrityViolationException;
import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    }

    // ─── findActiveCredentialByEmail ────────────────────────────────────

    @Test
    void findActiveCredentialByEmail_blank_email_returns_empty() {
        // Act
        Optional<MemberCredential> result = sut.findActiveCredentialByEmail("  ");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveCredentialByEmail_null_email_returns_empty() {
        // Act
        Optional<MemberCredential> result = sut.findActiveCredentialByEmail(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveCredentialByEmail_valid_email_trims_and_delegates_to_repository() {
        // Arrange
        MemberCredential credential = new MemberCredential(1L, "test@example.com", "山田", "太郎", "hash");
        when(memberRepository.findActiveCredentialByEmail("test@example.com"))
                .thenReturn(Optional.of(credential));

        // Act
        Optional<MemberCredential> result = sut.findActiveCredentialByEmail("  test@example.com  ");

        // Assert
        assertThat(result).contains(credential);
        verify(memberRepository).findActiveCredentialByEmail("test@example.com");
    }

    // ─── existsByEmail ───────────────────────────────────────────────────

    @Test
    void existsByEmail_blank_email_returns_false_without_calling_repository() {
        // Act
        boolean result = sut.existsByEmail("   ");

        // Assert
        assertThat(result).isFalse();
        verify(memberRepository, never()).existsByEmail(anyString());
    }

    @Test
    void existsByEmail_null_email_returns_false_without_calling_repository() {
        // Act
        boolean result = sut.existsByEmail(null);

        // Assert
        assertThat(result).isFalse();
        verify(memberRepository, never()).existsByEmail(anyString());
    }

    @Test
    void existsByEmail_registered_email_returns_true() {
        // Arrange
        when(memberRepository.existsByEmail("used@example.com")).thenReturn(true);

        // Act
        boolean result = sut.existsByEmail("used@example.com");

        // Assert
        assertThat(result).isTrue();
    }

    // ─── register ────────────────────────────────────────────────────────

    @Test
    void register_duplicate_email_throws_duplicate_email_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("重複エラー");
        MemberRegisterForm form = buildRegisterForm("dup@example.com");
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_valid_form_inserts_member_and_returns_session_user() {
        // Arrange
        MemberRegisterForm form = buildRegisterForm("new@example.com");
        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed");
        when(memberRepository.insertMember(any(), eq("hashed"))).thenReturn(99L);
        MemberSessionUser sessionUser = new MemberSessionUser(99L, "new@example.com", "山田", "太郎");
        when(memberRepository.findActiveById(99L)).thenReturn(Optional.of(sessionUser));

        // Act
        MemberSessionUser result = sut.register(form);

        // Assert
        assertThat(result.memberId()).isEqualTo(99L);
        assertThat(result.email()).isEqualTo("new@example.com");
    }

    // ─── withdraw ────────────────────────────────────────────────────────

    @Test
    void withdraw_successfully_updated_returns_true() {
        // Arrange
        when(memberRepository.withdrawMember(1L)).thenReturn(true);

        // Act
        boolean result = sut.withdraw(1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void withdraw_member_not_found_returns_false() {
        // Arrange
        when(memberRepository.withdrawMember(999L)).thenReturn(false);

        // Act
        boolean result = sut.withdraw(999L);

        // Assert
        assertThat(result).isFalse();
    }

    // ─── isAddressLimitReached ───────────────────────────────────────────

    @Test
    void isAddressLimitReached_count_below_limit_returns_false() {
        // Arrange
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(19L);

        // Act
        boolean result = sut.isAddressLimitReached(1L);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isAddressLimitReached_count_at_limit_returns_true() {
        // Arrange
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L);

        // Act
        boolean result = sut.isAddressLimitReached(1L);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isAddressLimitReached_count_above_limit_returns_true() {
        // Arrange
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(21L);

        // Act
        boolean result = sut.isAddressLimitReached(1L);

        // Assert
        assertThat(result).isTrue();
    }

    // ─── createAdditionalAddress ─────────────────────────────────────────

    @Test
    void createAdditionalAddress_at_limit_throws_address_limit_exceeded_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("上限エラー");
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L);

        // Act / Assert
        assertThatThrownBy(() -> sut.createAdditionalAddress(1L, new MemberAdditionalAddressForm()))
                .isInstanceOf(AddressLimitExceededException.class);
    }

    @Test
    void createAdditionalAddress_below_limit_calls_repository_insert() {
        // Arrange
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(5L);
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();

        // Act
        sut.createAdditionalAddress(1L, form);

        // Assert
        verify(memberRepository).insertAdditionalAddress(eq(1L), any());
    }

    // ─── toggleFavorite ──────────────────────────────────────────────────

    @Test
    void toggleFavorite_already_favorited_calls_delete() {
        // Arrange
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(true);

        // Act
        sut.toggleFavorite(1L, 10L);

        // Assert
        verify(memberRepository).deleteFavorite(1L, 10L);
        verify(memberRepository, never()).insertFavorite(anyLong(), anyLong());
    }

    @Test
    void toggleFavorite_not_favorited_and_within_limit_calls_insert() {
        // Arrange
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(5L);

        // Act
        sut.toggleFavorite(1L, 10L);

        // Assert
        verify(memberRepository).insertFavorite(1L, 10L);
    }

    @Test
    void toggleFavorite_not_favorited_and_at_limit_throws_favorites_limit_exceeded_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("お気に入り上限");
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(100L);

        // Act / Assert
        assertThatThrownBy(() -> sut.toggleFavorite(1L, 10L))
                .isInstanceOf(FavoritesLimitExceededException.class);
    }

    // ─── findAdditionalAddresses ─────────────────────────────────────────

    @Test
    void findAdditionalAddresses_page_zero_is_normalized_to_1() {
        // Arrange / Act
        sut.findAdditionalAddresses(1L, 0);

        // Assert - page=0 は 1 に補正される
        verify(memberRepository).findAdditionalAddresses(eq(1L), eq(1), eq(MemberService.MYPAGE_PAGE_SIZE));
    }

    @Test
    void findAdditionalAddresses_negative_page_is_normalized_to_1() {
        // Arrange / Act
        sut.findAdditionalAddresses(1L, -5);

        // Assert
        verify(memberRepository).findAdditionalAddresses(eq(1L), eq(1), eq(MemberService.MYPAGE_PAGE_SIZE));
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private MemberRegisterForm buildRegisterForm(String email) {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail(email);
        form.setPassword("Password1!");
        form.setGender("male");
        return form;
    }

    private MemberProfileEditForm buildProfileForm(String email) {
        MemberProfileEditForm form = new MemberProfileEditForm();
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail(email);
        form.setGender("male");
        return form;
    }

    // ─── findActiveById ──────────────────────────────────────────────────

    @Test
    void findActiveById_delegates_to_repository() {
        // Arrange
        MemberSessionUser user = new MemberSessionUser(1L, "a@b.com", "山田", "太郎");
        when(memberRepository.findActiveById(1L)).thenReturn(Optional.of(user));

        // Act / Assert
        assertThat(sut.findActiveById(1L)).contains(user);
    }

    // ─── findAdditionalAddressById ───────────────────────────────────────

    @Test
    void findAdditionalAddressById_delegates_to_repository() {
        // Arrange
        when(memberRepository.findAdditionalAddressById(1L, 2L)).thenReturn(Optional.empty());

        // Act / Assert
        assertThat(sut.findAdditionalAddressById(1L, 2L)).isEmpty();
        verify(memberRepository).findAdditionalAddressById(1L, 2L);
    }

    // ─── findMemberTypeById ──────────────────────────────────────────────

    @Test
    void findMemberTypeById_delegates_to_repository() {
        // Arrange
        when(memberRepository.findMemberTypeById(1L)).thenReturn(MemberType.PERSONAL);

        // Act / Assert
        assertThat(sut.findMemberTypeById(1L)).isEqualTo(MemberType.PERSONAL);
    }

    // ─── findProfileByMemberId ───────────────────────────────────────────

    @Test
    void findProfileByMemberId_delegates_to_repository() {
        // Arrange
        when(memberRepository.findProfileByMemberId(1L)).thenReturn(Optional.empty());

        // Act / Assert
        assertThat(sut.findProfileByMemberId(1L)).isEmpty();
        verify(memberRepository).findProfileByMemberId(1L);
    }

    // ─── updateProfile ────────────────────────────────────────────────────

    @Test
    void updateProfile_success_updates_and_logs() {
        // Arrange
        MemberProfileEditForm form = buildProfileForm("new@example.com");
        when(memberRepository.existsByEmailForOtherMember("new@example.com", 1L)).thenReturn(false);
        when(memberRepository.updateProfile(eq(1L), any())).thenReturn(true);

        // Act (no exception)
        sut.updateProfile(1L, form);

        // Assert
        verify(memberRepository).updateProfile(eq(1L), any());
    }

    @Test
    void updateProfile_duplicate_email_throws_duplicate_email_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("重複エラー");
        MemberProfileEditForm form = buildProfileForm("dup@example.com");
        when(memberRepository.existsByEmailForOtherMember("dup@example.com", 1L)).thenReturn(true);

        // Act / Assert
        assertThatThrownBy(() -> sut.updateProfile(1L, form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateProfile_race_condition_duplicate_email_throws_duplicate_email_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("重複エラー");
        MemberProfileEditForm form = buildProfileForm("race@example.com");
        when(memberRepository.existsByEmailForOtherMember("race@example.com", 1L))
                .thenReturn(false)   // 1回目(事前チェック): 重複なし
                .thenReturn(true);   // 2回目(競合後チェック): 重複あり
        when(memberRepository.updateProfile(eq(1L), any())).thenThrow(new DataIntegrityViolationException("race"));

        // Act / Assert
        assertThatThrownBy(() -> sut.updateProfile(1L, form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateProfile_member_not_updated_throws_illegal_state() {
        // Arrange
        MemberProfileEditForm form = buildProfileForm("ok@example.com");
        when(memberRepository.existsByEmailForOtherMember("ok@example.com", 1L)).thenReturn(false);
        when(memberRepository.updateProfile(eq(1L), any())).thenReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> sut.updateProfile(1L, form))
                .isInstanceOf(IllegalStateException.class);
    }

    // ─── updateAdditionalAddress ─────────────────────────────────────────

    @Test
    void updateAdditionalAddress_success_returns_true() {
        // Arrange
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        when(memberRepository.updateAdditionalAddress(eq(1L), eq(2L), any())).thenReturn(true);

        // Act
        boolean result = sut.updateAdditionalAddress(1L, 2L, form);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void updateAdditionalAddress_not_found_returns_false() {
        // Arrange
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        when(memberRepository.updateAdditionalAddress(eq(1L), eq(2L), any())).thenReturn(false);

        // Act
        boolean result = sut.updateAdditionalAddress(1L, 2L, form);

        // Assert
        assertThat(result).isFalse();
    }

    // ─── deleteAdditionalAddress ─────────────────────────────────────────

    @Test
    void deleteAdditionalAddress_delegates_to_repository() {
        // Act
        sut.deleteAdditionalAddress(1L, 2L);

        // Assert
        verify(memberRepository).deleteAdditionalAddress(1L, 2L);
    }

    // ─── findFavorites ────────────────────────────────────────────────────

    @Test
    void findFavorites_delegates_to_repository_with_normalized_page() {
        // Act
        sut.findFavorites(1L, 1);

        // Assert
        verify(memberRepository).findFavorites(eq(1L), eq(1), eq(MemberService.MYPAGE_PAGE_SIZE));
    }

    // ─── isFavorite ───────────────────────────────────────────────────────

    @Test
    void isFavorite_delegates_to_repository() {
        // Arrange
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(true);

        // Act / Assert
        assertThat(sut.isFavorite(1L, 10L)).isTrue();
    }

    // ─── removeFavorite ───────────────────────────────────────────────────

    @Test
    void removeFavorite_delegates_to_repository() {
        // Act
        sut.removeFavorite(1L, 10L);

        // Assert
        verify(memberRepository).deleteFavorite(1L, 10L);
    }

    // ─── register (race condition) ───────────────────────────────────────

    @Test
    void register_race_condition_duplicate_email_throws_duplicate_email_exception() {
        // Arrange
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("重複エラー");
        MemberRegisterForm form = buildRegisterForm("race@example.com");
        when(memberRepository.existsByEmail("race@example.com")).thenReturn(false)
                .thenReturn(true); // race: first call false, retry true
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(memberRepository.insertMember(any(), anyString()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        // Act / Assert
        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // ─── toggleFavorite (insert race condition) ──────────────────────────

    @Test
    void toggleFavorite_insert_race_condition_already_exists_does_nothing() {
        // Arrange
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(5L);
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false).thenReturn(true);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("race"))
                .when(memberRepository).insertFavorite(1L, 10L);

        // Act (no exception)
        sut.toggleFavorite(1L, 10L);
    }

    @Test
    void toggleFavorite_data_integrity_violation_not_duplicate_rethrows_exception() {
        // レース以外の DataIntegrityViolationException は再スローされる
        // Arrange
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(5L);
        // insertFavorite 後の existsFavorite (race check) は false → 重複でないのに例外が発生
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false).thenReturn(false);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("other constraint"))
                .when(memberRepository).insertFavorite(1L, 10L);

        // Act / Assert – 再スロー
        assertThatThrownBy(() -> sut.toggleFavorite(1L, 10L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updateProfile_data_integrity_violation_not_duplicate_rethrows_exception() {
        // updateProfile の DataIntegrityViolationException が重複メール以外なら再スロー
        // Arrange
        MemberProfileEditForm form = buildProfileForm("ok2@example.com");
        when(memberRepository.existsByEmailForOtherMember("ok2@example.com", 1L))
                .thenReturn(false) // 1回目（事前チェック）
                .thenReturn(false); // 2回目（競合後チェック）: false → 重複なし → rethrow
        when(memberRepository.updateProfile(eq(1L), any()))
                .thenThrow(new DataIntegrityViolationException("other constraint"));

        // Act / Assert
        assertThatThrownBy(() -> sut.updateProfile(1L, form))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void register_data_integrity_violation_not_duplicate_rethrows_exception() {
        // register の DataIntegrityViolationException が重複メール以外なら再スロー
        // Arrange
        MemberRegisterForm form = buildRegisterForm("other@example.com");
        when(memberRepository.existsByEmail("other@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(memberRepository.insertMember(any(), anyString()))
                .thenThrow(new DataIntegrityViolationException("other constraint"));
        // race check: existsByEmail → false
        when(memberRepository.existsByEmail("other@example.com"))
                .thenReturn(false).thenReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> sut.register(form))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
