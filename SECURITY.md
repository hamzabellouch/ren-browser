# Security Policy

## Supported Versions

The following versions of **Ren Browser** are actively maintained with security patches, privacy enhancements, and stability updates.

| Version          | Supported          |
| ---------------- | ------------------ |
| >= 0.0.1-beta | :white_check_mark: |
| < 0.0.1-beta  | :x:                |

---

## Reporting a Vulnerability

The Ren Browser team takes security and user privacy very seriously. If you discover a security vulnerability, privacy leak, or potential exploit, we encourage you to report it responsibly.

### Before Reporting
Please make sure that:
- The issue is reproducible
- You are using the latest supported version of Links
- The issue is not caused by third-party modifications, custom ROMs, or unsupported Android environments

### How to Report
You can report vulnerabilities through:
- GitHub Issues (for non-sensitive reports and general bug reports)
- E-mail: hamzabellouchcontact@gmail.com / [Froms](https://docs.google.com/forms/d/e/1FAIpQLSeknTjoOutJwH55Rgg1PSpRt4fO6nZqRmyNqLbIUPQj1xeqkA/viewform?usp=header) 

### Information to Include
When submitting a report, please provide:
1. **Device & OS Information:** Device model and Android OS version.
2. **Ren Browser Version:** App version and build number.
3. **Vulnerability Description:** Detailed explanation of the vulnerability and its potential impact.
4. **Reproduction Steps:** Step-by-step instructions, Proof of Concept (PoC) URL or script, and expected vs. observed behavior.
5. **Logs / Screenshots:** Relevant Logcat output or crash traces (ensure no sensitive personal info is included).

---

## Security & Privacy Architecture

Ren Browser implements multi-layered security and privacy controls:

- **Tor Onion Routing:** Secure SOCKS5 / proxy routing via native Tor binaries to anonymize web traffic.
- **Ad & Tracker Interception:** Real-time request filtering preventing known malware, tracker, and phishing domains from resolving.
- **WebRTC Protection:** Enforces strict ICE candidate policies to avoid leaking local and public IP addresses.
- **Anti-Fingerprinting:** Standardizes device canvas, audio, and browser metadata to reduce identifiable digital fingerprints.
- **Site Sandbox Isolation:** Segregates cookies, Web SQL, IndexedDB, and cache per website domain.
- **Zero Telemetry:** No analytics SDKs, third-party trackers, or crash-reporting daemons run in the background.

---

## Response & Disclosure Policy

1. **Acknowledgment:** We will acknowledge receipt of your vulnerability report within 48–72 hours.
2. **Investigation & Patching:** If confirmed, an emergency fix or scheduled security patch will be developed and verified.
3. **Coordinated Disclosure:** We kindly request that you refrain from disclosing the vulnerability publicly until a fix has been released.
4. **Attribution:** We will gladly credit security researchers in release notes and changelogs if desired.
