# Security Policy

Thank you for your interest in the security of **Re4DE** – the reference system
for data exchange in the energy sector (*Referenzsystem für den Datenaustausch
im Energiesektor*).

## Project Context and Expectations

Re4DE is a **research-driven software project** developed by the Future Energy
Lab of the German Energy Agency (dena) on behalf of the Federal Ministry for
Digitalisation and State Modernisation (BMDS), with development support from
the Fraunhofer Institute for Energy Economics and Energy System Technology
(Fraunhofer IEE).

The repositories are publicly available, but Re4DE is **not operated as a
continuously managed open-source community**. The software is provided
primarily for research and prototyping purposes. There is:

- **No guarantee of production readiness**
- **No guarantee of long-term maintenance**
- **No guarantee of backward compatibility**
- **No guaranteed response or fix times for security reports**

Please take this into account when using, deploying, or building upon these
components.

## Supported Versions

The project does not maintain a stable release line with long-term support.
Security reviews are performed **selectively and at the maintainers'
discretion**.

| Version                  | Supported                           |
|--------------------------|-------------------------------------|
| latest `main` branch     | Selectively, on a best-effort basis |
| older commits / releases | Not actively supported              |

If you intend to use Re4DE components in production or safety-relevant
environments, you are responsible for conducting your own security assessment
and hardening.

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues,
discussions, or pull requests.**

Instead, please report potential vulnerabilities **privately** through 
**GitHub Private Vulnerability Reporting**.
Open the *"Report a vulnerability"* option in the *Security* tab of the
affected repository.

When reporting, please include (where applicable):

- The commit hash or version affected
- A clear description of the issue and its potential impact
- Steps to reproduce or a proof of concept
- Any suggested mitigation

### What to Expect

| Stage                     | Expectation                                                |
|---------------------------|------------------------------------------------------------|
| Acknowledgement of report | Best-effort; no guaranteed response time                   |
| Assessment / triage       | Selective, at maintainers' discretion                      |
| Fix and disclosure        | No guarantee of a fix, timeline, or coordinated disclosure |

Given the research-oriented nature of the project, reports may be reviewed
selectively. Fixes may – or may not – be provided, at the discretion of the
maintainers.

## Responsible Disclosure

We ask researchers to:

- Give maintainers a reasonable amount of time to assess the report before
  any public disclosure.
- Avoid actions that could harm users, systems, or data (no intrusive
  scanning, exploitation, or data exfiltration on third-party deployments).
- Interact only with systems you own or are explicitly authorized to test.

## Scope

The security policy applies to all public repositories of the Re4DE
organization. It **does not** cover:

- Third-party deployments or hosted instances operated by other parties
- Forks of the repositories
- Vulnerabilities in third-party dependencies, unless they are directly
  exploitable through the Re4DE codebase
  (such findings should also be reported upstream)

## Secure Usage Recommendations

Since Re4DE components deal with identity (German Smart Meter PKI, SSI) and
dataspace connectivity:

- Always deploy **Identity Hub, connector, and issuer together and fully
  configured** – do not disable or bypass authentication/authorization
  mechanisms.
- Keep credentials, private keys, and Smart Meter PKI material **secure and
  separate** from repository contents.
- Review Helm charts and deployment configurations before use in any real
  environment.
- Regularly update dependencies; the maintainers do not guarantee up-to-date
  dependency baselines.

## Licensing Note

By contributing security fixes to these repositories, you agree that your
contributions will be licensed under the same license as the respective
repository (Apache-2.0), in line with the project's
[CONTRIBUTING.md](./CONTRIBUTING.md).

---

For general questions about Re4DE and dataspace solutions in the German energy
sector, please contact the Future Energy Lab: `futureenergylab@dena.de`.