pipeline {
  agent any
  environment {
    // assume these are set in Jenkins > Credentials and injected here
    SONAR_HOST_URL = credentials('sonar-url')
    SONAR_AUTH_TOKEN = credentials('sonar-token')
    ZAP_API_KEY     = credentials('zap-api-key')
  }
  options {
    // keep build logs to 30 days
    buildDiscarder(logRotator(daysToKeepStr: '30'))
    timestamps()
  }
  stages {
    stage('Checkout') {
      steps {
        checkout scm
        sh 'git --version'
      }
    }

    stage('SAST: SonarQube') {
      steps {
        // install sonar-scanner if needed
        sh '''
          sonar-scanner \
            -Dsonar.projectKey=${JOB_NAME} \
            -Dsonar.sources=. \
            -Dsonar.host.url=${SONAR_HOST_URL} \
            -Dsonar.login=${SONAR_AUTH_TOKEN}
        '''
      }
      post {
        failure {
          echo 'SonarQube Quality Gate failed.'
          error('Aborting pipeline due to SAST failures')
        }
      }
    }

    stage('DAST: OWASP ZAP') {
      steps {
        // assume your app is running on localhost:8080
        sh '''
          zap-baseline.py \
            -t http://localhost:8080 \
            -r zap_report.html \
            -z "-config api.key=${ZAP_API_KEY}"
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'zap_report.html', fingerprint: true
        }
      }
    }

    stage('Threat Modeling: threatspec') {
      steps {
        // threatspec requires Python; ensure it's installed
        sh '''
          pip install threatspec
          threatspec scan --report threatspec_report.md
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'threatspec_report.md'
        }
      }
    }

    stage('Compliance: Checkov') {
      steps {
        sh '''
          pip install checkov
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
      steps {
        sh '''
          wget https://github.com/zricethezav/gitleaks/releases/latest/download/gitleaks-linux-amd64
          chmod +x gitleaks-linux-amd64
          ./gitleaks-linux-amd64 detect --source . --report-path gitleaks_report.json
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'gitleaks_report.json'
        }
      }
    }

    stage('Dependency Check') {
      steps {
        // download & run OWASP Dependency-Check
        sh '''
          wget https://github.com/jeremylong/DependencyCheck/releases/download/v7.2.0/dependency-check-7.2.0-release.zip
          unzip dependency-check-7.2.0-release.zip
          ./dependency-check/bin/dependency-check.sh \
            --project ${JOB_NAME} \
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
      echo '✅ All security checks passed!'
    }
    unstable {
      echo '⚠️ Security issues detected – please review archived reports.'
    }
    failure {
      echo '❌ Pipeline failed.'
    }
    always {
      // Optionally notify team via email or Slack
      // slackSend channel: '#sec-alerts', message: "${JOB_NAME} - Build ${currentBuild.currentResult}"
    }
  }
}
