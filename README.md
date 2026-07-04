# springboot-kafka-microservices

Proyecto educativo de microservicios con **Spring Boot 3.3**, **Apache Kafka** y **MySQL**.  
Demuestra comunicación asíncrona entre servicios mediante eventos publicados en Kafka.

---

## Problema que resuelve

En una arquitectura monolítica, todos los módulos de negocio (pedidos, facturación, inventario, etc.) están acoplados en un único proceso: si falla la facturación, falla toda la aplicación; escalar el módulo de pedidos implica escalar también el resto aunque no lo necesiten.

Este proyecto ilustra cómo desacoplar esos módulos usando **comunicación asíncrona basada en eventos** con Apache Kafka:

| Problema en el monolito | Solución con microservicios + Kafka |
|---|---|
| Acoplamiento directo entre módulos | Cada servicio es independiente y se comunica por eventos |
| Una falla en facturación detiene los pedidos | Si billing-service cae, Kafka retiene los eventos y los procesa cuando vuelve |
| Escalar un módulo escala todo | Cada microservicio escala de forma independiente |
| Transacciones distribuidas complejas | Consistencia eventual: cada servicio gestiona su propia base de datos |
| Deploy conjunto de toda la app | Deploy independiente por servicio |

### Caso concreto implementado

Cuando un cliente realiza un **pedido** (`POST /api/orders`), el sistema debe generar automáticamente una **factura**. En lugar de que order-service llame directamente a billing-service (acoplamiento fuerte, falla síncrona), publica un evento `OrderEvent` en Kafka. billing-service lo consume de forma independiente y genera la factura. Si billing-service está caído, el mensaje queda en el tópico y se procesa en cuanto el servicio vuelve a estar disponible.

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                        Cliente HTTP                         │
└─────────────────────────────┬───────────────────────────────┘
                              │ POST /api/orders
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      order-service (:8081)                  │
│                                                             │
│  OrderController → OrderService → OrderRepository (MySQL)  │
│                         │                                   │
│                         │ Publica OrderEvent                │
│                         ▼                                   │
│              KafkaTemplate → topic: order-created           │
└─────────────────────────────┬───────────────────────────────┘
                              │ Kafka (localhost:9092)
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    billing-service (:8082)                  │
│                                                             │
│  OrderConsumer → BillingService → InvoiceRepository (MySQL) │
│                                                             │
│  GET /api/invoices → InvoiceController                      │
└─────────────────────────────────────────────────────────────┘
```

### Flujo principal

1. El cliente envía `POST /api/orders` con el producto, cantidad y precio.
2. **order-service** persiste la orden en MySQL y publica un `OrderEvent` en el tópico `order-created` de Kafka.
3. **billing-service** consume el evento, calcula el total y persiste una `Invoice` en su propia base de datos MySQL.
4. Las facturas pueden consultarse con `GET /api/invoices`.

---

## Módulos

| Módulo | Puerto | Responsabilidad |
|---|---|---|
| `order-service` | 8081 | Crear órdenes y publicar eventos Kafka |
| `billing-service` | 8082 | Consumir eventos y generar facturas |

---

## Tecnologías

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.3.3 | Framework de aplicación |
| Spring Kafka | (incluido en Boot) | Productor/Consumidor Kafka |
| Spring Data JPA | (incluido en Boot) | Persistencia ORM |
| Apache Kafka | 7.5.0 (imagen Confluent) | Message broker |
| MySQL | 8+ | Base de datos relacional |
| Docker Compose | — | Infraestructura local (Kafka + Zookeeper) |
| SLF4J + Logback | (incluido en Boot) | Logging estructurado |

---

## Requisitos previos

- Java 17+
- Maven 3.8+
- Docker + Docker Compose
- MySQL 8+ corriendo localmente (o ajustar `DB_HOST` según tu entorno)

---

## Configuración local

### 1. Clonar el repositorio

```bash
git clone https://github.com/<tu-usuario>/springboot-kafka-microservices.git
cd springboot-kafka-microservices
```

### 2. Crear el archivo de variables de entorno

```bash
cp .env.example .env
```

Editar `.env` con los valores reales de tu entorno. Ver sección **Variables de entorno** más abajo.

### 3. Levantar Kafka con Docker

```bash
docker compose up -d
```

Esto levanta **Zookeeper** (puerto 2181) y **Kafka** (puerto 9092). Verificá que ambos contenedores estén corriendo:

```bash
docker ps
```

Debería mostrar algo como:

```
CONTAINER ID   IMAGE                              STATUS         NAMES
b17bc9a8a1f3   confluentinc/cp-kafka:7.5.0        Up 5 minutes   springboot-kafka-microservices-kafka-1
1187bfe76ec3   confluentinc/cp-zookeeper:7.5.0    Up 5 minutes   springboot-kafka-microservices-zookeeper-1
```

#### Verificar / crear el tópico `order-created`

El tópico se crea automáticamente la primera vez que order-service publica un mensaje. Para verificarlo (o crearlo manualmente antes de arrancar los servicios):

```bash
# 1. Entrar al contenedor de Kafka
docker exec -it springboot-kafka-microservices-kafka-1 bash

