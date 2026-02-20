package com.keza.payment.domain.service;

import com.keza.common.enums.PaymentMethod;
import com.keza.common.exception.BusinessRuleException;
import com.keza.payment.domain.port.out.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentRouter {

    private final Map<String, PaymentGateway> gateways;

    public PaymentRouter(List<PaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
                .collect(Collectors.toMap(PaymentGateway::getName, g -> g));
        log.info("Initialized PaymentRouter with gateways: {}", gateways.keySet());
    }

    public PaymentGateway route(PaymentMethod method) {
        String gatewayName = resolveGatewayName(method);
        PaymentGateway gateway = gateways.get(gatewayName);

        if (gateway == null) {
            throw new BusinessRuleException("UNSUPPORTED_PAYMENT_METHOD",
                    String.format("No gateway registered for payment method: %s (resolved name: %s)", method, gatewayName));
        }

        log.debug("Routed payment method {} to gateway {}", method, gatewayName);
        return gateway;
    }

    private String resolveGatewayName(PaymentMethod method) {
        return switch (method) {
            case MPESA -> "mpesa";
            case CARD -> "card";
            case BANK_TRANSFER -> "kcb";
            case KCB_ESCROW -> "kcb";
        };
    }
}
