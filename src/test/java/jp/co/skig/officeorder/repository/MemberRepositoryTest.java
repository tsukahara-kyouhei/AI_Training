package jp.co.skig.officeorder.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.MemberMapper;
import jp.co.skig.officeorder.mapper.row.MemberColorCodeMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberFavoriteProductMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberProfileEditMapperRow;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressPage;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressView;
import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.model.member.MemberFavoritePage;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 会員リポジトリの単体テスト。
 *
 * <p>単体テスト計画 4.6 リポジトリ・データ変換に対応する。
 */
class MemberRepositoryTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-03T06:00:00Z");
    private static final OffsetDateTime FIXED_NOW = OffsetDateTime.ofInstant(FIXED_INSTANT, TOKYO);

    private MemberMapper memberMapper;
    private AppTimeProvider appTimeProvider;
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberMapper = mock(MemberMapper.class);
        appTimeProvider = mock(AppTimeProvider.class);
        when(appTimeProvider.nowOffsetDateTime()).thenReturn(FIXED_NOW);
        memberRepository = new MemberRepository(memberMapper, appTimeProvider);
    }

    @Nested
    @DisplayName("認証情報")
    class Credential {

        @Test
        @DisplayName("認証情報はMapper行をOptionalで返す")
        void findActiveCredentialByEmail_returnsCredential() {
            MemberCredential credential = new MemberCredential(
                    1L, "taro@example.com", "山田", "太郎", "hashed"
            );
            when(memberMapper.selectActiveCredentialByEmail("taro@example.com")).thenReturn(credential);

            Optional<MemberCredential> result = memberRepository.findActiveCredentialByEmail("taro@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().memberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("認証情報は0件ならOptional.emptyを返す")
        void findActiveCredentialByEmail_notFound_returnsEmpty() {
            when(memberMapper.selectActiveCredentialByEmail("taro@example.com")).thenReturn(null);

            assertThat(memberRepository.findActiveCredentialByEmail("taro@example.com")).isEmpty();
        }
    }

    @Nested
    @DisplayName("会員登録")
    class Register {

        @Test
        @DisplayName("insertMemberはMapperが返した会員IDを返す")
        void insertMember_returnsMemberId() {
            when(memberMapper.insertMember(any(MemberRegisterForm.class), anyString())).thenReturn(1L);

            long memberId = memberRepository.insertMember(new MemberRegisterForm(), "hashed");

            assertThat(memberId).isEqualTo(1L);
        }

        @Test
        @DisplayName("insertMemberはMapperがnullを返したら例外を送出する")
        void insertMember_nullMemberId_throws() {
            when(memberMapper.insertMember(any(MemberRegisterForm.class), anyString())).thenReturn(null);

            assertThatThrownBy(() -> memberRepository.insertMember(new MemberRegisterForm(), "hashed"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("会員登録");
        }
    }

    @Nested
    @DisplayName("プロフィール")
    class Profile {

        @Test
        @DisplayName("プロフィール取得はMapper行を編集フォームへ変換する")
        void findProfileByMemberId_convertsRow() {
            MemberProfileEditMapperRow row = new MemberProfileEditMapperRow(
                    "personal", "山田", "太郎", "ヤマダ", "タロウ",
                    null, null, "taro@example.com", "male",
                    java.time.LocalDate.of(1990, 1, 1), true,
                    "1000001", "東京都", "千代田区", "丸の内1-1-1",
                    3, true, "0312345678", null
            );
            when(memberMapper.selectProfileByMemberId(1L)).thenReturn(row);

            Optional<MemberProfileEditForm> result = memberRepository.findProfileByMemberId(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getLastName()).isEqualTo("山田");
            assertThat(result.get().getPostalCodePart1()).isEqualTo("100");
            assertThat(result.get().getPostalCodePart2()).isEqualTo("0001");
            assertThat(result.get().getDeliveryFloor()).isEqualTo("3");
        }

        @Test
        @DisplayName("プロフィールは0件ならOptional.emptyを返す")
        void findProfileByMemberId_notFound_returnsEmpty() {
            when(memberMapper.selectProfileByMemberId(1L)).thenReturn(null);

            assertThat(memberRepository.findProfileByMemberId(1L)).isEmpty();
        }

        @Test
        @DisplayName("プロフィールの郵便番号が7桁でない場合は分割しない")
        void findProfileByMemberId_invalidPostalCode_doesNotSplit() {
            MemberProfileEditMapperRow row = new MemberProfileEditMapperRow(
                    "personal", "山田", "太郎", "ヤマダ", "タロウ",
                    null, null, "taro@example.com", "male",
                    java.time.LocalDate.of(1990, 1, 1), true,
                    "12345", "東京都", "千代田区", "丸の内1-1-1",
                    3, true, "0312345678", null
            );
            when(memberMapper.selectProfileByMemberId(1L)).thenReturn(row);

            Optional<MemberProfileEditForm> result = memberRepository.findProfileByMemberId(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getPostalCodePart1()).isNull();
            assertThat(result.get().getPostalCodePart2()).isNull();
        }

        @Test
        @DisplayName("updateProfileはMapperの更新件数からbooleanを返す")
        void updateProfile_returnsTrueWhenUpdated() {
            when(memberMapper.updateProfile(anyLong(), any(MemberProfileEditForm.class))).thenReturn(1);

            boolean updated = memberRepository.updateProfile(1L, new MemberProfileEditForm());

            assertThat(updated).isTrue();
        }

        @Test
        @DisplayName("updateProfileは0件ならfalseを返す")
        void updateProfile_returnsFalseWhenNotUpdated() {
            when(memberMapper.updateProfile(anyLong(), any(MemberProfileEditForm.class))).thenReturn(0);

            boolean updated = memberRepository.updateProfile(1L, new MemberProfileEditForm());

            assertThat(updated).isFalse();
        }

        @Test
        @DisplayName("退会はMapperへ現在時刻を渡して更新を依頼する")
        void withdrawMember_passesNow() {
            when(memberMapper.withdrawMember(1L, FIXED_NOW)).thenReturn(1);

            boolean withdrawn = memberRepository.withdrawMember(1L);

            assertThat(withdrawn).isTrue();
            verify(memberMapper).withdrawMember(1L, FIXED_NOW);
        }
    }

    @Nested
    @DisplayName("会員種別")
    class MemberType {

        @Test
        @DisplayName("会員種別はDB値を列挙値へ変換する")
        void findMemberTypeById_convertsDbValue() {
            when(memberMapper.selectMemberTypeById(1L)).thenReturn("corporate");

            assertThat(memberRepository.findMemberTypeById(1L))
                    .isEqualTo(jp.co.skig.officeorder.model.member.MemberType.CORPORATE);
        }

        @Test
        @DisplayName("DB値が不明な場合はPERSONALを返す")
        void findMemberTypeById_unknownValue_returnsPersonal() {
            when(memberMapper.selectMemberTypeById(1L)).thenReturn("unknown");

            assertThat(memberRepository.findMemberTypeById(1L))
                    .isEqualTo(jp.co.skig.officeorder.model.member.MemberType.PERSONAL);
        }
    }

    @Nested
    @DisplayName("追加お届け先")
    class AdditionalAddress {

        @Test
        @DisplayName("追加お届け先一覧はMapper行と件数をページへ変換する")
        void findAdditionalAddresses_convertsRows() {
            MemberAdditionalAddressView view = new MemberAdditionalAddressView(
                    1L, 1L, "山田", "太郎", "ヤマダ", "タロウ",
                    null, null, "1000001", "東京都", "千代田区", "丸の内1-1-1",
                    3, true, "0312345678", null, FIXED_NOW
            );
            when(memberMapper.countAdditionalAddresses(1L)).thenReturn(1L);
            when(memberMapper.selectAdditionalAddresses(1L, 20, 0)).thenReturn(List.of(view));

            MemberAdditionalAddressPage page = memberRepository.findAdditionalAddresses(1L, 1, 20);

            assertThat(page.totalCount()).isEqualTo(1L);
            assertThat(page.items()).hasSize(1);
            assertThat(page.items().getFirst().fullName()).isEqualTo("山田 太郎");
        }

        @Test
        @DisplayName("追加お届け先一覧はcountがnullなら0件として扱う")
        void findAdditionalAddresses_nullCount_treatsAsZero() {
            when(memberMapper.countAdditionalAddresses(1L)).thenReturn(null);
            when(memberMapper.selectAdditionalAddresses(1L, 20, 0)).thenReturn(List.of());

            MemberAdditionalAddressPage page = memberRepository.findAdditionalAddresses(1L, 1, 20);

            assertThat(page.totalCount()).isZero();
        }

        @Test
        @DisplayName("追加お届け先件数はnullなら0件として扱う")
        void countAdditionalAddresses_null_returnsZero() {
            when(memberMapper.countAdditionalAddresses(1L)).thenReturn(null);

            assertThat(memberRepository.countAdditionalAddresses(1L)).isZero();
        }
    }

    @Nested
    @DisplayName("お気に入り")
    class Favorites {

        @Test
        @DisplayName("お気に入り一覧はMapper行を表示用モデルへ変換する")
        void findFavorites_convertsRows() {
            MemberFavoriteProductMapperRow row = new MemberFavoriteProductMapperRow(
                    10L, "デスク", new BigDecimal("30000"), 5, "P0001-C01"
            );
            when(memberMapper.countFavorites(1L, FIXED_NOW)).thenReturn(1L);
            when(memberMapper.selectFavorites(1L, 20, 0, FIXED_NOW)).thenReturn(List.of(row));
            when(memberMapper.selectFavoriteColorCodes(List.of(10L))).thenReturn(List.of());

            MemberFavoritePage page = memberRepository.findFavorites(1L, 1, 20);

            assertThat(page.totalCount()).isEqualTo(1L);
            assertThat(page.items()).hasSize(1);
            assertThat(page.items().getFirst().productName()).isEqualTo("デスク");
            assertThat(page.items().getFirst().inStock()).isTrue();
        }

        @Test
        @DisplayName("お気に入り一覧は在庫0なら在庫切れURLへ変換する")
        void findFavorites_zeroStock_marksOutOfStock() {
            MemberFavoriteProductMapperRow row = new MemberFavoriteProductMapperRow(
                    10L, "デスク", new BigDecimal("30000"), 0, "P0001-C01"
            );
            when(memberMapper.countFavorites(1L, FIXED_NOW)).thenReturn(1L);
            when(memberMapper.selectFavorites(1L, 20, 0, FIXED_NOW)).thenReturn(List.of(row));
            when(memberMapper.selectFavoriteColorCodes(List.of(10L))).thenReturn(List.of());

            MemberFavoritePage page = memberRepository.findFavorites(1L, 1, 20);

            assertThat(page.items().getFirst().inStock()).isFalse();
            assertThat(page.items().getFirst().detailUrl()).isEqualTo("/products/10?stock=out");
        }

        @Test
        @DisplayName("お気に入り一覧はcolorCodeがnullならスキップする")
        void findFavorites_skipsNullColorCodes() throws Exception {
            MemberFavoriteProductMapperRow row = new MemberFavoriteProductMapperRow(
                    10L, "デスク", new BigDecimal("30000"), 5, "P0001-C01"
            );
            when(memberMapper.countFavorites(1L, FIXED_NOW)).thenReturn(1L);
            when(memberMapper.selectFavorites(1L, 20, 0, FIXED_NOW)).thenReturn(List.of(row));
            when(memberMapper.selectFavoriteColorCodes(List.of(10L)))
                    .thenReturn(List.of(new MemberColorCodeMapperRow(10L, null, 1)));

            MemberFavoritePage page = memberRepository.findFavorites(1L, 1, 20);

            assertThat(page.items().getFirst().colorCodes()).isEmpty();
        }

        @Test
        @DisplayName("お気に入り件数はnullなら0件として扱う")
        void countFavorites_null_returnsZero() {
            when(memberMapper.countFavorites(1L, FIXED_NOW)).thenReturn(null);

            assertThat(memberRepository.countFavorites(1L)).isZero();
        }

        @Test
        @DisplayName("existsFavoriteはBoolean.TRUEをtrueへ変換する")
        void existsFavorite_true() {
            when(memberMapper.existsFavorite(1L, 10L)).thenReturn(Boolean.TRUE);

            assertThat(memberRepository.existsFavorite(1L, 10L)).isTrue();
        }

        @Test
        @DisplayName("existsFavoriteはnullをfalseへ変換する")
        void existsFavorite_null_returnsFalse() {
            when(memberMapper.existsFavorite(1L, 10L)).thenReturn(null);

            assertThat(memberRepository.existsFavorite(1L, 10L)).isFalse();
        }
    }
}