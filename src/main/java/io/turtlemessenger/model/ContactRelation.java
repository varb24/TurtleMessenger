package io.turtlemessenger.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "contacts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_contacts_pair", columnNames = {"user_id", "contact_id"})
})
public class ContactRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user; // owner of the contact list

    @ManyToOne(optional = false)
    @JoinColumn(name = "contact_id")
    private UserAccount contact; // the other person

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactStatus status = ContactStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public ContactRelation() {}

    public ContactRelation(UserAccount user, UserAccount contact, ContactStatus status) {
        this.user = user;
        this.contact = contact;
        this.status = status == null ? ContactStatus.PENDING : status;
    }

    public Long getId() { return id; }
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
    public UserAccount getContact() { return contact; }
    public void setContact(UserAccount contact) { this.contact = contact; }
    public ContactStatus getStatus() { return status; }
    public void setStatus(ContactStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
