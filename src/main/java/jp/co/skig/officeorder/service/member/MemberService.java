package jp.co.skig.officeorder.service.member;

import java.util.Optional;

import jp.co.skig.officeorder.logging.LogEvent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 会員登録後の顧客機能を扱うアプリケーションサービス。
 *
 * <p>
 * 認証用資格情報の取得、会員登録、プロフィール更新、
 * 追加お届け先管理、お気に入り管理、退会処理をここに集約している。
 */
@Service
public class MemberService {

    /** 会員系操作ログ出力用ロガー。 */
    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    /** 会員が登録できる追加お届け先の上限件数。 */
    public static final int ADDITIONAL_ADDRESS_LIMIT = 20;
    /** 会員がお気に入り登録できる商品の上限件数。 */
    public static final int FAVORITES_LIMIT = 100;
    /** マイページ一覧の標準ページサイズ。 */
    public static final int MYPAGE_PAGE_SIZE = 20;

    /** 会員データ永続化の窓口。 */
    private final MemberRepository memberRepository;
    /** パスワードハッシュ化に使うエンコーダ。 */
    private final PasswordEncoder passwordEncoder;
    /** 利用者向けメッセージ取得ヘルパ。 */
    private final MessageSourceAccessor messages;

    /**
     * 会員サービスを生成する。
     *
     * @param memberRepository 会員リポジトリ
     * @param passwordEncoder  パスワードエンコーダ
     * @param messageSource    利用者向けメッセージ取得元
     */
    public MemberService(MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            MessageSource messageSource) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * 有効会員の認証情報をメールアドレスで取得する。
     *
     * @param email メールアドレス
     * @return 認証情報
     */
    public Optional<MemberCredential> findActiveCredentialByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return memberRepository.findActiveCredentialByEmail(email.trim());
    }

    /**
     * 会員IDから有効会員情報を取得する。
     *
     * @param memberId 会員ID
     * @return 会員セッション用情報
     */
    public Optional<MemberSessionUser> findActiveById(long memberId) {
        return memberRepository.findActiveById(memberId);
    }

    /**
     * メールアドレスの既存登録有無を判定する。
     *
     * @param email メールアドレス
     * @return 登録済みなら {@code true}
     */
    public boolean existsByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return memberRepository.existsByEmail(email.trim());
    }

    /**
     * 会員登録を行い、ログインセッション用情報を返す。
     *
     * <p>
     * メールアドレス重複を事前・事後の両方で検知し、
     * パスワードはハッシュ化して保存する。
     *
     * @param inputForm 会員登録フォーム
     * @return 登録済み会員情報
     */
    @Transactional
    public MemberSessionUser register(MemberRegisterForm inputForm) {
        MemberRegisterForm form = inputForm.normalize();
        log.info("event={} personalOrCorporate={}",
                LogEvent.MEMBER_REGISTER_START.value(),
                form.getPersonalOrCorporate());
        if (existsByEmail(form.getEmail())) {
            log.warn("event={} reason=email_duplicate", LogEvent.MEMBER_REGISTER_REJECTED.value());
            throw new DuplicateEmailException(message("business.member.duplicateEmail"));
        }
        String passwordHash = passwordEncoder.encode(form.getPassword());
        long memberId;
        try {
            memberId = memberRepository.insertMember(form, passwordHash);
        } catch (DataIntegrityViolationException ex) {
            if (existsByEmail(form.getEmail())) {
                log.warn("event={} reason=email_duplicate_race", LogEvent.MEMBER_REGISTER_REJECTED.value());
                throw new DuplicateEmailException(message("business.member.duplicateEmail"));
            }
            throw ex;
        }
        log.info("event={} memberId={}", LogEvent.MEMBER_REGISTER_END.value(), memberId);
        return memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new IllegalStateException("登録済み会員の取得に失敗しました。"));
    }

    /**
     * 会員の追加お届け先一覧を取得する。
     *
     * @param memberId 会員ID
     * @param page     ページ番号
     * @return 追加お届け先一覧ページ
     */
    public MemberAdditionalAddressPage findAdditionalAddresses(long memberId, int page) {
        int normalizedPage = Math.max(page, 1);
        return memberRepository.findAdditionalAddresses(memberId, normalizedPage, MYPAGE_PAGE_SIZE);
    }

    /**
     * 指定追加お届け先を会員所有の範囲で取得する。
     *
     * @param memberId        会員ID
     * @param memberAddressId 追加お届け先ID
     * @return 追加お届け先
     */
    public Optional<MemberAdditionalAddressView> findAdditionalAddressById(long memberId, long memberAddressId) {
        return memberRepository.findAdditionalAddressById(memberId, memberAddressId);
    }

    /**
     * 会員種別を取得する。
     *
     * @param memberId 会員ID
     * @return 会員種別
     */
    public MemberType findMemberTypeById(long memberId) {
        return memberRepository.findMemberTypeById(memberId);
    }

    /**
     * 会員情報変更画面の初期値を取得する。
     *
     * @param memberId 会員ID
     * @return 会員情報変更フォーム
     */
    public Optional<MemberProfileEditForm> findProfileByMemberId(long memberId) {
        return memberRepository.findProfileByMemberId(memberId);
    }

    /**
     * 会員プロフィールを更新する。
     *
     * @param memberId  会員ID
     * @param inputForm 更新フォーム
     */
    @Transactional
    public void updateProfile(long memberId, MemberProfileEditForm inputForm) {
        MemberProfileEditForm form = inputForm.normalize();
        log.info("event={} memberId={}", LogEvent.MEMBER_PROFILE_UPDATE_START.value(), memberId);
        if (memberRepository.existsByEmailForOtherMember(form.getEmail(), memberId)) {
            log.warn("event={} memberId={} reason=email_duplicate",
                    LogEvent.MEMBER_PROFILE_UPDATE_REJECTED.value(),
                    memberId);
            throw new DuplicateEmailException(message("business.member.duplicateEmail"));
        }
        boolean updated;
        try {
            updated = memberRepository.updateProfile(memberId, form);
        } catch (DataIntegrityViolationException ex) {
            if (memberRepository.existsByEmailForOtherMember(form.getEmail(), memberId)) {
                log.warn("event={} memberId={} reason=email_duplicate_race",
                        LogEvent.MEMBER_PROFILE_UPDATE_REJECTED.value(),
                        memberId);
                throw new DuplicateEmailException(message("business.member.duplicateEmail"));
            }
            throw ex;
        }
        if (!updated) {
            log.warn("event={} memberId={} reason=member_not_found",
                    LogEvent.MEMBER_PROFILE_UPDATE_REJECTED.value(),
                    memberId);
            throw new IllegalStateException("会員情報の更新対象が見つかりません。");
        }
        log.info("event={} memberId={}", LogEvent.MEMBER_PROFILE_UPDATED.value(), memberId);
    }

    /**
     * 会員を退会状態へ更新する。
     *
     * @param memberId 会員ID
     * @return 更新成功時は {@code true}
     */
    @Transactional
    public boolean withdraw(long memberId) {
        boolean withdrawn = memberRepository.withdrawMember(memberId);
        if (withdrawn) {
            log.info("event={} memberId={}", LogEvent.MEMBER_WITHDRAWN.value(), memberId);
        } else {
            log.warn("event={} memberId={} reason=already_withdrawn_or_not_found",
                    LogEvent.MEMBER_WITHDRAW_REJECTED.value(),
                    memberId);
        }
        return withdrawn;
    }

    /**
     * 追加お届け先上限に達しているかを判定する。
     *
     * @param memberId 会員ID
     * @return 上限到達時は {@code true}
     */
    public boolean isAddressLimitReached(long memberId) {
        return memberRepository.countAdditionalAddresses(memberId) >= ADDITIONAL_ADDRESS_LIMIT;
    }

    /**
     * 追加お届け先を新規登録する。
     *
     * @param memberId  会員ID
     * @param inputForm 追加お届け先フォーム
     */
    @Transactional
    public void createAdditionalAddress(long memberId, MemberAdditionalAddressForm inputForm) {
        if (isAddressLimitReached(memberId)) {
            log.warn("event={} memberId={} limit={} reason=limit_exceeded",
                    LogEvent.MEMBER_ADDRESS_CREATE_REJECTED.value(),
                    memberId,
                    ADDITIONAL_ADDRESS_LIMIT);
            throw new AddressLimitExceededException(message("business.member.addressLimitExceeded"));
        }
        memberRepository.insertAdditionalAddress(memberId, inputForm.normalize());
        log.info("event={} memberId={}", LogEvent.MEMBER_ADDRESS_CREATED.value(), memberId);
    }

    /**
     * 追加お届け先を更新する。
     *
     * @param memberId        会員ID
     * @param memberAddressId 追加お届け先ID
     * @param inputForm       更新フォーム
     * @return 更新できた場合は {@code true}
     */
    @Transactional
    public boolean updateAdditionalAddress(long memberId, long memberAddressId, MemberAdditionalAddressForm inputForm) {
        boolean updated = memberRepository.updateAdditionalAddress(memberId, memberAddressId, inputForm.normalize());
        if (updated) {
            log.info("event={} memberId={} memberAddressId={}",
                    LogEvent.MEMBER_ADDRESS_UPDATED.value(),
                    memberId,
                    memberAddressId);
        } else {
            log.warn("event={} memberId={} memberAddressId={} reason=not_found",
                    LogEvent.MEMBER_ADDRESS_UPDATE_MISSED.value(),
                    memberId,
                    memberAddressId);
        }
        return updated;
    }

    /**
     * 追加お届け先を削除する。
     *
     * @param memberId        会員ID
     * @param memberAddressId 追加お届け先ID
     */
    @Transactional
    public void deleteAdditionalAddress(long memberId, long memberAddressId) {
        memberRepository.deleteAdditionalAddress(memberId, memberAddressId);
        log.info("event={} memberId={} memberAddressId={}",
                LogEvent.MEMBER_ADDRESS_DELETED.value(),
                memberId,
                memberAddressId);
    }

    /**
     * お気に入り一覧を取得する。
     *
     * @param memberId 会員ID
     * @param page     ページ番号
     * @return お気に入り一覧ページ
     */
    public MemberFavoritePage findFavorites(long memberId, int page) {
        int normalizedPage = Math.max(page, 1);
        return memberRepository.findFavorites(memberId, normalizedPage, MYPAGE_PAGE_SIZE);
    }

    /**
     * 指定商品がお気に入り済みか判定する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     * @return お気に入り済みなら {@code true}
     */
    public boolean isFavorite(long memberId, long productId) {
        return memberRepository.existsFavorite(memberId, productId);
    }

    /**
     * お気に入りの追加・解除をトグルで行う。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     */
    @Transactional
    public void toggleFavorite(long memberId, long productId) {
        if (memberRepository.existsFavorite(memberId, productId)) {
            memberRepository.deleteFavorite(memberId, productId);
            log.info("event={} memberId={} productId={}",
                    LogEvent.FAVORITE_REMOVED.value(),
                    memberId,
                    productId);
            return;
        }
        if (memberRepository.countFavorites(memberId) >= FAVORITES_LIMIT) {
            log.warn("event={} memberId={} limit={} reason=limit_exceeded",
                    LogEvent.FAVORITE_LIMIT_REJECTED.value(),
                    memberId,
                    FAVORITES_LIMIT);
            throw new FavoritesLimitExceededException(message("business.member.favoriteLimitExceeded"));
        }
        try {
            memberRepository.insertFavorite(memberId, productId);
            log.info("event={} memberId={} productId={}",
                    LogEvent.FAVORITE_ADDED.value(),
                    memberId,
                    productId);
        } catch (DataIntegrityViolationException ex) {
            if (memberRepository.existsFavorite(memberId, productId)) {
                log.info("event={} memberId={} productId={} reason=already_exists",
                        LogEvent.FAVORITE_INSERT_RACE_SKIPPED.value(),
                        memberId,
                        productId);
                return;
            }
            throw ex;
        }
    }

    /**
     * 指定商品をお気に入りから削除する。
     *
     * @param memberId  会員ID
     * @param productId 商品ID
     */
    @Transactional
    public void removeFavorite(long memberId, long productId) {
        memberRepository.deleteFavorite(memberId, productId);
        log.info("event={} memberId={} productId={}",
                LogEvent.FAVORITE_REMOVED.value(),
                memberId,
                productId);
    }

    /**
     * 利用者向けメッセージを取得する。
     *
     * @param code メッセージコード
     * @param args 埋め込み引数
     * @return 解決済みメッセージ
     */
    private String message(String code, Object... args) {
        return messages.getMessage(code, args);
    }
}
