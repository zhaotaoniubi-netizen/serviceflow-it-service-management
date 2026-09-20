package com.eisarabi.helpdesk.ticket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository repository;
        @Autowired
        private TicketActivityRepository activityRepository;

    @BeforeEach
        void clearDatabase() {
    activityRepository.deleteAll();
    repository.deleteAll();
        }

    @Test
    void supportsCreateReadUpdateFilterAndDeleteWorkflow() throws Exception {
        String createdJson = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Email unavailable","description":"Cannot connect to mailbox","requester":"Louis Zhao","assignee":"","category":"ACCOUNT","priority":"HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();

        long id = Long.parseLong(createdJson.replaceAll(".*\\\"id\\\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Email unavailable"));

        mockMvc.perform(put("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Email restored","description":"Mailbox is available","requester":"Louis Zhao","assignee":"Alex Chan","status":"RESOLVED","category":"ACCOUNT","priority":"HIGH"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(get("/api/tickets").param("status", "RESOLVED").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/tickets/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tickets/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void returnsStructuredValidationErrors() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"description\":\"\",\"requester\":\" \",\"assignee\":\"\",\"category\":null,\"priority\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.title").value("Title is required"))
                .andExpect(jsonPath("$.validationErrors.requester").value("Requester is required"))
                .andExpect(jsonPath("$.validationErrors.category").value("Category is required"))
                .andExpect(jsonPath("$.validationErrors.priority").value("Priority is required"));
    }

    @Test
    void allowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(get("/api/tickets").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
    @Test
        void rejectsAssignedStatusWithoutAssignee() throws Exception {
    mockMvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "title":"VPN access issue",
                              "description":"Unable to connect to company VPN",
                              "requester":"Louis Zhao",
                              "assignee":"",
                              "status":"ASSIGNED",
                              "category":"NETWORK",
                              "priority":"HIGH"
                            }
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                    .value("Assignee is required when status is ASSIGNED"));
}
@Test
void recordsCreatedActivityForNewTicket() throws Exception {
    String createdJson = mockMvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "title":"Audit trail integration test",
                              "description":"Verify ticket creation is recorded",
                              "requester":"Louis Zhao",
                              "assignee":"",
                              "status":"NEW",
                              "category":"SOFTWARE",
                              "priority":"MEDIUM"
                            }
                            """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    long id = Long.parseLong(
            createdJson.replaceAll(".*\\\"id\\\":(\\d+).*", "$1")
    );

    mockMvc.perform(get("/api/tickets/{id}/activities", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].ticketId").value(id))
            .andExpect(jsonPath("$[0].type").value("CREATED"))
            .andExpect(jsonPath("$[0].details").value("Ticket created"));
}
@Test
void reportsOverdueSlaWhenDueDateHasPassed() throws Exception {
    String createdJson = mockMvc.perform(post("/api/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "title":"Overdue SLA test",
                              "description":"Verify overdue SLA status",
                              "requester":"Louis Zhao",
                              "assignee":"",
                              "status":"NEW",
                              "category":"SOFTWARE",
                              "priority":"HIGH"
                            }
                            """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    long id = Long.parseLong(
            createdJson.replaceAll(".*\\\"id\\\":(\\d+).*", "$1")
    );

    Ticket ticket = repository.findById(id).orElseThrow();

    ticket.setDueAt(LocalDateTime.now().minusHours(1));
    repository.save(ticket);

    mockMvc.perform(get("/api/tickets/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slaStatus").value("OVERDUE"));
}
}
