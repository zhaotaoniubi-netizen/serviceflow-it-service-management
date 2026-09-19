package com.eisarabi.helpdesk.ticket;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TicketService {
    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
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
        ticket.setRequester(request.requester().trim());

        if (request.assignee() != null && !request.assignee().isBlank()) {
            ticket.setAssignee(request.assignee().trim());
        }
        validateAssignmentRule(request.status(), ticket.getAssignee());

        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse update(Long id, TicketRequest request) {
        Ticket ticket = getTicket(id);
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
        TicketStatus targetStatus =
        request.status() == null ? ticket.getStatus() : request.status();

        validateAssignmentRule(targetStatus, ticket.getAssignee());

        ticket.setStatus(targetStatus);
        return TicketResponse.from(ticketRepository.save(ticket));
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
}
