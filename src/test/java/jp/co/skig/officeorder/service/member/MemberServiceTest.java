package jp.co.skig.officeorder.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.member.MemberType;
import jp.co.skig.officeorder.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    MessageSource messageSource;

    MemberService memberService;

    @BeforeEach
    void setUp() {
        lenient().when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        memberService = new MemberService(memberRepository, passwordEncoder, messageSource);
    }

    // --- findActiveCredentialByEmail ---

    @Test
    void findActiveCredentialByEmail_blankInput_returnsEmpty() {
        Optional<MemberCredential> result = memberService.findActiveCredentialByEmail("   ");
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveCredentialByEmail_validEmail_delegatesToRepository() {
        MemberCredential credential = new MemberCredential(1L, "taro@example.com", "山田", "太郎", "hash");
        when(memberRepository.findActiveCredentialByEmail("taro@example.com"))
                .thenReturn(Optional.of(credential));

        Optional<MemberCredential> result = memberService.findActiveCredentialByEmail("taro@example.com");

        assertThat(result).contains(credential);
    }

    // --- existsByEmail ---

    @Test
    void existsByEmail_blankInput_returnsFalse() {
        assertThat(memberService.existsByEmail("")).isFalse();
        assertThat(memberService.existsByEmail(null)).isFalse();
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        when(memberRepository.existsByEmail("taro@example.com")).thenReturn(true);

        assertThat(memberService.existsByEmail("taro@example.com")).isTrue();
    }

    // --- register ---

    @Test
    void register_newEmail_registersAndReturnsMemberSessionUser() {
        MemberRegisterForm form = buildRegisterForm("taro@example.com", "Password1!");
        MemberSessionUser sessionUser = new MemberSessionUser(1L, "taro@example.com", "山田", "太郎");
        when(memberRepository.existsByEmail("taro@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashedPw");
        when(memberRepository.insertMember(any(), eq("hashedPw"))).thenReturn(1L);
        when(memberRepository.findActiveById(1L)).thenReturn(Optional.of(sessionUser));

        MemberSessionUser result = memberService.register(form);

        assertThat(result).isEqualTo(sessionUser);
    }

    @Test
    void register_duplicateEmail_throwsDuplicateEmailException() {
        MemberRegisterForm form = buildRegisterForm("dup@example.com", "Password1!");
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.register(form))
                .isInstanceOf(DuplicateEmailException.class);

        verify(memberRepository, never()).insertMember(any(), any());
    }

    @Test
    void register_raceConditionDuplicate_throwsDuplicateEmailException() {
        MemberRegisterForm form = buildRegisterForm("race@example.com", "Password1!");
        when(memberRepository.existsByEmail("race@example.com")).thenReturn(false, true);
        when(passwordEncoder.encode(any())).thenReturn("hashedPw");
        doThrow(DataIntegrityViolationException.class).when(memberRepository).insertMember(any(), any());

        assertThatThrownBy(() -> memberService.register(form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void register_raceConditionNonDuplicate_rethrowsDataIntegrityViolation() {
        MemberRegisterForm form = buildRegisterForm("other@example.com", "Password1!");
        when(memberRepository.existsByEmail("other@example.com")).thenReturn(false, false);
        when(passwordEncoder.encode(any())).thenReturn("hashedPw");
        doThrow(DataIntegrityViolationException.class).when(memberRepository).insertMember(any(), any());

        assertThatThrownBy(() -> memberService.register(form))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- updateProfile ---

    @Test
    void updateProfile_success_updatesProfile() {
        MemberProfileEditForm form = buildProfileEditForm("updated@example.com");
        when(memberRepository.existsByEmailForOtherMember("updated@example.com", 1L)).thenReturn(false);
        when(memberRepository.updateProfile(eq(1L), any())).thenReturn(true);

        memberService.updateProfile(1L, form);

        verify(memberRepository).updateProfile(eq(1L), any());
    }

    @Test
    void updateProfile_duplicateEmail_throwsDuplicateEmailException() {
        MemberProfileEditForm form = buildProfileEditForm("dup@example.com");
        when(memberRepository.existsByEmailForOtherMember("dup@example.com", 2L)).thenReturn(true);

        assertThatThrownBy(() -> memberService.updateProfile(2L, form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateProfile_memberNotFound_throwsIllegalStateException() {
        MemberProfileEditForm form = buildProfileEditForm("ghost@example.com");
        when(memberRepository.existsByEmailForOtherMember("ghost@example.com", 99L)).thenReturn(false);
        when(memberRepository.updateProfile(eq(99L), any())).thenReturn(false);

        assertThatThrownBy(() -> memberService.updateProfile(99L, form))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateProfile_raceConditionDuplicate_throwsDuplicateEmailException() {
        MemberProfileEditForm form = buildProfileEditForm("race@example.com");
        when(memberRepository.existsByEmailForOtherMember("race@example.com", 1L)).thenReturn(false, true);
        doThrow(DataIntegrityViolationException.class).when(memberRepository).updateProfile(eq(1L), any());

        assertThatThrownBy(() -> memberService.updateProfile(1L, form))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateProfile_raceConditionNonDuplicate_rethrowsDataIntegrityViolation() {
        MemberProfileEditForm form = buildProfileEditForm("other@example.com");
        when(memberRepository.existsByEmailForOtherMember("other@example.com", 1L)).thenReturn(false, false);
        doThrow(DataIntegrityViolationException.class).when(memberRepository).updateProfile(eq(1L), any());

        assertThatThrownBy(() -> memberService.updateProfile(1L, form))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- withdraw ---

    @Test
    void withdraw_success_returnsTrue() {
        when(memberRepository.withdrawMember(1L)).thenReturn(true);

        boolean result = memberService.withdraw(1L);

        assertThat(result).isTrue();
    }

    @Test
    void withdraw_alreadyWithdrawn_returnsFalse() {
        when(memberRepository.withdrawMember(99L)).thenReturn(false);

        boolean result = memberService.withdraw(99L);

        assertThat(result).isFalse();
    }

    // --- isAddressLimitReached ---

    @Test
    void isAddressLimitReached_underLimit_returnsFalse() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(19L);

        assertThat(memberService.isAddressLimitReached(1L)).isFalse();
    }

    @Test
    void isAddressLimitReached_atLimit_returnsTrue() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn((long) MemberService.ADDITIONAL_ADDRESS_LIMIT);

        assertThat(memberService.isAddressLimitReached(1L)).isTrue();
    }

    // --- createAdditionalAddress ---

    @Test
    void createAdditionalAddress_underLimit_insertsAddress() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(5L);
        MemberAdditionalAddressForm form = buildAddressForm();

        memberService.createAdditionalAddress(1L, form);

        verify(memberRepository).insertAdditionalAddress(eq(1L), any());
    }

    @Test
    void createAdditionalAddress_atLimit_throwsAddressLimitExceededException() {
        when(memberRepository.countAdditionalAddresses(1L))
                .thenReturn((long) MemberService.ADDITIONAL_ADDRESS_LIMIT);
        MemberAdditionalAddressForm form = buildAddressForm();

        assertThatThrownBy(() -> memberService.createAdditionalAddress(1L, form))
                .isInstanceOf(AddressLimitExceededException.class);

        verify(memberRepository, never()).insertAdditionalAddress(anyLong(), any());
    }

    // --- updateAdditionalAddress ---

    @Test
    void updateAdditionalAddress_success_returnsTrue() {
        MemberAdditionalAddressForm form = buildAddressForm();
        when(memberRepository.updateAdditionalAddress(eq(1L), eq(2L), any())).thenReturn(true);

        boolean result = memberService.updateAdditionalAddress(1L, 2L, form);

        assertThat(result).isTrue();
    }

    @Test
    void updateAdditionalAddress_notFound_returnsFalse() {
        MemberAdditionalAddressForm form = buildAddressForm();
        when(memberRepository.updateAdditionalAddress(eq(1L), eq(2L), any())).thenReturn(false);

        boolean result = memberService.updateAdditionalAddress(1L, 2L, form);

        assertThat(result).isFalse();
    }

    // --- deleteAdditionalAddress ---

    @Test
    void deleteAdditionalAddress_delegatesToRepository() {
        memberService.deleteAdditionalAddress(1L, 2L);

        verify(memberRepository).deleteAdditionalAddress(1L, 2L);
    }

    // --- toggleFavorite ---

    @Test
    void toggleFavorite_alreadyFavorited_removesFavorite() {
        when(memberRepository.existsFavorite(1L, 101L)).thenReturn(true);

        memberService.toggleFavorite(1L, 101L);

        verify(memberRepository).deleteFavorite(1L, 101L);
        verify(memberRepository, never()).insertFavorite(anyLong(), anyLong());
    }

    @Test
    void toggleFavorite_notFavoritedUnderLimit_addsFavorite() {
        when(memberRepository.existsFavorite(1L, 101L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(0L);

        memberService.toggleFavorite(1L, 101L);

        verify(memberRepository).insertFavorite(1L, 101L);
    }

    @Test
    void toggleFavorite_notFavoritedAtLimit_throwsFavoritesLimitExceededException() {
        when(memberRepository.existsFavorite(1L, 101L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn((long) MemberService.FAVORITES_LIMIT);

        assertThatThrownBy(() -> memberService.toggleFavorite(1L, 101L))
                .isInstanceOf(FavoritesLimitExceededException.class);

        verify(memberRepository, never()).insertFavorite(anyLong(), anyLong());
    }

    @Test
    void toggleFavorite_raceConditionAlreadyExists_swallowsDuplicateException() {
        when(memberRepository.existsFavorite(1L, 101L)).thenReturn(false, true);
        when(memberRepository.countFavorites(1L)).thenReturn(0L);
        doThrow(DataIntegrityViolationException.class).when(memberRepository).insertFavorite(1L, 101L);

        // 例外が発生しないこと（レースコンディションを吸収する）
        memberService.toggleFavorite(1L, 101L);
    }

    @Test
    void toggleFavorite_dataIntegrityViolationNotDuplicate_rethrowsException() {
        when(memberRepository.existsFavorite(1L, 101L)).thenReturn(false, false);
        when(memberRepository.countFavorites(1L)).thenReturn(0L);
        doThrow(DataIntegrityViolationException.class).when(memberRepository).insertFavorite(1L, 101L);

        assertThatThrownBy(() -> memberService.toggleFavorite(1L, 101L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // --- removeFavorite ---

    @Test
    void removeFavorite_delegatesToRepository() {
        memberService.removeFavorite(1L, 101L);

        verify(memberRepository).deleteFavorite(1L, 101L);
    }

    // --- isFavorite ---

    @Test
    void isFavorite_favorited_returnsTrue() {
        when(memberRepository.existsFavorite(1L, 101L)).thenReturn(true);

        assertThat(memberService.isFavorite(1L, 101L)).isTrue();
    }

    @Test
    void isFavorite_notFavorited_returnsFalse() {
        when(memberRepository.existsFavorite(1L, 101L)).thenReturn(false);

        assertThat(memberService.isFavorite(1L, 101L)).isFalse();
    }

    // --- findFavorites (page normalization) ---

    @Test
    void findFavorites_zeroPage_normalizesToPageOne() {
        memberService.findFavorites(1L, 0);

        verify(memberRepository).findFavorites(1L, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    @Test
    void findFavorites_negativePage_normalizesToPageOne() {
        memberService.findFavorites(1L, -5);

        verify(memberRepository).findFavorites(1L, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    @Test
    void findFavorites_validPage_keepsPage() {
        memberService.findFavorites(1L, 3);

        verify(memberRepository).findFavorites(1L, 3, MemberService.MYPAGE_PAGE_SIZE);
    }

    // --- findActiveById ---

    @Test
    void findActiveById_delegatesToRepository() {
        MemberSessionUser sessionUser = new MemberSessionUser(1L, "taro@example.com", "山田", "太郎");
        when(memberRepository.findActiveById(1L)).thenReturn(Optional.of(sessionUser));

        Optional<MemberSessionUser> result = memberService.findActiveById(1L);

        assertThat(result).contains(sessionUser);
        verify(memberRepository).findActiveById(1L);
    }

    // --- findAdditionalAddresses ---

    @Test
    void findAdditionalAddresses_zeroPage_normalizesToOne() {
        memberService.findAdditionalAddresses(1L, 0);

        verify(memberRepository).findAdditionalAddresses(1L, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    @Test
    void findAdditionalAddresses_validPage_keepsPage() {
        memberService.findAdditionalAddresses(1L, 2);

        verify(memberRepository).findAdditionalAddresses(1L, 2, MemberService.MYPAGE_PAGE_SIZE);
    }

    // --- findAdditionalAddressById ---

    @Test
    void findAdditionalAddressById_delegatesToRepository() {
        when(memberRepository.findAdditionalAddressById(1L, 2L)).thenReturn(Optional.empty());

        Optional<?> result = memberService.findAdditionalAddressById(1L, 2L);

        assertThat(result).isEmpty();
        verify(memberRepository).findAdditionalAddressById(1L, 2L);
    }

    // --- findMemberTypeById ---

    @Test
    void findMemberTypeById_delegatesToRepository() {
        when(memberRepository.findMemberTypeById(1L)).thenReturn(MemberType.PERSONAL);

        MemberType result = memberService.findMemberTypeById(1L);

        assertThat(result).isEqualTo(MemberType.PERSONAL);
        verify(memberRepository).findMemberTypeById(1L);
    }

    // --- findProfileByMemberId ---

    @Test
    void findProfileByMemberId_delegatesToRepository() {
        when(memberRepository.findProfileByMemberId(1L)).thenReturn(Optional.empty());

        Optional<MemberProfileEditForm> result = memberService.findProfileByMemberId(1L);

        assertThat(result).isEmpty();
        verify(memberRepository).findProfileByMemberId(1L);
    }

    // --- helpers ---

    private MemberRegisterForm buildRegisterForm(String email, String password) {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail(email);
        form.setGender("male");
        form.setPassword(password);
        form.setNewsletterOptIn(true);
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("丸の内1-1");
        form.setDeliveryFloor("1");
        form.setHasElevator(true);
        form.setDaytimePhone("0312345678");
        return form;
    }

    private MemberProfileEditForm buildProfileEditForm(String email) {
        MemberProfileEditForm form = new MemberProfileEditForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail(email);
        form.setGender("male");
        return form;
    }

    private MemberAdditionalAddressForm buildAddressForm() {
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("丸の内1-1");
        form.setDeliveryFloor("1");
        form.setHasElevator(true);
        return form;
    }
}
