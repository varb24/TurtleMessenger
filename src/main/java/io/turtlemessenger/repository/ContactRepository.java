package io.turtlemessenger.repository;

import io.turtlemessenger.model.ContactRelation;
import io.turtlemessenger.model.ContactStatus;
import io.turtlemessenger.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<ContactRelation, Long> {
    boolean existsByUserAndContact(UserAccount user, UserAccount contact);
    Optional<ContactRelation> findByUserAndContact(UserAccount user, UserAccount contact);

    List<ContactRelation> findByUserAndStatus(UserAccount user, ContactStatus status);
    List<ContactRelation> findByContactAndStatus(UserAccount contact, ContactStatus status);
}
