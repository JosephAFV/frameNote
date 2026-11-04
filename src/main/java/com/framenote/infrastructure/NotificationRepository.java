package com.framenote.infrastructure;
import com.framenote.domain.Notification;
import com.framenote.application.port.NotificationStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<Notification,String>, NotificationStore { java.util.List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId); }

