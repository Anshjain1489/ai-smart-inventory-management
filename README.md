@"
# AI Smart Inventory Management

An AI-powered inventory management backend built using Java, Spring Boot, MySQL, Spring AI and Google Gemini.

## Features

- Inventory and stock management
- Product and supplier management
- Sales and purchase order management
- JWT authentication
- Role-based authorization (ADMIN, MANAGER, VIEWER)
- AI-powered inventory insights
- Demand forecasting
- Database migrations using Flyway
- Docker containerization

## Technology Stack

- Java 25
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- MySQL 8
- Spring AI / Google Gemini
- Flyway
- Maven
- Docker

## Running Locally

Configure your environment variables in `.env` using `.env.example` as a template.

Start the application:

```powershell
docker compose up -d --build