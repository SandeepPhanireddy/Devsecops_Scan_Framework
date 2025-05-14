pipeline {
  agent any

  environment {
    // Email recipient(s) for notifications
    RECIPIENTS = credentials('EMAIL_RECIPIENTS')
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

    stage('STRIDE Threat Modeling') {
      agent {
        docker {
          image 'owasp/threat-dragon:stable'
        }
      }
      steps {
        // Export threats as text and PDF
        sh '''
          OWASP-Threat-Dragon --print threat-model-stride.json > stride_report.txt
          OWASP-Threat-Dragon --pdf threat-model-stride.json --verbose
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'stride_report.txt, threat-model-stride.pdf', fingerprint: true
        }
      }
    }

    stage('PASTA Threat Modeling') {
      agent {
        docker {
          image 'python:3.12-slim'
        }
      }
      steps {
        // Install PyTM and Graphviz, then run your PASTA model script
        sh '''
          pip install --no-cache-dir pytm graphviz
          python pasta_model.py --output pasta_report.html
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'pasta_report.html', fingerprint: true
        }
      }
    }
  }

  post {
    success {
      mail to: "${RECIPIENTS}",
           subject: " [${JOB_NAME} #${BUILD_NUMBER}] Threat Models Generated",
           body: """
           STRIDE and PASTA threat modeling completed successfully!

           • Build: ${env.BUILD_URL}
           • STRIDE reports: stride_report.txt, threat-model-stride.pdf
           • PASTA report: pasta_report.html

           Artifacts are archived for review.
           """
    }
    unstable {
      mail to: "${RECIPIENTS}",
           subject: " [${JOB_NAME} #${BUILD_NUMBER}] Threat Modeling Warnings",
           body: """
           One or more threat-model scans flagged issues:

           • Build: ${env.BUILD_URL}

           Please check the archived STRIDE and PASTA reports.
           """
    }
    failure {
      mail to: "${RECIPIENTS}",
           subject: " [${JOB_NAME} #${BUILD_NUMBER}] Threat Modeling Pipeline Failed",
           body: """
           Threat modeling pipeline encountered a failure.

           • Build: ${env.BUILD_URL}

           Inspect console output and any generated reports to troubleshoot.
           """
    }
    always {
      echo "Notifications sent to ${RECIPIENTS}"
    }
  }
}
