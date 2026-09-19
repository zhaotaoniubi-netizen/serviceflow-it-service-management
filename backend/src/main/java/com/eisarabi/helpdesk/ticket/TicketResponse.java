package com.eisarabi.helpdesk.ticket;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketCategory category,
        TicketPriority priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
