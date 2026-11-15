# Security policy

## Supported versions

| Version | Supported |
|---|---|
| 0.1.x | Yes |
| < 0.1 | No |

## Reporting a vulnerability

Please **do not open a public issue** for a security vulnerability.

Use GitHub's private vulnerability reporting feature (Security → Advisories →
Report a vulnerability) for this repository. If private reporting is not
available, contact the maintainers privately and include:

- the affected version or commit;
- the impacted screen or component;
- reproduction steps or a proof of concept;
- the potential impact;
- any suggested mitigation.

## Security baseline

- The frontend never stores credentials in `localStorage`; HTTP Basic
  credentials are kept in `sessionStorage` for the current tab only.
- The application does not contain mock data or a demo API mode.
- All bundled people and records are synthetic.
- Production deployments should serve the build behind TLS, with the security
  headers provided in `nginx.conf`.
- The frontend displays candidates, never automatic identifications.
