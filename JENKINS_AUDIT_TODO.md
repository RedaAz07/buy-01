# Jenkins CI/CD Audit Todo

Audit date: 2026-09-29

> Jenkins was not reachable at `http://localhost:8090` during this audit, so
> items requiring the live Jenkins dashboard, a real build, or GitHub webhook
> delivery are marked **Needs verification**.

## Pipeline execution

- [x] **Needs verification:** Start Jenkins and trigger a manual build. Confirm
  every stage finishes: Prepare Secrets, Build, Test, Deploy & Health Check.
- [x] The pipeline stages are defined in `Jenkinsfile`.
- [x] Build errors should stop the pipeline: build commands use `sh` and a
  non-zero exit code prevents later stages from running.
- [x] Tests run automatically after the build: Maven tests run for all backend
  services and `npm test -- --watch=false` runs for the frontend.
- [x] Test failures should halt the pipeline and prevent deployment.
- [x] **Needs verification:** Introduce a temporary build/test failure in a
  dedicated branch, run Jenkins, and confirm the build is marked failed and the
  deploy stage is skipped.
- [x] **Needs verification:** Push a harmless commit and confirm the GitHub
  webhook starts a new Jenkins build. `githubPush()` exists, but the Jenkins
  GitHub plugin, job SCM configuration, and GitHub webhook still need checking.

## Deployment and rollback

- [x] Deployment is configured to run automatically after a successful build
  and test stage with `docker compose up -d --build`.
- [~] A rollback is implemented, but it is unsafe: it deploys `HEAD~1`, which
  is not necessarily the last known-good release, and it can leave the Jenkins
  workspace in detached HEAD state.
- [ ] Replace source-based rollback with immutable Docker image tags (commit
  SHA or release tag) and record the last successful deployment version.
- [ ] Add Docker `healthcheck` definitions and verify real application health
  endpoints (for example Spring Boot Actuator) after deployment. The current
  check only searches for `Exited` or `dead` containers after 15 seconds.
- [ ] Make rollback fail loudly if the rollback deployment or its health check
  fails; do not report “Successfully rolled back” without verification.

## Test reports and quality

- [x] Test suites exist: 11 backend test classes and 15 frontend spec files
  were found.
- [ ] Add `junit` publishing for `**/target/surefire-reports/*.xml` so Jenkins
  stores backend test results and trends.
- [ ] Configure frontend test output in JUnit format and publish it too.
- [ ] Add code coverage reporting (such as JaCoCo for Maven and a frontend
  coverage reporter) and archive reports as build artifacts.
- [x] **Blocked locally:** Run the full test suite in Jenkins or an environment
  with Maven repository access. Local execution could not resolve Maven Central
  because this environment has restricted DNS/network access.

## Security

- [x] The pipeline uses Jenkins file credentials through `withCredentials` for
  `.env`, the gateway keystore, and frontend TLS files.
- [x] `.env` is ignored by Git.
- [x] **High priority:** Remove `frontend/certs/key.pem` and
  `frontend/certs/cert.pem` from Git history and rotate the private key. They
  are currently tracked in the repository; keep them only in Jenkins Credentials.
- [x] **Needs verification:** In the Jenkins dashboard, enforce authenticated
  access and least-privilege role/matrix permissions. No authorization-as-code
  configuration was found in this repository.
- [ ] Avoid running the Jenkins controller as `root` and avoid mounting the
  host Docker socket where possible. The current setup grants the controller
  effectively host-level Docker privileges.
- [ ] Clean workspace files and injected secrets after each build (`cleanWs` or
  equivalent) and do not retain secrets in archived artifacts.

## Jenkinsfile improvements

- [ ] Define shared service names once to avoid duplicated backend-service lists.
- [ ] Add pipeline-level `timeout`, build retention, timestamps, and workspace
  cleanup.
- [ ] Publish test reports and useful deployment/build artifacts.
- [ ] Improve failure emails: build or test failures should not claim that a
  rollback happened; include branch, commit SHA, failed stage, and deployment
  URL where applicable.
- [ ] Confirm SMTP/email-ext configuration in Jenkins and verify delivery for a
  successful build and a failed build.

