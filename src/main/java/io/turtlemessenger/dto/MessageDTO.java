package io.turtlemessenger.dto;

public class MessageDTO {
    private Long roomId;
    private String senderId;
    private String content;
    private long ts;

    public MessageDTO() {}

    public MessageDTO(Long roomId, String senderId, String content, long ts) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.ts = ts;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTs() { return ts; }
    public void setTs(long ts) { this.ts = ts; }
}

