# Releasing

Versioning
- Use tags of the form `vMAJOR.MINOR.PATCH` (e.g. `v0.1.0`).

How to create a release (recommended)
1. Update `CHANGELOG.md` (add your notes under the `Unreleased` section).
2. Commit the changes and push them to `main`.
3. Create an annotated tag and push it:

```bash
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0
```

The GitHub Actions workflow (`.github/workflows/release.yml`) will run on the pushed tag; it builds the fat JAR and publishes a GitHub Release with the JAR attached.

What is published
- The workflow builds `escampe/build/libs/*-all.jar` (fat JAR) and attaches it to the GitHub Release.

Local test of build

```bash
cd escampe
./gradlew clean fatJar
ls build/libs
```

If you need to create the release via the GitHub UI instead, create a release for the tag and upload the generated JAR manually.
