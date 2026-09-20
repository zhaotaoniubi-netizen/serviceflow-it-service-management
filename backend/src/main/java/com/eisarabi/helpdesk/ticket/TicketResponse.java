package com.eisarabi.helpdesk.ticket;

import java.time.LocalDateTime;

public record TicketResponse(

        Long id,

        String title,

        String description,

        String requester,

        String assignee,

        TicketStatus status,

        TicketCategory category,

        TicketPriority priority,

        LocalDateTime dueAt,

        SlaStatus slaStatus,

        LocalDateTime createdAt,

        LocalDateTime updatedAt) {

    public static TicketResponse from(Ticket ticket) {

        return new TicketResponse(

                ticket.getId(),

                ticket.getTitle(),

                ticket.getDescription(),

                ticket.getRequester(),

                ticket.getAssignee(),

                ticket.getStatus(),

                ticket.getCategory(),

                ticket.getPriority(),

                ticket.getDueAt(),

                calculateSlaStatus(ticket),

                ticket.getCreatedAt(),

                ticket.getUpdatedAt());
    }

    private static SlaStatus calculateSlaStatus(Ticket ticket) {

        if (ticket.getStatus() == TicketStatus.RESOLVED
                || ticket.getStatus() == TicketStatus.CLOSED) {
            return SlaStatus.COMPLETED;
        }

        if (ticket.getDueAt() != null
                && LocalDateTime.now().isAfter(ticket.getDueAt())) {
            return SlaStatus.OVERDUE;
        }

        return SlaStatus.ON_TRACK;
    }
}