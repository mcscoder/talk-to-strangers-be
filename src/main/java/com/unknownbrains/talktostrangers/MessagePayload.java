package com.unknownbrains.talktostrangers;


public record MessagePayload(String type, Object data, String recipientSessionId,
        String senderSessionId) {
}
