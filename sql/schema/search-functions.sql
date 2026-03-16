-- 検索用テキスト正規化関数
-- 全角数字・英字（大小）・カタカナを半角に変換し、英字を小文字に統一する。
-- JavaのSearchKeywordNormalizerと同じ変換ルールを適用する。
CREATE OR REPLACE FUNCTION normalize_search_text(input TEXT)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
STRICT
AS $$
DECLARE
    result TEXT;
BEGIN
    result := input;

    -- 全角数字 → 半角数字
    result := TRANSLATE(result,
        '０１２３４５６７８９',
        '0123456789');

    -- 全角英大文字 → 半角英大文字
    result := TRANSLATE(result,
        'ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ',
        'ABCDEFGHIJKLMNOPQRSTUVWXYZ');

    -- 全角英小文字 → 半角英小文字
    result := TRANSLATE(result,
        'ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ',
        'abcdefghijklmnopqrstuvwxyz');

    -- 濁音付き全角カタカナ → 半角カタカナ + 半角濁点（2文字に展開）
    result := REPLACE(result, 'ガ', 'ｶﾞ');
    result := REPLACE(result, 'ギ', 'ｷﾞ');
    result := REPLACE(result, 'グ', 'ｸﾞ');
    result := REPLACE(result, 'ゲ', 'ｹﾞ');
    result := REPLACE(result, 'ゴ', 'ｺﾞ');
    result := REPLACE(result, 'ザ', 'ｻﾞ');
    result := REPLACE(result, 'ジ', 'ｼﾞ');
    result := REPLACE(result, 'ズ', 'ｽﾞ');
    result := REPLACE(result, 'ゼ', 'ｾﾞ');
    result := REPLACE(result, 'ゾ', 'ｿﾞ');
    result := REPLACE(result, 'ダ', 'ﾀﾞ');
    result := REPLACE(result, 'ヂ', 'ﾁﾞ');
    result := REPLACE(result, 'ヅ', 'ﾂﾞ');
    result := REPLACE(result, 'デ', 'ﾃﾞ');
    result := REPLACE(result, 'ド', 'ﾄﾞ');
    result := REPLACE(result, 'バ', 'ﾊﾞ');
    result := REPLACE(result, 'ビ', 'ﾋﾞ');
    result := REPLACE(result, 'ブ', 'ﾌﾞ');
    result := REPLACE(result, 'ベ', 'ﾍﾞ');
    result := REPLACE(result, 'ボ', 'ﾎﾞ');
    result := REPLACE(result, 'ヴ', 'ｳﾞ');

    -- 半濁音付き全角カタカナ → 半角カタカナ + 半角半濁点（2文字に展開）
    result := REPLACE(result, 'パ', 'ﾊﾟ');
    result := REPLACE(result, 'ピ', 'ﾋﾟ');
    result := REPLACE(result, 'プ', 'ﾌﾟ');
    result := REPLACE(result, 'ペ', 'ﾍﾟ');
    result := REPLACE(result, 'ポ', 'ﾎﾟ');

    -- 清音・小文字全角カタカナ → 半角カタカナ（1対1変換）
    result := TRANSLATE(result,
        'アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲンァィゥェォッャュョーヵヶ',
        'ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜｦﾝｧｨｩｪｫｯｬｭｮｰｶｹ');

    -- 全角長音符・句読点・括弧 → 半角
    result := TRANSLATE(result,
        '・「」。、',
        '･｢｣｡､');

    -- 英字をすべて小文字化
    result := LOWER(result);

    RETURN result;
END;
$$;
