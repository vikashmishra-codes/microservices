package com.vikash.NotificationService;

import java.time.Instant;

public record Notification(
        String id,
        String orderNumber,
        String type,
        String message,
        NotificationStatus status,
        Instant createdAt
) {
}
