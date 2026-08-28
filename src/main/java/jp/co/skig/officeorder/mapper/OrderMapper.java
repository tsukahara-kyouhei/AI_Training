package jp.co.skig.officeorder.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jp.co.skig.officeorder.mapper.row.MemberOrderDetailMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberOrderItemMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberOrderHistoryMapperRow;
import jp.co.skig.officeorder.mapper.row.MemberOrderStatusHistoryMapperRow;
import jp.co.skig.officeorder.mapper.row.OrderCompleteMapperRow;
import jp.co.skig.officeorder.mapper.row.OrderReorderItemMapperRow;
import jp.co.skig.officeorder.model.coupon.CouponForm;
import jp.co.skig.officeorder.model.order.CheckoutMemberPrefill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 注文確定、購入履歴表示、再購入処理に必要な永続化を扱う Mapper。
 */
@Mapper
public interface OrderMapper {

    /**
     * ログイン会員の注文入力フォーム初期値を取得する。
     */
    CheckoutMemberPrefill selectCheckoutMemberPrefill(@Param("memberId") long memberId);

    /**
     * 注文ヘッダを保存し、採番された注文IDを返す。
     */
    Long insertOrder(@Param("params") Map<String, Object> params);

    /**
     * 注文明細を1件保存する。
     */
    int insertOrderItem(@Param("params") Map<String, Object> params);

    /**
     * 注文ステータス履歴を1件保存する。
     */
    int insertOrderStatusHistory(@Param("params") Map<String, Object> params);

    /**
     * 注文番号採番用に、当日の連番の次値を取得する。
     */
    Integer nextOrderSequence(@Param("orderDate") LocalDate orderDate);

    /**
     * 注文完了画面と完了メールの基礎情報を注文番号から取得する。
     */
    OrderCompleteMapperRow selectOrderCompleteByOrderNumber(@Param("orderNumber") String orderNumber);

    /**
     * 会員の購入履歴件数を取得する。
     */
    Long countMemberOrders(@Param("memberId") long memberId);

    /**
     * 会員の購入履歴一覧をページ単位で取得する。
     */
    List<MemberOrderHistoryMapperRow> selectMemberOrders(@Param("memberId") long memberId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * 注文詳細のヘッダ部と金額サマリーを取得する。
     */
    MemberOrderDetailMapperRow selectMemberOrderDetail(@Param("memberId") long memberId,
            @Param("orderNumber") String orderNumber);

    /**
     * 注文のステータス履歴を時系列で取得する。
     */
    List<MemberOrderStatusHistoryMapperRow> selectMemberOrderStatusHistories(@Param("memberId") long memberId,
            @Param("orderNumber") String orderNumber);

    /**
     * 注文詳細の明細行を取得する。
     */
    List<MemberOrderItemMapperRow> selectMemberOrderItems(@Param("memberId") long memberId,
            @Param("orderNumber") String orderNumber);

    /**
     * 再購入時にカートへ積み直すための注文明細情報を取得する。
     */
    List<OrderReorderItemMapperRow> selectMemberReorderItems(@Param("memberId") long memberId,
            @Param("orderNumber") String orderNumber);

    /**
     * 指定時点で有効な消費税率を取得する。
     */
    BigDecimal selectCurrentTaxRatePercent(@Param("now") OffsetDateTime now);

    /**
     * 指定されたクーポンコードと現在時刻をもとに、有効なクーポンを取得する。
     *
     * @param couponCode クーポンコード
     * @param now        現在時刻
     * @return クーポンDTO
     */
    CouponForm selectActiveCouponByCode(
            @Param("couponCode") String couponCode,
            @Param("now") OffsetDateTime now);

    /**
     * 会員の特定クーポン利用回数を取得する。
     *
     * @param memberId 会員ID
     * @param couponId クーポンID
     * @return 利用回数
     */
    Integer selectCustomerCouponUsageCount(
            @Param("memberId") long memberId,
            @Param("couponId") long couponId);

    /**
     * 会員のクーポン利用実績をインクリメント（初回はINSERT）する。
     *
     * @param memberId 会員ID
     * @param couponId クーポンID
     * @param now      現在時刻
     */
    void upsertCustomerCouponUsage(
            @Param("memberId") long memberId,
            @Param("couponId") long couponId,
            @Param("now") OffsetDateTime now);

    /**
     * 会員が指定した商品を購入したことがあるか確認する。
     */
    boolean existsPurchasedProduct(
            @Param("memberId") long memberId,
            @Param("productId") long productId);
}
