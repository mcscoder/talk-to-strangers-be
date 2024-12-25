package com.unknownbrains.talktostrangers;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new HashMap<>();
    private String queueSessionId = null;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("Connected: " + session.getId());
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
            throws Exception {
        sessions.remove(session.getId());
        System.out.println("Disconnected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        MessagePayload messagePayload =
                objectMapper.readValue(message.getPayload(), MessagePayload.class);
            
        System.out.println(messagePayload.type());

        switch (messagePayload.type()) {
            case "start": {
                handleOffer(session, messagePayload);
                break;
            }
            default: {
                WebSocketSession recipientSession =
                        sessions.get(messagePayload.recipientSessionId());

                if (recipientSession != null) {
                    MessagePayload newMessagePayload = new MessagePayload(messagePayload.type(),
                            messagePayload.data(), null, session.getId());
                    TextMessage newMessage =
                            new TextMessage(objectMapper.writeValueAsString(newMessagePayload));
                    recipientSession.sendMessage(newMessage);
                }

                break;
            }
        }
    }

    private void handleOffer(WebSocketSession session, MessagePayload messagePayload)
            throws Exception {
        // Check queue
        if (queueSessionId == null) {
            // Put current session id to queue if queue is empty
            queueSessionId = session.getId();
        } else {
            // Get queue session to grantee queueSessionId still presenting
            WebSocketSession queueSession = sessions.get(queueSessionId);

            // Check if queueSession still connecting or not
            if (queueSession == null) {
                // If queueSessionId is disconnected?
                // Put the current session id to queue session id again
                queueSessionId = session.getId();
            } else {
                // Otherwise send an offer to the queueSession
                MessagePayload newMessagePayload = new MessagePayload(messagePayload.type(),
                        messagePayload.data(), null, session.getId());
                TextMessage newMessage =
                        new TextMessage(objectMapper.writeValueAsString(newMessagePayload));

                queueSession.sendMessage(newMessage);

                // Clear queue
                queueSessionId = null;
            }
        }
    }
}
