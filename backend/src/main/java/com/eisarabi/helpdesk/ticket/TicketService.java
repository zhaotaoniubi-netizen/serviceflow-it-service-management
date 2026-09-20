package com.eisarabi.helpdesk.ticket;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketActivityRepository ticketActivityRepository;

    public TicketService(
        TicketRepository ticketRepository,
        TicketActivityRepository ticketActivityRepository) {
    this.ticketRepository = ticketRepository;
    this.ticketActivityRepository = ticketActivityRepository;
    }

    public List<TicketResponse> findAll(TicketStatus status, TicketPriority priority) {
        List<Ticket> tickets;
        if (status != null && priority != null) {
            tickets = ticketRepository.findByStatusAndPriorityOrderByCreatedAtDesc(status, priority);
        } else if (status != null) {
            tickets = ticketRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (priority != null) {
            tickets = ticketRepository.findByPriorityOrderByCreatedAtDesc(priority);
        } else {
            tickets = ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return tickets.stream().map(TicketResponse::from).toList();
    }

    public TicketResponse findById(Long id) {
        return TicketResponse.from(getTicket(id));
    }

    @Transactional
    public TicketResponse create(TicketRequest request) {
        Ticket ticket = new Ticket(request.title().trim(), request.description().trim(),  request.category(), request.priority());
        ticket.setDueAt(
        calculateDueAt(
                LocalDateTime.now(),
                request.priority()
        )
        );
        ticket.setRequester(request.requester().trim());

        if (request.assignee() != null && !request.assignee().isBlank()) {
            ticket.setAssignee(request.assignee().trim());
        }
        validateAssignmentRule(request.status(), ticket.getAssignee());

        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
        Ticket savedTicket = ticketRepository.save(ticket);

        ticketActivityRepository.save(
        new TicketActivity(
                savedTicket.getId(),
                TicketActivityType.CREATED,
                "Ticket created"
        )
);

return TicketResponse.from(savedTicket);
    }

    @Transactional
    public TicketResponse update(Long id, TicketRequest request) {
        Ticket ticket = getTicket(id);
        TicketStatus oldStatus = ticket.getStatus();
        String oldAssignee = ticket.getAssignee();
        TicketPriority oldPriority = ticket.getPriority();  
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setRequester(request.requester().trim());
            ticket.setAssignee(
                    request.assignee() == null || request.assignee().isBlank()
                            ? null
                            : request.assignee().trim()
            );
        ticket.setCategory(request.category());
        ticket.setPriority(request.priority());
        if (oldPriority != ticket.getPriority()) {
    ticket.setDueAt(
            calculateDueAt(
                    ticket.getCreatedAt(),
                    ticket.getPriority()
            )
    );
}
        TicketStatus targetStatus =
        request.status() == null ? ticket.getStatus() : request.status();

        validateAssignmentRule(targetStatus, ticket.getAssignee());

        ticket.setStatus(targetStatus);
        Ticket savedTicket = ticketRepository.save(ticket);

if (oldStatus != savedTicket.getStatus()) {
    ticketActivityRepository.save(
            new TicketActivity(
                    savedTicket.getId(),
                    TicketActivityType.STATUS_CHANGED,
                    "Status changed from "
                            + oldStatus
                            + " to "
                            + savedTicket.getStatus()
            )
    );
}

if (!Objects.equals(oldAssignee, savedTicket.getAssignee())) {
    ticketActivityRepository.save(
            new TicketActivity(
                    savedTicket.getId(),
                    TicketActivityType.ASSIGNEE_CHANGED,
                    "Assignee changed from "
                            + formatValue(oldAssignee)
                            + " to "
                            + formatValue(savedTicket.getAssignee())
            )
    );
}

if (oldPriority != savedTicket.getPriority()) {
    ticketActivityRepository.save(
            new TicketActivity(
                    savedTicket.getId(),
                    TicketActivityType.PRIORITY_CHANGED,
                    "Priority changed from "
                            + oldPriority
                            + " to "
                            + savedTicket.getPriority()
            )
    );
}

return TicketResponse.from(savedTicket);
    }

    @Transactional
    public void delete(Long id) {
        ticketRepository.delete(getTicket(id));
    }

    private Ticket getTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }
    private void validateAssignmentRule(TicketStatus status, String assignee) {
    if (status == TicketStatus.ASSIGNED
            && (assignee == null || assignee.isBlank())) {
        throw new TicketBusinessRuleException(
                "Assignee is required when status is ASSIGNED"
        );
    }
}
public List<TicketActivity> getActivities(Long ticketId) {
    getTicket(ticketId);

    return ticketActivityRepository
            .findByTicketIdOrderByCreatedAtDesc(ticketId);
}

private String formatValue(String value) {
    return value == null || value.isBlank()
            ? "Unassigned"
            : value;
}
private LocalDateTime calculateDueAt(
        LocalDateTime baseTime,
        TicketPriority priority) {

    LocalDateTime effectiveBaseTime =
            baseTime != null ? baseTime : LocalDateTime.now();

    long hours = switch (priority) {
        case CRITICAL -> 4;
        case HIGH -> 8;
        case MEDIUM -> 24;
        case LOW -> 48;
    };

    return effectiveBaseTime.plusHours(hours);
}
}
