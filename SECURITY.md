# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.5.x   | :white_check_mark: |
| < 0.5   | :x:                |

## Reporting a Vulnerability

We take the security of Silema · Are You Dead Yet? seriously, especially given its role in elderly health monitoring. If you discover a security vulnerability, please follow these steps:

### Private Reporting (Preferred)

1. **DO NOT** create a public GitHub issue
2. Email us at: [security@silema.app](mailto:security@silema.app) (placeholder - update with real contact)
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

We will respond within **48 hours** and aim to release a patch within **7 days** for critical issues.

### Public Disclosure

After the vulnerability is fixed, we will:
1. Credit the reporter (unless anonymity requested)
2. Publish a security advisory
3. Update this document if needed

## Security Best Practices

### For Users

1. **Data Privacy**: All health data stays on-device by default
2. **Remote Sync**: Only enable if you trust the backend provider
3. **SOS Contacts**: Verify emergency contact numbers regularly
4. **Updates**: Keep the app updated for latest security patches
5. **Permissions**: Review granted permissions periodically

### For Developers

1. **Never hardcode secrets** in source code
2. **Validate all input** from Bluetooth devices
3. **Encrypt sensitive data** if syncing to remote backends
4. **Follow OWASP Mobile Top 10** guidelines
5. **Test thoroughly** before submitting PRs

## Known Security Considerations

### Data Storage
- Health data stored locally in Room database
- No encryption at rest (user device security assumed)
- FHIR exports are plain text (user responsible for secure sharing)

### Network Communication
- Remote sync uses HTTPS (TLS 1.2+)
- No certificate pinning (vulnerable to MITM if CA compromised)
- API keys stored in `remote_config.json` (not encrypted)

### Bluetooth LE
- Standard GATT profiles used (0x180D, 0x1810, 0x1822)
- No pairing encryption enforced
- Device spoofing possible (user should verify device identity)

### Camera PPG
- Camera access required for heart rate measurement
- No video recording or storage
- Flash LED used briefly during measurement

## Security Audit History

| Date       | Auditor          | Scope                  | Result        |
| ---------- | ---------------- | ---------------------- | ------------- |
| 2026-08-29 | Internal         | Core algorithms, i18n  | ✅ Passed     |
| TBD        | Third-party      | Full application       | Pending       |

## Compliance

This app aims to comply with:
- **HIPAA** (Health Insurance Portability and Accountability Act) - Design principles
- **GDPR** (General Data Protection Regulation) - Data minimization, user consent
- **PIPL** (Personal Information Protection Law, China) - Local data storage

**Note**: This is not a certified medical device. Consult legal counsel for compliance requirements in your jurisdiction.

## Bug Bounty

We currently do not offer a bug bounty program, but we greatly appreciate responsible disclosure and will credit contributors in our security advisories.

## Contact

For security-related questions or concerns:
- Email: [security@silema.app](mailto:security@silema.app)
- GitHub Security Advisories: [Create private advisory](https://github.com/Morningstar202604/areyoudeadyet/security/advisories/new)

---

**Remember**: This app is for health management reference only. In medical emergencies, always contact professional healthcare providers immediately.
