# GitHub + Railway Deployment Guide

## GitHub Repository Searcher — Spring Boot 3.3.5, Java 17, PostgreSQL

This guide covers publishing the existing project to GitHub and deploying it to Railway. The deployment uses two Railway services:

1. **GitHub Searcher API** — the Spring Boot application.
2. **PostgreSQL** — the managed database used by the API.

The application should continue using the existing layered implementation. Do not rebuild the project or commit credentials just to deploy it.

---

## 1. Prepare the project locally

Open PowerShell in the project directory:

```powershell
cd E:\backend-Java\FindRepo
```

### 1.1 Run the test suite

Run the existing tests before publishing:

```powershell
.\mvn.cmd clean test
```

Expected result from the current test evidence: **43 tests, 0 failures, 0 errors, 0 skipped**. If the result differs, investigate the failure before deploying.

### 1.2 Build the application

```powershell
.\mvn.cmd clean package
```

Check that Maven creates an executable Spring Boot JAR under `target\`.

### 1.3 Check secrets and ignored files

The following files and folders should not be committed:

- `.env`
- Any file containing a real GitHub token or database password
- `target/`
- IDE-specific files, local logs, and temporary files

Confirm `.env` is ignored:

```powershell
git check-ignore .env
```

Check whether `.env` was already tracked:

```powershell
git ls-files .env
```

If that command prints `.env`, remove it from Git tracking without deleting the local file:

```powershell
git rm --cached .env
```

Then commit the `.gitignore` change. If a real token or password was ever committed or exposed, rotate it; deleting it in a later commit does not make the old secret safe.

### 1.4 Keep a safe environment example

You may commit an `.env.example`, but it must contain placeholders only. For example, use the variable names already read by your application:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/github_searcher
DB_USERNAME=YOUR_LOCAL_DB_USERNAME
DB_PASSWORD=YOUR_LOCAL_DB_PASSWORD
GITHUB_API_BASE_URL=https://api.github.com
GITHUB_TOKEN=YOUR_GITHUB_TOKEN
SERVER_PORT=8081
```

Do not put real values in this file. Confirm these names against `application.properties` / `application.yml` before using them; the application configuration is the source of truth.

---

## 2. Push the project to GitHub

Create an empty repository on GitHub. Do not initialize it with a README if the project already has one locally.

In PowerShell, check the current branch and configured remotes:

```powershell
git status
git branch --show-current
git remote -v
```

If no remote named `origin` exists, add your repository URL:

```powershell
git remote add origin https://github.com/<GITHUB_USERNAME>/<REPOSITORY_NAME>.git
```

Stage and commit the project:

```powershell
git add .
git status
git commit -m "Prepare GitHub Repository Searcher for deployment"
```

Review the `git status` output before committing. Make sure `.env` and credentials are not staged.

Push the current branch (replace `main` if your branch has a different name):

```powershell
git push -u origin main
```

Open the GitHub repository in your browser and confirm that the source code, `pom.xml`, README, and Postman collection are present, while `.env` is absent.

---

## 3. Create a Railway project

