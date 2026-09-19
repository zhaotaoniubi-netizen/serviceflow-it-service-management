package com.eisarabi.helpdesk.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be 120 characters or fewer")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description must be 2000 characters or fewer")
        String description,

        @NotBlank(message = "Requester is required")
        @Size(max = 100, message = "Requester must be 100 characters or fewer")
        String requester,

        @Size(max = 100, message = "Assignee must be 100 characters or fewer")
        String assignee,

        TicketStatus status,

        @NotNull(message = "Category is required")
        TicketCategory category,

        @NotNull(message = "Priority is required")
        TicketPriority priority
) {
}
