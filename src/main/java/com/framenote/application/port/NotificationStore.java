package com.framenote.application.port;

import com.framenote.domain.Notification;

public interface NotificationStore {
    java.util.Optional<Notification> findById(String id);
    <S extends Notification> S save(S entity);
    java.util.List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
}
