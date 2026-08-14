# Arquitectura C4 - Botica Inteligente

## 1. Diagrama de Contexto (Nivel 1)

Muestra el sistema en su conjunto y cómo los usuarios interactúan con él.

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml

title Diagrama de Contexto de Sistema para Botica Inteligente

Person(gerente, "Gerente (Dueño)", "Visualiza reportes, gestiona stock y alertas")
Person(vendedor, "Vendedor (Farmacéutico)", "Realiza ventas y consulta chatbot")

System(botica_system, "Sistema de Gestión de Botica Inteligente", "Plataforma central que maneja inventario y ventas")

Rel(gerente, botica_system, "Gestiona el negocio usando", "HTTPS")
Rel(vendedor, botica_system, "Registra ventas y consultas", "HTTPS")

@enduml
```

> La autenticación es **JWT nativo** (Spring Security): el backend firma, emite y valida los tokens. No hay servidor de identidades externo (Keycloak/OAuth2).

## 2. Diagrama de Contenedores (Nivel 2)

Desglosa el sistema en sus principales contenedores (Frontend, Backend, Base de Datos, IA).

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

title Diagrama de Contenedores para Botica Inteligente

Person(gerente, "Gerente (Dueño)", "Supervisa reportes e inventario.")
Person(vendedor, "Vendedor (Farmacéutico)", "Realiza ventas y consultas.")

System_Boundary(c1, "Sistema Botica Inteligente") {
    Container(spa, "Aplicación Web (Frontend)", "React + Vite", "Provee la interfaz de usuario interactiva.")
    Container(backend, "Monolito API (Backend)", "Spring Boot", "Lógica de negocio, reglas de ventas, autenticación JWT nativa.")
    ContainerDb(db, "Base de Datos", "PostgreSQL", "Almacena catálogo, inventario, ventas y roles.")
    Container(chatbot, "Módulo de Chatbot IA", "LLM", "Accede a la BD para recomendar alternativas de pastillas.")
}

Rel(gerente, spa, "Usa", "HTTPS")
Rel(vendedor, spa, "Usa", "HTTPS")

Rel(spa, backend, "Inicia sesión y consume API REST", "JSON/HTTPS (JWT Bearer)")

Rel(backend, db, "Lee y escribe datos", "JDBC")
Rel(backend, chatbot, "Consulta recomendaciones", "API Interna")
Rel(chatbot, db, "Consulta propiedades de productos", "JDBC/VectorSearch")

@enduml
```

## 3. Diagrama de Componentes (Nivel 3) - Backend Monolito

El Diagrama de Componentes hace "zoom" dentro de un contenedor específico (en este caso, el Backend) para mostrar sus módulos internos principales y cómo se relacionan. **No necesitamos tener el frontend desarrollado para realizar este nivel**, ya que describe puramente la estructura interna del backend.

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

title Diagrama de Componentes - Monolito API (Backend)

Container(spa, "Aplicación Web (Frontend)", "React + Vite", "Interfaz de usuario.")
ContainerDb(db, "Base de Datos", "PostgreSQL", "Almacena datos del negocio.")

Container_Boundary(backend_api, "Monolito API (Spring Boot)") {
    Component(security_config, "Módulo de Seguridad", "Spring Security, JWT nativo", "Filtro JWT, autenticación con BCrypt y roles ROLE_OWNER/ROLE_SELLER.")
    Component(ventas_module, "Módulo de Ventas", "REST Controller, Service", "Lógica de creación de ventas con usuario autenticado.")
    Component(productos_module, "Módulo de Productos", "REST Controller, Service", "Catálogo y propiedades de productos.")
    Component(categorias_module, "Módulo de Categorías/Lab", "REST Controller, Service", "Clasificación e inventario base.")
}

Rel(spa, security_config, "Peticiones a endpoints", "JSON/HTTPS")

Rel(security_config, ventas_module, "Enruta peticiones autenticadas a")
Rel(security_config, productos_module, "Enruta peticiones autenticadas a")
Rel(security_config, categorias_module, "Enruta peticiones autenticadas a")

Rel(ventas_module, productos_module, "Consulta precios y stock", "Llamada de método")

Rel(ventas_module, db, "Registra ventas y detalles", "JPA/Hibernate")
Rel(productos_module, db, "Lee/Escribe catálogo", "JPA/Hibernate")
Rel(categorias_module, db, "Lee/Escribe clasificación", "JPA/Hibernate")

@enduml
```

> **Nota sobre el Nivel 4 (Código):**
> El modelo C4 consta de 4 niveles. Sin embargo, **el Nivel 4 (Diagrama de Clases/Código) rara vez se dibuja manualmente** en la actualidad, ya que cambia muy rápido con cada commit y es preferible generarlo automáticamente con herramientas del IDE (como IntelliJ). Con los Niveles 1, 2 y 3 tienes la arquitectura completamente documentada a nivel conceptual.
