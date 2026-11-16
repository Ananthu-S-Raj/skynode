# SkyNode GitHub release readiness

Review date: 2026-11-16. This review addresses publication blockers identified in `FINAL_AUDIT_REPORT.md`; it does not replace the full audit. No Git history, database record, or booking logic was changed during this review.

## Release decision

**Do not publish this working tree yet.** Commit `4fd218b19d25772b1fd778790e02a464f04dd758` contains a literal PostgreSQL datasource password and is already the tip of the public GitHub repository at `git@github.com:Ananthu-S-Raj/skynode.git`. A clean new release cannot make that historical exposure disappear. Rotate the credential and decide how to handle the existing public repository before publishing further work. Public passenger lookup and cancellation also need stronger verification before a live deployment with real passenger information.

## 1. Credential exposure findings

| Finding | Evidence, without credential values |
| --- | --- |
| Exposed credential | Commit `4fd218b1` contains the same nonempty datasource password in `src/main/resources/application.properties` and `src/test/resources/application-test.properties`. Both configurations referred to PostgreSQL. |
| Current source | `src/main/resources/application.properties` now uses file-based H2 with an empty H2 password and environment-provided administrator settings. `src/test/resources/application.properties` uses an isolated H2 test database. The exposed PostgreSQL password is not in current application source. |
| Other history | The earlier examined main-branch commits `79c9d1e9`, `59f11198`, and `c2c794ae` had PostgreSQL configuration but no literal datasource password. A scan of locally reachable Git history found no other candidate committed credential locations. This is a pattern-based scan, not a guarantee that every possible secret format was detected. |
| References | Local main, `origin/main`, and the remote `refs/heads/main` point to the affected commit. Remote enumeration found no other branches or tags at review time. Local Codex internal refs were included in the history scan. Unadvertised GitHub pull-request refs, forks, clones, and caches cannot be ruled out. |
| Published status | `git ls-remote origin HEAD refs/heads/main` returned the affected commit; the repository's GitHub web URL returned HTTP 200 without authentication. The earlier audit assumption that it had not been pushed is therefore incorrect. |
| Other sensitive values | The inspected source/history scan found no additional credential locations. Ignored local `.env.admin` and `data/skynode.mv.db` exist and were intentionally not scanned for or printed as secrets. They must remain private. |

## 2. Credential rotation

Treat the historical PostgreSQL password as compromised, even though the application now uses H2. Determine which PostgreSQL role and server used it, whether that role still exists, and whether the same password was reused elsewhere. Rotate or revoke it through the PostgreSQL administrator or hosting provider; update any still-active applications or secret stores through private environment configuration. If the database is retired, disable the old role or server access. Review relevant access logs if available. The test properties used the same value, so this is one exposed value, but check every service where it was reused. Do not put a replacement password or its hash in Git, a shell command line, chat, or this report. No credential was changed automatically.

Making the existing repository private may reduce future access, but it does not revoke a password or retract copies already obtained. GitHub notes that existing public forks remain public when a repository is made private. See [GitHub repository visibility guidance](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/managing-repository-settings/setting-repository-visibility).

## 3. History remediation strategy

**Recommended: a fresh repository with clean initial history, after credential rotation.** The current repository is already public, so a fresh repository protects the new release history but does **not** remediate the old public repository. Preserve this original local repository and its history separately. Create a new directory from reviewed current source, explicitly excluding `.git`, `.env.admin`, H2 data, build outputs, dependencies, and local backups. Review the staged manifest and run the secret scan again before its first commit. Use a new empty GitHub repository and a normal push. Separately decide whether to make the old public repository private; do not assume this removes forks or clones. No fresh repository or Git operation was created here.

If the original URL and commit history must be retained, use a coordinated history rewrite in a disposable clone only after rotation and explicit approval. Inventory every branch, tag, remote, pull-request reference, fork, and collaborator clone; remove the sensitive property value from all affected references with a reviewed tool such as `git-filter-repo`; scan every rewritten reference; then coordinate replacement of remote history and stale clones. Rewriting changes commit IDs and can break links, signatures, open pull requests, and collaborators' branches. Old clones or cached GitHub references can reintroduce the secret. A force push would be required for the original remote and is **not authorized or performed** by this task. GitHub may need to assist with affected cached or pull-request references; see [GitHub's sensitive-data removal guidance](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository).

## 4. Booking privacy assessment

The existing public API permits `GET /api/v1/bookings/pnr/{pnr}`, `GET /api/v1/bookings/{id}`, `GET /api/v1/bookings/passenger/{passengerName}`, and `PUT /api/v1/bookings/{id}/cancel` without administrator login. The booking response includes booking ID, PNR, passenger name, email, phone, age, gender, flight and route, seat, schedule, status, total amount, and payment status. Lookup by passenger name and sequential numeric ID make enumeration easier. The PNR is an eight-character hexadecimal value, so it is not a strong access secret. Cancellation requires the numeric booking ID, but no additional passenger verification. A publicly obtainable CSRF token prevents some browser cross-site requests; it does not authenticate a person controlling an HTTP client. No rate limit was identified in the application.

