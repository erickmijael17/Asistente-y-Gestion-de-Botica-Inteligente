# Arquitectura C4 - Botica Inteligente

## 1. Diagrama de Contexto (Nivel 1)

Muestra el sistema en su conjunto y cómo los usuarios interactúan con él.

```mermaid
C4Context
    title Diagrama de Contexto de Sistema para Botica Inteligente
    
    Person(gerente, "Gerente (Dueño)", "Supervisa reportes, alertas, inventario y ventas globales.")
    Person(vendedor, "Vendedor (Farmacéutico)", "Atiende clientes, registra ventas y consulta recomendaciones de pastillas.")
    
    System(botica_system, "Sistema de Gestión de Botica Inteligente", "Plataforma central que maneja inventario, ventas, usuarios y provee un asistente IA (Chatbot) para alternativas médicas basadas en la base de datos local.")
    
    Rel(gerente, botica_system, "Visualiza reportes, gestiona stock y alertas")
    Rel(vendedor, botica_system, "Realiza ventas y consulta chatbot para sugerir alternativas al cliente")
```

## 2. Diagrama de Contenedores (Nivel 2)

Desglosa el sistema en sus principales contenedores (Frontend, Backend, Base de Datos, IA).

```mermaid
C4Container
    title Diagrama de Contenedores para Botica Inteligente
    
    Person(gerente, "Gerente (Dueño)", "Supervisa reportes e inventario.")
    Person(vendedor, "Vendedor (Farmacéutico)", "Realiza ventas y consultas.")

    System_Boundary(c1, "Sistema Botica Inteligente") {
        Container(spa, "Aplicación Web (Frontend)", "Angular / React", "Provee la interfaz de usuario interactiva para ventas, inventario y chat.")
        Container(backend, "Monolito API (Backend)", "Spring Boot 3", "Lógica de negocio, reglas de ventas, alertas y endpoints.")
        ContainerDb(db, "Base de Datos", "PostgreSQL", "Almacena catálogo, inventario, ventas y roles.")
        Container(keycloak, "Gestor de Identidades", "Keycloak", "Autenticación, validación de usuarios y roles JWT.")
        Container(chatbot, "Módulo de Chatbot IA", "LLM / RAG (LangChain)", "Accede a la BD para recomendar alternativas de pastillas según síntomas (ej. 'para la fiebre').")
    }

    Rel(gerente, spa, "Usa", "HTTPS")
    Rel(vendedor, spa, "Usa", "HTTPS")
    
    Rel(spa, keycloak, "Inicia Sesión", "OAuth2 / OIDC")
    Rel(spa, backend, "Consume API REST", "JSON/HTTPS")
    
    Rel(backend, keycloak, "Valida Tokens JWT", "HTTPS")
    Rel(backend, db, "Lee y escribe datos", "JDBC")
    Rel(backend, chatbot, "Consulta recomendaciones médicas", "API Interna")
    Rel(chatbot, db, "Consulta propiedades de productos", "JDBC/VectorSearch")
```
