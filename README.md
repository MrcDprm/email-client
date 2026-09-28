<p align="center">
  <img src="src/main/resources/com/mrcdprm/emailclient/icon.png" alt="Email Client icon" width="96">
</p>

<h1 align="center">Email Client</h1>

<p align="center">
  <b>English</b> | <a href="README.tr.md">Türkçe</a>
</p>

<p align="center">
  A desktop email client written in Java and JavaFX. It reads mail over IMAP and sends mail over SMTP<br>
  using Jakarta Mail, with a modern interface and the password stored encrypted.
</p>

<p align="center">
  <a href="https://github.com/MrcDprm/email-client/releases/latest"><b>⬇️ Download for Windows</b></a>
</p>

<p align="center">
  <img src="docs/main.png" alt="Main window with folders, message list and reading pane" width="820">
</p>

> The user interface is in Turkish.

## Features

**Reading (IMAP)**
- Folders with icons (Inbox, Sent, Drafts, Spam, Trash, Starred…), newest messages first
- "Load more" paging (30 messages at a time), search within loaded messages
- Reading pane with sender avatar; unread messages are bold with a blue dot
- Mark as read / unread, delete (moves to Trash)
- Changes are made on the server, so they stay in sync with the Gmail web and mobile apps
- HTML emails are shown as **plain text**: scripts and remote content never run

**Writing (SMTP)**
- New message, **reply** (with quoted original, `Re:` subject, kept in the same conversation) and **forward**
- To and Cc with multiple addresses; every address is validated and the invalid one is named
- Ctrl+Enter to send, a warning for an empty subject, and a confirmation before discarding an unsent message
- Sending runs in the background; the window closes only when the message is actually sent

**Account and security**
- Presets for Gmail, Yandex and iCloud (detected from the address) or a custom IMAP/SMTP server
- The login is checked on both IMAP and SMTP before the account is saved
- The password is encrypted with **Windows DPAPI**; it is never stored as plain text
- Server certificates are verified, and STARTTLS is required so the password never travels unencrypted
- Clear error messages instead of technical details (wrong password, no connection, timeout…)
- Log out removes the saved account from the computer

**Interface**
- Modern theme (AtlantaFX), Feather icons, colored avatars
- The interface never freezes: all mail operations run on a background thread
- Automatic reconnection when the server closes the connection
- Shortcuts: Ctrl+N new message, Ctrl+R reply, F5 refresh, Delete delete

## Screenshots

| Reply | Account setup |
|:---:|:---:|
| <img src="docs/compose.png" alt="Reply window with quoted message" width="440"> | <img src="docs/account.png" alt="Account setup dialog" width="360"> |

## Installation

1. Download `EmailClient-1.0.0-Setup.exe` from the [Releases](https://github.com/MrcDprm/email-client/releases/latest) page and run it. Java does not need to be installed; it is bundled.
2. On first launch, enter your email address and password.

**Gmail:** Google does not accept your normal password in email apps. Turn on 2-Step Verification, then create a 16-character **app password** at [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) and use it in the app.

> **Outlook / Hotmail** accounts are not supported: Microsoft only allows OAuth2 sign-in for these accounts, and password (app password) sign-in is disabled.

The encrypted account file is stored in `%APPDATA%\EmailClient`. Uninstall from the Windows "Apps" settings.

## Tech Stack

- **Java 21**, **Maven**
- **JavaFX 21**: user interface
- **Jakarta Mail (Eclipse Angus)**: IMAP and SMTP
- **AtlantaFX** (theme), **Ikonli + Feather** (icons)
- **jsoup**: converting HTML emails to plain text
- **JNA**: Windows DPAPI encryption
- **JUnit 5** + **GreenMail**: tests against an in-memory mail server
- **jpackage** + **Inno Setup**: Windows installer

## Project Structure

```
src/main/java/com/mrcdprm/emailclient/
├── mail/        Account model, provider presets, IMAP reader, SMTP sender, HTML-to-text
├── security/    Password encryption with Windows DPAPI
├── settings/    Saving the account to the user folder, environment variables
├── ui/          Main view, account setup, compose window, about, avatars, icons
├── EmailClientApp.java
└── Launcher.java
src/test/java/   JUnit tests (GreenMail)
installer/       Inno Setup script and icon
```

## Building from Source

Requires JDK 21 and Maven.

```
mvn test          # tests
mvn javafx:run    # run the app
```

For development, the account can be read from environment variables instead of the setup dialog (see `.env.example`):

```
$env:EMAIL_ADDRESS = "you@gmail.com"
$env:EMAIL_APP_PASSWORD = "your app password"
mvn javafx:run
```

### Building the installer

Requires [Inno Setup 6](https://jrsoftware.org/isinfo.php).

```
powershell -ExecutionPolicy Bypass -File installer\build.ps1
ISCC installer\EmailClient.iss
```

`build.ps1` builds the app, finds the JDK modules it needs with `jdeps` and uses `jpackage` to create `dist\EmailClient` with a trimmed Java runtime inside. The installer is created in `installer\Output\`.

**When releasing a new version:** update the version in `pom.xml`, `EmailClientApp.VERSION`, `installer/build.ps1` and `installer/EmailClient.iss`, run the tests, run both commands and upload the installer to a new GitHub Release.

## What I Learned

- **How email actually works.** IMAP keeps the mail on the server and every client looks at the same mailbox, which is why reading, deleting and marking in my app show up in Gmail too. SMTP only sends. I learned the difference between a message's changing sequence number and its stable UID, and how the `In-Reply-To` header puts a reply in the same conversation.
- **An email is a tree.** A message is made of nested parts: a plain-text and an HTML version of the same text, plus attachments. I wrote a recursive function that walks this tree to find the text, and I turn HTML into plain text instead of rendering it, so nothing inside an email can run.
- **Keeping the UI responsive.** Network calls take seconds, so I ran them with JavaFX `Task` on a single background thread and brought the results back to the UI thread. I also had to handle the user clicking faster than the server answers, so an old reply never overwrites a newer one.
- **Security details that are off by default.** Jakarta Mail does not check server certificates unless you tell it to, and STARTTLS can silently fall back to plain text. I turned on both checks, encrypted the password with Windows DPAPI through JNA, hid the password from `toString()`, and showed users friendly messages instead of stack traces.
- **Java compared to C#.** Records, `Optional`, lambdas, `switch` expressions and `try`-with-resources felt familiar. Checked exceptions were new: Java makes you declare and handle them.
- **The "Turkish i" problem.** On a Turkish system, `"INBOX".toLowerCase()` becomes `"ınbox"`. This broke provider detection and a folder icon until I used `Locale.ROOT` for technical text and the Turkish locale only for text shown to people.
- **Styling JavaFX with CSS and a theme library.** I used AtlantaFX variables instead of hard-coded colors. I also found that the theme makes toolbar buttons flat, which hid my blue "New message" button once it lost focus, and fixed it with a more specific CSS rule.
- **Testing without a real account.** GreenMail starts a real IMAP/SMTP server in memory, so the tests send, read, reply to and delete messages without ever touching my Gmail.

## Future Plans

- Viewing and saving attachments
- Optional HTML view (with remote content blocked)
- Multiple accounts
- New-mail notifications (IMAP IDLE)
- OAuth2 sign-in for Outlook / Hotmail and Gmail
- Dark theme

## License

[MIT](LICENSE) © 2026 Miraç Deprem
