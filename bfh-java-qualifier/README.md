# BFH Qualifier (JAVA) – Sprint-Ready Template

This project automates the required flow:

1) On startup, POST to generate a webhook and access token.  
2) Decide your SQL question by regNo last two digits (odd → Q1, even → Q2).  
3) Read your **final SQL** from `application.yml` (`app.finalQuery`) or `final-query.sql`.  
4) Store it to `target/final-query.sql` and POST it to the webhook with the provided JWT in the `Authorization` header.

> Endpoints, headers, and submission format match the task PDF.

## Quick Start

### 0) Prereqs
- Java 17+ and Maven installed.

### 1) Configure
Edit `src/main/resources/application.yml`:
```yaml
app:
  name: "Your Real Name"
  regNo: "YourRegNo"
  email: "your@email.com"
  finalQuery: "SELECT 1"   # replace with your real SQL
```
Alternatively, leave `finalQuery` empty and place your SQL in a file named `final-query.sql` at the project root.

### 2) Build & Run
```bash
mvn -q -DskipTests package
java -jar target/bfh-java-qualifier-0.0.1-SNAPSHOT.jar
```
(Or run with inline properties, no file edits needed:)
```bash
java -jar target/bfh-java-qualifier-0.0.1-SNAPSHOT.jar   --app.name="Your Name"   --app.regNo="REG12345"   --app.email="you@example.com"   --app.finalQuery="YOUR_SQL_HERE"
```

### 3) Output
- App prints whether you are **Question 1 (Odd)** or **Question 2 (Even)** based on regNo.
- Saves your SQL at `target/final-query.sql`.
- Submits `{ "finalQuery": "..." }` to the webhook with `Authorization: <accessToken>`.

### 4) Repo + Jar
- Push this repo to GitHub (public).
- Ensure your built JAR is in `target/`. Upload that JAR as a release or commit the file and copy the **raw** downloadable link.
- Fill the form with your GitHub repo + JAR raw download link.

## Notes
- If the response includes a `webhook`, the app uses it. Otherwise, it falls back to the public submission endpoint.
- Replace the placeholder SQL with your actual solution from the assigned question.
