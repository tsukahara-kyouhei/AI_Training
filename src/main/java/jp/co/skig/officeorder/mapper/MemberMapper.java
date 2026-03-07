package jp.co.skig.officeorder.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import jp.co.skig.officeorder.mapper.row.MemberColorCodeMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberFavoriteProductMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberProfileEditMapperRow;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressForm;
import jp.co.skig.officeorder.model.member.MemberAdditionalAddressView;
import jp.co.skig.officeorder.model.member.MemberCredential;
import jp.co.skig.officeorder.model.member.MemberProfileEditForm;
import jp.co.skig.officeorder.model.member.MemberRegisterForm;
import jp.co.skig.officeorder.model.member.MemberSessionUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会員認証、会員情報、追加お届け先、お気に入りの永続化を扱う Mapper。
 */
@Mapper
public interface MemberMapper {

    /**
     * メールアドレスが既存会員に使用されているかを確認する。
     */
    Boolean existsByEmail(@Param("email") String email);

    /**
     * 指定会員自身を除いて、メールアドレスが他会員に使用されているかを確認する。
     */
    Boolean existsByEmailForOtherMember(@Param("email") String email,
                                        @Param("memberId") long memberId);

    /**
     * ログイン認証用に有効会員の資格情報を取得する。
     */
    MemberCredential selectActiveCredentialByEmail(@Param("email") String email);

    /**
     * セッション復元用に有効会員の基本情報を取得する。
     */
    MemberSessionUser selectActiveById(@Param("memberId") long memberId);

    /**
     * 会員情報変更画面に表示する現在のプロフィール情報を取得する。
     */
    MemberProfileEditMapperRow selectProfileByMemberId(@Param("memberId") long memberId);

    /**
     * 会員登録情報を保存し、採番された会員IDを返す。
     */
    Long insertMember(@Param("form") MemberRegisterForm form,
                      @Param("passwordHash") String passwordHash);

    /**
     * 会員種別の判定に必要な区分値を取得する。
     */
    String selectMemberTypeById(@Param("memberId") long memberId);

    /**
     * 追加お届け先の登録件数を取得する。
     */
    Long countAdditionalAddresses(@Param("memberId") long memberId);

    /**
     * 追加お届け先一覧をページ単位で取得する。
     */
    List<MemberAdditionalAddressView> selectAdditionalAddresses(@Param("memberId") long memberId,
                                                                @Param("limit") int limit,
                                                                @Param("offset") int offset);

    /**
     * 追加お届け先の編集や注文入力に使う1件分の情報を取得する。
     */
    MemberAdditionalAddressView selectAdditionalAddressById(@Param("memberId") long memberId,
                                                            @Param("memberAddressId") long memberAddressId);

    /**
     * 新しい追加お届け先を登録する。
     */
    int insertAdditionalAddress(@Param("memberId") long memberId,
                                @Param("form") MemberAdditionalAddressForm form);

    /**
     * 既存の追加お届け先を更新する。
     */
    int updateAdditionalAddress(@Param("memberId") long memberId,
                                @Param("memberAddressId") long memberAddressId,
                                @Param("form") MemberAdditionalAddressForm form);

    /**
     * 追加お届け先を削除する。
     */
    int deleteAdditionalAddress(@Param("memberId") long memberId,
                                @Param("memberAddressId") long memberAddressId);

    /**
     * 現在販売中の商品に紐づくお気に入り件数を取得する。
     */
    Long countFavorites(@Param("memberId") long memberId,
                        @Param("now") OffsetDateTime now);

    /**
     * お気に入り一覧表示用の商品行をページ単位で取得する。
     */
    List<MemberFavoriteProductMapperRow> selectFavorites(@Param("memberId") long memberId,
                                                         @Param("limit") int limit,
                                                         @Param("offset") int offset,
                                                         @Param("now") OffsetDateTime now);

    /**
     * お気に入り商品パネルに表示するカラーコードを商品単位で取得する。
     */
    List<MemberColorCodeMapperRow> selectFavoriteColorCodes(@Param("productIds") List<Long> productIds);

    /**
     * 指定商品がお気に入り済みかを確認する。
     */
    Boolean existsFavorite(@Param("memberId") long memberId,
                           @Param("productId") long productId);

    /**
     * お気に入りを追加する。
     */
    int insertFavorite(@Param("memberId") long memberId,
                       @Param("productId") long productId);

    /**
     * お気に入りを削除する。
     */
    int deleteFavorite(@Param("memberId") long memberId,
                       @Param("productId") long productId);

    /**
     * 会員プロフィールを更新する。
     */
    int updateProfile(@Param("memberId") long memberId,
                      @Param("form") MemberProfileEditForm form);

    /**
     * 会員を退会状態に更新する。
     */
    int withdrawMember(@Param("memberId") long memberId,
                       @Param("now") OffsetDateTime now);
}