# 2. Listar los tópicos existentes
kafka-topics --list --bootstrap-server localhost:9092

# Salida esperada:
# __consumer_offsets
# order-created
```

Si el tópico aún no existe (antes del primer envío), podés crearlo manualmente:

```bash
# Dentro del contenedor
kafka-topics --create \
  --topic order-created \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1
```

Para salir del contenedor:

```bash
exit
```

> **Nota:** El nombre del contenedor Kafka sigue el patrón `<directorio-del-proyecto>-kafka-1`.  
> Si clonaste el repo en otro directorio, ajustá el nombre en consecuencia.

### 4. Crear las bases de datos MySQL

```sql
CREATE DATABASE order_db;
CREATE DATABASE billing_db;
```

### 5. Compilar el proyecto

```bash
./mvnw clean install -DskipTests
```

### 6. Ejecutar los servicios

En terminales separadas:

```bash
# Terminal 1 — order-service
cd order-service
../mvnw spring-boot:run

# Terminal 2 — billing-service
cd billing-service
../mvnw spring-boot:run
```

---

## API Reference

### order-service

#### Crear una orden

```
POST http://localhost:8081/api/orders
Content-Type: application/json
```

**Body:**

```json
{
  "product": "Laptop",
  "quantity": 2,
  "price": 1500.00
}
```

**Respuesta exitosa (200 OK):**

```json
{
  "id": 1,
  "product": "Laptop",
  "quantity": 2,
  "price": 1500.00
}
```

---

### billing-service

#### Listar facturas

```
GET http://localhost:8082/api/invoices
```

**Respuesta exitosa (200 OK):**

```json
[
  {
    "id": 1,
    "orderId": 1,
    "total": 3000.00,
    "status": "GENERATED"
  }
]
```

---

## Variables de entorno

Todas las variables se configuran en el archivo `.env` (copiado desde `.env.example`).  
Las propiedades de `application.properties` las consumen con la sintaxis `${VARIABLE:default}`.

| Variable | Descripción | Obligatoria | Valor por defecto |
|---|---|---|---|
| `JAVA_VERSION` | Versión de Java del proyecto | No | `17` |
| `ORDER_SERVER_PORT` | Puerto del order-service | No | `8081` |
| `BILLING_SERVER_PORT` | Puerto del billing-service | No | `8082` |
| `DB_HOST` | Host de MySQL | **Sí** (prod) | `localhost` |
| `DB_PORT` | Puerto de MySQL | No | `3306` |
| `DB_NAME` | Nombre de la base de datos del order-service | No | `order_db` |
| `BILLING_DB_NAME` | Nombre de la base de datos del billing-service | No | `billing_db` |
| `DB_USERNAME` | Usuario de MySQL | **Sí** (prod) | `root` |
| `DB_PASSWORD` | Contraseña de MySQL | **Sí** | *(vacío)* |
| `KAFKA_BOOTSTRAP_SERVERS` | Dirección del broker Kafka | **Sí** | `localhost:9092` |
| `KAFKA_CONSUMER_GROUP_ID` | Consumer group del billing-service | No | `billing-group` |
| `KAFKA_TOPIC_ORDER_CREATED` | Nombre del tópico de órdenes creadas | No | `order-created` |

> **Variables obligatorias en producción:** `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`.

---

## Logging

El proyecto usa **SLF4J + Logback** (incluido en `spring-boot-starter-web`).

### Niveles configurados

| Paquete | Nivel |
|---|---|
| `com.ecommerce.order` | `DEBUG` |
| `com.ecommerce.billing` | `DEBUG` |
| `org.springframework.kafka` | `WARN` |

### Ejemplo de traza de una petición completa

```
INFO  OrderController      : POST /api/orders — product: Laptop, quantity: 2, price: 1500.0
INFO  OrderService         : Creating order — product: Laptop, quantity: 2, price: 1500.0
DEBUG OrderService         : Order persisted to database — orderId: 1
INFO  OrderService         : Publishing OrderEvent to topic 'order-created' — orderId: 1
INFO  OrderService         : OrderEvent published successfully — topic: order-created, partition: 0, offset: 0
INFO  OrderService         : Order creation completed — orderId: 1
INFO  OrderController      : POST /api/orders — completed in 87ms — orderId: 1

