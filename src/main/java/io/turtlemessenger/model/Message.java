package io.turtlemessenger.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_room_created", columnList = "room_id,created_at")
})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id")
    private ChatRoom room;

    @ManyToOne(optional = true)
    @JoinColumn(name = "sender_user_id")
    private UserAccount senderUser;

    @Column(name = "sender_username", nullable = false, length = 64)
    private String senderUsername;

    @Column(name = "text", nullable = false, length = 2000)
    private String content;

    // Temporary legacy column support: some dev DBs have a NOT NULL 'content' column
    @Column(name = "content", nullable = true, length = 2000)
    private String contentLegacy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Message() {}

    @PrePersist
    @PreUpdate
    public void syncLegacyColumns() {
        if (this.content == null && this.contentLegacy != null) {
            this.content = this.contentLegacy;
        }
        this.contentLegacy = this.content;
    }

    public Long getId() { return id; }
    public ChatRoom getRoom() { return room; }
    public void setRoom(ChatRoom room) { this.room = room; }
    public UserAccount getSenderUser() { return senderUser; }
    public void setSenderUser(UserAccount senderUser) { this.senderUser = senderUser; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
