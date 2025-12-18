package com.yuxian.backend.service;

import com.yuxian.backend.entity.User;
import com.yuxian.backend.repository.UserRepository;
import com.yuxian.backend.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws/orders")
@Component
public class WebSocketServer {

    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<>();
    private static JwtUtils jwtUtils;
    private static UserRepository userRepository;

    @Autowired
    public void setJwtUtils(JwtUtils jwtUtils) {
        WebSocketServer.jwtUtils = jwtUtils;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        WebSocketServer.userRepository = userRepository;
    }

    private Session session;
    private boolean isAdmin = false;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        this.isAdmin = checkAdminRole(session);

        webSocketSet.add(this);
        System.out.println("【WebSocket】有新的连接，是否管理员: " + isAdmin + "，当前在线人数：" + webSocketSet.size());
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

    public static void sendInfo(String message) {
        for (WebSocketServer item : webSocketSet) {
            try {
                if (item.isAdmin) {
                    item.session.getBasicRemote().sendText(message);
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    private boolean checkAdminRole(Session session) {
        try {
            String queryString = session.getQueryString();
            if (queryString != null && queryString.contains("token=")) {
                String token = queryString.split("token=")[1].split("&")[0];

                String username = jwtUtils.validateToken(token);
                if (username != null) {
                    User user = userRepository.findByUsername(username);
                    if (user != null && "ADMIN".equals(user.getRole())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }
}