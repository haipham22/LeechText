# Release Workflow Guide

## How Releases Work

The CI/CD pipeline creates releases when you push version tags to GitHub.

## Version Management

1. **Update Version in `build.gradle`**
   ```gradle
   version = '1.0.0'  // Change this
   ```

2. **Commit Version Change**
   ```bash
   git add build.gradle
   git commit -m "chore: bump version to 1.0.0"
   ```

3. **Push to Main/Dev**
   ```bash
   git push origin main  # or dev
   ```

4. **Create Version Tag**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

## What Happens Next

When you push `v1.0.0`:

1. **Build Job Runs**
   - Builds JAR for macOS and Linux
   - Creates native packages (DMG, DEB)
   - Uploads artifacts

2. **Release Job Runs**
   - Downloads all artifacts
   - Creates checksums (SHA256, MD5)
   - Creates GitHub release
   - Uploads packages as release assets

## Release Assets

Each release includes:
- `LeechText.dmg` (macOS)
- `LeechText.dmg.sha256`
- `LeechText.dmg.md5`
- `leechtext_1.0.0_amd64.deb` (Linux)
- `leechtext_1.0.0_amd64.deb.sha256`
- `leechtext_1.0.0_amd64.deb.md5`

## Branch Strategy

- **`main`**: Production releases
- **`dev`**: Development builds

Both branches can create releases when version tags are pushed.

## Example Release Process

```bash
# 1. Update version
vim build.gradle  # Change version to '1.0.0'

# 2. Commit and push
git add build.gradle
git commit -m "chore: bump version to 1.0.0"
git push origin main

# 3. Create release tag
git tag v1.0.0
git push origin v1.0.0

# 4. Monitor CI
# GitHub Actions → Build Multi-Platform → Watch progress
```

## Pre-release Checklist

- [ ] Version updated in `build.gradle`
- [ ] All tests passing locally (`make build`)
- [ ] Changelog updated
- [ ] Documentation updated
- [ ] No uncommitted changes

## Post-release

- [ ] Verify release assets on GitHub
- [ ] Test downloaded packages
- [ ] Update documentation links
- [ ] Announce release
