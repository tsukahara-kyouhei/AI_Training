package jp.co.skig.officeorder.service.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
        // lenient() を追加して未使用時でもエラーにならないようにする
        org.mockito.Mockito.lenient()
                .when(messageSource.getMessage(anyString(), any(), any()))
                .thenReturn("メッセージテキスト");
        
        memberService = new MemberService(memberRepository, passwordEncoder, messageSource);
    }

    @Nested
    @DisplayName("findActiveCredentialByEmailのテスト")
    class FindActiveCredentialByEmailTest {

        @Test
        @DisplayName("メールアドレスがNullまたは空文字の場合はEmptyを返すこと")
        void shouldReturnEmptyWhenEmailIsEmpty() {
            assertThat(memberService.findActiveCredentialByEmail(null)).isEmpty();
            assertThat(memberService.findActiveCredentialByEmail("   ")).isEmpty();
        }

        @Test
        @DisplayName("有効なメールアドレスの場合はトリムしてリポジトリを呼び出すこと")
        void shouldReturnCredentialWhenEmailIsValid() {
            MemberCredential credential = mock(MemberCredential.class);
            when(memberRepository.findActiveCredentialByEmail("test@example.com")).thenReturn(Optional.of(credential));

            Optional<MemberCredential> result = memberService.findActiveCredentialByEmail(" test@example.com ");

            assertThat(result).contains(credential);
            verify(memberRepository).findActiveCredentialByEmail("test@example.com");
        }
    }

    @Nested
    @DisplayName("findActiveByIdのテスト")
    class FindActiveByIdTest {

        @Test
        @DisplayName("会員IDから有効な会員情報を取得できること")
        void shouldReturnMemberSessionUser() {
            MemberSessionUser user = mock(MemberSessionUser.class);
            when(memberRepository.findActiveById(1L)).thenReturn(Optional.of(user));

            Optional<MemberSessionUser> result = memberService.findActiveById(1L);

            assertThat(result).contains(user);
        }
    }

    @Nested
    @DisplayName("existsByEmailのテスト")
    class ExistsByEmailTest {

        @Test
        @DisplayName("メールアドレスがNullまたは空文字の場合はfalseを返すこと")
        void shouldReturnFalseWhenEmailIsEmpty() {
            assertThat(memberService.existsByEmail(null)).isFalse();
            assertThat(memberService.existsByEmail("")).isFalse();
        }

        @Test
        @DisplayName("トリムされたメールアドレスで存在チェックを実行すること")
        void shouldCheckExistenceWithTrimmedEmail() {
            when(memberRepository.existsByEmail("test@example.com")).thenReturn(true);

            boolean result = memberService.existsByEmail(" test@example.com ");

            assertThat(result).isTrue();
            verify(memberRepository).existsByEmail("test@example.com");
        }
    }

    @Nested
    @DisplayName("registerのテスト")
    class RegisterTest {

        @Test
        @DisplayName("事前チェックでメールアドレスが重複している場合はDuplicateEmailExceptionを投げること")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            MemberRegisterForm form = mock(MemberRegisterForm.class);
            when(form.normalize()).thenReturn(form);
            when(form.getEmail()).thenReturn("test@example.com");
            when(memberRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThatThrownBy(() -> memberService.register(form))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("DB挿入時のRace Conditionで重複が検知された場合はDuplicateEmailExceptionを投げること")
        void shouldThrowExceptionWhenDataIntegrityViolationOccursWithDuplicateEmail() {
            MemberRegisterForm form = mock(MemberRegisterForm.class);
            when(form.normalize()).thenReturn(form);
            when(form.getEmail()).thenReturn("test@example.com");
            when(form.getPassword()).thenReturn("rawPassword");
            when(memberRepository.existsByEmail("test@example.com")).thenReturn(false).thenReturn(true);
            when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
            when(memberRepository.insertMember(form, "hashedPassword"))
                    .thenThrow(new DataIntegrityViolationException("重複キーエラー"));

            assertThatThrownBy(() -> memberService.register(form))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("DB挿入時のエラーがメール重複以外の場合は例外を再スローすること")
        void shouldReThrowExceptionWhenDataIntegrityViolationOccursWithoutDuplicateEmail() {
            MemberRegisterForm form = mock(MemberRegisterForm.class);
            when(form.normalize()).thenReturn(form);
            when(form.getEmail()).thenReturn("test@example.com");
            when(form.getPassword()).thenReturn("rawPassword");
            when(memberRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
            when(memberRepository.insertMember(form, "hashedPassword"))
                    .thenThrow(new DataIntegrityViolationException("その他制約違反"));

            assertThatThrownBy(() -> memberService.register(form))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("正常に会員登録が成功すること")
        void shouldRegisterSuccessfully() {
            MemberRegisterForm form = mock(MemberRegisterForm.class);
            MemberSessionUser user = mock(MemberSessionUser.class);

            when(form.normalize()).thenReturn(form);
            when(form.getEmail()).thenReturn("test@example.com");
            when(form.getPassword()).thenReturn("rawPassword");
            when(memberRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
            when(memberRepository.insertMember(form, "hashedPassword")).thenReturn(10L);
            when(memberRepository.findActiveById(10L)).thenReturn(Optional.of(user));

            MemberSessionUser result = memberService.register(form);

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("登録直後の会員取得に失敗した場合はIllegalStateExceptionを投げること")
        void shouldThrowExceptionWhenRegisteredUserNotFound() {
            MemberRegisterForm form = mock(MemberRegisterForm.class);
            when(form.normalize()).thenReturn(form);
            when(form.getEmail()).thenReturn("test@example.com");
            when(form.getPassword()).thenReturn("rawPassword");
            when(memberRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
            when(memberRepository.insertMember(form, "hashedPassword")).thenReturn(10L);
            when(memberRepository.findActiveById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.register(form))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("findAdditionalAddressesのテスト")
    class FindAdditionalAddressesTest {

        @Test
        @DisplayName("ページ番号が1未満の場合は1に補正して取得すること")
        void shouldNormalizePageNumberAndFetch() {
            MemberAdditionalAddressPage page = mock(MemberAdditionalAddressPage.class);
            when(memberRepository.findAdditionalAddresses(1L, 1, MemberService.MYPAGE_PAGE_SIZE))
                    .thenReturn(page);

            MemberAdditionalAddressPage result = memberService.findAdditionalAddresses(1L, 0);

            assertThat(result).isEqualTo(page);
        }
    }

    @Nested
    @DisplayName("findAdditionalAddressByIdのテスト")
    class FindAdditionalAddressByIdTest {

        @Test
        @DisplayName("会員IDとアドレスIDに合致するお届け先を取得できること")
        void shouldReturnAddressView() {
            MemberAdditionalAddressView view = mock(MemberAdditionalAddressView.class);
            when(memberRepository.findAdditionalAddressById(1L, 100L)).thenReturn(Optional.of(view));

            Optional<MemberAdditionalAddressView> result = memberService.findAdditionalAddressById(1L, 100L);

            assertThat(result).contains(view);
        }
    }

    @Nested
    @DisplayName("findMemberTypeByIdのテスト")
    class FindMemberTypeByIdTest {

        @Test
        @DisplayName("会員IDに対応する会員種別を取得できること")
        void shouldReturnMemberType() {
            when(memberRepository.findMemberTypeById(1L)).thenReturn(MemberType.PERSONAL);

            MemberType result = memberService.findMemberTypeById(1L);

            assertThat(result).isEqualTo(MemberType.PERSONAL);
        }
    }

    @Nested
    @DisplayName("findProfileByMemberIdのテスト")
    class FindProfileByMemberIdTest {

        @Test
        @DisplayName("会員IDに対応するプロフィール編集フォーム情報を取得できること")
        void shouldReturnProfileEditForm() {
            MemberProfileEditForm form = mock(MemberProfileEditForm.class);
            when(memberRepository.findProfileByMemberId(1L)).thenReturn(Optional.of(form));

            Optional<MemberProfileEditForm> result = memberService.findProfileByMemberId(1L);

            assertThat(result).contains(form);
        }
    }

    @Nested
    @DisplayName("updateProfileのテスト")
    class UpdateProfileTest {

        @Test
        @DisplayName("他会員のメールアドレスと重複している場合はDuplicateEmailExceptionを投げること")
        void shouldThrowExceptionWhenEmailExistsForOtherMember() {
            MemberProfileEditForm form = mock(MemberProfileEditForm.class);
            when(form.normalize()).thenReturn(form);
            when(form.getEmail()).thenReturn("other@example.com");
            when(memberRepository.existsByEmailForOtherMember("other@example.com", 1L)).thenReturn(true);

            assertThatThrownBy(() -> memberService.updateProfile(1L, form))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("更新対象の会員が存在しない場合はIllegalStateExceptionを投げること")
        void shouldThrowExceptionWhenUpdateFails() {
            MemberProfileEditForm form = mock(MemberProfileEditForm.class);
            when(form.normalize()).thenReturn(form);
            when(form.getEmail()).thenReturn("test@example.com");
            when(memberRepository.existsByEmailForOtherMember("test@example.com", 1L)).thenReturn(false);
            when(memberRepository.updateProfile(1L, form)).thenReturn(false);

            assertThatThrownBy(() -> memberService.updateProfile(1L, form))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("正常にプロフィールを更新できること")
        void shouldUpdateProfileSuccessfully() {
            MemberProfileEditForm form = mock(MemberProfileEditForm.class);
            when(form.normalize()).thenReturn(form);
            when(form.getEmail()).thenReturn("test@example.com");
            when(memberRepository.existsByEmailForOtherMember("test@example.com", 1L)).thenReturn(false);
            when(memberRepository.updateProfile(1L, form)).thenReturn(true);

            memberService.updateProfile(1L, form);

            verify(memberRepository).updateProfile(1L, form);
        }
    }

    @Nested
    @DisplayName("withdrawのテスト")
    class WithdrawTest {

        @Test
        @DisplayName("退会処理の実行結果を返すこと")
        void shouldReturnWithdrawResult() {
            when(memberRepository.withdrawMember(1L)).thenReturn(true);
            assertThat(memberService.withdraw(1L)).isTrue();

            when(memberRepository.withdrawMember(2L)).thenReturn(false);
            assertThat(memberService.withdraw(2L)).isFalse();
        }
    }

    @Nested
    @DisplayName("createAdditionalAddressのテスト")
    class CreateAdditionalAddressTest {

        @Test
        @DisplayName("お届け先件数が上限（20件）に達している場合はAddressLimitExceededExceptionを投げること")
        void shouldThrowExceptionWhenLimitReached() {
            MemberAdditionalAddressForm form = mock(MemberAdditionalAddressForm.class);
            when(memberRepository.countAdditionalAddresses(1L)).thenReturn(20L); // 20 から 20L に変更

            assertThatThrownBy(() -> memberService.createAdditionalAddress(1L, form))
                    .isInstanceOf(AddressLimitExceededException.class);

            verify(memberRepository, never()).insertAdditionalAddress(anyLong(), any());
        }

        @Test
        @DisplayName("上限未満の場合はお届け先を正常に追加できること")
        void shouldCreateAddressSuccessfully() {
            MemberAdditionalAddressForm form = mock(MemberAdditionalAddressForm.class);
            MemberAdditionalAddressForm normalizedForm = mock(MemberAdditionalAddressForm.class);
            when(form.normalize()).thenReturn(normalizedForm);
            when(memberRepository.countAdditionalAddresses(1L)).thenReturn(19L); // 19 から 19L に変更

            memberService.createAdditionalAddress(1L, form);

            verify(memberRepository).insertAdditionalAddress(1L, normalizedForm);
        }
    }

    @Nested
    @DisplayName("updateAdditionalAddressのテスト")
    class UpdateAdditionalAddressTest {

        @Test
        @DisplayName("お届け先の更新結果を返すこと")
        void shouldReturnUpdateResult() {
            MemberAdditionalAddressForm form = mock(MemberAdditionalAddressForm.class);
            MemberAdditionalAddressForm normalizedForm = mock(MemberAdditionalAddressForm.class);
            when(form.normalize()).thenReturn(normalizedForm);

            when(memberRepository.updateAdditionalAddress(1L, 10L, normalizedForm)).thenReturn(true);
            assertThat(memberService.updateAdditionalAddress(1L, 10L, form)).isTrue();

            when(memberRepository.updateAdditionalAddress(1L, 20L, normalizedForm)).thenReturn(false);
            assertThat(memberService.updateAdditionalAddress(1L, 20L, form)).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteAdditionalAddressのテスト")
    class DeleteAdditionalAddressTest {

        @Test
        @DisplayName("お届け先を削除できること")
        void shouldDeleteAddress() {
            memberService.deleteAdditionalAddress(1L, 10L);
            verify(memberRepository).deleteAdditionalAddress(1L, 10L);
        }
    }

    @Nested
    @DisplayName("findFavoritesのテスト")
    class FindFavoritesTest {

        @Test
        @DisplayName("ページ番号を1以上に補正してお気に入り一覧を取得すること")
        void shouldNormalizePageAndFindFavorites() {
            MemberFavoritePage page = mock(MemberFavoritePage.class);
            when(memberRepository.findFavorites(1L, 1, MemberService.MYPAGE_PAGE_SIZE)).thenReturn(page);

            MemberFavoritePage result = memberService.findFavorites(1L, -1);

            assertThat(result).isEqualTo(page);
        }
    }

    @Nested
    @DisplayName("isFavoriteのテスト")
    class IsFavoriteTest {

        @Test
        @DisplayName("お気に入り登録状態を正しく判定すること")
        void shouldCheckFavoriteExistence() {
            when(memberRepository.existsFavorite(1L, 100L)).thenReturn(true);
            assertThat(memberService.isFavorite(1L, 100L)).isTrue();
        }
    }

    @Nested
    @DisplayName("toggleFavoriteのテスト")
    class ToggleFavoriteTest {

        @Test
        @DisplayName("すでにお気に入り登録されている場合は削除すること")
        void shouldRemoveWhenAlreadyFavorite() {
            when(memberRepository.existsFavorite(1L, 100L)).thenReturn(true);

            memberService.toggleFavorite(1L, 100L);

            verify(memberRepository).deleteFavorite(1L, 100L);
            verify(memberRepository, never()).insertFavorite(anyLong(), anyLong());
        }

        @Test
        @DisplayName("未登録かつ上限（100件）に達している場合はFavoritesLimitExceededExceptionを投げること")
        void shouldThrowExceptionWhenFavoriteLimitReached() {
            when(memberRepository.existsFavorite(1L, 100L)).thenReturn(false);
            when(memberRepository.countFavorites(1L)).thenReturn(100L); // 100L に修正

            assertThatThrownBy(() -> memberService.toggleFavorite(1L, 100L))
                    .isInstanceOf(FavoritesLimitExceededException.class);
        }

        @Test
        @DisplayName("未登録かつ上限未満の場合はお気に入り追加されること")
        void shouldAddFavoriteSuccessfully() {
            when(memberRepository.existsFavorite(1L, 100L)).thenReturn(false);
            when(memberRepository.countFavorites(1L)).thenReturn(99L); // 99L に修正

            memberService.toggleFavorite(1L, 100L);

            verify(memberRepository).insertFavorite(1L, 100L);
        }

        @Test
        @DisplayName("追加時に競合（DataIntegrityViolationException）が発生しても既に存在していれば正常終了すること")
        void shouldHandleInsertRaceConditionGracefully() {
            when(memberRepository.existsFavorite(1L, 100L)).thenReturn(false).thenReturn(true);
            when(memberRepository.countFavorites(1L)).thenReturn(10L); // 10L に修正
            doThrow(new DataIntegrityViolationException("競合エラー"))
                    .when(memberRepository).insertFavorite(1L, 100L);

            memberService.toggleFavorite(1L, 100L);

            verify(memberRepository).insertFavorite(1L, 100L);
        }
    }

    @Nested
    @DisplayName("removeFavoriteのテスト")
    class RemoveFavoriteTest {

        @Test
        @DisplayName("お気に入りを削除できること")
        void shouldRemoveFavorite() {
            memberService.removeFavorite(1L, 100L);
            verify(memberRepository).deleteFavorite(1L, 100L);
        }
    }
}
