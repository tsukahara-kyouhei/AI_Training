package jp.co.skig.officeorder.service.member;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MemberService の単体テスト。
 *
 * <p>会員登録（メール重複チェック・パスワードハッシュ化・レースコンディション対応）、
 * お気に入り上限、追加お届け先上限、ページ番号補正を検証する。
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MessageSource messageSource;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        lenient().when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(0, String.class));
        memberService = new MemberService(memberRepository, passwordEncoder, messageSource);
    }

    // -----------------------------------------------------------------------
    // 正常系：会員登録
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("新規メールアドレスで会員登録が成功しMemberSessionUserが返る")
    void register_returnsSessionUser_whenNewEmailProvided() {
        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(memberRepository.insertMember(any(), eq("hashed"))).thenReturn(1L);
        var sessionUser = new MemberSessionUser(1L, "new@example.com", "田中", "一郎");
        when(memberRepository.findActiveById(1L)).thenReturn(Optional.of(sessionUser));

        MemberSessionUser result = memberService.register(buildRegisterForm("new@example.com", "password123"));

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("登録時にパスワードがハッシュ化されてリポジトリへ渡される")
    void register_encodesPasswordBeforeInsert() {
        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plainPass")).thenReturn("$2a$10$hashed");
        when(memberRepository.insertMember(any(), eq("$2a$10$hashed"))).thenReturn(1L);
        when(memberRepository.findActiveById(1L)).thenReturn(Optional.of(
                new MemberSessionUser(1L, "e@example.com", "山", "花")));

        memberService.register(buildRegisterForm("e@example.com", "plainPass"));

        // パスワードハッシュ値が正しく insertMember に渡されていることを確認
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(memberRepository).insertMember(any(), hashCaptor.capture());
        assertThat(hashCaptor.getValue()).isEqualTo("$2a$10$hashed");
    }

    // -----------------------------------------------------------------------
    // 正常系：existsByEmail
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("existsByEmail でnull入力はfalseを返す")
    void existsByEmail_returnsFalse_whenNullEmail() {
        assertThat(memberService.existsByEmail(null)).isFalse();
    }

    @Test
    @DisplayName("existsByEmail で空文字入力はfalseを返す")
    void existsByEmail_returnsFalse_whenBlankEmail() {
        assertThat(memberService.existsByEmail("   ")).isFalse();
    }

    // -----------------------------------------------------------------------
    // 正常系：findActiveCredentialByEmail
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findActiveCredentialByEmail でnull入力はOptional.emptyを返す")
    void findActiveCredentialByEmail_returnsEmpty_whenNullEmail() {
        assertThat(memberService.findActiveCredentialByEmail(null)).isEmpty();
    }

    @Test
    @DisplayName("findActiveCredentialByEmail で前後空白はtrimされてリポジトリへ渡される")
    void findActiveCredentialByEmail_trimsEmailBeforeDelegating() {
        when(memberRepository.findActiveCredentialByEmail("trimmed@example.com"))
                .thenReturn(Optional.empty());

        memberService.findActiveCredentialByEmail("  trimmed@example.com  ");

        verify(memberRepository).findActiveCredentialByEmail("trimmed@example.com");
    }

    // -----------------------------------------------------------------------
    // 正常系：お気に入り
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("お気に入り上限（100件）に達していない場合に追加できる")
    void toggleFavorite_addsSuccessfully_whenBelowFavoritesLimit() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(99L);

        // 例外が発生しないこと（お気に入り追加成功）
        memberService.toggleFavorite(1L, 10L);

        verify(memberRepository).insertFavorite(1L, 10L);
    }

    // -----------------------------------------------------------------------
    // 正常系：追加お届け先ページ補正
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findAdditionalAddresses でページ番号0以下は1に補正される")
    void findAdditionalAddresses_normalizesPageToOne_whenPageZeroOrNegative() {
        var mockPage = new jp.co.skig.officeorder.model.member.MemberAdditionalAddressPage(
                java.util.List.of(), 0, 1, 20);
        when(memberRepository.findAdditionalAddresses(1L, 1, 20)).thenReturn(mockPage);

        memberService.findAdditionalAddresses(1L, 0);

        verify(memberRepository).findAdditionalAddresses(1L, 1, 20);
    }

    // -----------------------------------------------------------------------
    // 異常系：メール重複
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("既存メールアドレスで登録した場合DuplicateEmailExceptionがスローされる")
    void register_throwsDuplicateEmailException_whenEmailAlreadyExists() {
        when(memberRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.register(buildRegisterForm("dup@example.com", "pass")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("INSERT後に重複が判明した場合（レースコンディション）もDuplicateEmailExceptionがスローされる")
    void register_throwsDuplicateEmailException_whenRaceConditionDetected() {
        // 事前チェックは通過する
        when(memberRepository.existsByEmail("race@example.com")).thenReturn(false, true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(memberRepository.insertMember(any(), anyString()))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> memberService.register(buildRegisterForm("race@example.com", "pass")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    // -----------------------------------------------------------------------
    // 異常系：お気に入り上限
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("お気に入り上限（100件）超過時にFavoritesLimitExceededExceptionがスローされる")
    void toggleFavorite_throwsFavoritesLimitExceededException_whenAtLimit() {
        when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
        when(memberRepository.countFavorites(1L)).thenReturn(100L);

        assertThatThrownBy(() -> memberService.toggleFavorite(1L, 10L))
                .isInstanceOf(FavoritesLimitExceededException.class);
    }

    // -----------------------------------------------------------------------
    // 異常系：追加お届け先上限
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("追加お届け先上限（20件）超過時にAddressLimitExceededExceptionがスローされる")
    void createAdditionalAddress_throwsAddressLimitExceededException_whenAtLimit() {
        when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L);

        assertThatThrownBy(() -> memberService.createAdditionalAddress(1L, buildAddressForm()))
                .isInstanceOf(AddressLimitExceededException.class);
    }

    // -----------------------------------------------------------------------
    // ヘルパ
    // -----------------------------------------------------------------------

    private MemberRegisterForm buildRegisterForm(String email, String password) {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("田中");
        form.setFirstName("一郎");
        form.setLastNameKana("タナカ");
        form.setFirstNameKana("イチロウ");
        form.setEmail(email);
        form.setGender("male");
        form.setAnniversaryDate(LocalDate.of(2000, 1, 1));
        form.setPassword(password);
        form.setNewsletterOptIn(true);
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("一番町1-1");
        form.setDeliveryFloor("1");
        form.setHasElevator(true);
        form.setDaytimePhone("0312345678");
        return form;
    }

    private MemberAdditionalAddressForm buildAddressForm() {
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        form.setLastName("田中");
        form.setFirstName("一郎");
        form.setLastNameKana("タナカ");
        form.setFirstNameKana("イチロウ");
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("一番町1-1");
        form.setDeliveryFloor("1");
        form.setHasElevator(true);
        form.setDaytimePhone("0312345678");
        return form;
    }
}
