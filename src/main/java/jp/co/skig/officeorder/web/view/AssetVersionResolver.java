package jp.co.skig.officeorder.web.view;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * 静的アセットの最終更新日時を返すThymeleaf補助コンポーネント。
 *
 * <p>
 * CSS/JSのURLへクエリ文字列でバージョンを付け、ブラウザキャッシュの取り違えを防ぐ。
 */
@Component("assetVersion")
public class AssetVersionResolver {

    /** classpathリソース読み込みに使う ResourceLoader。 */
    private final ResourceLoader resourceLoader;

    /**
     * アセットバージョン解決コンポーネントを生成する。
     *
     * @param resourceLoader リソースローダ
     */
    public AssetVersionResolver(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 公開パスに対応する静的ファイルの最終更新日時を返す。
     *
     * @param publicPath `/css/...` 形式の公開パス
     * @return 最終更新日時ミリ秒。見つからない場合は 0
     */
    public long version(String publicPath) {
        String normalizedPath = normalize(publicPath);
        if (normalizedPath.isEmpty()) {
            return 0L;
        }

        Resource resource = resourceLoader.getResource("classpath:/static" + normalizedPath);
        if (!resource.exists()) {
            return 0L;
        }

        try {
            return resource.lastModified();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    /**
     * 公開パスを classpath 静的リソース参照用の形式へ正規化する。
     *
     * @param path 公開パス
     * @return 正規化済みパス
     */
    private String normalize(String path) {
        if (path == null) {
            return "";
        }
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
