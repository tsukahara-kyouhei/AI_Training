package jp.co.skig.officeorder.model.review;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewFormTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("正常な入力ならバリデーションエラーなし")
    void validForm() {
        ReviewForm form = new ReviewForm();
        form.setRating(5);
        form.setBody("良い商品です");

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("rating が null ならエラー")
    void ratingNull() {
        ReviewForm form = new ReviewForm();
        form.setBody("本文");

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
    }

    @Test
    @DisplayName("rating が 0 ならエラー")
    void ratingZero() {
        ReviewForm form = new ReviewForm();
        form.setRating(0);
        form.setBody("本文");

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
    }

    @Test
    @DisplayName("rating が 6 ならエラー")
    void ratingSix() {
        ReviewForm form = new ReviewForm();
        form.setRating(6);
        form.setBody("本文");

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
    }

    @Test
    @DisplayName("body が空ならエラー")
    void bodyBlank() {
        ReviewForm form = new ReviewForm();
        form.setRating(3);
        form.setBody("");

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("body"));
    }

    @Test
    @DisplayName("body が 1001 文字ならエラー")
    void bodyTooLong() {
        ReviewForm form = new ReviewForm();
        form.setRating(3);
        form.setBody("あ".repeat(1001));

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("body"));
    }

    @Test
    @DisplayName("body が 1000 文字ならエラーなし")
    void bodyMaxLength() {
        ReviewForm form = new ReviewForm();
        form.setRating(3);
        form.setBody("あ".repeat(1000));

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("title が 101 文字ならエラー")
    void titleTooLong() {
        ReviewForm form = new ReviewForm();
        form.setRating(3);
        form.setTitle("あ".repeat(101));
        form.setBody("本文");

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
    }

    @Test
    @DisplayName("title が 100 文字ならエラーなし")
    void titleMaxLength() {
        ReviewForm form = new ReviewForm();
        form.setRating(3);
        form.setTitle("あ".repeat(100));
        form.setBody("本文");

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("title が null でもエラーなし")
    void titleNull() {
        ReviewForm form = new ReviewForm();
        form.setRating(3);
        form.setBody("本文");

        Set<ConstraintViolation<ReviewForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("デフォルトで published は true")
    void defaultPublished() {
        ReviewForm form = new ReviewForm();
        assertThat(form.isPublished()).isTrue();
    }
}
