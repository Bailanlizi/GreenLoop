package com.campus.trade.mapper;

import com.campus.trade.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface NotificationMapper {
    int insert(Notification notification);
    List<Notification> findByUserId(@Param("userId") String userId, @Param("readStatus") String readStatus);
    long countUnreadByUserId(String userId);
    int markAsRead(@Param("notificationId") String notificationId, @Param("userId") String userId);
    int markAllAsReadByUserId(String userId);
}
