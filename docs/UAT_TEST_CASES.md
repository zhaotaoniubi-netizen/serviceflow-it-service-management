# ServiceFlow UAT Test Cases

| ID | Scenario | Test Steps | Expected Result | Status |
| --- | --- | --- | --- | --- |
| UAT-01 | Create ticket | Create a ticket with valid requester, category and priority | Ticket is created with status NEW | PASS |
| UAT-02 | Assignment rule | Set status to ASSIGNED without an assignee | System rejects request and displays assignment validation message | PASS |
| UAT-03 | Assign ticket | Add an assignee and change status to ASSIGNED | Ticket is successfully updated | PASS |
| UAT-04 | Audit trail | Change ticket status, assignee and priority | Activity History records each change | PASS |
| UAT-05 | SLA calculation | Create a HIGH priority ticket | Due time is calculated as creation time + 8 hours | PASS |
| UAT-06 | SLA completion | Change ticket status to RESOLVED | SLA status changes to COMPLETED | PASS |
| UAT-07 | Search and filtering | Search by keyword and filter by status / priority | Only matching tickets are displayed | PASS |
| UAT-08 | Employee permissions | Select EMPLOYEE role | Edit and Delete actions are unavailable | PASS |
| UAT-09 | IT Support permissions | Select IT_SUPPORT role | Edit is available but Delete is unavailable | PASS |
| UAT-10 | Admin permissions | Select ADMIN role | Edit and Delete actions are available | PASS |