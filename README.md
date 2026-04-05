# Cinema tickets Java

## Requirements

* Java 21
* Maven

From the project root (`cinema-tickets-java`), and run:

```bash
mvn test
```

This compiles the codebase and executes the full unit test suite.

---

## How I designed the solution

### What I charge

I model payment in **whole pounds as integer values**, which aligns directly with the exercise API contract and avoids unnecessary currency conversion complexity.

The ticket pricing is:

* **Adult:** 25
* **Child:** 15
* **Infant:** 0

This keeps the payment boundary clean, predictable, and fully aligned with the supplied brief.

---

### Who I reserve seats for

I reserve seats **only for adult and child tickets**, because those are the only attendees who require physical seats.

I deliberately exclude **infants** from seat reservations because they are free of charge and are expected to sit on an adult’s lap.

However, infants still contribute to:

* the **overall basket size**
* the **25-ticket maximum purchase limit**

This ensures the validation logic remains faithful to the business constraints even though infants do not consume seat capacity.

---

### Adult supervision rule

I enforce the business rule that **any basket containing child or infant tickets must also include at least one adult ticket**.

This prevents invalid purchases from progressing into either the payment or seat-reservation workflow.

---

### My lap-capacity rule (intentional product decision)

I made a deliberate engineering decision to apply a **stricter interpretation** of the lap-seating requirement.

Because the brief states that infants sit on an adult’s lap, I only allow:

> **one infant per adult ticket**

Examples:

* 2 adults + 2 infants → valid
* 1 adult + 2 infants → rejected

I recognise this goes beyond the minimum written requirement, which only specifies that at least one adult must be present.

I intentionally documented this as a **product-level assumption**, so if the business later changes the lap-capacity policy, the change remains isolated to a single validation rule without impacting pricing or seat allocation.

This kind of explicit rule isolation improves maintainability and makes the decision-making process clear during code reviews and interview walkthroughs.

---

## Defensive validation and engineering safeguards

### Invalid ticket lines

If a ticket request is created with a **null ticket type**, the constructor fails immediately.

I prefer failing fast at the boundary so invalid state never enters the core booking workflow.

---

### Dependency safety

`TicketServiceImpl` requires valid seat and payment services at construction time.

If either dependency is missing, Java immediately surfaces the wiring issue.

From a senior engineering perspective, I treat this as an important **misconfiguration safeguard**, because it moves infrastructure failures to startup time rather than allowing runtime surprises during a booking transaction.

---

## Implementation detail

For implementation-level detail, I describe the validation flow directly in `TicketServiceImpl`, particularly within `validateAndAggregate`, where I centralise:

* basket validation
* business-rule enforcement
* pricing aggregation
* seat calculation

This keeps the orchestration flow readable while ensuring the core business rules remain easy to evolve.
