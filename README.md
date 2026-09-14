# MiniBlog API — Spring Boot Learning Journal

A hands-on Spring Boot REST API built while learning modern backend development.
This README doubles as my **learning journal**, **revision notes**, and **interview prep**.

---

## 🎯 Project Goal

Build a job-ready Spring Boot REST API using the **modern** stack:

- Spring Boot 3.x / 4.x (Jakarta EE — no XML, no JSP, no Thymeleaf views)
- Spring Web (REST controllers + JSON)
- Spring Data JPA + Hibernate
- PostgreSQL (hosted on Supabase)
- Lombok
- Bean Validation
- (Coming) Spring Security + JWT
- (Coming) Docker + Cloud deployment

**Explicitly NOT using:**
- XML configuration
- Legacy Spring MVC (manual `DispatcherServlet`, JSP, view resolvers)
- Frontend (React / Angular / Flutter)
- Server-side rendering (Thymeleaf)

---

## 📚 Table of Contents

1. [Spring Core Recap](#1-spring-core-recap)
2. [Modern Spring Boot MVC vs Legacy Spring MVC](#2-modern-spring-boot-mvc-vs-legacy-spring-mvc)
3. [Layered Architecture](#3-layered-architecture)
4. [JPA & Hibernate — Entity Mapping](#4-jpa--hibernate--entity-mapping)
5. [All JPA Annotations Used](#5-all-jpa-annotations-used)
6. [Primary Key Generation — Deep Dive](#6-primary-key-generation--deep-dive)
7. [Hibernate Auto DDL — How the Table Was Created](#7-hibernate-auto-ddl--how-the-table-was-created)
8. [Database Connection & Configuration](#8-database-connection--configuration)
9. [Project Setup Workflow](#9-project-setup-workflow)
10. [Request Flow (REST API)](#10-request-flow-rest-api)
11. [Lessons Log](#11-lessons-log)
12. [Security Notes](#12-security-notes)
13. [Interview Q&A Prep](#13-interview-qa-prep)
14. [Progress Tracker](#14-progress-tracker)

---

## 1. Spring Core Recap

Learned earlier — included here for interview revision.

| Concept | One-line explanation |
|---|---|
| IoC (Inversion of Control) | Spring creates and manages objects instead of you |
| Spring Bean | An object managed by the Spring container |
| Spring Container / `ApplicationContext` | The runtime that holds and wires beans |
| Dependency Injection | Spring injects required objects into a class |
| Constructor Injection | Preferred injection method — via constructor |
| Tight coupling | Class depends on a concrete implementation |
| Loose coupling | Class depends on an interface |
| Programming to interfaces | Depend on abstraction, not concrete class |
| `@Component` | Generic Spring-managed bean |
| `@Service` | Bean in the business-logic layer |
| `@Repository` | Bean in the data-access layer (adds exception translation) |
| `@Controller` | Bean in the web layer (returns view names) |
| `@RestController` | `@Controller` + `@ResponseBody` → returns JSON |
| `@Bean` + `@Configuration` | Manual bean definition (instead of auto-scan) |
| `@Qualifier` | Resolve ambiguity when multiple beans match a type |
| `@Primary` | Mark one bean as the default when multiple match |
| `@SpringBootApplication` | Boot's entry-point annotation (combines `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`) |

### Bean scopes (basic)

| Scope | Meaning |
|---|---|
| `singleton` (default) | One instance per Spring container |
| `prototype` | New instance per request |
| `request` | One per HTTP request (web only) |
| `session` | One per HTTP session (web only) |

---

## 2. Modern Spring Boot MVC vs Legacy Spring MVC

| Aspect | Legacy Spring MVC | Modern Spring Boot MVC |
|---|---|---|
| Config | XML | Annotations + `application.properties` |
| Servlet | Manual `DispatcherServlet` setup | Auto-configured by Boot |
| Views | JSP / Thymeleaf | JSON via `@RestController` |
| Server | External Tomcat | Embedded Tomcat |
| Output | HTML | JSON (REST API) |
| Bootstrapping | `web.xml` | `@SpringBootApplication` |

**Server-side rendering (SSR):** `@Controller` + `Model` + return view name → HTML.
**REST API:** `@RestController` + return data → JSON.

Both are modern Spring Boot MVC. The difference is the **output type** and the **view layer**.

---

## 3. Layered Architecture

```
Client (Postman / React / mobile)
   │  HTTP (JSON)
   ▼
Controller   (@RestController)
   │  Java objects
   ▼
Service      (@Service)
   │
   ▼
Repository   (@Repository + Spring Data JPA)
   │  SQL
   ▼
PostgreSQL (Supabase)
```

**Rules:**
- Controller → Service → Repository. **Never Controller → Repository.**
- Each layer talks only to the layer directly below it.
- Business logic lives in `Service`, not in `Controller` or `Repository`.
- Web layer must never know about the DB schema.

---

## 4. JPA & Hibernate — Entity Mapping

**JPA** = Java Persistence API — a specification.
**Hibernate** = an implementation of JPA (the one Spring Boot uses by default).

An **entity** is a Java class that maps to a database table.
Each object = one row.

### Example — `Post` entity

```java
package com.learning.miniblog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String author;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Note:** Spring Boot 3+ uses `jakarta.persistence.*`, NOT the old `javax.persistence.*`.

---

## 5. All JPA Annotations Used

### Entity-level

| Annotation | Meaning | Without it |
|---|---|---|
| `@Entity` | This class maps to a DB table | Hibernate ignores the class |
| `@Table(name = "posts")` | Explicit table name | Defaults to class name (`post`) |

### Primary-key annotations

| Annotation | Meaning |
|---|---|
| `@Id` | Marks the primary key. **Required on every entity.** |
| `@GeneratedValue(strategy = IDENTITY)` | Database generates the id (auto-increment) |

### Column-level annotations

| Annotation | Meaning |
|---|---|
| `@Column(nullable = false)` | Adds `NOT NULL` to the column |
| `@Column(length = 150)` | `VARCHAR(150)` instead of default `VARCHAR(255)` |
| `@Column(columnDefinition = "TEXT")` | Use DB's `TEXT` type (no practical size limit) |
| `@Column(updatable = false)` | Never included in UPDATE statements |

### Timestamp annotations (Hibernate-specific)

| Annotation | Meaning |
|---|---|
| `@CreationTimestamp` | Hibernate sets this on INSERT only |
| `@UpdateTimestamp` | Hibernate sets this on INSERT **and** every UPDATE |

### Lombok annotations

| Annotation | Meaning |
|---|---|
| `@Data` | Getters, setters, `toString()`, `equals()`, `hashCode()` |
| `@NoArgsConstructor` | Empty constructor — **required by JPA/Hibernate** |
| `@AllArgsConstructor` | Constructor with all fields |
| `@Builder` | `Post.builder().title("...").build()` |

> **Rule:** If you use `@Builder`, you must also add `@NoArgsConstructor` and `@AllArgsConstructor`. Otherwise Hibernate can't instantiate the entity.

---

## 6. Primary Key Generation — Deep Dive

### How `IDENTITY` works with PostgreSQL

1. Hibernate translates `@GeneratedValue(strategy = IDENTITY)` into a
   PostgreSQL `GENERATED BY DEFAULT AS IDENTITY` column
   (older Hibernate generated `BIGSERIAL` — same behavior).
2. PostgreSQL creates:
   - A **sequence** (`posts_id_seq`)
   - A column with default `nextval('posts_id_seq')`
3. On INSERT:
   - Hibernate **omits `id`** from the INSERT statement
   - PostgreSQL fills it via `nextval()`
   - PostgreSQL returns the generated id to Hibernate
4. Hibernate sets `post.getId()` with the returned value.

### Who generates the id?

**PostgreSQL**, not Hibernate. Hibernate just tells the DB how.

### The sequence never goes backward

- Insert 3 rows → sequence value = 3
- Delete row with id = 2 → sequence still = 3
- Next insert → id = **4**

The sequence is a **monotonic counter**, not a gap-filler.

### Why you should never set the id manually

- The sequence doesn't know you did it
- Next auto-generated insert may **collide** with your manual id
- You can permanently desync the sequence

### `GenerationType` strategies

| Strategy | Who generates | Best for |
|---|---|---|
| `IDENTITY` | Database auto-increment | Simple apps (default in modern PG) |
| `SEQUENCE` | DB sequence, called before insert | High-throughput / batch inserts |
| `AUTO` | Hibernate picks based on DB | Portable, less predictable |
| `TABLE` | Hibernate via a special table | Legacy, avoid |

---

## 7. Hibernate Auto DDL — How the Table Was Created

### Configuration that makes this happen

```properties
spring.jpa.hibernate.ddl-auto=update
```

### What `update` does on startup

1. Hibernate scans all `@Entity` classes.
2. For each, it compares the class structure with the actual DB schema.
3. It **creates missing tables** and **adds missing columns**.
4. It **never deletes** existing tables or columns.

### DDL modes comparison

| Value | Behavior |
|---|---|
| `none` | Do nothing |
| `validate` | Check schema matches entities, fail if not |
| `update` | Add missing tables/columns (safe for dev) |
| `create` | Drop and recreate on every start (destructive) |
| `create-drop` | Like `create`, plus drops at shutdown (tests) |

### SQL generated from our `Post` entity

```sql
create table posts (
    id bigint generated by default as identity,
    author varchar(100) not null,
    content Text not null,
    created_at timestamp(6) not null,
    title varchar(150) not null,
    updated_at timestamp(6) not null,
    primary key (id)
)
```

**Every column traces back to a specific annotation:**

| SQL | Source annotation |
|---|---|
| `id bigint generated by default as identity` | `@Id` + `@GeneratedValue(IDENTITY)` |
| `title varchar(150) not null` | `@Column(nullable = false, length = 150)` |
| `content Text not null` | `@Column(nullable = false, columnDefinition = "TEXT")` |
| `author varchar(100) not null` | `@Column(nullable = false, length = 100)` |
| `created_at timestamp(6) not null` | `@CreationTimestamp` |
| `updated_at timestamp(6) not null` | `@UpdateTimestamp` |

### Where to verify

- Supabase → **Table Editor** → `posts`
- Or SQL Editor:
  ```sql
  SELECT column_name, data_type, is_nullable, column_default
  FROM information_schema.columns
  WHERE table_name = 'posts';
  ```

---

## 8. Database Connection & Configuration

### `application.properties`

```properties
spring.application.name=miniblog

spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### What each property does

| Property | Purpose |
|---|---|
| `spring.datasource.url` | JDBC connection string |
| `spring.datasource.username` | DB user |
| `spring.datasource.password` | DB password (always from env) |
| `spring.datasource.driver-class-name` | JDBC driver class |
| `spring.jpa.hibernate.ddl-auto` | Schema generation mode |
| `spring.jpa.show-sql` | Log SQL to console |
| `spring.jpa.properties.hibernate.format_sql` | Pretty-print SQL |

### Environment variables in IntelliJ

`Run → Edit Configurations → Environment variables`:

```
DB_URL=jdbc:postgresql://<host>:5432/postgres;DB_USERNAME=<user>;DB_PASSWORD=<password>
```

### Supabase connection — key rule

Use the **Session Pooler** (port `5432`), NOT the Transaction Pooler (port `6543`).
Hibernate uses prepared statements, which the Transaction pooler doesn't support well.

### HikariCP — the connection pool

Spring Boot uses **HikariCP** by default. From our log:
```
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
HikariPool-1 - Start completed.
```

### Warning we saw (and why it's fine for now)

```
WARN ... spring.jpa.open-in-view is enabled by default.
```
This is a *performance* warning about lazy-loading during view rendering.
We'll disable it in Week 2 when we add DTOs.

---

## 9. Project Setup Workflow

1. Generate project at [start.spring.io](https://start.spring.io) or IntelliJ → New → Spring Boot.
2. Dependencies: **Spring Web**, **Spring Data JPA**, **PostgreSQL Driver**, **Lombok**, **Validation**.
3. Open the unzipped folder in IntelliJ via **File → Open** (NOT "New Project").
4. Set `application.properties` to use `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`.
5. Set the env vars in IntelliJ **Run Configuration**.
6. Push to GitHub via **Git → GitHub → Share Project on GitHub**.

### Package structure

```
com.learning.miniblog/
├── MiniblogApplication.java
├── entity/          ← JPA entities
├── repository/      ← Spring Data JPA repositories
├── service/         ← Business logic
├── controller/      ← REST controllers
└── dto/             ← Request / response DTOs
```

---

## 10. Request Flow (REST API)

```
Client (Postman / browser / React)
      │  HTTP GET /api/posts
      ▼
PostController  (@RestController)
      │  PostService.findAll()
      ▼
PostService     (@Service)
      │  postRepository.findAll()
      ▼
PostRepository  (@Repository, Spring Data JPA)
      │  SELECT * FROM posts;
      ▼
PostgreSQL
      │  rows
      ▼
Hibernate maps rows → List<Post>
      │
      ▼
Jackson serializes → JSON
      │
      ▼
HTTP 200 OK { [...] }
```

---

## 11. Lessons Log

### Lesson 1 — The `Post` Entity

- Created `Post.java` in `entity/`
- Mapped to table `posts` via `@Entity`, `@Table`, `@Id`, `@GeneratedValue`
- Used `@Column` to enforce NOT NULL, lengths, and TEXT type
- Used Hibernate timestamp annotations for auto-filled `createdAt` / `updatedAt`
- Verified the table was auto-created in Supabase from Hibernate logs

**Hibernate DDL log captured:**

```sql
create table posts (
    id bigint generated by default as identity,
    author varchar(100) not null,
    content Text not null,
    created_at timestamp(6) not null,
    title varchar(150) not null,
    updated_at timestamp(6) not null,
    primary key (id)
)
```

### Lesson 2 — `PostRepository`  *(coming next)*

### Lesson 3 — `PostService`  *(coming)*

### Lesson 4 — `PostController`  *(coming)*

### Lesson 5 — Postman Testing  *(coming)*

---

## 12. Security Notes

- Never commit real DB passwords. Use `${ENV_VAR}` placeholders.
- Never paste real passwords in chat, AI tools, GitHub issues, or screenshots.
- If a secret leaks anywhere, **treat it as compromised** and rotate it immediately.
  - Supabase → Project Settings → Database → Reset Password.
- Add `.gitignore` entries for `target/`, `.idea/`, `.env`, `application-local.properties`.

---

## 13. Interview Q&A Prep

**Q: What's the difference between `@Controller` and `@RestController`?**
A: `@RestController` = `@Controller` + `@ResponseBody`. It returns the method's value directly as the HTTP body (auto-serialized to JSON), instead of resolving a view name.

**Q: Why use `@Repository` instead of `@Component`?**
A: `@Repository` adds automatic exception translation — DB-specific exceptions (e.g., `PSQLException`) are converted to Spring's `DataAccessException` hierarchy, decoupling your code from the DB vendor.

**Q: What's the difference between JPA and Hibernate?**
A: JPA is the specification (interfaces). Hibernate is one implementation of it. Spring Data JPA builds on top of JPA and generates repository implementations for you.

**Q: How does Hibernate generate primary keys with `IDENTITY` strategy?**
A: Hibernate omits `id` from INSERT and relies on the DB's auto-increment (PostgreSQL: a sequence + `nextval()`). PostgreSQL returns the generated id, which Hibernate maps back to the entity.

**Q: Why is the sequence never reused after a delete?**
A: It's a monotonic counter, not a gap-filler. Filling gaps would require table scans and risk race conditions under concurrency.

**Q: Why `nullable = false` in `@Column` if Java validation already enforces it?**
A: Defense in depth. The DB constraint is the last line of defense — it protects against bugs, scripts, and other apps that bypass Java validation.

**Q: What does `@Column(updatable = false)` do?**
A: Hibernate excludes this column from UPDATE statements. Useful for immutable fields like `createdAt`.

**Q: What does `ddl-auto=update` do?**
A: On startup, Hibernate compares entities to the DB schema and adds missing tables/columns. It never deletes. Fine for development; use Flyway/Liquibase in production.

**Q: Why `@NoArgsConstructor` on a JPA entity?**
A: Hibernate instantiates entities via a no-args constructor when loading rows from the DB.

**Q: What is HikariCP?**
A: The default JDBC connection pool in Spring Boot. It maintains a pool of reusable DB connections for performance.

**Q: What is `open-in-view` and why is it warned about?**
A: It keeps the Hibernate session open during view rendering, allowing lazy-loading in the view layer. Convenient but can hide N+1 problems and hold DB connections too long. Best practice: disable it and use DTOs.

---

## 14. Progress Tracker

| Week | Topic | Status |
|---|---|---|
| 1 | Entities | ✅ Done (`Post`) |
| 1 | Repositories | ⬜ Next |
| 1 | Services | ⬜ |
| 1 | Controllers | ⬜ |
| 1 | CRUD + Postman | ⬜ |
| 2 | PostgreSQL + Relationships | ⬜ |
| 2 | DTOs + Validation | ⬜ |
| 2 | Exception Handling | ⬜ |
| 2 | Pagination + Sorting | ⬜ |
| 3 | Spring Security + JWT | ⬜ |
| 4 | Docker + Deployment | ⬜ |

---

## 📌 Conventions Used

- Package root: `com.learning.miniblog`
- Table names: plural, snake_case (`posts`)
- Column names: snake_case (`created_at`)
- Timestamps: `createdAt` (immutable), `updatedAt` (auto-updated)
- JSON responses: camelCase (Spring default)
- DB secrets: always via environment variables

---

*Continuously updated as I learn.*
