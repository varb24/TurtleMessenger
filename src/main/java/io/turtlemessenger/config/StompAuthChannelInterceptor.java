package io.turtlemessenger.config;

import io.turtlemessenger.security.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

public class StompAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwt;
    public StompAuthChannelInterceptor(JwtUtil jwt) { this.jwt = jwt; }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = accessor.getFirstNativeHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                if (jwt.isValid(token)) {
                    String username = jwt.getSubject(token);
                    var principal = new User(username, "", Collections.emptyList());
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    accessor.setUser(authentication);
                }
            }
        }
        return message;
    }
}

