package jp.co.skig.officeorder.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.OffsetDateTime;

import jp.co.skig.officeorder.common.MoneyFormatter;
import jp.co.skig.officeorder.common.AppTimeProvider;
import jp.co.skig.officeorder.mapper.row.MemberColorCodeMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberFavoriteProductMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberProfileEditMapperRow;
import jp.co.skig.officeorder.mapper.MemberMapper;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressPage;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressView;
import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.model.member.MemberFavoritePage;
import jp.co.skig.officeorder.model.member.MemberFavoriteView;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import jp.co.skig.officeorder.model.member.MemberType;
import org.springframework.stereotype.Repository;

/**
 * 会員関連データの取得・更新と画面表示用整形を担当するリポジトリ。
 *
 * <p>会員認証情報、プロフィール、追加お届け先、お気に入りの永続化アクセスをまとめて扱う。
 */
@Repository
public class MemberRepository {

    /** 会員SQLを呼び出す MyBatis Mapper。 */
    private final MemberMapper memberMapper;
    /** 時刻依存条件に使う共通時刻プロバイダ。 */
    private final AppTimeProvider appTimeProvider;

    /**
     * 会員リポジトリを生成する。
     *
     * @param memberMapper 会員Mapper
     * @param appTimeProvider 共通時刻プロバイダ
     */
    public MemberRepository(MemberMapper memberMapper, AppTimeProvider appTimeProvider) {
        this.memberMapper = memberMapper;
        this.appTimeProvider = appTimeProvider;
    }

