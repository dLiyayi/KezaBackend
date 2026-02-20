package com.keza.notification.domain.port.out;

public interface SmsSender {
    void send(String phoneNumber, String message);
}
