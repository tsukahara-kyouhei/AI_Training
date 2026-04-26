package jp.co.skig.officeorder.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressPage;
import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.model.member.MemberFavoritePage;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.MemberRepository;

/**
 * {@link MemberService} のユニットテスト。
 *
 * <p>
 * M-01〜M-34 のテストケースを網羅する。
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  private static final String TEST_EMAIL = "test@example.com";
  private static final long MEMBER_ID = 100L;

  private MemberRepository memberRepository;
  private PasswordEncoder passwordEncoder;
  private MemberService service;

  @BeforeEach
  void setUp() {
    memberRepository = mock(MemberRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    MessageSource ms = mock(MessageSource.class);
    lenient().when(ms.getMessage(any(String.class), any(Object[].class), any(Locale.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    service = new MemberService(memberRepository, passwordEncoder, ms);
  }

  // ============================================================
  // M-01〜M-05: findActiveCredentialByEmail
  // ============================================================

  @Nested
  @DisplayName("findActiveCredentialByEmail")
  class FindActiveCredentialByEmail {

    @Test
    @DisplayName("M-01: email=null のとき Optional.empty を返す（repository は呼ばれない）")
    void nullEmail_returnsEmpty() {
      Optional<MemberCredential> result = service.findActiveCredentialByEmail(null);
      assertThat(result).isEmpty();
      verify(memberRepository, never()).findActiveCredentialByEmail(anyString());
    }

    @Test
    @DisplayName("M-02: email=空文字 のとき Optional.empty を返す")
    void emptyEmail_returnsEmpty() {
      Optional<MemberCredential> result = service.findActiveCredentialByEmail("");
      assertThat(result).isEmpty();
      verify(memberRepository, never()).findActiveCredentialByEmail(anyString());
    }

    @Test
    @DisplayName("M-03: email=空白のみ のとき Optional.empty を返す")
    void blankEmail_returnsEmpty() {
      Optional<MemberCredential> result = service.findActiveCredentialByEmail("   ");
      assertThat(result).isEmpty();
      verify(memberRepository, never()).findActiveCredentialByEmail(anyString());
    }

    @Test
    @DisplayName("M-04: 有効なメールアドレスのとき repository に委譲する")
    void validEmail_delegatesToRepository() {
      MemberCredential credential = new MemberCredential(MEMBER_ID, TEST_EMAIL, "山田", "太郎", "hash");
      when(memberRepository.findActiveCredentialByEmail(TEST_EMAIL))
          .thenReturn(Optional.of(credential));

      Optional<MemberCredential> result = service.findActiveCredentialByEmail(TEST_EMAIL);

      assertThat(result).contains(credential);
    }

    @Test
    @DisplayName("M-05: 前後空白がトリムされて repository が呼ばれる")
    void emailWithWhitespace_isTrimmed() {
      when(memberRepository.findActiveCredentialByEmail(TEST_EMAIL))
          .thenReturn(Optional.empty());

      service.findActiveCredentialByEmail("  " + TEST_EMAIL + "  ");

      verify(memberRepository).findActiveCredentialByEmail(TEST_EMAIL);
    }
  }

  // ============================================================
  // M-06〜M-08: existsByEmail
  // ============================================================

  @Nested
  @DisplayName("existsByEmail")
  class ExistsByEmail {

    @Test
    @DisplayName("M-06: email=null のとき false を返す（repository は呼ばれない）")
    void nullEmail_returnsFalse() {
      boolean result = service.existsByEmail(null);
      assertThat(result).isFalse();
      verify(memberRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("M-07: email=空文字 のとき false を返す")
    void emptyEmail_returnsFalse() {
      boolean result = service.existsByEmail("");
      assertThat(result).isFalse();
      verify(memberRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("M-08: 有効なメールアドレスのとき repository の戻り値をそのまま返す")
    void validEmail_delegatesToRepository() {
      when(memberRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);
      assertThat(service.existsByEmail(TEST_EMAIL)).isTrue();

      when(memberRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
      assertThat(service.existsByEmail(TEST_EMAIL)).isFalse();
    }
  }

  // ============================================================
  // M-09〜M-13: register
  // ============================================================

  @Nested
  @DisplayName("register")
  class Register {

    @Test
    @DisplayName("M-09: 正常登録される（insertMember が呼ばれ MemberSessionUser が返る）")
    void success_returnsSessionUser() {
      MemberRegisterForm form = registerForm();
      when(memberRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
      when(memberRepository.insertMember(any(), anyString())).thenReturn(MEMBER_ID);
      MemberSessionUser sessionUser = new MemberSessionUser(MEMBER_ID, TEST_EMAIL, "山田", "太郎");
      when(memberRepository.findActiveById(MEMBER_ID)).thenReturn(Optional.of(sessionUser));

      MemberSessionUser result = service.register(form);

      assertThat(result).isEqualTo(sessionUser);
      verify(memberRepository).insertMember(any(), eq("hashedPassword"));
    }

    @Test
    @DisplayName("M-10: 事前チェックでメール重複を検出したとき DuplicateEmailException")
    void preCheckDuplicate_throwsDuplicateEmailException() {
      MemberRegisterForm form = registerForm();
      when(memberRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

      assertThatThrownBy(() -> service.register(form))
          .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("M-11: DB 挿入時に DataIntegrityViolation かつ重複あり → DuplicateEmailException（race condition）")
    void raceDuplicate_throwsDuplicateEmailException() {
      MemberRegisterForm form = registerForm();
      when(memberRepository.existsByEmail(TEST_EMAIL))
          .thenReturn(false) // 1回目：事前チェック （通過）
          .thenReturn(true); // 2回目：catch 内再確認（重複あり）
      when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
      when(memberRepository.insertMember(any(), anyString()))
          .thenThrow(new DataIntegrityViolationException("unique constraint"));

      assertThatThrownBy(() -> service.register(form))
          .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("M-12: DB 挿入時に DataIntegrityViolation かつ重複なし → 例外を再スロー")
    void raceNoDuplicate_rethrowsDataIntegrityViolationException() {
      MemberRegisterForm form = registerForm();
      when(memberRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
      DataIntegrityViolationException ex = new DataIntegrityViolationException("unique key");
      when(memberRepository.insertMember(any(), anyString())).thenThrow(ex);

      assertThatThrownBy(() -> service.register(form))
          .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("M-13: 登録後 findActiveById が empty → IllegalStateException")
    void findActiveByIdEmpty_throwsIllegalStateException() {
      MemberRegisterForm form = registerForm();
      when(memberRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
      when(memberRepository.insertMember(any(), anyString())).thenReturn(MEMBER_ID);
      when(memberRepository.findActiveById(MEMBER_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.register(form))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  // ============================================================
  // M-14〜M-18: updateProfile
  // ============================================================

  @Nested
  @DisplayName("updateProfile")
  class UpdateProfile {

    @Test
    @DisplayName("M-14: 正常更新される（重複なし、updateProfile が true）")
    void success_callsUpdateProfile() {
      MemberProfileEditForm form = profileForm();
      when(memberRepository.existsByEmailForOtherMember(TEST_EMAIL, MEMBER_ID)).thenReturn(false);
      when(memberRepository.updateProfile(eq(MEMBER_ID), any())).thenReturn(true);

      service.updateProfile(MEMBER_ID, form);

      verify(memberRepository, times(1)).updateProfile(eq(MEMBER_ID), any());
    }

    @Test
    @DisplayName("M-15: 他会員とメールが重複する場合は DuplicateEmailException")
    void existingEmailForOtherMember_throwsDuplicateEmailException() {
      MemberProfileEditForm form = profileForm();
      when(memberRepository.existsByEmailForOtherMember(TEST_EMAIL, MEMBER_ID)).thenReturn(true);

      assertThatThrownBy(() -> service.updateProfile(MEMBER_ID, form))
          .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("M-16: DB 更新時に DataIntegrityViolation かつ重複あり → DuplicateEmailException")
    void raceDuplicate_throwsDuplicateEmailException() {
      MemberProfileEditForm form = profileForm();
      when(memberRepository.existsByEmailForOtherMember(TEST_EMAIL, MEMBER_ID))
          .thenReturn(false) // 1回目：事前チェック（通過）
          .thenReturn(true); // 2回目：catch 内再確認（重複あり）
      when(memberRepository.updateProfile(eq(MEMBER_ID), any()))
          .thenThrow(new DataIntegrityViolationException("unique constraint"));

      assertThatThrownBy(() -> service.updateProfile(MEMBER_ID, form))
          .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("M-17: DB 更新時に DataIntegrityViolation かつ重複なし → 再スロー")
    void raceNoDuplicate_rethrowsDataIntegrityViolationException() {
      MemberProfileEditForm form = profileForm();
      when(memberRepository.existsByEmailForOtherMember(TEST_EMAIL, MEMBER_ID)).thenReturn(false);
      DataIntegrityViolationException ex = new DataIntegrityViolationException("unique");
      when(memberRepository.updateProfile(eq(MEMBER_ID), any())).thenThrow(ex);

      assertThatThrownBy(() -> service.updateProfile(MEMBER_ID, form))
          .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("M-18: updateProfile が false（会員見つからず）→ IllegalStateException")
    void updatedFalse_throwsIllegalStateException() {
      MemberProfileEditForm form = profileForm();
      when(memberRepository.existsByEmailForOtherMember(TEST_EMAIL, MEMBER_ID)).thenReturn(false);
      when(memberRepository.updateProfile(eq(MEMBER_ID), any())).thenReturn(false);

      assertThatThrownBy(() -> service.updateProfile(MEMBER_ID, form))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  // ============================================================
  // M-19〜M-20: withdraw
  // ============================================================

  @Nested
  @DisplayName("withdraw")
  class Withdraw {

    @Test
    @DisplayName("M-19: 退会成功時は true が返る")
    void success_returnsTrue() {
      when(memberRepository.withdrawMember(MEMBER_ID)).thenReturn(true);
      assertThat(service.withdraw(MEMBER_ID)).isTrue();
    }

    @Test
    @DisplayName("M-20: 対象会員が見つからない場合は false が返る")
    void notFound_returnsFalse() {
      when(memberRepository.withdrawMember(MEMBER_ID)).thenReturn(false);
      assertThat(service.withdraw(MEMBER_ID)).isFalse();
    }
  }

  // ============================================================
  // M-21〜M-25: isAddressLimitReached / createAdditionalAddress
  // ============================================================

  @Nested
  @DisplayName("isAddressLimitReached / createAdditionalAddress")
  class AddressLimit {

    @Test
    @DisplayName("M-21: 登録数=19（上限-1）のとき isAddressLimitReached は false")
    void count19_notReached() {
      when(memberRepository.countAdditionalAddresses(MEMBER_ID)).thenReturn(19L);
      assertThat(service.isAddressLimitReached(MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("M-22: 登録数=20（上限ちょうど）のとき isAddressLimitReached は true")
    void count20_reached() {
      when(memberRepository.countAdditionalAddresses(MEMBER_ID)).thenReturn(20L);
      assertThat(service.isAddressLimitReached(MEMBER_ID)).isTrue();
    }

    @Test
    @DisplayName("M-23: 登録数=21（上限+1）のとき isAddressLimitReached は true")
    void count21_reached() {
      when(memberRepository.countAdditionalAddresses(MEMBER_ID)).thenReturn(21L);
      assertThat(service.isAddressLimitReached(MEMBER_ID)).isTrue();
    }

    @Test
    @DisplayName("M-24: 上限未満のとき追加お届け先の登録成功（insertAdditionalAddress が呼ばれる）")
    void underLimit_insertsAddress() {
      when(memberRepository.countAdditionalAddresses(MEMBER_ID)).thenReturn(19L);
      MemberAdditionalAddressForm form = addressForm();

      service.createAdditionalAddress(MEMBER_ID, form);

      verify(memberRepository).insertAdditionalAddress(eq(MEMBER_ID), any());
    }

    @Test
    @DisplayName("M-25: 上限到達時に AddressLimitExceededException")
    void atLimit_throwsAddressLimitExceededException() {
      when(memberRepository.countAdditionalAddresses(MEMBER_ID)).thenReturn(20L);
      MemberAdditionalAddressForm form = addressForm();

      assertThatThrownBy(() -> service.createAdditionalAddress(MEMBER_ID, form))
          .isInstanceOf(AddressLimitExceededException.class);
    }
  }

  // ============================================================
  // M-26〜M-31: toggleFavorite
  // ============================================================

  @Nested
  @DisplayName("toggleFavorite")
  class ToggleFavorite {

    private static final long PRODUCT_ID = 50L;

    @Test
    @DisplayName("M-26: 既にお気に入り登録済みのとき deleteFavorite が呼ばれる")
    void existingFavorite_deletesCalled() {
      when(memberRepository.existsFavorite(MEMBER_ID, PRODUCT_ID)).thenReturn(true);

      service.toggleFavorite(MEMBER_ID, PRODUCT_ID);

      verify(memberRepository).deleteFavorite(MEMBER_ID, PRODUCT_ID);
      verify(memberRepository, never()).insertFavorite(MEMBER_ID, PRODUCT_ID);
    }

    @Test
    @DisplayName("M-27: 未登録かつ件数=99（上限-1）のとき insertFavorite が呼ばれる")
    void count99_insertsSuccessfully() {
      when(memberRepository.existsFavorite(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
      when(memberRepository.countFavorites(MEMBER_ID)).thenReturn(99L);

      service.toggleFavorite(MEMBER_ID, PRODUCT_ID);

      verify(memberRepository).insertFavorite(MEMBER_ID, PRODUCT_ID);
    }

    @Test
    @DisplayName("M-28: 未登録かつ件数=100（上限ちょうど）のとき FavoritesLimitExceededException")
    void count100_throwsFavoritesLimitExceededException() {
      when(memberRepository.existsFavorite(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
      when(memberRepository.countFavorites(MEMBER_ID)).thenReturn(100L);

      assertThatThrownBy(() -> service.toggleFavorite(MEMBER_ID, PRODUCT_ID))
          .isInstanceOf(FavoritesLimitExceededException.class);
    }

    @Test
    @DisplayName("M-29: 未登録かつ件数=101（上限+1）のとき FavoritesLimitExceededException")
    void count101_throwsFavoritesLimitExceededException() {
      when(memberRepository.existsFavorite(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
      when(memberRepository.countFavorites(MEMBER_ID)).thenReturn(101L);

      assertThatThrownBy(() -> service.toggleFavorite(MEMBER_ID, PRODUCT_ID))
          .isInstanceOf(FavoritesLimitExceededException.class);
    }

    @Test
    @DisplayName("M-30: insertFavorite で DataIntegrityViolation かつ事後確認で存在あり → 正常終了（race condition スキップ）")
    void raceInsertExists_noException() {
      when(memberRepository.existsFavorite(MEMBER_ID, PRODUCT_ID))
          .thenReturn(false) // 1回目：存在しない（新規挿入へ）
          .thenReturn(true); // 2回目：catch 内再確認（存在あり → スキップ）
      when(memberRepository.countFavorites(MEMBER_ID)).thenReturn(50L);
      doThrow(new DataIntegrityViolationException("unique"))
          .when(memberRepository).insertFavorite(MEMBER_ID, PRODUCT_ID);

      // 例外なしで正常終了
      service.toggleFavorite(MEMBER_ID, PRODUCT_ID);
    }

    @Test
    @DisplayName("M-31: insertFavorite で DataIntegrityViolation かつ事後確認で存在なし → 例外を再スロー")
    void raceInsertNotExists_rethrows() {
      when(memberRepository.existsFavorite(MEMBER_ID, PRODUCT_ID)).thenReturn(false);
      when(memberRepository.countFavorites(MEMBER_ID)).thenReturn(50L);
      doThrow(new DataIntegrityViolationException("unique"))
          .when(memberRepository).insertFavorite(MEMBER_ID, PRODUCT_ID);

      assertThatThrownBy(() -> service.toggleFavorite(MEMBER_ID, PRODUCT_ID))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
  }

  // ============================================================
  // M-32〜M-34: ページ補正
  // ============================================================

  @Nested
  @DisplayName("ページ補正系（findAdditionalAddresses / findFavorites）")
  class PageNormalization {

    @Test
    @DisplayName("M-32: findAdditionalAddresses で page=0 → page=1 に補正して repository が呼ばれる")
    void additionalAddressesPageZero_normalizedToOne() {
      when(memberRepository.findAdditionalAddresses(eq(MEMBER_ID), eq(1), eq(MemberService.MYPAGE_PAGE_SIZE)))
          .thenReturn(mock(MemberAdditionalAddressPage.class));

      service.findAdditionalAddresses(MEMBER_ID, 0);

      verify(memberRepository).findAdditionalAddresses(MEMBER_ID, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    @Test
    @DisplayName("M-33: findAdditionalAddresses で page=-5 → page=1 に補正して repository が呼ばれる")
    void additionalAddressesNegativePage_normalizedToOne() {
      when(memberRepository.findAdditionalAddresses(eq(MEMBER_ID), eq(1), eq(MemberService.MYPAGE_PAGE_SIZE)))
          .thenReturn(mock(MemberAdditionalAddressPage.class));

      service.findAdditionalAddresses(MEMBER_ID, -5);

      verify(memberRepository).findAdditionalAddresses(MEMBER_ID, 1, MemberService.MYPAGE_PAGE_SIZE);
    }

    @Test
    @DisplayName("M-34: findFavorites で page=0 → page=1 に補正して repository が呼ばれる")
    void favoritesPageZero_normalizedToOne() {
      when(memberRepository.findFavorites(eq(MEMBER_ID), eq(1), eq(MemberService.MYPAGE_PAGE_SIZE)))
          .thenReturn(mock(MemberFavoritePage.class));

      service.findFavorites(MEMBER_ID, 0);

      verify(memberRepository).findFavorites(MEMBER_ID, 1, MemberService.MYPAGE_PAGE_SIZE);
    }
  }

  // ============================================================
  // ヘルパメソッド
  // ============================================================

  /** テスト用の最小フィールドを設定した会員登録フォームを生成する。 */
  private static MemberRegisterForm registerForm() {
    MemberRegisterForm form = new MemberRegisterForm();
    form.setPersonalOrCorporate("personal");
    form.setLastName("山田");
    form.setFirstName("太郎");
    form.setLastNameKana("ヤマダ");
    form.setFirstNameKana("タロウ");
    form.setEmail(TEST_EMAIL);
    form.setGender("male");
    form.setAnniversaryDate(java.time.LocalDate.of(1990, 1, 1));
    form.setPassword("Password1!");
    form.setNewsletterOptIn(Boolean.TRUE);
    form.setPostalCodePart1("100");
    form.setPostalCodePart2("0001");
    form.setPrefecture("東京都");
    form.setCity("千代田区");
    form.setAddressLine("千代田1-1");
    form.setDeliveryFloor("3");
    form.setHasElevator(Boolean.TRUE);
    form.setDaytimePhone("0312345678");
    return form;
  }

  /** テスト用の最小フィールドを設定した会員情報変更フォームを生成する。 */
  private static MemberProfileEditForm profileForm() {
    MemberProfileEditForm form = new MemberProfileEditForm();
    form.setPersonalOrCorporate("personal");
    form.setLastName("山田");
    form.setFirstName("太郎");
    form.setLastNameKana("ヤマダ");
    form.setFirstNameKana("タロウ");
    form.setEmail(TEST_EMAIL);
    form.setGender("male");
    form.setAnniversaryDate(java.time.LocalDate.of(1990, 1, 1));
    form.setNewsletterOptIn(Boolean.TRUE);
    form.setPostalCodePart1("100");
    form.setPostalCodePart2("0001");
    form.setPrefecture("東京都");
    form.setCity("千代田区");
    form.setAddressLine("千代田1-1");
    form.setDeliveryFloor("3");
    form.setHasElevator(Boolean.TRUE);
    form.setDaytimePhone("0312345678");
    return form;
  }

  /** テスト用の最小フィールドを設定した追加お届け先フォームを生成する。 */
  private static MemberAdditionalAddressForm addressForm() {
    MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
    form.setLastName("山田");
    form.setFirstName("太郎");
    form.setLastNameKana("ヤマダ");
    form.setFirstNameKana("タロウ");
    form.setPostalCodePart1("100");
    form.setPostalCodePart2("0001");
    form.setPrefecture("東京都");
    form.setCity("千代田区");
    form.setAddressLine("千代田1-1");
    form.setDaytimePhone("0312345678");
    return form;
  }
}
