# Prueba Izertis Microservicio de Precios (Zara)

## Índice

Introducción

1. Motivación de usar Spring WebFlux
2. Arquitectura Hexagonal + DDD
3. Puertos de Entrada y Salida
4. Principios SOLID aplicados
5. Estructura del proyecto
6. Cómo ejecutar
7. Docker
8. Tests

# 1.Introducción

Microservicio reactivo que expone el precio aplicable a un producto de ZARA en una fecha concreta. Parte de un ejercicio técnico: la tabla PRICES contiene tarifas con rangos de fechas y prioridad. El servicio recibe applicationDate, productId y brandId y devuelve el precio vigente.

## ¿Por qué WebFlux?

| Razón                         | Impacto en Zara                                                                                                                                 |
|-------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| Escalabilidad reactiva        | Alto tráfico en campañas (Black Friday, rebajas). WebFlux usa back‑pressure y event‑loop → mejor uso de hilos, menor latencia bajo carga.       |
| No‑blocking I/O               |   La API sólo consulta BBDD; con R2DBC evitamos bloquear hilos y sacamos mayor rendimiento por pod.                                                                                                                                              |
| Menor huella de memoria       |   Clúster Kubernetes con muchos microservicios; menos hilos ⇒ menos RAM.                                                                                                                                              |
|Streaming y SSE nativos        | Futuro: push de cambios de precios en tiempo real.                                                                                                                                                |


Para un microservicio de sólo lectura, WebFlux reduce coste de infraestructura sin complicar la lógica de negocio.

## Arquitectura Hexagonal + DDD

```bash
       ┌──────────────┐
       │  Controller  │  ← Adaptador de Entrada (HTTP)
       └─────┬────────┘
             │ GetPriceQuery
    ┌────────────▼────────────┐
    │   Application Service   │  ← Caso de uso (GetPriceUseCase)
    └────────────┬────────────┘
    │ PriceRepositoryPort (out)
    ┌────────────▼────────────┐
    │  Adapter R2DBC / H2     │  ← Infra: BBDD reactiva
    └────────────┬────────────┘
    │ PriceEntity ↔ PriceMapper
           ┌─────▼────┐
           │  Domain  │  ← Modelo de negocio (Price)
           └──────────┘
```
* Dominio: Price y reglas sin frameworks.
* Application: orquesta casos de uso (PriceServiceImpl), aplica CO. Circuit Breaker.
* Adapters: HTTP entrada, R2DBC salida.
* Infra: mapeo, config, seguridad.

## Ventajas

* Separación clara de responsabilidades.
* Fácil test unitario: se mockean puertos.
* Sustituir BBDD o añadir Kafka = nuevo adaptador, sin tocar dominio.


## Puertos de Entrada y Salida

* Entrada (GetPriceQuery): DTO inmutable que representa la petición.
* Salida (PriceRepositoryPort): contrato para obtener el precio aplicable.

Esto permite inversión de dependencias: dominio define interfaces, infra las implementa.

## Principios SOLID

| Principio             | Aplicación                                                                                                                      |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------|
| Single‑Responsibility | Cada clase tiene una sola razón de cambio.Ej.: PriceServiceImpl sólo orquesta lógica; PriceR2dbcRepository sólo accede a datos. |
| Open/Closed           | Añadir nuevo adaptador (REST, gRPC) sin tocar dominio.                                                                                                                                |
| Liskov                | Puertos definen contratos; implementaciones pueden sustituirse sin romper código.                                                                                                                                |
|Interface Segreg.      |  Puertos pequeños y específicos (PriceRepositoryPort sólo tiene findApplicablePrice).                                                                                                                               |
| Dependency Inversion  |  Dominio depende de abstracciones, no de detalles (PriceRepositoryPort).                                                                                                                               |


## Estructura del proyecto

```bash
prices
├── pom.xml
├── Dockerfile
├── src
│   ├── main
│   │   ├── java/com/prueba/izertis
│   │   │   ├── PruebaIzertisApplication.java
│   │   │   ├── domain         ← Modelo & puertos dominio
│   │   │   ├── application    ← Casos de uso & DTOs
│   │   │   └──infrastructure ← Adaptadores, config, seguridad, mappers
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── schema.sql
│   │       └── data.sql
│   └── test
│       ├── java/com/prueba/izertis
│       │   └── PriceControllerIntegrationTest.java
└── README.md
```


# Como ejecutar
```bash
# Compilar
mvn clean package
# Arrancar
java -jar target/Prueba_Izertis-0.0.1-SNAPSHOT.jar \
--spring.profiles.active=dev
```
## Circuit Breaker (Resilience4j)

El microservicio envuelve el caso de uso getPrice con un Circuit Breaker configurado mediante Resilience4j:

* Objetivo -Evitar la cascada de fallos si la capa de persistencia (o cualquier dependencia externa futura, como un API de catálogo) presenta latencia elevada o caídas.
* Cómo -`@CircuitBreaker` en PriceServiceImpl monitoriza la señal Mono devuelta por la llamada al repositorio. Si el ratio de fallos supera el umbral (failureRateThreshold) el circuito se abre y corta las llamadas posteriores durante waitDurationInOpenState.
* Fallback -Cuando el circuito está abierto se ejecuta fallbackGetPrice, que responde con error 503 controlado para que el cliente reintente más tarde, manteniendo la UX consistente.
* Métricas -Resilience4j expone métricas que pueden integrarse con Prometheus/Grafana y alertar al equipo de SRE.

De esta forma el servicio sigue siendo resiliente ante picos de error de la BBDD sin saturar hilos ni degradar el resto del ecosistema.

## Docker

```bash
mvn package
docker build -f Dockerfile.dev -t izertis-dev .
# Variables opcionales pueden pasarse con -e
docker run -p 8082:8082 izertis-dev
```

## Tests

* WebTestClient valida los 5 escenarios de negocio (tabla de precios).
* Mocks de repositorio para unit tests de servicio.
* Cobertura >90% en la capa de dominio.

Proyecto construido con Java21, SpringBoot3.5, WebFlux, R2DBC y Resilience4j.

