# Email Client

**English** | [Türkçe](README.tr.md)

A desktop email client written in Java and JavaFX. It reads mail over IMAP and sends mail over SMTP using Jakarta Mail.

> Work in progress. The plan below will become the full README when the project reaches v1.0.

## Features (plan)

**MVP**
- [ ] Account setup: email address and app password; presets for Gmail, Yandex and iCloud, or a custom server; "Test connection" button
- [ ] Inbox (IMAP): folders, latest messages with "load more", reading pane
- [ ] Mark as read / unread, delete (move to trash), search in loaded messages
- [ ] Sending (SMTP): new message, reply, forward; recipient address validation
- [ ] Network operations in the background with a loading indicator, the UI never freezes
- [ ] Password encrypted with Windows DPAPI (never stored as plain text); environment variables supported for development and tests
- [ ] HTML emails shown as plain text (raw HTML is never rendered)
- [ ] Settings in the user folder, app icon, version number, About window
- [ ] Unit and integration tests (JUnit 5 + GreenMail in-memory mail server)
- [ ] Windows installer (jpackage + Inno Setup)

**Later**
- Viewing and saving attachments
- HTML view
- Multiple accounts
- New mail notifications
- OAuth2 sign-in (required for Outlook / Hotmail)

## Tech Stack

- Java 21, Maven
- JavaFX
- Jakarta Mail (Eclipse Angus)
- JUnit 5, GreenMail
- jpackage, Inno Setup
