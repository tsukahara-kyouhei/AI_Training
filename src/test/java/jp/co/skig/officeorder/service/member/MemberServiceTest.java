package jp.co.skig.officeorder.service.member;

import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberFavoritePage;
import jp.co.skig.officeorder.model.member.MemberFavoriteView;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.repository.MemberRepository;
import jp.co.skig.officeorder.testutil.TestMessageSourceFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberAdditionalAddressForm;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberProfileEditForm;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberRegisterForm;
import static jp.co.skig.officeorder.testutil.MemberTestFixtures.memberSessionUser;

class MemberServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final MemberService service = new MemberService(
            memberRepository,
            passwordEncoder,
            TestMessageSourceFactory.create()
    );

    /**
     * 空文字や空白だけのメールアドレスでは、認証情報検索を行わず空の結果を返すことを確認する。
     */
    @Test
    void findActiveCredentialByEmail_returnsEmptyWhenEmailIsBlank() {
        assertThat(service.findActiveCredentialByEmail("  ")).isEmpty();
        verify(memberRepository, never()).findActiveCredentialByEmail(any());
    }

    /**
     * register で入力正規化とパスワードハッシュ化を行い、登録後に取得した会員情報を返すことを確認する。
     */
    @Test
    void register_normalizesInputEncodesPasswordAndReturnsRegisteredMember() {
        var form = memberRegisterForm();
        var expected = memberSessionUser(10L, "member@example.com", "山田", "花子");
        when(memberRepository.existsByEmail("member@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password!1")).thenReturn("hashed-password");
        when(memberRepository.insertMember(any(MemberRegisterForm.class), eq("hashed-password"))).thenReturn(10L);
        when(memberRepository.findActiveById(10L)).thenReturn(Optional.of(expected));

        MemberSessionUser actual = service.register(form);

        assertThat(actual).isEqualTo(expected);
        ArgumentCaptor<MemberRegisterForm> captor = ArgumentCaptor.forClass(MemberRegisterForm.class);
        verify(memberRepository).insertMember(captor.capture(), eq("hashed-password"));
        assertThat(captor.getValue().getEmail()).isEqualTo("member@example.com");
        assertThat(captor.getValue().getCompanyName()).isNull();
    }

    /**
     * register で事前重複が分かった場合は、登録処理に進まず重複例外を返すことを確認する。
     */
    @Test
    void register_throwsDuplicateEmailExceptionWhenEmailAlreadyExists() {
        var form = memberRegisterForm();
        when(memberRepository.existsByEmail("member@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("メールアドレスが重複しています。");

        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).insertMember(any(), any());
    }

    /**
     * register 中の一意制約違反がメールアドレス競合によるものだった場合は、重複例外へ読み替えることを確認する。
     */
    @Test
    void register_translatesDuplicateRaceToDuplicateEmailException() {
        var form = memberRegisterForm();
        when(memberRepository.existsByEmail("member@example.com")).thenReturn(false, true);
        when(passwordEncoder.encode("Password!1")).thenReturn("hashed-password");
        when(memberRepository.insertMember(any(MemberRegisterForm.class), eq("hashed-password")))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.register(form))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("メールアドレスが重複しています。");
    }

    /**
     * updateProfile で他会員とメールアドレスが重複する場合は、更新せず重複例外を返すことを確認する。
     */
    @Test
    void updateProfile_throwsDuplicateEmailExceptionWhenEmailBelongsToOtherMember() {
        var form = memberProfileEditForm();
        when(memberRepository.existsByEmailForOtherMember("updated@example.com", 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateProfile(3L, form))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("メールアドレスが重複しています。");

        verify(memberRepository, never()).updateProfile(any(Long.class), any(MemberProfileEditForm.class));
    }

    /**
     * createAdditionalAddress で上限件数に達している場合は、新規登録を拒否することを確認する。
     */
    @Test
    void createAdditionalAddress_throwsWhenAddressLimitReached() {
        when(memberRepository.countAdditionalAddresses(7L)).thenReturn((long) MemberService.ADDITIONAL_ADDRESS_LIMIT);

        assertThatThrownBy(() -> service.createAdditionalAddress(7L, memberAdditionalAddressForm()))
                .isInstanceOf(AddressLimitExceededException.class)
                .hasMessage("これ以上お届け先を追加できません。");

        verify(memberRepository, never())
                .insertAdditionalAddress(any(Long.class), any(MemberAdditionalAddressForm.class));
    }

    /**
     * お気に入り済みの商品で toggleFavorite を呼ぶと、追加ではなく削除として扱われることを確認する。
     */
    @Test
    void toggleFavorite_removesExistingFavorite() {
        when(memberRepository.existsFavorite(5L, 99L)).thenReturn(true);

        service.toggleFavorite(5L, 99L);

        verify(memberRepository).deleteFavorite(5L, 99L);
        verify(memberRepository, never()).insertFavorite(any(Long.class), any(Long.class));
    }

    /**
     * 未登録のお気に入りは、上限未満であれば新規追加されることを確認する。
     */
    @Test
    void toggleFavorite_addsFavoriteWhenBelowLimit() {
        when(memberRepository.existsFavorite(5L, 99L)).thenReturn(false);
        when(memberRepository.countFavorites(5L)).thenReturn(10L);

        service.toggleFavorite(5L, 99L);

        verify(memberRepository).insertFavorite(5L, 99L);
    }

    /**
     * 未登録のお気に入りでも、上限件数に達している場合は追加を拒否することを確認する。
     */
    @Test
    void toggleFavorite_throwsWhenFavoriteLimitReached() {
        when(memberRepository.existsFavorite(5L, 99L)).thenReturn(false);
        when(memberRepository.countFavorites(5L)).thenReturn((long) MemberService.FAVORITES_LIMIT);

        assertThatThrownBy(() -> service.toggleFavorite(5L, 99L))
                .isInstanceOf(FavoritesLimitExceededException.class)
                .hasMessage("これ以上お気に入りに追加できません。");

        verify(memberRepository, never()).insertFavorite(any(Long.class), any(Long.class));
    }

    /**
     * お気に入り追加の競合で一意制約違反になっても、再確認で既存登録済みなら成功扱いで終えることを確認する。
     */
    @Test
    void toggleFavorite_ignoresDuplicateRaceWhenFavoriteAlreadyExistsAfterInsertFailure() {
        when(memberRepository.existsFavorite(5L, 99L)).thenReturn(false, true);
        when(memberRepository.countFavorites(5L)).thenReturn(0L);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("duplicate"))
                .when(memberRepository)
                .insertFavorite(5L, 99L);

        assertThatNoException().isThrownBy(() -> service.toggleFavorite(5L, 99L));
    }

    /**
     * findFavorites では負のページ番号が渡されても1ページ目へ補正して一覧取得することを確認する。
     */
    @Test
    void findFavorites_normalizesPageToOneBeforeDelegating() {
        MemberFavoritePage expected = new MemberFavoritePage(
                List.of(new MemberFavoriteView(1L, "Nordis ワークデスク", "12,000円", List.of("#ffffff"),
                        "P0001-C01", true, "/products/1")),
                1,
                1,
                MemberService.MYPAGE_PAGE_SIZE
        );
        when(memberRepository.findFavorites(4L, 1, MemberService.MYPAGE_PAGE_SIZE)).thenReturn(expected);

        MemberFavoritePage actual = service.findFavorites(4L, -1);

        assertThat(actual).isSameAs(expected);
    }
}
