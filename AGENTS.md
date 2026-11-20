# Bourget Development Guidelines

## Philosophy

- Keep the product minimal.
- Privacy is a core requirement.
- Do not add features without a clear user benefit.

## Security

- Never add INTERNET permission.
- Never integrate analytics or advertising SDKs.
- Never persist accessibility screen contents, visited URLs or browsing history.
- Read only what a rule needs: package names from window events, and for website rules only
  the address bar node of a supported browser, looked up by view id. Never traverse or read
  page content.
- Never infer a visit from page text. If the address bar cannot be read reliably, do nothing.
- Use the minimum necessary Android permissions.
- Keep the in-app disclosure and accessibility service description accurate whenever what
  the service reads or stores changes.

## Engineering

- Prefer standard Android APIs.
- Avoid unnecessary abstractions.
- Keep dependencies minimal.
- Run Gradle build after changes.

## Scope

- App launch detection
- Website visit detection in supported browsers (address bar only; add a browser only after
  verifying its address bar node on a real device)
- Confirmation overlay
- Optional launch of another app
- Cooldown and simple settings