Current transaction handling, pessimistic seat locking, and cancellation state checks protect booking integrity, not passenger identity. For a **local portfolio demonstration using fictional records**, this limitation can be documented. It is a **blocker for a publicly accessible live service with real passenger data**. A small future design could issue a long, random per-booking management secret at booking time, store only its hash, and require it for detailed lookup and cancellation. Add request rate limiting and minimize public response fields. PNR plus contact detail is a simpler but weaker alternative. Review privacy and authorization before changing the existing API contract. No booking logic was changed here.

## 5. Publishable and private files

Review and publish the current Java source, frontend source, tests, Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`), `pom.xml`, `frontend/package.json`, `frontend/pnpm-lock.yaml`, `frontend/.env.example`, `README.md`, `AUTHENTICATION.md`, `docs/API.md`, `docs/ARCHITECTURE.md`, and relevant scripts/docs only after their staged contents pass the checks below. The source currently has many uncommitted changes and an untracked `frontend/`; a normal push of the current commit would publish the old credential, not the reviewed current application. Do not use `git add -A` without checking the resulting manifest.

Keep `.git` history from the old repository, `.env.admin`, real credentials or hashes, `data/skynode.mv.db` and all H2 locks/backups, real passenger exports, `target/`, `frontend/node_modules/`, `frontend/dist/`, logs, editor files, and temporary files private. `.gitignore` covers the local database, credentials, build outputs, and frontend dependencies. A tracked-file review found 64 tracked paths and none of those private file categories; Git ignores are not a substitute for reviewing staged files. The existing local H2 database was not opened, copied, reset, or modified.

The README documents Java 21, the Maven wrapper, Node/pnpm setup, hidden admin-password setup, local profile and H2 console, frontend proxy, isolated tests, and database location. `docs/API.md` and `docs/ARCHITECTURE.md` document the API and architecture. The frontend example environment file contains only nonsecret API/proxy addresses. Fresh-clone startup still depends on the owner creating a **new private admin password/hash** through the documented setup script; no account credential is shipped.

## 6. Actual verification on 2026-11-16

| Check | Result |
| --- | --- |
| Backend | `./mvnw clean test`: **30 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. The first sandboxed attempt could not open the existing Maven cache lock; rerun with cache access passed. |
| Frontend | Node test runner: **19 tests passed, 0 failed**. |
| Frontend production build | Vite: **49 modules transformed; build succeeded**. |
| Git diff | `git diff --check`: passed. Existing uncommitted changes were preserved. |
| Tracked-file review | 64 tracked paths; no tracked `.env`, H2 database, `target/`, `node_modules/`, or `dist/` path. |
| Working-tree secret scan | Pattern scan of Git candidate source files found no confirmed live credential. Hits in documentation were placeholders/variable references; datasource password assignments in current main/test configuration are empty. |
| History secret scan | Found the PostgreSQL datasource password in the two properties paths at `4fd218b1`; no other candidate credential locations in reachable local history. Values were not printed. |
| Remote | Remote main and public web URL confirmed exposed commit is online. Remote branch/tag enumeration found only main at review time. |

These checks do not validate third-party forks, external clones, GitHub caches, or every possible secret format. Frontend tests and build do not prove passenger authorization is safe for a live public deployment.

## 7. Remaining publication blockers

1. Rotate/revoke the exposed PostgreSQL credential and check reuse.
2. Choose and approve treatment of the already-public old repository. A fresh repository alone does not retract old history.
3. Review the full current staged manifest and scan it again before any initial public commit. Current source, docs, and frontend are largely uncommitted/untracked.
4. Keep real passenger data out of the release. Strengthen booking lookup/cancellation authorization before a live public deployment with real users.

## 8. Safe commands for an eventual first push

Only **after** credential rotation, old-repository decision, and explicit approval to publish: copy reviewed source into a new directory without the old `.git` directory or private files. In that new directory, use a new empty remote URL. Inspect the staged list and scan it before committing. These commands are illustrative and have **not** been run:

```bash
git init -b main
git add README.md .gitignore pom.xml mvnw mvnw.cmd .mvn src frontend docs scripts AUTHENTICATION.md GITHUB_RELEASE_READINESS.md
git diff --cached --check
git diff --cached --name-only
# Review every staged file and run a secret scanner before the next command.
git commit -m "Initial public release"
git remote add origin <NEW_EMPTY_GITHUB_REPOSITORY_SSH_URL>
git push -u origin main
```

Do not copy the old `.git` metadata. Do not push this repository's current `main` to a new remote. Do not force-push or rewrite the existing remote without a separately reviewed plan and approval.
