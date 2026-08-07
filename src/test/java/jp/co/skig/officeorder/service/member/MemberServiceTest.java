package jp.co.skig.officeorder.service.member;

import java.util.Optional;

import jp.co.skig.officeorder.logging.LogEvent;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberServiceTest {

    private final MemberRepository memberRepository = Mockito.mock(MemberRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    private final MessageSource messageSource = Mockito.mock(MessageSource.class);
    private MemberService service;

    @BeforeEach
    void setUp() {
        when(messageSource.getMessage(any(String.class), any(Object[].class), any(java.util.Locale.class))).thenReturn("message");
        service = new MemberService(memberRepository, passwordEncoder, messageSource);
    }

    @Test
    void findActiveCredentialByEmail_shouldReturnEmptyForBlankEmail() {
        assertEquals(Optional.empty(), service.findActiveCredentialByEmail("  "));
    }

    @Test
    void existsByEmail_shouldTrimInput() {
        when(memberRepository.existsByEmail("user@example.com")).thenReturn(true);

        boolean result = service.existsByEmail(" user@example.com ");

        assertEquals(true, result);
    }

    @Test
    void register_whenEmailAlreadyExists_shouldThrowDuplicateEmailException() {
        MemberRegisterForm input = new MemberRegisterForm();
        input.setEmail("user@example.com");
        input.setPassword("password");
        when(memberRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.register(input));
    }

    @Test
    void register_whenEmailIsNew_shouldInsertAndReturnSessionUser() {
        MemberRegisterForm input = new MemberRegisterForm();
        input.setEmail("user@example.com");
        input.setPassword("password");
        when(memberRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hash");
        when(memberRepository.insertMember(any(MemberRegisterForm.class), eq("hash"))).thenReturn(42L);
        MemberSessionUser sessionUser = new MemberSessionUser(42L, "user@example.com", "Yamada", "Taro");
        when(memberRepository.findActiveById(42L)).thenReturn(Optional.of(sessionUser));

        MemberSessionUser actual = service.register(input);

        assertEquals(sessionUser, actual);
        verify(memberRepository).insertMember(any(MemberRegisterForm.class), eq("hash"));
    }

    @Test
    void createAdditionalAddress_whenLimitReached_shouldThrowAddressLimitExceededException() {
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        when(memberRepository.countAdditionalAddresses(7L)).thenReturn((long) MemberService.ADDITIONAL_ADDRESS_LIMIT);

        assertThrows(AddressLimitExceededException.class, () -> service.createAdditionalAddress(7L, form));
    }

    @Test
    void toggleFavorite_whenAlreadyFavorite_shouldDeleteFavorite() {
        when(memberRepository.existsFavorite(8L, 100L)).thenReturn(true);

        service.toggleFavorite(8L, 100L);

        verify(memberRepository).deleteFavorite(8L, 100L);
    }

    @Test
    void toggleFavorite_whenNotFavorite_shouldInsertFavorite() {
        when(memberRepository.existsFavorite(9L, 200L)).thenReturn(false);
        when(memberRepository.countFavorites(9L)).thenReturn(0L);

        service.toggleFavorite(9L, 200L);

        verify(memberRepository).insertFavorite(9L, 200L);
    }

    @Test
    void removeFavorite_shouldDeleteFavorite() {
        service.removeFavorite(10L, 300L);

        verify(memberRepository).deleteFavorite(10L, 300L);
    }

    @Test
    void updateProfile_whenEmailDuplicate_shouldThrowDuplicateEmailException() {
        MemberProfileEditForm input = new MemberProfileEditForm();
        input.setEmail("existing@example.com");
        when(memberRepository.existsByEmailForOtherMember("existing@example.com", 5L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.updateProfile(5L, input));
    }
}
