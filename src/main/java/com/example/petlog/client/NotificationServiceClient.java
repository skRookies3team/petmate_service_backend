package com.example.petlog.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// application.yml의 external.notification-service.url 참조
@FeignClient(name = "notification-service", url = "${external.notification-service.url}")
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/send")
    void sendNotification(@RequestBody NotificationRequest request);

    record NotificationRequest(Long userId, String title, String message, String type) {}
}