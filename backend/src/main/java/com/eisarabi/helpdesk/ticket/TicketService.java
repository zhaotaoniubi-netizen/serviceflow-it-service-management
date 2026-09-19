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
        if (request.status() != null) ticket.setStatus(request.status());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse update(Long id, TicketRequest request) {
        Ticket ticket = getTicket(id);
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCategory(request.category());
        ticket.setPriority(request.priority());
        ticket.setStatus(request.status() == null ? ticket.getStatus() : request.status());
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public void delete(Long id) {
        ticketRepository.delete(getTicket(id));
    }

    private Ticket getTicket(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }
}
