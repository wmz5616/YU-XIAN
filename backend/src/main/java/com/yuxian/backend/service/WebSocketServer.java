package com.yuxian.backend.service;

import org.springframework.stereotype.Component;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket服务端，用于向前端推送消息
 */
@ServerEndpoint("/ws/orders")
@Component
public class WebSocketServer {

    // 线程安全的Set，存放每个客户端对应的WebSocketServer对象
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<>();
    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        webSocketSet.add(this);
        System.out.println("【WebSocket】有新的连接，当前在线人数：" + webSocketSet.size());
    }

    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
        System.out.println("【WebSocket】连接断开，当前在线人数：" + webSocketSet.size());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("【WebSocket】收到客户端消息：" + message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    // 群发消息 (当有新订单时调用此方法)
    public static void sendInfo(String message) {
        for (WebSocketServer item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                continue;
            }
        }
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
}