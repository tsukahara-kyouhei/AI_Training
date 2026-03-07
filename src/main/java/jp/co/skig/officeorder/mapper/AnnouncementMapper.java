package jp.co.skig.officeorder.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import jp.co.skig.officeorder.mapper.row.AnnouncementMapperRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * お知らせ表示に必要な行データを取得する MyBatis Mapper。
 */
@Mapper
public interface AnnouncementMapper {

    /**
     * 指定時点で公開中のお知らせを掲載日の新しい順で取得する。
     */
    List<AnnouncementMapperRow> selectActiveAnnouncements(@Param("limit") Integer limit,
                                                          @Param("now") OffsetDateTime now);
}


