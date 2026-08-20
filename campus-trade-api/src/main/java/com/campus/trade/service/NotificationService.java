package com.campus.trade.service;

import com.campus.trade.entity.Notification;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;
import com.campus.trade.dto.PageResult;

public interface NotificationService {
    void createNotification(String userId, String type, String content, String relatedId);
    void createNotification(String userId, String type, String content, String relatedId, String relatedType, String sourceEventId);
    PageResult<Notification> getUserNotifications(String userId, String readStatus, Integer page, Integer size);
    long getUnreadNotificationCount(String userId);
    boolean markNotificationAsRead(@Param("notificationId") String notificationId, @Param("userId") String userId);
    void markAllNotificationsAsRead(String userId);
}
