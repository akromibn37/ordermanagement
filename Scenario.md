Scenario
You are a backend engineer on a team developing an e-commerce platform. A
merchant sells a popular product, “Popular T-Shirt”, across multiple e-commerce
channels: their own Shopify store and another Shopify store. All of their physical
inventory is managed centrally in a Warehouse Management System (WMS).
The critical business challenge is to synchronize inventory levels across all
channels in near real-time to prevent overselling and to reliably process orders by
communicating with the WMS.

Core Business Flow
1. A customer places an order for a "Popular T-Shirt" on Shopify.
2. Shopify sends a webhook notification to our Order Management System
(OMS).
3. The OMS receives and validates the order data, then checks the central inventory
in the WMS.
4. If stock is available, the OMS performs the following actions:
a. Decrements the inventory count in its own database (to allocate the stock).
b. Sends an asynchronous event to update the stock level on the other Shopify
store to prevent overselling.
c. Sends a fulfillment request to the WMS so the warehouse team can pick, pack,
and ship the order.
5. If stock is not available, the order is marked appropriately, and a notification
might be sent.
Your Task
Your task is to design and partially implement a system that handles this process.
1. System Design (Most Important)
This is the most critical part of the challenge. In a README.md file, please provide a
detailed explanation of your system design, covering the following points:
● a. High-Level Architecture: Create a diagram illustrating the main components
of your proposed solution and describe the responsibilities of each.
● b. Service Boundaries & Communication: Considering a microservices
architecture, how would you decompose this problem? Justify your chosen
boundaries. For each communication scenario (synchronous/asynchronous), what
protocol would you choose (gRPC, REST, message queue, etc.) and why?
● c. API Design and Data Mapping: Design a DTO for the incoming Shopify
webhook, explaining which fields you chose to include/omit and why. Also, briefly
describe the API contract for sending a fulfillment request to the WMS.
● d. Data Consistency & Race Conditions: Explain your strategy for preventing
overselling. Discuss the trade-offs of your approach.
● e. Data Modeling: Provide the PostgreSQL schema for the core tables and
explain your key design choices.
● f. Error Handling & Resiliency: Describe your retry/recovery strategy for failures
in updating the other Shopify store's inventory or if the WMS is temporarily
unavailable.
2. Implementation
Implement the core parts of your design in Kotlin. The implementation must reflect
the principles of Domain-Driven Design (DDD) and Clean Architecture.
Your implementation should cover:
1. Processing the incoming order data.
2. Checking for available inventory.
3. Persisting the order and updating inventory if stock is available.
4. Simulating the publishing of an event to update other channels.
5. Simulating sending a fulfillment request.
3. Testing
Write tests for critical business logic.
Out of Scope
To help you focus, you do not need to consider: User Management,
Authentication/Authorization, Multi-tenancy, Complex Product Management
(variations), or any UI.

References
I'm attaching some API references to help with your implementation.
As for the requirements, you must use Shopify as the Sales Channel, but the choice of WMS is
up to you. You can use the links below as a reference to make a realistic API request, or you
can simply extract the core ideas and reproduce them with a simplified mock. Either approach is
acceptable.
● Shopify
○ Shopify developer documentation (https://shopify.dev/docs)
● WMS
○ ShipBob's API (https://developer.shipbob.com/introduction)
○ Developer Portal | Flexport (https://developers.flexport.com/)




Missing
4. Race Condition Prevention
Requirement: "Explain your strategy for preventing overselling"
Diagram: Shows basic inventory updates but no concurrency control mechanisms