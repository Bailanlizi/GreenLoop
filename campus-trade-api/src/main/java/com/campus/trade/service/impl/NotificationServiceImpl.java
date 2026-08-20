package com.campus.trade.service.impl;

import com.campus.trade.entity.Notification;
import com.campus.trade.mapper.NotificationMapper;
import com.campus.trade.service.NotificationService;
import com.campus.trade.dto.PageResult;
import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void createNotification(String userId, String type, String content, String relatedId) {
        createNotification(userId, type, content, relatedId, "ORDER", null);
    }

    @Override
    public void createNotification(String userId, String type, String content, String relatedId, String relatedType, String sourceEventId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setSourceEventId(sourceEventId);
        notificationMapper.insert(notification);
    }

    @Override
    public PageResult<Notification> getUserNotifications(String userId, String readStatus, Integer page, Integer size) {
        String normalized = "UNREAD".equals(readStatus) ? "UNREAD" : "ALL";
        PageHelper.startPage(page == null || page < 1 ? 1 : page, size == null ? 20 : Math.max(1, Math.min(size, 100)));
        return new PageResult<>(notificationMapper.findByUserId(userId, normalized));
    }

    @Override
    public long getUnreadNotificationCount(String userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Override
    public boolean markNotificationAsRead(String notificationId, String userId) {
        return notificationMapper.markAsRead(notificationId, userId) == 1;
    }

    @Override
    public void markAllNotificationsAsRead(String userId) {
        notificationMapper.markAllAsReadByUserId(userId);
    }
}
