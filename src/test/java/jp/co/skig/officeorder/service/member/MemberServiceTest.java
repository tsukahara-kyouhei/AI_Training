package jp.co.skig.officeorder.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;

class MemberServiceTest {

    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private MessageSource messageSource;
    private MemberService service;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        messageSource = mock(MessageSource.class);
        service = new MemberService(memberRepository, passwordEncoder, messageSource);
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("message");
    }

    @Test
    void register_正常系_会員を登録してセッション情報を返す() {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setEmail("test@example.com");
        form.setPassword("Password123!");
        form.setPersonalOrCorporate("personal");
        when(memberRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hash");
        when(memberRepository.insertMember(any(MemberRegisterForm.class), eq("hash"))).thenReturn(7L);
        MemberSessionUser expected = new MemberSessionUser(7L, "test@example.com", "山田", "太郎");
        when(memberRepository.findActiveById(7L)).thenReturn(Optional.of(expected));

        MemberSessionUser result = service.register(form);

        assertThat(result).isEqualTo(expected);
        verify(memberRepository).insertMember(any(MemberRegisterForm.class), eq("hash"));
    }

    @Test
    void register_異常系_重複メールアドレスなら例外を送出する() {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setEmail("dup@example.com");
        form.setPassword("Password123!");
        form.setPersonalOrCorporate("personal");
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.register(form));
        verify(memberRepository, never()).insertMember(any(MemberRegisterForm.class), any());
    }

    @Test
    void updateProfile_異常系_他会員とメールアドレスが重複した場合は例外を送出する() {
        MemberProfileEditForm form = new MemberProfileEditForm();
        form.setEmail("dup@example.com");
        when(memberRepository.existsByEmailForOtherMember("dup@example.com", 3L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.updateProfile(3L, form));
    }

    @Test
    void createAdditionalAddress_異常系_上限超過なら例外を送出する() {
        when(memberRepository.countAdditionalAddresses(99L)).thenReturn((long) MemberService.ADDITIONAL_ADDRESS_LIMIT);
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();

        assertThrows(AddressLimitExceededException.class, () -> service.createAdditionalAddress(99L, form));
        verify(memberRepository, never()).insertAdditionalAddress(any(Long.class),
                any(MemberAdditionalAddressForm.class));
    }

    @Test
    void toggleFavorite_正常系_未登録なら追加できる() {
        when(memberRepository.existsFavorite(10L, 20L)).thenReturn(false);
        when(memberRepository.countFavorites(10L)).thenReturn(0L);

        service.toggleFavorite(10L, 20L);

        verify(memberRepository).insertFavorite(10L, 20L);
    }

    @Test
    void toggleFavorite_異常系_上限超過なら例外を送出する() {
        when(memberRepository.existsFavorite(10L, 20L)).thenReturn(false);
        when(memberRepository.countFavorites(10L)).thenReturn((long) MemberService.FAVORITES_LIMIT);

        assertThrows(FavoritesLimitExceededException.class, () -> service.toggleFavorite(10L, 20L));
        verify(memberRepository, never()).insertFavorite(any(Long.class), any(Long.class));
    }
}
