package io.turtlemessenger.controller;

import io.turtlemessenger.dto.ContactDTO;
import io.turtlemessenger.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public List<ContactDTO> list(Principal principal) {
        return contactService.listContacts(principal);
    }

    @GetMapping("/requests")
    public List<ContactDTO> requests(Principal principal) {
        return contactService.incomingRequests(principal);
    }

    public record AddContactRequest(String user) {}

    @PostMapping
    public ContactDTO add(@RequestBody AddContactRequest req, Principal principal) {
        return contactService.addContact(req.user(), principal);
    }

    @PostMapping("/accept")
    public ContactDTO accept(@RequestBody Map<String, String> body, Principal principal) {
        String user = body.get("user");
        return contactService.accept(user, principal);
    }

    @DeleteMapping
    public ResponseEntity<Void> remove(@RequestParam("user") String user, Principal principal) {
        contactService.remove(user, principal);
        return ResponseEntity.noContent().build();
    }
}
