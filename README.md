# CI/CD Security Pipelines

This repository contains a suite of Jenkins pipelines (written in Groovy) to automate end-to-end security testing across your application lifecycle:

- **Jenkinsfile-basic**: Static (SAST), Dynamic (DAST), Threat Modeling, Compliance, Secrets & Dependency checks  
- **Jenkinsfile-docker-email**: Containerized SAST/DAST/Threat Modeling/Compliance/Secrets/Dependency checks + email notifications  
- **Jenkinsfile-container-scan**: Builds your app image + Trivy container‐scanning + all above checks + notifications  
- **Jenkinsfile-owasp-tools**: OWASP SpotBugs (SAST), ZAP Baseline (DAST), AppSensor (IAST) + email notifications  
- **Jenkinsfile-guardrails-garak**: NeMo Guardrails CLI & Garak LLM/vulnerability scans + email notifications  
- **Jenkinsfile-threat-modeling**: STRIDE (Threat Dragon) & PASTA (PyTM) threat-modeling stages + email notifications  

---

## Prerequisites

1. **Jenkins**  
   - Version 2.300+ with Docker, Pipeline, and Email-Extension plugins installed.  
2. **Docker**  
   - Installed on the Jenkins agent(s) for containerized stages.  
3. **Credential Store**  
   - Create the following credentials in Jenkins > **Credentials**:

   | ID                         | Type           | Value                                      |
   |----------------------------|----------------|--------------------------------------------|
   | `sonar-url`                | Secret text    | SonarQube server URL                       |
   | `sonar-token`              | Secret text    | SonarQube authentication token             |
   | `zap-api-key`              | Secret text    | OWASP ZAP API key                          |
   | `RECIPIENTS`               | Secret text    | Comma-separated email addresses            |
   | `guardrails-cli-token`     | Secret text    | NeMo Guardrails CLI token                  |
   | `openai-api-key`           | Secret text    | OpenAI API key (for Garak LLM scans)       |
   | `EMAIL_RECIPIENTS`         | Secret text    | Email list (for threat-modeling pipelines) |

---

## File Descriptions

### 1. Jenkinsfile-basic

- **Purpose**: Runs SAST (SonarQube), DAST (ZAP), Threat Modeling (`threatspec`), Compliance (Checkov), Secrets (Gitleaks), Dependency (OWASP Dependency-Check).  
- **Key steps**:
  1. Checkout  
  2. SonarQube scan + Quality Gate  
  3. ZAP baseline scan  
  4. ThreatSpec modeling  
  5. Checkov IaC scan  
  6. Gitleaks secrets detection  
  7. Dependency-Check report  
- **Artifacts**: Reports in HTML/XML/JSON, archived under “Build Artifacts”.

### 2. Jenkinsfile-docker-email

- **Purpose**: Containerizes each security tool in its official Docker image, archives reports, and emails results.  
- **Adds**:
  - Docker agents per stage  
  - `mail` steps on `success`, `unstable`, and `failure`  

### 3. Jenkinsfile-container-scan

- **Purpose**: Builds your app Docker image, scans it with Trivy (HIGH/CRITICAL), then runs all SAST/DAST/etc.  
- **Adds**:
  - **Build Docker image** stage  
  - **Trivy** container scanning stage  

### 4. Jenkinsfile-owasp-tools

- **Purpose**: Demonstrates pure-OWASP tooling:  
  - **SpotBugs** for SAST  
  - **ZAP** for DAST  
  - **AppSensor** for IAST  
- **Workflow**:
  1. Maven build & package  
  2. SpotBugs analysis  
  3. ZAP baseline scan  
  4. AppSensor-instrumented run + integration tests  

### 5. Jenkinsfile-guardrails-garak

- **Purpose**: Scans your code & NeMo Guardrails configs with Guardrails CLI, and probes LLMs with Garak.  
- **Workflow**:
  1. Guardrails CLI JSON report  
  2. Garak “guardrails” config scan  
  3. Garak LLM vulnerability scan  

### 6. Jenkinsfile-threat-modeling

- **Purpose**: Automates two threat-modeling frameworks:  
  - **STRIDE** via OWASP Threat Dragon CLI  
  - **PASTA** via PyTM  
- **Artifacts**:  
  - `stride_report.txt` & PDF  
  - `pasta_report.html`  

---

## Usage

1. **Clone** this repo into your Jenkins SCM project.  
2. **Select** the appropriate `Jenkinsfile-…` in your multibranch or pipeline job.  
3. **Configure** credentials IDs in Jenkins global settings.  
4. **Run** the job. Reports will be archived under **Build Artifacts** and notifications sent via email.

---

## Customization

- **Parallel Stages**  
  Group independent scans (Checkov, Gitleaks, ThreatSpec) under `parallel {}` blocks to speed up.  
- **Thresholds & Failures**  
  Adjust `--exit-code` or fail conditions per stage (e.g. treat only CRITICAL findings as build-breaking).  
- **Container Images**  
  Pin specific versions (e.g. `owasp/zap2docker-stable:2.13.0`) for consistency.  
- **Notifications**  
  Integrate with Slack or Teams using respective Jenkins plugins in place of or alongside email.

 


