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
12. [Confusion Q&A — My Own Questions Answered](#12-confusion-qa--my-own-questions-answered)
13. [Security Notes](#13-security-notes)
14. [Interview Q&A Prep](#14-interview-qa-prep)
15. [Progress Tracker](#15-progress-tracker)

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

### Validation annotations (Jakarta Bean Validation)

| Annotation | Meaning | Where used |
|---|---|---|
| `@NotBlank` | Field cannot be `null`, `""`, or whitespace | On DTO fields |
| `@NotNull` | Field cannot be `null` (but empty string is allowed) | On DTO fields |
| `@Size(max = 150)` | Length constraint | On DTO fields |
| `@Valid` | Trigger validation on a parameter | On controller method params |

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

### Full workflow with DTOs + Validation (POST example)

```
Client (curl)
   │  POST /api/posts + JSON body { id:999, title:"", content, author }
   ▼
Tomcat
   │  parses HTTP
   ▼
DispatcherServlet
   │  routes to PostController.createPost
   ▼
Jackson
   │  JSON → PostRequest (id=999 dropped — PostRequest has no id field)
   ▼
@Valid
   │  runs validation rules on PostRequest
   │  title = "" → @NotBlank fails → 400 Bad Request
   │  controller method NOT called
   ▼
Response: 400 Bad Request
```

If validation passes:

```
@Valid passes
   │
PostController.createPost(request)
   │
PostService.createPost(request)
   │  toEntity(request) → Post entity (id=null)
   │  postRepository.save(post) → Hibernate INSERT
   │  PostgreSQL generates id, created_at, updated_at
   │  toResponse(saved) → PostResponse
   ▼
Controller returns PostResponse
   │
Jackson → JSON
   │
Response: 200 OK
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

### Lesson 3 — `PostService` (initial version)

**Key idea — `save()` does both INSERT and UPDATE:**
- `post.id == null` → INSERT → PostgreSQL generates id
- `post.id != null` → UPDATE the existing row

### Lesson 4 — `PostController`

**Annotations used:**

| Annotation | Purpose | Example URL |
|---|---|---|
| `@RestController` | Return JSON, not HTML | — |
| `@RequestMapping("/api/posts")` | Base URL for the class | — |
| `@GetMapping` | Read all | `GET /api/posts` |
| `@GetMapping("/{id}")` | Read one | `GET /api/posts/1` |
| `@PostMapping` | Create | `POST /api/posts` |
| `@PutMapping("/{id}")` | Update | `PUT /api/posts/1` |
| `@DeleteMapping("/{id}")` | Delete | `DELETE /api/posts/1` |
| `@PathVariable` | Extract `{id}` from URL | id = 1 |
| `@RequestBody` | Read JSON body → Java object | `{ "title": "..." }` |

### Lesson 5 — CRUD Testing with cURL

Tested all four CRUD operations. All worked.

### Lesson 6 — `PUT /api/posts/{id}` (Update)

- Fetch existing row → overwrite fields → `save()` runs UPDATE.
- `createdAt` unchanged (`updatable = false`).
- `updatedAt` changes (`@UpdateTimestamp`).

### Lesson 7 — Full CRUD Complete ✅

**Week 1 goal achieved — a fully functional CRUD REST API with PostgreSQL.**

### Lesson 8 (Week 2) — DTOs: `PostRequest` and `PostResponse`

**Why DTOs?**

Without DTOs, the controller accepts and returns the **entity** directly. That means:
- Client can send `id`, `createdAt`, `updatedAt` — fields they shouldn't control
- Client sees every field — including future sensitive fields
- Changing the DB schema would break the API

**The solution:** separate classes for different jobs.

| Class | Direction | Fields |
|---|---|---|
| `Post` (entity) | DB ↔ Java | id, title, content, author, createdAt, updatedAt |
| `PostRequest` (DTO) | Client → Server | title, content, author |
| `PostResponse` (DTO) | Server → Client | id, title, content, author, createdAt, updatedAt |

**`PostRequest.java`:**

```java
package com.learning.miniblog.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    private String title;

    private String content;

    private String author;
}
```

**`PostResponse.java`:**

```java
package com.learning.miniblog.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {

    private Long id;

    private String title;

    private String content;

    private String author;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
```

**Conversion methods added to `PostService`:**

```java
private Post toEntity(PostRequest request) {
    return Post.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .author(request.getAuthor())
            .build();
}

private PostResponse toResponse(Post post) {
    return PostResponse.builder()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .author(post.getAuthor())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
}
```

**Updated `PostService` methods** — all accept/return DTOs now:

```java
public List<PostResponse> getAllPosts() {
    return postRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
}

public PostResponse getPostById(Long id) {
    Post post = postRepository.findById(id).orElse(null);
    if (post == null) return null;
    return toResponse(post);
}

public PostResponse createPost(PostRequest request) {
    Post post = toEntity(request);
    Post saved = postRepository.save(post);
    return toResponse(saved);
}

public PostResponse updatePost(Long id, PostRequest request) {
    Post existing = postRepository.findById(id).orElse(null);
    if (existing == null) return null;

    existing.setTitle(request.getTitle());
    existing.setContent(request.getContent());
    existing.setAuthor(request.getAuthor());

    Post saved = postRepository.save(existing);
    return toResponse(saved);
}
```

**Verification:** Sent `id=999` in the POST body → response showed `id=3` (DB-generated). The DTO dropped `id` because it has no such field.

### Lesson 9 (Week 2) — Validation with Jakarta Bean Validation

**Why validation?**

Without it, the API accepts empty strings, nulls, or oversized input. Bad data reaches the database.

**The fix:** annotation-based validation on the DTO, triggered by `@Valid` in the controller.

**`PostRequest.java` with validation:**

```java
package com.learning.miniblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotBlank(message = "Author is required")
    @Size(max = 100, message = "Author must be at most 100 characters")
    private String author;
}
```

**`PostController` with `@Valid`:**

```java
@PostMapping
public PostResponse createPost(@Valid @RequestBody PostRequest request) {
    return postService.createPost(request);
}

@PutMapping("/{id}")
public PostResponse updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
    return postService.updatePost(id, request);
}
```

**Import added:**

```java
import jakarta.validation.Valid;
```

**Annotations used:**

| Annotation | Meaning |
|---|---|
| `@NotBlank` | Cannot be `null`, `""`, or whitespace-only |
| `@Size(max = 150)` | Length cannot exceed 150 characters |
| `@Valid` | Trigger validation before calling the method |

**How it works:**

1. Client sends JSON
2. Jackson converts JSON → `PostRequest` object
3. `@Valid` triggers Spring to run the validation rules on the DTO
4. If any rule fails → HTTP 400 Bad Request, controller method not called, nothing saved
5. If all rules pass → controller runs normally

**Verification:**

| Test | Input | Result |
|---|---|---|
| A | Empty title (`""`) | 400 Bad Request ✅ |
| B | Missing title | 400 Bad Request ✅ |
| C | Valid data | 200 OK with created post ✅ |

**Current limitation:** Spring returns its default ugly 400 error:
```json
{"timestamp":"...","status":400,"error":"Bad Request","path":"/api/posts"}
```
It doesn't say which field failed. Next lesson: **exception handling** to return clean JSON like:
```json
{"title": "Title is required"}
```

---

## 12. Confusion Q&A — My Own Questions Answered

This section is a personal revision log of the questions I asked while learning, in the order I asked them.

---

### 🔹 Q: What is a client? What is a server? Am I the client?

**Client** = anything that sends HTTP requests to your server.
**Server** = your Spring Boot app (running on port 8080) that handles the requests.

In our project, the clients have been:

| Client | How you used it |
|---|---|
| **curl** | In the terminal, to send POST/PUT/DELETE |
| **Browser** | To send GET requests (`localhost:8080/api/posts`) |
| **Postman** (later) | A GUI tool |
| **React / mobile** (future) | Real apps that will call your API |

**You are not the client.** You are the human using a client. The client sends the request, receives the response, and shows it to you.

```
YOU → use a CLIENT (curl/browser) → talk to the SERVER (Spring Boot) → which talks to DATABASE
```

---

### 🔹 Q: Does the client send both the request AND receive the response?

**Yes.** Same client does both.

```
Client  ──── REQUEST ────▶  Server
Client  ◀─── RESPONSE ───  Server
```

Example with curl:
- curl **sends** `POST /api/posts` with a JSON body
- curl **receives** the JSON response and prints it in the terminal

Example with browser:
- browser **sends** `GET /api/posts`
- browser **receives** the JSON and displays it

The client is the messenger both ways.

---

### 🔹 Q: What is a DTO and why do we need it?

A **DTO (Data Transfer Object)** is a simple class that holds only the data needed for a specific job — separate from the entity.

**Without DTOs — problems:**
1. Client can send `id`, `createdAt`, `updatedAt` in the request body. These should be DB-only.
2. Client sees every entity field in the response — including ones they shouldn't (future passwords, internal notes).
3. Changing the DB schema would break the API.

**With DTOs:**
- `PostRequest` — only `title`, `content`, `author`. No `id`. Client can't set it.
- `PostResponse` — same fields plus `id`, `createdAt`, `updatedAt` for the client to see.

**Analogy:** Think of a form:
- The **application form** = `PostRequest` (what the client fills in)
- The **receipt** = `PostResponse` (what you give back)
- The **filing cabinet record** = `Post` entity (internal)

---

### 🔹 Q: Why does `PostRequest` have no `@Builder` but `PostResponse` does?

Because **who creates the object** differs.

| Class | Created by | Uses builder? |
|---|---|---|
| `PostRequest` | Jackson (from JSON) | ❌ Jackson uses `new` + setters |
| `PostResponse` | Your `toResponse()` method | ✅ Uses `.builder()...build()` |
| `Post` | Your `toEntity()` and Hibernate | ✅ Uses `.builder()...build()` |

**Rule:** `@Builder` is only useful when **you write** `ClassName.builder()...build()` in your own code. If something else (Jackson, Hibernate) creates the object, you don't need `@Builder`.

---

### 🔹 Q: Why was `@Builder` on `Post` even before I used it?

Convention. Almost every entity has these four Lombok annotations together:
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
```

You may not use `@Builder` immediately — but as your code grows, you will. When we added `toEntity()`, we finally used it.

**Not a problem to have it early.** No cost, no side effect. Removing it later would be more work.

---

### 🔹 Q: Are `PostService` and `PostController` overriding methods?

**No.** Overriding requires:

1. Class A **extends** Class B
2. Class A redefines a method from Class B with the **same signature**
3. Usually marked with `@Override`

Neither `PostService` nor `PostController` extends anything. So nothing is overridden.

The `getAllPosts()` method appears in both classes, but they are **two separate methods with the same name** — the controller calls the service's version.

**Example of real overriding:**
```java
class Animal {
    public void makeSound() { System.out.println("Some sound"); }
}
class Dog extends Animal {
    @Override
    public void makeSound() { System.out.println("Woof"); }  // ← override
}
```

---

### 🔹 Q: Can I name the controller's method differently from the service's?

**Yes.** Method names are convention, not rules.

```java
@GetMapping
public List<PostResponse> fetchAllFromServer() {
    return postService.getAllPosts();   // ← different names, works fine
}
```

Spring looks at **annotations**, not method names. Keeping them the same is a **style choice** for readability.

**Rule of thumb:** Same concept → same name. Different job → different name.

---

### 🔹 Q: Why is `getTitle()` needed inside `existing.setTitle(updatedPost.getTitle())`?

Because the setter needs a value to set. That value comes from the getter.

Read it like English:
> **"Set the title of `existing` to the title of `updatedPost`."**

- `updatedPost.getTitle()` → **reads** the new title from the request
- `existing.setTitle(...)` → **writes** it onto the DB-bound entity

Without the getter, the setter has nothing to set.

---

### 🔹 Q: In `existing.setTitle(updatedPost.getTitle())`, which runs first?

The **getter**. Java evaluates method arguments before calling the outer method.

Order:
1. `updatedPost.getTitle()` → returns `"Updated Title"`
2. `existing.setTitle("Updated Title")` → sets it

Same as writing on a box: you read the label from the notebook **first**, then write it on the box.

---

### 🔹 Q: Where does the title actually get updated?

In `existing.setTitle(...)`. The getter only **reads**. The setter is where the **change** happens.

Then `postRepository.save(existing)` persists the change to the DB (Hibernate runs `UPDATE`).

Flow:
```
updatedPost.getTitle()      ← read new value
     ↓
existing.setTitle(value)    ← write to entity (in-memory change)
     ↓
postRepository.save(...)    ← Hibernate UPDATE (DB change)
```

---

### 🔹 Q: What does `toResponse()` do again?

It **copies** values from a `Post` entity into a new `PostResponse` DTO using the builder, so the DTO can be sent to the client as JSON.

```java
private PostResponse toResponse(Post post) {
    return PostResponse.builder()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .author(post.getAuthor())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
}
```

**One copy per field.** Simple.

---

### 🔹 Q: Do DTO fields have to match entity field names?

**Not required** for the code to compile or run. But **recommended** because:

1. The JSON field names come from the DTO — rename the DTO field, and the JSON key changes.
2. If your API is public, renaming is a breaking change for clients.
3. Readability: developers expect `title` to be `title`, not `headline`.

**Example:**
| If DTO field is | JSON response shows |
|---|---|
| `id` | `"id": 3` |
| `postId` | `"postId": 3` |

Only rename if you have a specific reason (e.g., `authorName` to avoid clash with an `Author` entity later).

---

### 🔹 Q: What does `.stream().map(this::toResponse).toList()` do?

Four steps, left to right:

| Step | Expression | Type |
|---|---|---|
| 1 | `postRepository.findAll()` | `List<Post>` |
| 2 | `.stream()` | `Stream<Post>` |
| 3 | `.map(this::toResponse)` | `Stream<PostResponse>` |
| 4 | `.toList()` | `List<PostResponse>` |

**What each does:**
- **`findAll()`** — the repository returns entities, never DTOs
- **`.stream()`** — turns the list into a "conveyor belt" so you can process each item one by one
- **`.map(this::toResponse)`** — for each `Post` on the belt, run `toResponse(post)` and put the result on a new belt
- **`.toList()`** — collect the new belt into a `List<PostResponse>`

**`this::toResponse` is shorthand** for `post -> toResponse(post)` — a **method reference**.

---

### 🔹 Q: Is the whole curl command JSON?

**No.** Only the part after `-d` is JSON.

```bash
curl -X POST http://localhost:8080/api/posts -H "Content-Type: application/json" -d '{"id":999,"title":"..."}'
```

| Part | Language |
|---|---|
| `curl` | Shell command |
| `-X POST` | Shell option |
| URL | URL |
| `-H "..."` | Shell + HTTP header |
| `-d '{...}'` | **JSON** ✅ |

**Analogy:** You're a courier. The route, the box, the label — all instructions. Only what's **inside** the box is JSON.

---

### 🔹 Q: Does `toResponse()` set values on `PostResponse` variables and return the object?

**Yes. Exactly.**

It:
1. Creates a new `PostResponse` object (via builder)
2. Sets each field from the entity
3. Returns the completed DTO

Then Spring/Jackson converts it to JSON and sends it to the client via Tomcat.

---

### 🔹 Q: Why can't I write `PostResponse postResponse = postRepository.findById(id).orElse(null)`?

Because **types don't match**.

| Expression | Type |
|---|---|
| `postRepository.findById(id)` | `Optional<Post>` |
| `.orElse(null)` | `Post` |
| Assigned to | `PostResponse` ❌ |

The repository deals only in **entities**, not DTOs. So you must:

```java
Post post = postRepository.findById(id).orElse(null);   // fetch as entity
if (post == null) return null;                          // handle not found
return toResponse(post);                                // convert to DTO
```

**The repository never returns DTOs.** Only your service converts entities to DTOs.

---

### 🔹 Q: How does the full curl → server → response workflow look end-to-end?

Given this command:
```bash
curl -X POST http://localhost:8080/api/posts -H "Content-Type: application/json" -d '{"id":999,"title":"Should Ignore ID","content":"Test","author":"Semanta"}'
```

Step by step:

1. **Shell runs curl** with method `POST`, URL, header, and JSON body
2. **curl builds an HTTP request** and connects to `localhost:8080`
3. **Tomcat** receives the request and passes it to **DispatcherServlet**
4. **DispatcherServlet** matches `POST /api/posts` to `PostController.createPost`
5. **Jackson** converts the JSON body → `PostRequest` object. **`id=999` is dropped** because `PostRequest` has no `id` field.
6. **Controller** calls `postService.createPost(request)`
7. **Service** calls `toEntity(request)` → creates a `Post` with `id = null`
8. **Service** calls `postRepository.save(post)`
9. **Hibernate** generates `INSERT INTO posts (title, content, author, created_at, updated_at) VALUES (?, ?, ?, ?, ?)`
10. **PostgreSQL** assigns `id = 3` (next in sequence), sets timestamps
11. **PostgreSQL** returns the generated values to Hibernate
12. **Hibernate** fills them into the `post` object
13. **Service** calls `toResponse(saved)` → creates a `PostResponse`
14. **Controller** returns `PostResponse`
15. **Jackson** converts it to JSON
16. **Tomcat** sends the HTTP response
17. **curl** receives the response and prints it to the terminal

**Result:** You see `{"id":3,"title":"Should Ignore ID",...}` — `999` was silently ignored by the DTO.

---

### 🔹 Q: Do I need to update `PostService` when I add DTOs?

**Yes.** Before DTOs, `PostService` worked directly with `Post`. After DTOs, it must:

- Accept `PostRequest` in `createPost` and `updatePost`
- Return `PostResponse` in `getAllPosts`, `getPostById`, `createPost`, `updatePost`
- Add the two conversion helpers `toEntity()` and `toResponse()`

The `PostRepository` stays the same — it still works with entities only.

**The controller also updates** — it now accepts `@RequestBody PostRequest` and returns `PostResponse`.

---

### 🔹 Q: How does validation work in the request flow?

**Step by step for a POST with invalid data:**

1. Client sends JSON
2. Tomcat receives the request
3. DispatcherServlet routes to `PostController.createPost`
4. Jackson converts JSON → `PostRequest` object
5. `@Valid` triggers Spring to run the validation rules on `PostRequest`
6. If a rule fails → **400 Bad Request** — controller method is **NOT called**
7. If all rules pass → controller method runs as normal

**Result:** Bad data never reaches the service or the database.

---

### 🔹 Q: Where do validation annotations go — the DTO or the entity?

**On the DTO.**

- `PostRequest` — validation annotations (`@NotBlank`, `@Size`)
- `Post` (entity) — no validation annotations

**Why:**
- The DTO is the boundary — validate input at the edge
- The entity is for the DB — its `@Column(nullable = false)` enforces NOT NULL at the DB level
- Two layers of protection: DTO validation + DB constraint

**Validation belongs where the client's input first arrives** — the DTO.

---

### 🔹 Q: What's the difference between `@NotBlank` and `@NotNull`?

| Annotation | Rejects |
|---|---|
| `@NotNull` | Only `null` |
| `@NotBlank` | `null`, `""`, and whitespace-only strings |

| Value | `@NotNull` passes? | `@NotBlank` passes? |
|---|---|---|
| `"Hello"` | ✅ | ✅ |
| `""` (empty) | ✅ | ❌ |
| `"   "` (spaces) | ✅ | ❌ |
| `null` | ❌ | ❌ |

**Use `@NotBlank` for strings** — it's what you almost always want.

---

### 🔹 Q: What does `@Valid` do?

`@Valid` on a controller method parameter tells Spring:

> **"Before calling this method, run all the validation rules on this parameter."**

- Spring reads the annotations (`@NotBlank`, `@Size`) on the DTO
- It runs each rule against the data in the DTO object
- If any rule fails → 400 Bad Request, method not called
- If all pass → method runs normally

**Without `@Valid`:** The annotations on the DTO are ignored — validation never happens.

---

### 🔹 Q: Are Jakarta annotations different from Spring annotations?

**Yes — they're two different things.**

| Piece | Role |
|---|---|
| `jakarta.validation.constraints.*` (`@NotBlank`, `@Size`) | Define the **rules** |
| `jakarta.validation.Valid` (`@Valid`) | Trigger validation |
| Spring MVC integration | Runs the validation before controller methods |
| `spring-boot-starter-validation` (Maven dependency) | Brings in Hibernate Validator — the engine that actually checks |

**Jakarta = the rule book. Spring = the enforcer.**

Both are needed.

**Why "Jakarta" and not "Java"?**
- Before 2019: `javax.validation.*` (used by Spring Boot 2.x)
- Spring Boot 3+: `jakarta.validation.*`
- Oracle kept the `javax` name, so the community (Eclipse Foundation) renamed to `jakarta`

**Rule:** On Spring Boot 3+, always use `jakarta.*`, never `javax.*`.

---

## 13. Spring Shortcut Translation Notes

This section exists because I learn better by understanding the expanded Java logic before the Spring shortcut.

---

### 🔹 Shortcut 1 — `orElseThrow()`

#### Spring Version

```java
Post existing = postRepository.findById(id)
        .orElseThrow(() ->
                new ResourceNotFoundException(
                        "Post not found with id: " + id
                )
        );
```

#### Expanded Java Version

```java
Optional<Post> optionalPost =
        postRepository.findById(id);

if (optionalPost.isEmpty()) {

    throw new ResourceNotFoundException(
            "Post not found with id: " + id
    );
}

Post existing =
        optionalPost.get();
```

#### Mental Model

```text
If value exists
      ↓
Return it

If value does not exist
      ↓
Throw exception
```

---

### 🔹 Shortcut 2 — `forEach()`

#### Spring Version

```java
fieldErrors.forEach(fieldError ->
        errors.put(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        )
);
```

#### Expanded Java Version

```java
for (FieldError fieldError : fieldErrors) {

    errors.put(
            fieldError.getField(),
            fieldError.getDefaultMessage()
    );
}
```

#### Mental Model

```text
Loop through every item
      ↓
Execute code once for each item
```

---

### 🔹 Shortcut 3 — `map()`

#### Spring Version

```java
return postRepository.findAll()
        .stream()
        .map(this::toResponse)
        .toList();
```

#### Expanded Java Version

```java
List<Post> posts =
        postRepository.findAll();

List<PostResponse> responses =
        new ArrayList<>();

for (Post post : posts) {

    PostResponse response =
            toResponse(post);

    responses.add(response);
}

return responses;
```

#### Mental Model

```text
Take each Post
      ↓
Convert it into PostResponse
      ↓
Collect results into new list
```

---

### 🔹 Shortcut 4 — Method Reference (`this::toResponse`)

#### Spring Version

```java
this::toResponse
```

#### Equivalent Lambda

```java
post -> toResponse(post)
```

#### Expanded Java Thinking

```java
PostResponse response =
        toResponse(post);
```

#### Mental Model

```text
Method reference = shorthand for calling a method
```

---

### 🔹 Shortcut 5 — `ResponseEntity`

#### Spring Version

```java
return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(error);
```

#### Mental Visualization

```java
{
    status : 404,

    body : {
        "error" :
        "Post not found with id: 9999"
    }
}
```

> Note: This is NOT actual Java syntax. It is only a visualization.

#### What Spring Actually Does

```text
Read status
     ↓
Use as HTTP status code

Read body
     ↓
Send body object to Jackson

Jackson converts body to JSON
     ↓
Client receives response
```

---

### 🔹 Shortcut 6 — `Optional`

#### Optional With Value

```java
Optional<String> name =
        Optional.of("Semanta");
```

Mental Model:

```text
Box
 └── Semanta
```

---

#### Empty Optional

```java
Optional<String> name =
        Optional.empty();
```

Mental Model:

```text
Box
 └── Empty
```

---

#### `orElse()`

```java
name.orElse("Unknown");
```

Expanded Logic:

```java
if (value exists)
    return value;
else
    return "Unknown";
```

---

#### `orElseThrow()`

```java
name.orElseThrow();
```

Expanded Logic:

```java
if (value exists)
    return value;
else
    throw exception;
```

---

## 🔹 Optional and Exception Handling Flow

### Spring Version

```java
Post existing = postRepository.findById(id)
        .orElseThrow(() ->
                new ResourceNotFoundException(
                        "Post not found with id: " + id
                )
        );
```

### Expanded Java Version

```java
Optional<Post> optionalPost =
        postRepository.findById(id);

if (optionalPost.isEmpty()) {

    throw new ResourceNotFoundException(
            "Post not found with id: " + id
    );
}

Post existing = optionalPost.get();
```

### Internal Execution Flow

```text
findById(id)
      ↓
Optional<Post>
      ↓
Empty?
      ↓
YES
      ↓
Create ResourceNotFoundException object
      ↓
Call constructor
      ↓
Pass message to RuntimeException using super(message)
      ↓
Message stored internally
      ↓
Throw exception
      ↓
Spring catches exception
      ↓
Find matching @ExceptionHandler
      ↓
Execute handler method
      ↓
Create ResponseEntity
      ↓
Return HTTP response
```

---

## 🔹 Static Methods vs Objects

### Uses Static Methods

```java
Optional.of("Semanta");

ResponseEntity.status(HttpStatus.NOT_FOUND);

Math.max(10, 20);
```

Reason:

```text
Method belongs to the class.
No object required.
```

---

### Uses Objects

```java
ex.getMessage();

error.put("error", "Not Found");

post.getTitle();
```

Reason:

```text
Method belongs to an object.
Object required.
```

---

## ✅ Key Rule for Learning

Whenever Spring code looks difficult:

### Translate

```java
.orElseThrow(...)
```

into

```java
if (...) {
    throw ...
}
```

---

### Translate

```java
.forEach(...)
```

into

```java
for (...) {
    ...
}
```

---

### Translate

```java
.map(...)
```

into

```java
for (...) {
    convert item
}
```

---

### Translate

```java
this::method
```

into

```java
value -> method(value)
```

---
## Lesson 11 (Week 2) — Pagination Fundamentals

### Definition

Pagination is a technique used to return data in smaller chunks (pages) instead of returning all records at once.

Without pagination:

```java
postRepository.findAll();
```

returns:

```text
All records from the database.
```

With pagination:

```java
postRepository.findAll(pageable);
```

returns:

```text
Only the requested page of records.
```

---

## Why Pagination?

Imagine the database contains:

```text
10 records
```

Returning everything is fine.

Now imagine:

```text
100,000 records
```

Problems:

- Slow response
- More memory usage
- Larger network traffic
- Poor user experience

Pagination solves this problem by returning only a small subset of records.

---

## Page Number

### Definition

The page number identifies which page of data should be returned.

Example:

```java
page = 0
```

means:

```text
First page
```

Important:

```text
Spring pagination starts at 0, not 1.
```

---

### Example

Database:

```text
Post1
Post2
Post3
Post4
Post5
Post6
```

Page Size:

```text
2
```

Results:

```text
Page 0 -> Post1 Post2
Page 1 -> Post3 Post4
Page 2 -> Post5 Post6
```

---

## Page Size

### Definition

The number of records returned per page.

Example:

```java
size = 3
```

means:

```text
Return 3 records per page.
```

---

## Pageable

### Definition

`Pageable` is a Spring interface that represents pagination instructions.

Purpose:

```text
Store information about:

- Page number
- Page size
- Sorting information
```

Think of it as:

```text
A pagination request form.
```

---

### Example

```java
Pageable pageable;
```

means:

```text
This variable will hold pagination instructions.
```

---

## PageRequest

### Definition

`PageRequest` is a Spring class that implements the `Pageable` interface.

Relationship:

```java
Pageable pageable =
        PageRequest.of(0,5);
```

Similar to:

```java
List<String> names =
        new ArrayList<>();
```

---

### Purpose

Creates an actual pagination object.

Example:

```java
PageRequest.of(0,5);
```

means:

```text
Page Number = 0
Page Size = 5
```

---

### Syntax

```java
PageRequest.of(page, size);
```

Example:

```java
PageRequest.of(1,3);
```

means:

```text
Give me page 1
with 3 records per page.
```

---

## Page

### Definition

`Page<T>` is a Spring object that holds:

1. Records
2. Pagination Metadata

---

### Difference Between List and Page

#### List

```java
List<Post>
```

Contains:

```text
Only records
```

Example:

```java
[
    Post1,
    Post2,
    Post3
]
```

---

#### Page

```java
Page<Post>
```

Contains:

```text
Records
+
Metadata
```

Think:

```java
{
    content : [Post1, Post2],

    pageNumber : 0,

    pageSize : 2,

    totalPages : 4,

    totalElements : 8
}
```

(Not actual Java syntax)

---

## Important Methods of Page

### Get Records

```java
page.getContent()
```

Returns:

```java
List<Post>
```

Only the actual records.

---

### Get Current Page Number

```java
page.getNumber()
```

Returns:

```text
Current page number
```

---

### Get Total Pages

```java
page.getTotalPages()
```

Returns:

```text
Total number of pages
```

---

### Get Total Records

```java
page.getTotalElements()
```

Returns:

```text
Total records in database
```

---

## Service Layer Example

### Professional Spring Version

```java
public Page<PostResponse> getAllPosts(
        int page,
        int size){

    Pageable pageable =
            PageRequest.of(page, size);

    return postRepository.findAll(pageable)
            .map(this::toResponse);
}
```

---

### What Happens Internally?

```text
Create pagination instructions
        ↓
Fetch page from database
        ↓
Convert Post → PostResponse
        ↓
Preserve pagination metadata
        ↓
Return Page<PostResponse>
```

---

## @RequestParam

### Definition

`@RequestParam` tells Spring to read values from URL query parameters.

Example:

```java
@RequestParam
int page
```

means:

```text
Read page value from URL.
```

---

### Example URL

```http
/api/posts?page=1&size=5
```

Spring automatically extracts:

```java
page = 1
```

```java
size = 5
```

and passes them into the controller method.

---

## Controller Example

```java
@GetMapping
public Page<PostResponse> getAllPosts(

        @RequestParam(defaultValue = "0")
        int page,

        @RequestParam(defaultValue = "5")
        int size
){

    return postService.getAllPosts(
            page,
            size
    );
}
```

---

## defaultValue

### Definition

Used when the client does not provide a value.

Example:

```java
@RequestParam(defaultValue = "0")
int page
```

means:

```text
If page is missing,
use page 0.
```

---

Example:

```java
@RequestParam(defaultValue = "5")
int size
```

means:

```text
If size is missing,
use size 5.
```

---

### Request Without Parameters

```http
GET /api/posts
```

Spring automatically uses:

```java
page = 0
size = 5
```

---

### Request With Parameters

```http
GET /api/posts?page=2&size=3
```

Spring automatically uses:

```java
page = 2
size = 3
```

---

## Full Request Flow

```text
Client Request
GET /api/posts?page=1&size=2
        ↓
@RequestParam extracts values
        ↓
page = 1
size = 2
        ↓
Controller
        ↓
Service
        ↓
PageRequest.of(1,2)
        ↓
Repository
        ↓
Database
        ↓
Page<Post>
        ↓
Convert Post → PostResponse
        ↓
Page<PostResponse>
        ↓
JSON Response
```

---

## Key Concepts Learned

✅ Pagination

✅ Page Number

✅ Page Size

✅ Pageable

✅ PageRequest

✅ Page

✅ Page Metadata

✅ getContent()

✅ @RequestParam

✅ defaultValue

✅ URL Query Parameters

---

## Interview Questions

### Q: What is Pagination?

Pagination is the process of dividing large datasets into smaller pages and returning only a subset of records.

---

### Q: What is Pageable?

`Pageable` is a Spring interface that stores pagination instructions such as page number, page size, and sorting information.

---

### Q: What is PageRequest?

`PageRequest` is a class that implements `Pageable` and is used to create pagination instructions.

Example:

```java
PageRequest.of(0,5);
```

---

### Q: What is Page?

`Page<T>` is a Spring object that contains both:

```text
Records
+
Pagination metadata
```

such as:

- Total pages
- Total elements
- Current page
- Page size

---

### Q: Difference Between List and Page?

`List<T>` contains only records.

`Page<T>` contains records plus pagination metadata.

---

### Q: What does @RequestParam do?

It extracts values from URL query parameters and injects them into controller method parameters.

Example:

```http
GET /api/posts?page=1&size=5
```

↓

```java
page = 1
size = 5
```

---

### Q: What does defaultValue do?

Provides a fallback value when the client does not supply a query parameter.

Example:

```java
@RequestParam(defaultValue = "0")
int page
```

If page is missing:

```java
page = 0
```

--- 
Understanding the expanded Java version is more important than memorizing the shortcut syntax.
## 14. Security Notes

- Never commit real DB passwords. Use `${ENV_VAR}` placeholders.
- Never paste real passwords in chat, AI tools, or GitHub.
- If a secret leaks anywhere, **treat it as compromised** and rotate it.
  - Supabase → Project Settings → Database → Reset Password.
- `.gitignore` should include `target/`, `.idea/`, `.env`, `application-local.properties`.

---

## 15. Interview Q&A Prep

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

**Q: What's the difference between HTTP method and URL?**
A: The method (`GET`, `POST`, `PUT`, `DELETE`) is the verb — *what* you want to do. The URL is the address — *what* you're acting on.

**Q: What's the difference between a Controller and a Service?**
A: The Controller handles HTTP. The Service contains business logic. Controllers must call the Service, never the Repository directly.

**Q: What is `@RequestBody`?**
A: It tells Spring to read the JSON body of the request and convert it into a Java object (using Jackson).

**Q: What is `@PathVariable`?**
A: It extracts a value from the URL path — e.g., `{id}` in `/api/posts/{id}` — and puts it into the method parameter.

**Q: Difference between client and server?**
A: The server provides data (Spring Boot app). The client requests data (browser, Postman, React). The same server can serve many clients.

**Q: Why does `PUT` need `@PathVariable` AND `@RequestBody`?**
A: `@PathVariable` identifies *which* row. `@RequestBody` supplies *what* to update it with.

**Q: Why does `save()` insert on create but update on PUT?**
A: `save()` checks `id`. If null → INSERT. If set → UPDATE.

**Q: Why does `createdAt` never change on update?**
A: Because of `@Column(updatable = false)`. Hibernate excludes it from UPDATEs.

**Q: What HTTP status code does the controller return when the post isn't found?**
A: Currently `200 OK` with a `null` body. In Week 2 we'll change this to `404 Not Found` with a proper JSON error.

**Q: What's the difference between PUT and PATCH?**
A: `PUT` replaces the whole resource (all fields). `PATCH` updates only specific fields.

**Q: What is a DTO and why use one?**
A: A Data Transfer Object carries data between layers — separate from the entity. Used so the client can't set DB-generated fields (id, timestamps) and can't see fields they shouldn't. Also decouples the API contract from the DB schema.

**Q: Why does `PostRequest` have no `id` field?**
A: Because the client should never set the id — the DB generates it. Removing `id` means even if the client sends it, Jackson silently drops it.

**Q: Where do the two conversions happen in the service?**
A: `toEntity(PostRequest)` when receiving data (request → entity). `toResponse(Post)` when returning data (entity → response).

**Q: Why does `.map(this::toResponse)` work?**
A: Because `this::toResponse` is a method reference — shorthand for `post -> toResponse(post)`. It takes a `Post` and returns a `PostResponse`.

**Q: What is Bean Validation?**
A: A Java standard for declaring validation rules on Java classes using annotations like `@NotBlank`, `@Size`, `@Email`. It's a specification — Hibernate Validator implements it. Spring integrates it via `@Valid`.

**Q: What's the difference between `@Valid` and `@Validated`?**
A: `@Valid` is the standard Jakarta annotation. `@Validated` is Spring's variant that additionally supports **validation groups** — useful when different scenarios need different rule sets. For simple cases, `@Valid` is enough.

**Q: Why validate on the DTO instead of the entity?**
A: The DTO is the API boundary — validate input where it arrives. The entity is for the DB — its `@Column(nullable = false)` enforces DB constraints. Two layers of protection.

**Q: What happens to a request if validation fails?**
A: Spring rejects it with HTTP 400 Bad Request **before** the controller method runs. The service and database are never touched.

---

## 16. Progress Tracker

| Week | Topic | Status |
|---|---|---|
| 1 | Entities | ✅ Done (`Post`) |
| 1 | Repositories | ✅ Done (`PostRepository`) |
| 1 | Services | ✅ Done (`PostService`) |
| 1 | Controllers | ✅ Done (`PostController`) |
| 1 | CRUD + cURL Testing | ✅ Done (POST, GET, PUT, DELETE all working) |
| 2 | DTOs (`PostRequest`, `PostResponse`) | ✅ Done (id=999 dropped test passed) |
| 2 | Validation (`@NotBlank`, `@Size`, `@Valid`) | ✅ Done (400 on empty title, 200 on valid) |
| 2 | Exception Handling | ✅ Done|
| 2 | Pagination + Sorting | ⬜ Next|
| 2 | PostgreSQL + Relationships | ⬜ |
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
- Controller base path: `/api/...`
- HTTP verbs: `GET` (read), `POST` (create), `PUT` (update), `DELETE` (remove)
- Local testing URL: `http://localhost:8080`
- DTOs live in `dto/` package: `XxxRequest` (client → server), `XxxResponse` (server → client)
- Validation annotations go on **DTOs** (not entities)
- Always `jakarta.*`, never `javax.*` (Spring Boot 3+)

---

*Continuously updated as I learn.*
