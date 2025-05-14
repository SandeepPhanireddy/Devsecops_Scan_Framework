pipeline {
  agent any

  environment {
    // Comma-separated email list stored in Jenkins Credentials (Secret text)
    RECIPIENTS = credentials('EMAIL_RECIPIENTS')
  }

  options {
    timestamps()
    // keep only the last 30 builds
    buildDiscarder(logRotator(daysToKeepStr: '30'))
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Run Security Scans') {
      parallel {
        stage('SAST Scan') {
          steps {
            script {
              
              def sast = load 'sast_scan.groovy'
              sast.run()
            }
          }
        }
        stage('LLM Scan') {
          steps {
            script {
              def dast = load 'llm_scan.groovy'
              dast.run()
            }
          }
        }
    stage('Run Security Scans') {
      parallel {
        stage('threatmodeling Scan') {
          steps {
            script {
              
              def sast = load 'threatmodeling_scan.groovy'
              sast.run()
            }
          }
        }
      }
    }
  }

  post {
    success {
      mail to: "${RECIPIENTS}",
           subject: " [${JOB_NAME} #${BUILD_NUMBER}] All Scans Completed",
           body: """
All security scans ran successfully!

Build: ${env.BUILD_URL}
"""
    }
    unstable {
      mail to: "${RECIPIENTS}",
           subject: " [${JOB_NAME} #${BUILD_NUMBER}] Some Scans Reported Warnings",
           body: """
One or more scans reported issues.  
Build: ${env.BUILD_URL}

Please review the archived scan reports.
"""
    }
    failure {
      mail to: "${RECIPIENTS}",
           subject: " [${JOB_NAME} #${BUILD_NUMBER}] Pipeline Failed",
           body: """
The pipeline failed before completion.  
Build: ${env.BUILD_URL}

Check the console output and scan reports to troubleshoot.
"""