    /**
     * メールアドレスの登録有無を判定する。
     *
     * @param email メールアドレス
     * @return 登録済みなら {@code true}
     */
    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(memberMapper.existsByEmail(email));
    }

    /**
     * 指定会員以外に同じメールアドレスが存在するか判定する。
     *
     * @param email メールアドレス
     * @param memberId 除外対象会員ID
     * @return 重複があれば {@code true}
     */
    public boolean existsByEmailForOtherMember(String email, long memberId) {
        return Boolean.TRUE.equals(memberMapper.existsByEmailForOtherMember(email, memberId));
    }

    /**
     * ログイン可能な会員認証情報を取得する。
     *
     * @param email メールアドレス
     * @return 認証情報
     */
    public Optional<MemberCredential> findActiveCredentialByEmail(String email) {
        MemberCredential credential = memberMapper.selectActiveCredentialByEmail(email);
        if (credential == null) {
            return Optional.empty();
        }
        return Optional.of(credential);
    }

    /**
     * 会員IDから有効会員のセッション情報を取得する。
     *
     * @param memberId 会員ID
     * @return セッション用会員情報
     */
    public Optional<MemberSessionUser> findActiveById(long memberId) {
        MemberSessionUser sessionUser = memberMapper.selectActiveById(memberId);
        if (sessionUser == null) {
            return Optional.empty();
        }
        return Optional.of(sessionUser);
    }

    /**
     * 会員情報変更画面の初期フォームを取得する。
     *
     * @param memberId 会員ID
     * @return フォーム初期値
     */
    public Optional<MemberProfileEditForm> findProfileByMemberId(long memberId) {
        MemberProfileEditMapperRow row = memberMapper.selectProfileByMemberId(memberId);
        if (row == null) {
            return Optional.empty();
        }
        MemberProfileEditForm form = new MemberProfileEditForm();
        form.setPersonalOrCorporate(row.personalOrCorporate());
        form.setLastName(row.lastName());
        form.setFirstName(row.firstName());
        form.setLastNameKana(row.lastNameKana());
        form.setFirstNameKana(row.firstNameKana());
        form.setCompanyName(row.companyName());
        form.setDepartmentName(row.departmentName());
        form.setEmail(row.email());
        form.setGender(row.gender());
        form.setAnniversaryDate(row.anniversaryDate());
        form.setNewsletterOptIn(row.newsletterOptIn());
        if (row.postalCode() != null && row.postalCode().length() == 7) {
            form.setPostalCodePart1(row.postalCode().substring(0, 3));
            form.setPostalCodePart2(row.postalCode().substring(3));
        }
        form.setPrefecture(row.prefecture());
        form.setCity(row.city());
        form.setAddressLine(row.addressLine());
        form.setDeliveryFloor(row.deliveryFloor() == null ? null : String.valueOf(row.deliveryFloor()));
        form.setHasElevator(row.hasElevator());
        form.setDaytimePhone(row.daytimePhone());
        form.setFax(row.fax());
        return Optional.of(form);
    }

    /**
     * 会員を登録する。
     *
     * @param form 会員登録フォーム
     * @param passwordHash ハッシュ化済みパスワード
     * @return 採番された会員ID
     */
    public long insertMember(MemberRegisterForm form, String passwordHash) {
        Long memberId = memberMapper.insertMember(form, passwordHash);
        if (memberId == null) {
            throw new IllegalStateException("会員登録に失敗しました。");
        }
        return memberId;
    }

    /**
     * 会員プロフィールを更新する。
     *
     * @param memberId 会員ID
     * @param form 更新フォーム
     * @return 更新成功時は {@code true}
     */
    public boolean updateProfile(long memberId, MemberProfileEditForm form) {
        return memberMapper.updateProfile(memberId, form) > 0;
    }

    /**
     * 会員を退会状態へ更新する。
     *
     * @param memberId 会員ID
     * @return 更新成功時は {@code true}
     */
    public boolean withdrawMember(long memberId) {
        OffsetDateTime now = appTimeProvider.nowOffsetDateTime();
        return memberMapper.withdrawMember(memberId, now) > 0;
    }

    /**
     * 会員種別を取得する。
     *
     * @param memberId 会員ID
     * @return 会員種別
     */
    public MemberType findMemberTypeById(long memberId) {
        return MemberType.fromDbValue(memberMapper.selectMemberTypeById(memberId));
    }

    /**
     * 追加お届け先一覧をページング付きで取得する。
     *
     * @param memberId 会員ID
     * @param page ページ番号
     * @param size ページサイズ
     * @return 追加お届け先ページ
     */
    public MemberAdditionalAddressPage findAdditionalAddresses(long memberId, int page, int size) {
        long totalCount = Optional.ofNullable(memberMapper.countAdditionalAddresses(memberId)).orElse(0L);
        int offset = Math.max(0, (page - 1) * size);
        List<MemberAdditionalAddressView> items = memberMapper.selectAdditionalAddresses(memberId, size, offset);
        return new MemberAdditionalAddressPage(items, totalCount, page, size);
    }

    /**
     * 指定追加お届け先を取得する。
     *
     * @param memberId 会員ID
     * @param memberAddressId 追加お届け先ID
     * @return 追加お届け先
     */
    public Optional<MemberAdditionalAddressView> findAdditionalAddressById(long memberId, long memberAddressId) {
        MemberAdditionalAddressView row = memberMapper.selectAdditionalAddressById(memberId, memberAddressId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(row);
    }

    /**
     * 追加お届け先件数を取得する。
     *
     * @param memberId 会員ID
     * @return 登録件数
     */
    public long countAdditionalAddresses(long memberId) {
        return Optional.ofNullable(memberMapper.countAdditionalAddresses(memberId)).orElse(0L);
    }

    /**
     * 追加お届け先を登録する。
     *
     * @param memberId 会員ID
     * @param form 登録フォーム
     */
    public void insertAdditionalAddress(long memberId, MemberAdditionalAddressForm form) {
        memberMapper.insertAdditionalAddress(memberId, form);
    }

    /**
     * 追加お届け先を更新する。
     *
     * @param memberId 会員ID
     * @param memberAddressId 追加お届け先ID
     * @param form 更新フォーム
     * @return 更新成功時は {@code true}
     */
    public boolean updateAdditionalAddress(long memberId, long memberAddressId, MemberAdditionalAddressForm form) {
        return memberMapper.updateAdditionalAddress(memberId, memberAddressId, form) > 0;
    }

    /**
     * 追加お届け先を削除する。
     *
     * @param memberId 会員ID
     * @param memberAddressId 追加お届け先ID
     */
    public void deleteAdditionalAddress(long memberId, long memberAddressId) {
        memberMapper.deleteAdditionalAddress(memberId, memberAddressId);
    }

    /**
     * お気に入り一覧を取得し、色コード付き表示用モデルへ整形する。
     *
     * @param memberId 会員ID
     * @param page ページ番号
     * @param size ページサイズ
     * @return お気に入りページ
     */
    public MemberFavoritePage findFavorites(long memberId, int page, int size) {
        var now = appTimeProvider.nowOffsetDateTime();
        long totalCount = Optional.ofNullable(memberMapper.countFavorites(memberId, now)).orElse(0L);
        int offset = Math.max(0, (page - 1) * size);
        List<MemberFavoriteProductMapperRow> rows = memberMapper.selectFavorites(memberId, size, offset, now);
        Map<Long, List<String>> colorCodesByProductId = findFavoriteColorCodes(rows.stream()
                .map(MemberFavoriteProductMapperRow::productId)
                .toList());
        List<MemberFavoriteView> items = rows.stream()
                .map(row -> toFavoriteView(row, colorCodesByProductId.getOrDefault(row.productId(), List.of())))
                .toList();
        return new MemberFavoritePage(items, totalCount, page, size);
    }

    /**
     * お気に入り件数を取得する。
     *
     * @param memberId 会員ID
     * @return お気に入り件数
     */
    public long countFavorites(long memberId) {
        return Optional.ofNullable(memberMapper.countFavorites(memberId, appTimeProvider.nowOffsetDateTime()))
                .orElse(0L);
    }

    /**
     * 指定商品がお気に入り済みか判定する。
     *
     * @param memberId 会員ID
     * @param productId 商品ID
     * @return お気に入り済みなら {@code true}
     */
    public boolean existsFavorite(long memberId, long productId) {
        return Boolean.TRUE.equals(memberMapper.existsFavorite(memberId, productId));
    }

    /**
     * お気に入りを登録する。
     *
     * @param memberId 会員ID
     * @param productId 商品ID
     */
    public void insertFavorite(long memberId, long productId) {
        memberMapper.insertFavorite(memberId, productId);
    }

    /**
     * お気に入りを削除する。
     *
     * @param memberId 会員ID
     * @param productId 商品ID
     */
    public void deleteFavorite(long memberId, long productId) {
        memberMapper.deleteFavorite(memberId, productId);
    }

    /**
     * お気に入り商品行を画面表示用へ変換する。
     *
     * @param row 商品行
     * @param colorCodes カラーコード一覧
     * @return 表示用お気に入り行
     */
    private MemberFavoriteView toFavoriteView(MemberFavoriteProductMapperRow row, List<String> colorCodes) {
        boolean inStock = row.maxStock() > 0;
        String detailUrl = "/products/" + row.productId() + (inStock ? "" : "?stock=out");
        return new MemberFavoriteView(
                row.productId(),
                row.productName(),
                MoneyFormatter.formatYen(row.minPrice()),
                colorCodes,
                row.productCode(),
                inStock,
                detailUrl
        );
    }

    /**
     * お気に入り一覧に表示するカラーコードを商品単位で取得する。
     *
     * @param productIds 商品ID一覧
     * @return 商品IDごとのカラーコード一覧
     */
    private Map<Long, List<String>> findFavoriteColorCodes(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<MemberColorCodeMapperRow> rows = memberMapper.selectFavoriteColorCodes(productIds);
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (MemberColorCodeMapperRow row : rows) {
            if (row.colorCode() == null) {
                continue;
            }
            result.computeIfAbsent(row.productId(), ignored -> new ArrayList<>()).add(row.colorCode());
        }
        return result;
    }
}

