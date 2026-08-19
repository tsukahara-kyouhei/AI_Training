package jp.co.skig.officeorder.web;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.co.skig.officeorder.service.coupon.CouponService;

@Controller
@RequestMapping("/cart/coupon")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/apply")
    public String applyCoupon(@RequestParam("couponCode") String couponCode, HttpSession session) {
        session.setAttribute("appliedCouponCode", couponCode);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeCoupon(HttpSession session) {
        session.removeAttribute("appliedCouponCode");
        return "redirect:/cart";
    }
}
