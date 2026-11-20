# Bourget Development Guidelines

## Philosophy

- Keep the product minimal.
- Privacy is a core requirement.
- Do not add features without a clear user benefit.

## Security

- Never add INTERNET permission.
- Never integrate analytics or advertising SDKs.
- Never persist accessibility screen contents.
- Use the minimum necessary Android permissions.

## Engineering

- Prefer standard Android APIs.
- Avoid unnecessary abstractions.
- Keep dependencies minimal.
- Run Gradle build after changes.

## Scope

- App launch detection
- Confirmation overlay
- Optional launch of another app
- Cooldown and simple settings
