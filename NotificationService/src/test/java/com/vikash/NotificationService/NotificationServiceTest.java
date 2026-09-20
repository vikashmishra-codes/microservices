package com.vikash.NotificationService;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationService();

    @Test
    void recordsOneOrderConfirmationForDuplicateEvents() {
        Notification first = notificationService.recordOrderConfirmation("order-123");
        Notification duplicate = notificationService.recordOrderConfirmation("order-123");

        assertThat(duplicate).isSameAs(first);
        assertThat(notificationService.getNotifications()).containsExactly(first);
        assertThat(first.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(first.message()).contains("order-123");
    }
}
