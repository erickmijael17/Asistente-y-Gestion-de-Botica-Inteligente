# Botica Amanecer — Proyecto Spring Boot Base

## Resumen
Scaffolding inicial del proyecto Botica Amanecer como aplicación Spring Boot con Java 17, Maven, JPA y MySQL.

## Git Operations
- Eliminar rama remota `origin/proyecto`
- Renombrar remote `origin` → `Asistente y Gestion de Botica Inteligente`
- Push estructura inicial a `main`

## Estructura del proyecto

```
BoticaAmanecer/
├── pom.xml
├── src/main/java/com/botica/amanecer/
│   ├── BoticaAmanecerApplication.java
│   ├── config/
│   │   └── CorsConfig.java
│   ├── controller/
│   │   └── HealthController.java
│   ├── service/
│   ├── repository/
│   ├── model/
│   │   ├── entity/
│   │   └── dto/
│   └── exception/
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   └── application.yml
├── src/test/java/com/botica/amanecer/
│   └── BoticaAmanecerApplicationTests.java
├── .gitignore
└── README.md
```

## Tecnologías
- Java 22
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Lombok
- Validation
- Spring Boot Test (JUnit 5)

## Arquitectura
Capas: Controller → Service → Repository → Entity. Configuración CORS para frontend. Manejo global de excepciones.

## Próximos pasos
Implementar scaffolding según diseño aprobado.
