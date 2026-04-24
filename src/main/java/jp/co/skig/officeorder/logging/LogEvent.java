package jp.co.skig.officeorder.logging;

/**
 * 構造化ログで利用する event 値定義。
 */
public enum LogEvent {
    AUTH_LOGIN_SUCCESS("auth_login_success"),
    AUTH_LOGIN_FAILURE("auth_login_failure"),
    AUTH_REQUIRED_REDIRECT("auth_required_redirect"),
    AUTH_LOGIN_PAGE_REDIRECT("auth_login_page_redirect"),
    ORDER_PLACE_START("order_place_start"),
    ORDER_PLACE_END("order_place_end"),
    ORDER_PLACE_REJECTED("order_place_rejected"),
    ORDER_NUMBER_RETRY("order_number_retry"),
    CHECKOUT_INPUT_INVALID("checkout_input_invalid"),
    CHECKOUT_TOKEN_MISMATCH("checkout_token_mismatch"),
    CHECKOUT_ORDER_ACCEPTED("checkout_order_accepted"),
    CHECKOUT_ORDER_REJECTED("checkout_order_rejected"),
    ORDER_PAYMENT_INSTRUCTION_PARSE_FAILED("order_payment_instruction_parse_failed"),
    MEMBER_REGISTER_START("member_register_start"),
    MEMBER_REGISTER_INPUT_INVALID("member_register_input_invalid"),
    MEMBER_REGISTER_REJECTED("member_register_rejected"),
    MEMBER_REGISTER_END("member_register_end"),
    MEMBER_PROFILE_UPDATE_START("member_profile_update_start"),
    MEMBER_PROFILE_UPDATED("member_profile_updated"),
    MEMBER_PROFILE_UPDATE_REJECTED("member_profile_update_rejected"),
    MEMBER_WITHDRAWN("member_withdrawn"),
    MEMBER_WITHDRAW_REJECTED("member_withdraw_rejected"),
    MYPAGE_ORDER_DETAIL_MISSED("mypage_order_detail_missed"),
    MYPAGE_REORDER_REJECTED("mypage_reorder_rejected"),
    MEMBER_ADDRESS_CREATE_REJECTED("member_address_create_rejected"),
    MEMBER_ADDRESS_CREATED("member_address_created"),
    MEMBER_ADDRESS_UPDATED("member_address_updated"),
    MEMBER_ADDRESS_UPDATE_MISSED("member_address_update_missed"),
    MEMBER_ADDRESS_DELETED("member_address_deleted"),
    FAVORITE_REDIRECT_LOGIN("favorite_redirect_login"),
    FAVORITE_ACTION_ACCEPTED("favorite_action_accepted"),
    FAVORITE_ACTION_REJECTED("favorite_action_rejected"),
    FAVORITE_ADDED("favorite_added"),
    FAVORITE_REMOVED("favorite_removed"),
    FAVORITE_LIMIT_REJECTED("favorite_limit_rejected"),
    FAVORITE_INSERT_RACE_SKIPPED("favorite_insert_race_skipped"),
    CONTACT_SUBMITTED("contact_submitted"),
    CONTACT_INPUT_INVALID("contact_input_invalid"),
    CONTACT_SUBMIT_FAILED("contact_submit_failed"),
    CART_ITEM_ADDED("cart_item_added"),
    CART_ITEM_UPDATED("cart_item_updated"),
    CART_ITEM_REMOVED("cart_item_removed"),
    CART_CLEARED("cart_cleared"),
    CART_ADD_FAILED("cart_add_failed"),
    CART_UPDATE_FAILED("cart_update_failed"),
    BATCH_JOBS_TRIGGER_REQUESTED("batch_jobs_trigger_requested"),
    BATCH_REQUEST_ACCEPTED("batch_request_accepted"),
    BATCH_REQUEST_REJECTED("batch_request_rejected"),
    BATCH_EXECUTION_START("batch_execution_start"),
    BATCH_EXECUTION_END("batch_execution_end"),
    BATCH_EXECUTION_FAILED("batch_execution_failed"),
    BATCH_STOP_REQUESTED("batch_stop_requested"),
    BATCH_ASYNC_START("batch_async_start"),
    BATCH_ASYNC_END("batch_async_end"),
    BATCH_ASYNC_FAILED("batch_async_failed"),
    BATCH_SCHEDULED_START("batch_scheduled_start"),
    BATCH_SCHEDULED_END("batch_scheduled_end"),
    BATCH_SCHEDULED_FAILED("batch_scheduled_failed"),
    BATCH_SCHEDULED_SKIPPED("batch_scheduled_skipped"),
    BATCH_TRIGGER_FAILED("batch_trigger_failed"),
    BATCH_TRIGGER_SKIPPED("batch_trigger_skipped"),
    BATCH_JOB_UPDATED("batch_job_updated"),
    MAIL_SEND_SUCCESS("mail_send_success"),
    MAIL_SEND_RETRY("mail_send_retry"),
    MAIL_SEND_FAILED_FINAL("mail_send_failed_final"),
    MAIL_SEND_SKIPPED("mail_send_skipped"),
    SESSION_MEMBER_INVALIDATED("session_member_invalidated"),
    SESSION_CREATED("session_created"),
    SESSION_CLEARED("session_cleared"),
    REVIEW_POSTED("review_posted"),
    REVIEW_UPDATED("review_updated"),
    REVIEW_DELETED("review_deleted"),
    REVIEW_REDIRECT_LOGIN("review_redirect_login"),
    REVIEW_ACTION_REJECTED("review_action_rejected"),
    UNHANDLED_EXCEPTION("unhandled_exception");

    private final String value;

    /**
     * event 値を持つ列挙子を生成する。
     *
     * @param value event値
     */
    LogEvent(String value) {
        this.value = value;
    }

    /**
     * 構造化ログに出力する event 値を返す。
     *
     * @return event値
     */
    public String value() {
        return value;
    }
}
