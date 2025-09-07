package io.turtlemessenger.service;

import io.turtlemessenger.dto.ContactDTO;
import io.turtlemessenger.model.ContactRelation;
import io.turtlemessenger.model.ContactStatus;
import io.turtlemessenger.model.UserAccount;
import io.turtlemessenger.repository.ContactRepository;
import io.turtlemessenger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContactService {
    private final ContactRepository contacts;
    private final UserRepository users;

    public ContactService(ContactRepository contacts, UserRepository users) {
        this.contacts = contacts;
        this.users = users;
    }

    private UserAccount requireUser(Principal principal) {
        if (principal == null) throw new IllegalArgumentException("unauthenticated");
        return users.findByUsername(principal.getName()).orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    @Transactional(readOnly = true)
    public List<ContactDTO> listContacts(Principal principal) {
        UserAccount me = requireUser(principal);
        return contacts.findByUserAndStatus(me, ContactStatus.ACCEPTED).stream()
                .map(c -> new ContactDTO(c.getContact().getId(), c.getContact().getUsername(), c.getStatus().name()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContactDTO> incomingRequests(Principal principal) {
        UserAccount me = requireUser(principal);
        // Only include true incoming requests. If an inverse pending relation exists and was created earlier,
        // it means I initiated the request; exclude those mirrored records from legacy data.
        return contacts.findByContactAndStatus(me, ContactStatus.PENDING).stream()
                .filter(c -> {
                    Optional<ContactRelation> inverse = contacts.findByUserAndContact(me, c.getUser());
                    return inverse.isEmpty() || c.getCreatedAt().isBefore(inverse.get().getCreatedAt());
                })
                .map(c -> new ContactDTO(c.getUser().getId(), c.getUser().getUsername(), c.getStatus().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ContactDTO addContact(String usernameOrId, Principal principal) {
        UserAccount me = requireUser(principal);
        // try by id then username
        UserAccount target = null;
        try {
            Long id = Long.parseLong(usernameOrId);
            target = users.findById(id).orElse(null);
        } catch (NumberFormatException ignored) {}
        if (target == null) {
            String u = normalize(usernameOrId);
            target = users.findByUsername(u).orElse(null);
        }
        if (target == null) throw new IllegalArgumentException("user not found");
        if (target.getId().equals(me.getId())) throw new IllegalArgumentException("cannot add yourself");

        Optional<ContactRelation> mineOpt = contacts.findByUserAndContact(me, target);
        if (mineOpt.isPresent()) {
            ContactRelation existing = mineOpt.get();
            return new ContactDTO(existing.getContact().getId(), existing.getContact().getUsername(), existing.getStatus().name());
        }
        Optional<ContactRelation> theirsOpt = contacts.findByUserAndContact(target, me);

        // If the other user has already sent me a request, auto-accept on add
        if (theirsOpt.isPresent()) {
            ContactRelation incoming = theirsOpt.get();
            if (incoming.getStatus() == ContactStatus.PENDING) {
                // create my mirror relation if missing and accept both
                ContactRelation mine = new ContactRelation(me, target, ContactStatus.ACCEPTED);
                contacts.save(mine);
                incoming.setStatus(ContactStatus.ACCEPTED);
                contacts.save(incoming);
                return new ContactDTO(target.getId(), target.getUsername(), ContactStatus.ACCEPTED.name());
            } else if (incoming.getStatus() == ContactStatus.ACCEPTED) {
                // ensure my side exists as ACCEPTED
                ContactRelation mine = new ContactRelation(me, target, ContactStatus.ACCEPTED);
                contacts.save(mine);
                return new ContactDTO(target.getId(), target.getUsername(), ContactStatus.ACCEPTED.name());
            } else if (incoming.getStatus() == ContactStatus.BLOCKED) {
                throw new IllegalArgumentException("cannot add contact: blocked");
            }
        }

        // Create only my directed relation as PENDING (do NOT create reverse entry)
        ContactRelation a = new ContactRelation(me, target, ContactStatus.PENDING);
        contacts.save(a);
        return new ContactDTO(target.getId(), target.getUsername(), a.getStatus().name());
    }

    @Transactional
    public ContactDTO accept(String usernameOrId, Principal principal) {
        UserAccount me = requireUser(principal);
        UserAccount other = resolveUser(usernameOrId);
        if (other == null) throw new IllegalArgumentException("user not found");
        // Must have a pending incoming request from 'other' to 'me'
        ContactRelation incoming = contacts.findByUserAndContact(other, me)
                .orElseThrow(() -> new IllegalArgumentException("no request found"));
        if (incoming.getStatus() != ContactStatus.PENDING) {
            throw new IllegalArgumentException("no pending request to accept");
        }

        Optional<ContactRelation> mineOpt = contacts.findByUserAndContact(me, other);

        // Guard: Only the recipient of the original request can accept.
        // If my relation exists and predates the incoming one, I was the requester; reject.
        if (mineOpt.isPresent() && mineOpt.get().getStatus() == ContactStatus.PENDING
                && mineOpt.get().getCreatedAt().isBefore(incoming.getCreatedAt())) {
            throw new IllegalArgumentException("only the recipient can accept this request");
        }

        ContactRelation mine = mineOpt.orElseGet(() -> new ContactRelation(me, other, ContactStatus.PENDING));

        incoming.setStatus(ContactStatus.ACCEPTED);
        mine.setStatus(ContactStatus.ACCEPTED);
        contacts.save(incoming);
        contacts.save(mine);
        return new ContactDTO(other.getId(), other.getUsername(), "ACCEPTED");
    }

    @Transactional
    public void remove(String usernameOrId, Principal principal) {
        UserAccount me = requireUser(principal);
        UserAccount other = resolveUser(usernameOrId);
        if (other == null) return;
        contacts.findByUserAndContact(me, other).ifPresent(c -> contacts.delete(c));
        contacts.findByUserAndContact(other, me).ifPresent(c -> contacts.delete(c));
    }

    private UserAccount resolveUser(String usernameOrId) {
        try {
            Long id = Long.parseLong(usernameOrId);
            return users.findById(id).orElse(null);
        } catch (NumberFormatException ignored) {}
        String u = normalize(usernameOrId);
        return users.findByUsername(u).orElse(null);
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
