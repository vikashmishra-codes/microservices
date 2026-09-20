package com.vikash.NotificationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class NotificationService {

    private final ConcurrentMap<String, Notification> notificationsByOrderNumber = new ConcurrentHashMap<>();

    /**
     * Records the confirmation that would be delivered to a customer. Kafka can deliver
     * the same event more than once, so an order number is used as the idempotency key.
     */
    public Notification recordOrderConfirmation(String orderNumber) {
        return notificationsByOrderNumber.computeIfAbsent(orderNumber, this::createOrderConfirmation);
    }

    public List<Notification> getNotifications() {
        return notificationsByOrderNumber.values().stream()
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .toList();
    }

    public Optional<Notification> getNotificationForOrder(String orderNumber) {
        return Optional.ofNullable(notificationsByOrderNumber.get(orderNumber));
    }

    private Notification createOrderConfirmation(String orderNumber) {
        Notification notification = new Notification(
                UUID.randomUUID().toString(),
                orderNumber,
                "ORDER_CONFIRMATION",
                "Your order " + orderNumber + " has been placed successfully.",
                NotificationStatus.SENT,
                Instant.now()
        );
        log.info("Recorded order confirmation notification for order {}", orderNumber);
        return notification;
    }
}
