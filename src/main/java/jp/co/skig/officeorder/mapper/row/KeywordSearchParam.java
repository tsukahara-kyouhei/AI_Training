package jp.co.skig.officeorder.mapper.row;

/**
 * キーワード検索 SQL に渡す 1 トークン分のパラメータ。
 *
 * @param keywordLike 部分一致用リテラル（{@code "%token%"} 形式）
 * @param codeLike    前方一致用リテラル（{@code "token%"} 形式）
 */
public record KeywordSearchParam(String keywordLike, String codeLike) {
}