INFO  OrderConsumer        : Received OrderEvent from Kafka — orderId: 1, product: Laptop, quantity: 2, price: 1500.0
INFO  BillingService       : Generating invoice — orderId: 1, quantity: 2, price: 1500.0
DEBUG BillingService       : Invoice details — orderId: 1, total: 3000.0, status: GENERATED
INFO  BillingService       : Invoice persisted — invoiceId: 1, orderId: 1, total: 3000.0, status: GENERATED
INFO  OrderConsumer        : OrderEvent processed successfully — orderId: 1
```

---

## Observabilidad — Recomendaciones

El proyecto **no incluye** observabilidad avanzada actualmente. Se recomienda incorporar las siguientes herramientas:

### Spring Boot Actuator + Micrometer

```xml
<!-- pom.xml de cada módulo -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```properties
# application.properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
```

### Distributed Tracing — Micrometer Tracing + Zipkin

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### Stack completo sugerido

| Herramienta | Función |
|---|---|
| Spring Boot Actuator | Health checks, métricas HTTP |
| Micrometer + Prometheus | Exposición de métricas |
| Grafana | Dashboards y alertas |
| Zipkin / Jaeger | Trazabilidad distribuida |
| Loki + Promtail | Centralización de logs |

---

## Riesgos encontrados

### 🔴 ALTO

| # | Gravedad | Archivo | Descripción | Recomendación |
|---|---|---|---|---|
| 1 | 🔴 Alto | `order-service/application.properties` | Contraseña `root` hardcodeada | Externalizar a variable de entorno `DB_PASSWORD` ✅ *(corregido)* |
| 2 | 🔴 Alto | `billing-service/application.properties` | Contraseña `root` hardcodeada | Externalizar a variable de entorno `DB_PASSWORD` ✅ *(corregido)* |
| 3 | 🔴 Alto | `KafkaProducerConfig.java` L27 | `"localhost:9092"` hardcodeado en código Java | Externalizar con `@Value` ✅ *(corregido)* |
| 4 | 🔴 Alto | `KafkaConsumerConfig.java` L37,42 | `"localhost:9092"` y `"billing-group"` hardcodeados | Externalizar con `@Value` ✅ *(corregido)* |

### 🟡 MEDIO

| # | Gravedad | Archivo | Descripción | Recomendación |
|---|---|---|---|---|
| 5 | 🟡 Medio | `OrderService.java` L13 | Tópico `"order-created"` como constante estática | Externalizar a `application.properties` ✅ *(corregido)* |
| 6 | 🟡 Medio | `OrderConsumer.java` L19 | Tópico `"order-created"` hardcodeado en `@KafkaListener` | Externalizar con SpEL `${...}` ✅ *(corregido)* |
| 7 | 🟡 Medio | `OrderService.java` L37,39 | `System.out.println()` usado para logging de errores Kafka | Reemplazar con `log.error()` SLF4J ✅ *(corregido)* |
| 8 | 🟡 Medio | `OrderConsumer.java` L24 | `System.out.println()` como log de consumo | Reemplazar con `log.info()` SLF4J ✅ *(corregido)* |
| 9 | 🟡 Medio | `KafkaConsumerConfig.java` L29 | `deserializer.addTrustedPackages("*")` confía en todos los paquetes | Limitar al paquete `com.ecommerce.*` |
| 10 | 🟡 Medio | Ambas apps | Sin autenticación en endpoints REST | Considerar Spring Security para entornos no educativos |

