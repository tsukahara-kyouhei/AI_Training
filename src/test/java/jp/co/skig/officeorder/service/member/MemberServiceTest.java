package jp.co.skig.officeorder.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressPage;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressView;
import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.model.member.MemberFavoritePage;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.member.MemberType;
import jp.co.skig.officeorder.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 会員サービスの単体テスト。
 *
 * <p>単体テスト計画の MEM-001〜MEM-013 に対応する。
 */
class MemberServiceTest {

    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private MessageSource messageSource;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        messageSource = new ResourceBundleMessageSource();
        ((ResourceBundleMessageSource) messageSource).setBasename("messages");
        ((ResourceBundleMessageSource) messageSource).setDefaultEncoding("UTF-8");
        memberService = new MemberService(memberRepository, passwordEncoder, messageSource);
    }

    private MemberRegisterForm validRegisterForm() {
        MemberRegisterForm form = new MemberRegisterForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail("taro@example.com");
        form.setGender("male");
        form.setAnniversaryDate(LocalDate.of(1990, 1, 1));
        form.setPassword("password123");
        form.setNewsletterOptIn(Boolean.TRUE);
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("丸の内1-1-1");
        form.setDeliveryFloor("3");
        form.setHasElevator(Boolean.TRUE);
        form.setDaytimePhone("0312345678");
        return form;
    }

    private MemberProfileEditForm validProfileForm() {
        MemberProfileEditForm form = new MemberProfileEditForm();
        form.setPersonalOrCorporate("personal");
        form.setLastName("山田");
        form.setFirstName("太郎");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("タロウ");
        form.setEmail("taro@example.com");
        form.setGender("male");
        form.setAnniversaryDate(LocalDate.of(1990, 1, 1));
        form.setNewsletterOptIn(Boolean.TRUE);
        form.setPostalCodePart1("100");
        form.setPostalCodePart2("0001");
        form.setPrefecture("東京都");
        form.setCity("千代田区");
        form.setAddressLine("丸の内1-1-1");
        form.setDeliveryFloor("3");
        form.setHasElevator(Boolean.TRUE);
        form.setDaytimePhone("0312345678");
        return form;
    }

    private MemberAdditionalAddressForm validAddressForm() {
        MemberAdditionalAddressForm form = new MemberAdditionalAddressForm();
        form.setLastName("山田");
        form.setFirstName("花子");
        form.setLastNameKana("ヤマダ");
        form.setFirstNameKana("ハナコ");
        form.setPostalCodePart1("150");
        form.setPostalCodePart2("0002");
        form.setPrefecture("東京都");
        form.setCity("渋谷区");
        form.setAddressLine("渋谷1-1-1");
        form.setDeliveryFloor("5");
        form.setHasElevator(Boolean.FALSE);
        form.setDaytimePhone("0311112222");
        return form;
    }

    @Nested
    @DisplayName("MEM-001〜MEM-002 認証情報取得")
    class Credential {

        @Test
        @DisplayName("MEM-001 有効会員の認証情報を返す")
        void findActiveCredentialByEmail_activeMember_returnsCredential() {
            MemberCredential credential = new MemberCredential(
                    1L, "taro@example.com", "山田", "太郎", "hashed"
            );
            when(memberRepository.findActiveCredentialByEmail("taro@example.com"))
                    .thenReturn(Optional.of(credential));

            Optional<MemberCredential> result = memberService.findActiveCredentialByEmail("  taro@example.com  ");

            assertThat(result).isPresent();
            assertThat(result.get().memberId()).isEqualTo(1L);
            verify(memberRepository).findActiveCredentialByEmail("taro@example.com");
        }

        @Test
        @DisplayName("MEM-002 未登録メール・退会会員は認証対象なしとして扱う")
        void findActiveCredentialByEmail_notFound_returnsEmpty() {
            when(memberRepository.findActiveCredentialByEmail("unknown@example.com"))
                    .thenReturn(Optional.empty());

            assertThat(memberService.findActiveCredentialByEmail("unknown@example.com")).isEmpty();
        }

        @Test
        @DisplayName("MEM-002 空メールはリポジトリを呼ばずOptional.emptyを返す")
        void findActiveCredentialByEmail_blank_returnsEmptyWithoutRepositoryCall() {
            assertThat(memberService.findActiveCredentialByEmail("   ")).isEmpty();
            assertThat(memberService.findActiveCredentialByEmail(null)).isEmpty();
            verify(memberRepository, never()).findActiveCredentialByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("MEM-003〜MEM-005 会員登録")
    class Register {

        @Test
        @DisplayName("MEM-003 パスワードをハッシュ化して会員を登録する")
        void register_hashesPasswordAndInsertsMember() {
            MemberRegisterForm form = validRegisterForm();
            when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
            when(memberRepository.insertMember(any(MemberRegisterForm.class), eq("hashed-password")))
                    .thenReturn(1L);
            when(memberRepository.findActiveById(1L))
                    .thenReturn(Optional.of(new MemberSessionUser(1L, "taro@example.com", "山田", "太郎")));

            MemberSessionUser result = memberService.register(form);

            assertThat(result.memberId()).isEqualTo(1L);
            verify(passwordEncoder).encode("password123");
            verify(memberRepository).insertMember(any(MemberRegisterForm.class), eq("hashed-password"));
        }

        @Test
        @DisplayName("MEM-004 登録前メール重複は重複例外となり登録しない")
        void register_duplicateEmail_throwsAndDoesNotInsert() {
            MemberRegisterForm form = validRegisterForm();
            when(memberRepository.existsByEmail("taro@example.com")).thenReturn(true);

            assertThatThrownBy(() -> memberService.register(form))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("メールアドレスが重複");

            verify(memberRepository, never()).insertMember(any(MemberRegisterForm.class), anyString());
        }

        @Test
        @DisplayName("MEM-005 登録時の重複競合はDB例外を重複例外へ変換する")
        void register_duplicateRace_convertsDbExceptionToDuplicateEmail() {
            MemberRegisterForm form = validRegisterForm();
            when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
            when(memberRepository.insertMember(any(MemberRegisterForm.class), anyString()))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));
            when(memberRepository.existsByEmail("taro@example.com")).thenReturn(true);

            assertThatThrownBy(() -> memberService.register(form))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("メールアドレスが重複");
        }

        @Test
        @DisplayName("MEM-005 重複でないDB例外はそのまま伝播する")
        void register_nonDuplicateDbException_propagates() {
            MemberRegisterForm form = validRegisterForm();
            when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
            when(memberRepository.insertMember(any(MemberRegisterForm.class), anyString()))
                    .thenThrow(new DataIntegrityViolationException("other constraint"));
            when(memberRepository.existsByEmail("taro@example.com")).thenReturn(false);

            assertThatThrownBy(() -> memberService.register(form))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("other constraint");
        }
    }

    @Nested
    @DisplayName("MEM-006〜MEM-007 プロフィール更新")
    class ProfileUpdate {

        @Test
        @DisplayName("MEM-006 正常に更新し、更新後情報を再取得する")
        void updateProfile_success_updatesProfile() {
            MemberProfileEditForm form = validProfileForm();
            when(memberRepository.existsByEmailForOtherMember("taro@example.com", 1L)).thenReturn(false);
            // サービスは正規化済みフォームの複製をリポジトリへ渡すため any() で照合する
            when(memberRepository.updateProfile(eq(1L), any(MemberProfileEditForm.class))).thenReturn(true);

            memberService.updateProfile(1L, form);

            verify(memberRepository).updateProfile(eq(1L), any(MemberProfileEditForm.class));
        }

        @Test
        @DisplayName("MEM-007 他会員とメール重複は更新を拒否する")
        void updateProfile_duplicateEmailForOtherMember_throws() {
            MemberProfileEditForm form = validProfileForm();
            when(memberRepository.existsByEmailForOtherMember("taro@example.com", 1L)).thenReturn(true);

            assertThatThrownBy(() -> memberService.updateProfile(1L, form))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("メールアドレスが重複");

            verify(memberRepository, never()).updateProfile(anyLong(), any(MemberProfileEditForm.class));
        }

        @Test
        @DisplayName("MEM-006 更新対象が見つからない場合は例外となる")
        void updateProfile_notFound_throwsIllegalState() {
            MemberProfileEditForm form = validProfileForm();
            when(memberRepository.existsByEmailForOtherMember("taro@example.com", 1L)).thenReturn(false);
            when(memberRepository.updateProfile(eq(1L), any(MemberProfileEditForm.class))).thenReturn(false);

            assertThatThrownBy(() -> memberService.updateProfile(1L, form))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("更新対象が見つかりません");
        }
    }

    @Nested
    @DisplayName("MEM-008 退会")
    class Withdraw {

        @Test
        @DisplayName("MEM-008 会員状態と退会日時の更新を依頼する")
        void withdraw_success_returnsTrue() {
            when(memberRepository.withdrawMember(1L)).thenReturn(true);

            boolean result = memberService.withdraw(1L);

            assertThat(result).isTrue();
            verify(memberRepository).withdrawMember(1L);
        }

        @Test
        @DisplayName("MEM-008 既に退会済み・対象なしはfalseを返す")
        void withdraw_alreadyWithdrawn_returnsFalse() {
            when(memberRepository.withdrawMember(1L)).thenReturn(false);

            boolean result = memberService.withdraw(1L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("MEM-009〜MEM-011 追加お届け先")
    class AdditionalAddress {

        @Test
        @DisplayName("MEM-009 上限未満なら追加お届け先を登録する")
        void createAdditionalAddress_belowLimit_inserts() {
            when(memberRepository.countAdditionalAddresses(1L)).thenReturn(19L);

            memberService.createAdditionalAddress(1L, validAddressForm());

            verify(memberRepository).insertAdditionalAddress(eq(1L), any(MemberAdditionalAddressForm.class));
        }

        @Test
        @DisplayName("MEM-010 上限到達時は登録せず上限超過例外となる")
        void createAdditionalAddress_atLimit_throwsAndDoesNotInsert() {
            when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L);

            assertThatThrownBy(() -> memberService.createAdditionalAddress(1L, validAddressForm()))
                    .isInstanceOf(AddressLimitExceededException.class)
                    .hasMessageContaining("お届け先を追加できません");

            verify(memberRepository, never()).insertAdditionalAddress(anyLong(), any(MemberAdditionalAddressForm.class));
        }

        @Test
        @DisplayName("MEM-011 追加お届け先更新は対象会員の所有範囲だけを操作する")
        void updateAdditionalAddress_ownedAddress_updates() {
            // サービスは正規化済みフォームの複製をリポジトリへ渡すため any() で照合する
            when(memberRepository.updateAdditionalAddress(eq(1L), eq(5L), any(MemberAdditionalAddressForm.class)))
                    .thenReturn(true);

            boolean result = memberService.updateAdditionalAddress(1L, 5L, validAddressForm());

            assertThat(result).isTrue();
            verify(memberRepository).updateAdditionalAddress(eq(1L), eq(5L), any(MemberAdditionalAddressForm.class));
        }

        @Test
        @DisplayName("MEM-011 他人の追加お届け先は更新されずfalseを返す")
        void updateAdditionalAddress_notOwned_returnsFalse() {
            when(memberRepository.updateAdditionalAddress(eq(1L), eq(99L), any(MemberAdditionalAddressForm.class)))
                    .thenReturn(false);

            boolean result = memberService.updateAdditionalAddress(1L, 99L, validAddressForm());

            assertThat(result).isFalse();
            verify(memberRepository).updateAdditionalAddress(eq(1L), eq(99L), any(MemberAdditionalAddressForm.class));
        }

        @Test
        @DisplayName("MEM-011 追加お届け先削除は対象会員の所有範囲だけを操作する")
        void deleteAdditionalAddress_ownedAddress_deletes() {
            memberService.deleteAdditionalAddress(1L, 5L);

            verify(memberRepository).deleteAdditionalAddress(1L, 5L);
        }
    }

    @Nested
    @DisplayName("MEM-012〜MEM-013 お気に入り")
    class Favorites {

        @Test
        @DisplayName("MEM-012 お気に入り追加は中間テーブルの登録を依頼する")
        void toggleFavorite_notFavorite_inserts() {
            when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
            when(memberRepository.countFavorites(1L)).thenReturn(0L);

            memberService.toggleFavorite(1L, 10L);

            verify(memberRepository).insertFavorite(1L, 10L);
            verify(memberRepository, never()).deleteFavorite(anyLong(), anyLong());
        }

        @Test
        @DisplayName("MEM-012 お気に入り解除は中間テーブルの削除を依頼する")
        void toggleFavorite_alreadyFavorite_deletes() {
            when(memberRepository.existsFavorite(1L, 10L)).thenReturn(true);

            memberService.toggleFavorite(1L, 10L);

            verify(memberRepository).deleteFavorite(1L, 10L);
            verify(memberRepository, never()).insertFavorite(anyLong(), anyLong());
        }

        @Test
        @DisplayName("MEM-013 お気に入り上限到達時は登録を拒否する")
        void toggleFavorite_atLimit_throws() {
            when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false);
            when(memberRepository.countFavorites(1L)).thenReturn(100L);

            assertThatThrownBy(() -> memberService.toggleFavorite(1L, 10L))
                    .isInstanceOf(FavoritesLimitExceededException.class)
                    .hasMessageContaining("お気に入りに追加できません");

            verify(memberRepository, never()).insertFavorite(anyLong(), anyLong());
        }

        @Test
        @DisplayName("MEM-012 登録競合時は既存登録として扱い例外を出さない")
        void toggleFavorite_insertRace_skips() {
            // 1回目の存在確認は未登録、insert競合後の再確認は登録済みを返す
            when(memberRepository.existsFavorite(1L, 10L)).thenReturn(false, true);
            when(memberRepository.countFavorites(1L)).thenReturn(0L);
            doThrow(new DataIntegrityViolationException("duplicate"))
                    .when(memberRepository).insertFavorite(1L, 10L);

            memberService.toggleFavorite(1L, 10L);

            verify(memberRepository).insertFavorite(1L, 10L);
            verify(memberRepository, never()).deleteFavorite(anyLong(), anyLong());
        }

        @Test
        @DisplayName("MEM-012 お気に入り削除は中間テーブルの削除を依頼する")
        void removeFavorite_deletes() {
            memberService.removeFavorite(1L, 10L);

            verify(memberRepository).deleteFavorite(1L, 10L);
        }
    }

    @Nested
    @DisplayName("一覧・ページング")
    class Paging {

        @Test
        @DisplayName("追加お届け先一覧はページ番号を1以上へ補正する")
        void findAdditionalAddresses_normalizesPage() {
            when(memberRepository.findAdditionalAddresses(1L, 1, 20))
                    .thenReturn(new MemberAdditionalAddressPage(List.of(), 0, 1, 20));

            MemberAdditionalAddressPage page = memberService.findAdditionalAddresses(1L, 0);

            assertThat(page.page()).isEqualTo(1);
            verify(memberRepository).findAdditionalAddresses(1L, 1, 20);
        }

        @Test
        @DisplayName("お気に入り一覧はページ番号を1以上へ補正する")
        void findFavorites_normalizesPage() {
            when(memberRepository.findFavorites(1L, 1, 20))
                    .thenReturn(new MemberFavoritePage(List.of(), 0, 1, 20));

            MemberFavoritePage page = memberService.findFavorites(1L, 0);

            assertThat(page.page()).isEqualTo(1);
            verify(memberRepository).findFavorites(1L, 1, 20);
        }
    }

    @Nested
    @DisplayName("会員種別・メール重複判定")
    class MemberTypeAndEmail {

        @Test
        @DisplayName("会員種別を取得する")
        void findMemberTypeById_returnsType() {
            when(memberRepository.findMemberTypeById(1L)).thenReturn(MemberType.CORPORATE);

            assertThat(memberService.findMemberTypeById(1L)).isEqualTo(MemberType.CORPORATE);
        }

        @Test
        @DisplayName("メール重複判定はtrimしてから判定する")
        void existsByEmail_trimsEmail() {
            when(memberRepository.existsByEmail("taro@example.com")).thenReturn(true);

            assertThat(memberService.existsByEmail("  taro@example.com  ")).isTrue();
            verify(memberRepository).existsByEmail("taro@example.com");
        }

        @Test
        @DisplayName("空メールは重複なしとして扱う")
        void existsByEmail_blank_returnsFalse() {
            assertThat(memberService.existsByEmail("   ")).isFalse();
            assertThat(memberService.existsByEmail(null)).isFalse();
            verify(memberRepository, never()).existsByEmail(anyString());
        }
    }
}