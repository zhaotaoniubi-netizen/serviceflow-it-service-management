package com.eisarabi.helpdesk.ticket;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<TicketResponse> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority) {
        return ticketService.findAll(status, priority);
    }

    @GetMapping("/{id}")
    public TicketResponse getTicketById(@PathVariable Long id) {
        return ticketService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(@Valid @RequestBody TicketRequest ticket) {
        return ticketService.create(ticket);
    }

    @PutMapping("/{id}")
    public TicketResponse update(
        @PathVariable Long id,
        @Valid @RequestBody TicketRequest request,
        @RequestHeader(
                value = "X-User-Role",
                defaultValue = "EMPLOYEE"
        ) UserRole role) {

    requireSupportOrAdmin(role);

    return ticketService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long id,
        @RequestHeader(
                value = "X-User-Role",
                defaultValue = "EMPLOYEE"
        ) UserRole role) {

    requireAdmin(role);

    ticketService.delete(id);
}

    @GetMapping("/{id}/activities")
    public List<TicketActivity> getActivities(@PathVariable Long id) {
    return ticketService.getActivities(id);
    }
    private void requireSupportOrAdmin(UserRole role) {
    if (role == UserRole.EMPLOYEE) {
        throw new AccessDeniedException(
                "IT Support or Admin role is required for this operation"
        );
    }
}

    private void requireAdmin(UserRole role) {
    if (role != UserRole.ADMIN) {
        throw new AccessDeniedException(
                "Admin role is required for this operation"
        );
    }
}


}
