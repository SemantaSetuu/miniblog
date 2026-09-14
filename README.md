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
| `@SpringBootApplication` | Boot's entry-point annotation |

### Bean scopes

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
**Hibernate** = an implementation of JPA (default in Spring Boot).

An **entity** is a Java class that maps to a database table.
Each object = one row.

### `Post` entity

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

**Note:** Spring Boot 3+ uses `jakarta.persistence.*`, NOT `javax.persistence.*`.

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
| `@Column(columnDefinition = "TEXT")` | Use DB's `TEXT` type |
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

> **Rule:** If you use `@Builder`, you must also add `@NoArgsConstructor` and `@AllArgsConstructor`.

---

## 6. Primary Key Generation — Deep Dive

### How `IDENTITY` works with PostgreSQL

1. Hibernate translates `@GeneratedValue(strategy = IDENTITY)` into a
   PostgreSQL `GENERATED BY DEFAULT AS IDENTITY` column.
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

### Why you should never set the id manually

- The sequence doesn't know you did it
- Next auto-generated insert may **collide** with your manual id
- You can permanently desync the sequence

### `GenerationType` strategies

| Strategy | Who generates | Best for |
|---|---|---|
| `IDENTITY` | Database auto-increment | Simple apps |
| `SEQUENCE` | DB sequence, called before insert | High-throughput / batch inserts |
| `AUTO` | Hibernate picks based on DB | Portable |
| `TABLE` | Hibernate via a special table | Legacy, avoid |

---

## 7. Hibernate Auto DDL — How the Table Was Created

### Configuration

```properties
spring.jpa.hibernate.ddl-auto=update
```

### What `update` does on startup

1. Hibernate scans all `@Entity` classes.
2. Compares the class structure with the DB schema.
3. Creates missing tables and adds missing columns.
4. Never deletes existing tables or columns.

### DDL modes

| Value | Behavior |
|---|---|
| `none` | Do nothing |
| `validate` | Check schema matches entities, fail if not |
| `update` | Add missing tables/columns (safe for dev) |
| `create` | Drop and recreate on every start (destructive) |
| `create-drop` | Like `create`, plus drops at shutdown (tests) |

### SQL generated from `Post`

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

| SQL | Source annotation |
|---|---|
| `id bigint generated by default as identity` | `@Id` + `@GeneratedValue(IDENTITY)` |
| `title varchar(150) not null` | `@Column(nullable = false, length = 150)` |
| `content Text not null` | `@Column(nullable = false, columnDefinition = "TEXT")` |
| `author varchar(100) not null` | `@Column(nullable = false, length = 100)` |
| `created_at timestamp(6) not null` | `@CreationTimestamp` |
| `updated_at timestamp(6) not null` | `@UpdateTimestamp` |

### Verify in Supabase

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

### Environment variables

Set in IntelliJ Run Configuration or Codespaces terminal:

```
DB_URL=jdbc:postgresql://<host>:5432/postgres
DB_USERNAME=<user>
DB_PASSWORD=<password>
```

### Supabase — key rule

Use the **Session Pooler** (port `5432`), NOT the Transaction Pooler (port `6543`).

### HikariCP — the connection pool

Spring Boot's default JDBC connection pool. From our log:
```
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
HikariPool-1 - Start completed.
```

### `open-in-view` warning

```
WARN ... spring.jpa.open-in-view is enabled by default.
```
Performance warning about lazy-loading during view rendering.
We'll disable it in Week 2 when we add DTOs.

---

## 9. Project Setup Workflow