### 🟢 BAJO

| # | Gravedad | Archivo | Descripción | Recomendación |
|---|---|---|---|---|
| 11 | 🟢 Bajo | `root/src/main/resources/application.properties` | Archivo de configuración huérfano en el módulo raíz | Eliminar; no pertenece al multi-módulo Maven |
| 12 | 🟢 Bajo | Ambas apps | `spring.jpa.show-sql=true` expondrá SQL en logs de producción | Mover a perfil `local` únicamente |
| 13 | 🟢 Bajo | Ambas apps | Sin Health Checks ni Actuator | Agregar `spring-boot-starter-actuator` |
| 14 | 🟢 Bajo | Ambas apps | Sin tests de integración para flujo Kafka | Implementar con `@EmbeddedKafka` |

---

## Mejoras aplicadas

- ✅ `System.out.println()` reemplazado por SLF4J en todos los archivos
- ✅ Logs estructurados con parámetros `{}` en Controllers, Services y Consumer
- ✅ `localhost:9092` removido del código Java — inyectado vía `@Value`
- ✅ `billing-group` removido del código Java — inyectado vía `@Value`
- ✅ Tópico `order-created` externalizado a `application.properties` en ambos servicios
- ✅ Credenciales MySQL externalizadas a variables de entorno con defaults seguros
- ✅ `.env.example` creado con todos los parámetros configurables
- ✅ `.env` agregado a `.gitignore`

## Mejoras pendientes

- ⏳ Agregar Spring Boot Actuator + Micrometer para health checks y métricas
- ⏳ Implementar trazabilidad distribuida con Micrometer Tracing + Zipkin
- ⏳ Limitar `addTrustedPackages("*")` al paquete `com.ecommerce.*`
- ⏳ Agregar Spring Security para proteger los endpoints REST
- ⏳ Mover `spring.jpa.show-sql=true` a un perfil `local` (no producción)
- ⏳ Escribir tests de integración con `@EmbeddedKafka`
- ⏳ Eliminar el archivo `src/main/resources/application.properties` del módulo raíz (huérfano)
- ⏳ Agregar MySQL al `docker-compose.yml` para un entorno completamente dockerizado

---

## Estructura del proyecto

```
springboot-kafka-microservices/
├── order-service/
│   ├── src/main/java/com/ecommerce/order/service/
│   │   ├── config/
│   │   │   └── KafkaProducerConfig.java
│   │   ├── controller/
│   │   │   └── OrderController.java
│   │   ├── dto/
│   │   │   └── OrderEvent.java
│   │   ├── entity/
│   │   │   └── Order.java
│   │   ├── repository/
│   │   │   └── OrderRepository.java
│   │   ├── service/
│   │   │   └── OrderService.java
│   │   └── OrderServiceApplication.java
│   └── src/main/resources/
│       └── application.properties
├── billing-service/
│   ├── src/main/java/com/ecommerce/billing/service/
│   │   ├── config/
│   │   │   └── KafkaConsumerConfig.java
│   │   ├── consumer/
│   │   │   └── OrderConsumer.java
│   │   ├── controller/
│   │   │   └── InvoiceController.java
│   │   ├── dto/
│   │   │   └── OrderEvent.java
│   │   ├── entity/
│   │   │   └── Invoice.java
│   │   ├── repository/
│   │   │   └── InvoiceRepository.java
│   │   ├── service/
│   │   │   └── BillingService.java
│   │   └── BillingServiceApplication.java
│   └── src/main/resources/
│       └── application.properties
├── docker-compose.yml
├── .env.example
├── .gitignore
└── pom.xml
```