1. Open [Railway](https://railway.com/) and sign in with GitHub.
2. Create a new project.
3. Add a service from the GitHub repository you just pushed.
4. Authorize Railway to access that repository if prompted.
5. Select the correct repository and deployment branch (normally `main`).
6. Name the application service something recognizable, such as `github-searcher-api`.

Railway's build system, Railpack, supports Java and can detect many projects automatically. Since this project has a Maven `pom.xml`, first try the automatic build configuration. Only add custom build/start commands if the detected commands fail or are incorrect.

### Optional build/start command overrides

If you need to specify commands manually:

- **Build command:** `mvn -B clean package -DskipTests`
- **Start command:** `java -jar target/<actual-application-jar-name>.jar`

Use the actual executable JAR name produced by your Maven build. Do not guess the artifact name. If tests are intended to run during Railway builds, omit `-DskipTests`; the local test run should still be completed before deployment.

---

## 4. Add PostgreSQL to the Railway project

1. In the Railway project canvas, select **New** / **Add Service**.
2. Choose **Database → PostgreSQL**.
3. Wait for the PostgreSQL service to become ready.
4. Keep the database private. The API and database are in the same Railway project, so the API should connect using Railway's internal service variables rather than exposing PostgreSQL publicly.
5. Keep the database's persistent storage/volume configuration in place. Do not delete the database service or its volume when redeploying the API.

Railway's PostgreSQL service provides variables such as `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`, and `DATABASE_URL`.

### 4.1 Connect the API to PostgreSQL

Open the **GitHub Searcher API** service → **Variables**. Add the variables your existing Spring Boot configuration expects.

For this project, the README identifies these application variables:

| API service variable | Suggested Railway value |
|---|---|
| `DB_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_USERNAME` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `GITHUB_API_BASE_URL` | `https://api.github.com` |
| `GITHUB_TOKEN` | Your GitHub personal access token, entered as a secret value |

**Important:** In the examples above, `Postgres` is the example PostgreSQL service name. Replace it with the exact name of your PostgreSQL service in Railway if you named it differently. Railway variable references use the service name and variable name.

`DB_URL` is shown as a JDBC URL because Spring Boot's PostgreSQL JDBC driver expects a URL beginning with `jdbc:postgresql://`. Do not blindly assign Railway's raw `postgresql://...` `DATABASE_URL` to `DB_URL` unless your application explicitly converts that format.

If your source code uses different environment variable names, follow the names in `application.properties` / `application.yml` and update this table accordingly. Do not create duplicate configuration keys that the application does not read.

After adding variables, review and deploy the staged changes in Railway.

---

## 5. Configure the application port

Railway provides a runtime `PORT` environment variable. The Spring Boot application must listen on that port for public HTTP traffic and health checks to work correctly.

If the current application configuration only reads `SERVER_PORT`, update the port property in `src/main/resources/application.properties` to prefer Railway's `PORT` while retaining the local default:

```properties
server.port=${PORT:${SERVER_PORT:8081}}
```

If the project uses `application.yml`, apply the equivalent nested environment-variable fallback there instead. Do not maintain two conflicting `server.port` settings.

Locally, `PORT` will normally be absent, so the application can continue using `SERVER_PORT` or the default `8081`. On Railway, the injected `PORT` takes precedence.

Commit and push this change if you needed to make it:

```powershell
git add src/main/resources
git commit -m "Support Railway runtime port"
git push
```

---

## 6. Deploy and inspect logs

1. Open the API service's **Deployments** tab.
2. Start or redeploy the latest deployment if it did not start automatically.
3. Open the build and runtime logs.
4. Confirm that the Maven build succeeds and Spring Boot starts without errors.
5. Check that the application connects to PostgreSQL successfully.
6. If the deployment fails, read the first meaningful build/runtime error before changing configuration.

Common configuration issues:

| Symptom | What to inspect |
|---|---|
| Application does not start | Runtime logs, executable JAR name, Java version, start command |
| Database connection refused | `DB_URL`, PostgreSQL service name, private host/port references, credentials |
| Authentication failed for database | `DB_USERNAME` and `DB_PASSWORD` references |
| Application starts but public URL fails | Application is listening on Railway's `PORT` |
| GitHub API returns 401 | `GITHUB_TOKEN` value and token validity |
| GitHub API returns 403/429 | GitHub rate-limit response and reset/retry headers |
| No automatic deployment after push | Connected branch, GitHub app repository access, Railway autodeploy setting |

Do not paste full deployment logs into a public issue if they contain credentials, tokens, or connection strings.

---

## 7. Generate a public API domain

1. Open the API service in Railway.
2. Go to **Settings → Networking** (the label may change as the Railway dashboard evolves).
3. Generate a public domain.
4. Copy the HTTPS domain Railway provides.

The API's base URL will look similar to:

```text
https://<your-service>.up.railway.app
```

This is an example format, not your actual deployed URL. Use the domain generated for your service.

Do not enable public networking for PostgreSQL just to make the API work. The database should remain private unless you have a separate, deliberate need for external database access.

---

## 8. Verify the deployed API

Use the generated Railway domain in Postman or a browser. Do not use `localhost` for these checks.

### 8.1 Swagger UI

Open:

```text
https://<your-service>.up.railway.app/swagger-ui.html
```

If that path does not resolve, try the Springdoc default UI path:

```text
https://<your-service>.up.railway.app/swagger-ui/index.html
```

The project README documents `/swagger-ui.html` and `/api-docs`; confirm the actual configured paths in the deployed application.

### 8.2 Search GitHub and save results

**POST** `/api/github/search`

```json
{
  "query": "spring boot",
  "language": "Java",
  "sort": "stars"
}
```

Expected: HTTP 200 when the GitHub request succeeds, with a response message and repository results. The API should persist the returned repositories in Railway PostgreSQL.

### 8.3 Retrieve saved repositories

**GET** `/api/github/repositories?language=Java&minStars=100&sort=stars`

Expected: HTTP 200 with locally stored results matching the filters. This endpoint should read from PostgreSQL rather than issue a new GitHub search request.

### 8.4 Check validation

Try an invalid request, such as a blank search query or unsupported sort value. Confirm that the API returns the expected 400 response.

### 8.5 Confirm persistence

Search for repositories, redeploy or restart only the API service, and call the GET endpoint again. Previously saved data should still be present in PostgreSQL. Do not delete or recreate the PostgreSQL service during this check.

---

## 9. Enable automatic deployments

When the API service is connected to your GitHub repository, configure its deployment branch and make sure GitHub autodeploys are enabled.

After that, a normal push to the connected branch should trigger a new Railway deployment:

```powershell
git add .
git commit -m "Update repository searcher"
git push
```

Check the Railway deployment history to confirm that the new commit was deployed. Autodeploy requires Railway's GitHub app to have access to the repository and the correct branch to be selected.

---

## 10. Final submission checklist

Before submitting the assignment, confirm each item:

- [ ] Project source code is available in GitHub.
- [ ] `.env`, GitHub tokens, database passwords, and other secrets are not committed.
- [ ] Railway Spring Boot service is deployed successfully.
- [ ] Railway PostgreSQL service is running and retains data.
- [ ] Spring Boot connects to Railway PostgreSQL through private service variables.
- [ ] Application listens on Railway's injected `PORT`.
- [ ] Public API domain opens over HTTPS.
- [ ] Swagger UI is accessible through the public domain.
- [ ] `POST /api/github/search` works against the deployed service.
- [ ] `GET /api/github/repositories` returns saved data and filtering works.
- [ ] Invalid input returns the expected error response.
- [ ] Application logs do not expose secrets.
- [ ] Loom video demonstrates the deployed application and API flow.
- [ ] Submission form contains the live API URL, Swagger URL, and Loom video link.

### Submission details to record

```text
GitHub Repository: https://github.com/<GITHUB_USERNAME>/<REPOSITORY_NAME>
Live API Base URL: https://<your-service>.up.railway.app
Swagger UI:        https://<your-service>.up.railway.app/swagger-ui.html
OpenAPI JSON:      https://<your-service>.up.railway.app/api-docs
Loom Video:        <YOUR_LOOM_VIDEO_URL>
```

Replace every placeholder with the real URL after deployment. Do not submit the example Railway domain above.

---

## Official Railway references

- [Railway Quick Start — deploy from GitHub](https://docs.railway.com/quick-start)
- [Railway Services — GitHub repository source](https://docs.railway.com/services)
- [Railpack — supported languages, including Java](https://docs.railway.com/builds/railpack)
- [Build and Start Commands](https://docs.railway.com/builds/build-and-start-commands)
- [PostgreSQL — provision and connect](https://docs.railway.com/databases/postgresql)
- [Railway Variables](https://docs.railway.com/variables)
- [Railway Variables Reference](https://docs.railway.com/variables/reference)
- [Healthchecks](https://docs.railway.com/deployments/healthchecks)
- [GitHub Autodeploys](https://docs.railway.com/deployments/github-autodeploys)

> **Important:** Railway's dashboard and pricing can change. Review the current Railway plan and usage limits before deploying. This document is a deployment checklist, not a guarantee that hosting will be free.
