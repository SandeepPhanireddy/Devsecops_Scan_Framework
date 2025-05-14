pipeline {
  agent any

  environment {
    SONAR_HOST_URL  = credentials('sonar-url')
    SONAR_AUTH_TOKEN= credentials('sonar-token')
    ZAP_API_KEY     = credentials('zap-api-key')
    RECIPIENTS      = credentials('RECIPIENTS')  // your email address(es)
  }

  options {
    timestamps()
    buildDiscarder(logRotator(daysToKeepStr: '30'))
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('SAST: SonarQube') {
      agent {
        docker {
          image 'sonarsource/sonar-scanner-cli:latest'
          args  '-u root:root'
        }
      }
      steps {
        sh """
          sonar-scanner \
            -Dsonar.projectKey=${JOB_NAME} \
            -Dsonar.sources=. \
            -Dsonar.host.url=${SONAR_HOST_URL} \
            -Dsonar.login=${SONAR_AUTH_TOKEN}
        """
      }
      post {
        failure {
          echo '❌ SonarQube Quality Gate failed.'
          error('Stopping pipeline due to SAST failure')
        }
        always {
          // Sonar publishes results to its server; nothing local to archive
        }
      }
    }

    stage('DAST: OWASP ZAP') {
      agent {
        docker {
          image 'owasp/zap2docker-stable'
        }
      }
      steps {
        // assumes your app is up at localhost:8080 (use Docker Compose or spin up before)
        sh """
          zap-baseline.py \
            -t http://host.docker.internal:8080 \
            -r zap_report.html \
            -z "-config api.key=${ZAP_API_KEY}"
        """
      }
      post {
        always {
          archiveArtifacts artifacts: 'zap_report.html', fingerprint: true
        }
      }
    }

    stage('Threat Modeling: threatspec') {
      agent {
        docker {
          image 'python:3.12-slim'
        }
      }
      steps {
        sh '''
          pip install --no-cache-dir threatspec
          threatspec scan --report-file threatspec_report.md
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'threatspec_report.md'
        }
      }
    }

    stage('Compliance: Checkov') {
      agent {
        docker {
          image 'bridgecrew/checkov:latest'
        }
      }
      steps {
        sh '''
          checkov -d . --output html --output-file checkov_report.html
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'checkov_report.html'
        }
      }
    }

    stage('Secrets Detection: Gitleaks') {
      agent {
        docker {
          image 'zricethezav/gitleaks:latest'
        }
      }
      steps {
        sh '''
          gitleaks detect --source . --report-path gitleaks_report.json
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'gitleaks_report.json'
        }
      }
    }

    stage('Dependency Check') {
      agent {
        docker {
          image 'owasp/dependency-check:latest'
        }
      }
      steps {
        sh '''
          dependency-check.sh \
            --project "${JOB_NAME}" \
            --scan . \
            --format HTML \
            --out dependency-check-report.html
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'dependency-check-report.html'
        }
      }
    }
  }

  post {
    success {
      mail to: "${RECIPIENTS}",
           subject: "✅ [${JOB_NAME} #${BUILD_NUMBER}] All AppSec Checks Passed",
           body: """
           Good news – all AppSec scans completed successfully!

           • Build: ${env.BUILD_URL}
           • Status: SUCCESS

           Reports are archived in the build artifacts.
           """
    }
    unstable {
      mail to: "${RECIPIENTS}",
           subject: "⚠️ [${JOB_NAME} #${BUILD_NUMBER}] AppSec Issues Detected",
           body: """
           Some security checks flagged issues:

           • Build: ${env.BUILD_URL}
           • Status: UNSTABLE

           Please review the archived reports for details.
           """
    }
    failure {
      mail to: "${RECIPIENTS}",
           subject: "❌ [${JOB_NAME} #${BUILD_NUMBER}] AppSec Pipeline Failed",
           body: """
           The AppSec pipeline encountered a failure:

           • Build: ${env.BUILD_URL}
           • Status: FAILURE

           Check console output and reports to troubleshoot.
           """
    }
    always {
      echo "Notifications sent to ${RECIPIENTS}"
    }
  }
}
