package com.keza.payment.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MpesaCallbackRequest {

    @JsonProperty("Body")
    private Body body;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Body {

        @JsonProperty("stkCallback")
        private StkCallback stkCallback;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StkCallback {

        @JsonProperty("MerchantRequestID")
        private String merchantRequestID;

        @JsonProperty("CheckoutRequestID")
        private String checkoutRequestID;

        @JsonProperty("ResultCode")
        private int resultCode;

        @JsonProperty("ResultDesc")
        private String resultDesc;

        @JsonProperty("CallbackMetadata")
        private CallbackMetadata callbackMetadata;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallbackMetadata {

        @JsonProperty("Item")
        private List<CallbackItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallbackItem {

        @JsonProperty("Name")
        private String name;

        @JsonProperty("Value")
        private Object value;
    }

    /**
     * Extracts a metadata value by name from the callback metadata items.
     *
     * @param name the metadata item name (e.g., "Amount", "MpesaReceiptNumber", "PhoneNumber")
     * @return the value if found, or null
     */
    public Object getMetadataValue(String name) {
        if (body == null || body.getStkCallback() == null
                || body.getStkCallback().getCallbackMetadata() == null
                || body.getStkCallback().getCallbackMetadata().getItems() == null) {
            return null;
        }

        return body.getStkCallback().getCallbackMetadata().getItems().stream()
                .filter(item -> name.equals(item.getName()))
                .map(CallbackItem::getValue)
                .findFirst()
                .orElse(null);
    }

    public boolean isSuccessful() {
        return body != null
                && body.getStkCallback() != null
                && body.getStkCallback().getResultCode() == 0;
    }

    public String getCheckoutRequestId() {
        if (body != null && body.getStkCallback() != null) {
            return body.getStkCallback().getCheckoutRequestID();
        }
        return null;
    }
}