1. Generate project at [start.spring.io](https://start.spring.io) or IntelliJ → New → Spring Boot.
2. Dependencies: **Spring Web**, **Spring Data JPA**, **PostgreSQL Driver**, **Lombok**, **Validation**.
3. Open the unzipped folder in IntelliJ via **File → Open**.
4. Set `application.properties` to use `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`.
5. Set env vars in IntelliJ **Run Configuration**.
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
      │  PostService.getAllPosts()
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
HTTP 200 OK [ ... ]
```

---

## 11. Lessons Log

### Lesson 1 — The `Post` Entity

- Created `Post.java` in `entity/`
- Mapped to table `posts` via `@Entity`, `@Table`, `@Id`, `@GeneratedValue`
- Used `@Column` for NOT NULL, lengths, TEXT
- Used Hibernate timestamps for auto-filled `createdAt` / `updatedAt`
- Verified table auto-created in Supabase

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

### Lesson 2 — `PostRepository`

```java
package com.learning.miniblog.repository;

import com.learning.miniblog.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
```

- Extends `JpaRepository<Post, Long>` — `Post` is the entity, `Long` is the PK type
- Spring generates the implementation at runtime
- Free methods: `save`, `findAll`, `findById`, `deleteById`, `count`, `existsById`
- Verified in logs: `Found 1 JPA repository interface.`

### Lesson 3 — `PostService`

```java
package com.learning.miniblog.service;

import com.learning.miniblog.entity.Post;
import com.learning.miniblog.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    public Post createPost(Post post) {
        return postRepository.save(post);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
}
```

**Key idea — `save()` does both INSERT and UPDATE:**
- `post.id == null` → INSERT → PostgreSQL generates id
- `post.id != null` → UPDATE the existing row

### Lesson 4 — `PostController`  *(next)*

### Lesson 5 — Postman Testing  *(coming)*

---

## 12. Security Notes

- Never commit real DB passwords. Use `${ENV_VAR}` placeholders.
- Never paste real passwords in chat, AI tools, or GitHub.
- If a secret leaks anywhere, **treat it as compromised** and rotate it.
  - Supabase → Project Settings → Database → Reset Password.
- `.gitignore` should include `target/`, `.idea/`, `.env`, `application-local.properties`.

---

## 13. Interview Q&A Prep

**Q: What's the difference between `@Controller` and `@RestController`?**
A: `@RestController` = `@Controller` + `@ResponseBody`. It returns the method's value directly as the HTTP body (JSON), instead of resolving a view name.

**Q: Why use `@Repository` instead of `@Component`?**
A: `@Repository` adds automatic exception translation — DB-specific exceptions are converted to Spring's `DataAccessException` hierarchy.

**Q: JPA vs Hibernate?**
A: JPA is the specification. Hibernate is one implementation. Spring Data JPA builds on top of JPA and generates repository implementations.

**Q: How does Hibernate generate IDs with `IDENTITY`?**
A: Hibernate omits `id` from INSERT; PostgreSQL fills it via a sequence and returns it. Hibernate maps it back.

**Q: Why is the sequence never reused after a delete?**
A: It's a monotonic counter, not a gap-filler. Filling gaps would require table scans and risk race conditions.

**Q: Why `nullable = false` in `@Column` if Java validation already enforces it?**
A: Defense in depth. The DB constraint protects against bugs, scripts, and other apps that bypass Java validation.

**Q: What does `@Column(updatable = false)` do?**
A: Hibernate excludes this column from UPDATE statements. Useful for immutable fields like `createdAt`.

**Q: What does `ddl-auto=update` do?**
A: On startup, Hibernate compares entities to the DB schema and adds missing tables/columns. Never deletes. Fine for development; use Flyway/Liquibase in production.

**Q: Why `@NoArgsConstructor` on a JPA entity?**
A: Hibernate instantiates entities via a no-args constructor when loading rows.

**Q: What is HikariCP?**
A: The default JDBC connection pool in Spring Boot.

**Q: What is `open-in-view`?**
A: Keeps the Hibernate session open during view rendering, allowing lazy-loading in the view layer. Convenient but hides N+1 problems and holds connections. Best practice: disable and use DTOs.

**Q: What does `save()` do — INSERT or UPDATE?**
A: Both. If the entity's `id` is `null`, it INSERTs. If `id` has a value, it UPDATEs.

---

## 14. Progress Tracker

| Week | Topic | Status |
|---|---|---|
| 1 | Entities | ✅ Done (`Post`) |
| 1 | Repositories | ✅ Done (`PostRepository`) |
| 1 | Services | ✅ Done (`PostService`) |
| 1 | Controllers | ⬜ Next |
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