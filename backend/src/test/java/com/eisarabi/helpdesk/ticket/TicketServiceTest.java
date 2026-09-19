package com.eisarabi.helpdesk.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock
    private TicketRepository repository;

    private TicketService service;

    @BeforeEach
    void setUp() {
        service = new TicketService(repository);
    }

    @Test
    void filtersByStatusAndPriority() {
        Ticket ticket = new Ticket("Printer offline", "The office printer is unreachable", TicketCategory.HARDWARE, TicketPriority.HIGH);
        when(repository.findByStatusAndPriorityOrderByCreatedAtDesc(TicketStatus.NEW, TicketPriority.HIGH))
                .thenReturn(List.of(ticket));

        List<TicketResponse> result = service.findAll(TicketStatus.NEW, TicketPriority.HIGH);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("Printer offline");
    }

    @Test
    void throwsClearExceptionWhenTicketDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ticket with id 99 was not found");
    }

    @Test
    void trimsInputWhenUpdating() {
        Ticket ticket = new Ticket("Old", "Old description", TicketCategory.OTHER, TicketPriority.LOW);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.save(ticket)).thenReturn(ticket);

        TicketResponse result = service.update(1L,
                new TicketRequest("  New title  ", "  New description  ", "Louis Zhao", "Alex Chan", TicketStatus.RESOLVED, TicketCategory.SOFTWARE, TicketPriority.HIGH));

        assertThat(result.title()).isEqualTo("New title");
        assertThat(result.description()).isEqualTo("New description");
        assertThat(result.status()).isEqualTo(TicketStatus.RESOLVED);
        verify(repository).save(ticket);
    }
}
