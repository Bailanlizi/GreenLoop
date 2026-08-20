package com.campus.trade.mapper;

import com.campus.trade.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OutboxEventMapper {
    int insert(OutboxEvent event);
    List<OutboxEvent> findPublishable(@Param("limit") int limit);
    List<OutboxEvent> findAll(@Param("status") String status);
    int markPublished(@Param("id") Long id);
    int markRetry(@Param("id") Long id, @Param("status") String status, @Param("nextRetryTime") java.util.Date nextRetryTime, @Param("lastError") String lastError);
    int requeue(@Param("id") Long id);
}
