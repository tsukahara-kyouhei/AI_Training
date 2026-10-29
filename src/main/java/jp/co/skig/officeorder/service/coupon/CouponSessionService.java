package jp.co.skig.officeorder.service.coupon;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class CouponSessionService {
    private static final String COUPON_CODE_KEY = "applied_coupon_code";
    private static final String MEMBER_ID_KEY = "applied_coupon_member_id";

    public String getCouponCode(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(COUPON_CODE_KEY);
        return value instanceof String code ? code : null;
    }

    public Long getMemberId(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(MEMBER_ID_KEY);
        return value instanceof Long memberId ? memberId : null;
    }

    public void apply(HttpSession session, String couponCode, Long memberId) {
        session.setAttribute(COUPON_CODE_KEY, couponCode);
        session.setAttribute(MEMBER_ID_KEY, memberId);
    }

    public void clear(HttpSession session) {
        if (session != null) {
            session.removeAttribute(COUPON_CODE_KEY);
            session.removeAttribute(MEMBER_ID_KEY);
        }
    }

    public boolean clearIfMemberChanged(HttpSession session, Long currentMemberId) {
        String couponCode = getCouponCode(session);
        if (couponCode == null) {
            return false;
        }
        if (!java.util.Objects.equals(getMemberId(session), currentMemberId)) {
            clear(session);
            return true;
        }
        return false;
    }
}
