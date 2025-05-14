pipeline {
    agent any

    environment {
        REPORT_DIR = "security-reports"
    }

    stages {
        stage('Checkout Code') {
            steps {
                git 'https://github.com/example/repo.git'
            }
        }

        stage('SAST Scan') {
            steps {
                script {
                    load 'scripts/sast.groovy'
                }
            }
        }

        stage('DAST Scan') {
            steps {
                script {
                    load 'scripts/dast.groovy'
                }
            }
        }

        stage('Threat Modeling') {
            steps {
                script {
                    load 'scripts/threat_model.groovy'
                }
            }
        }

        stage('Compliance Checks') {
            steps {
                script {
                    load 'scripts/compliance.groovy'
                }
            }
        }

        stage('Secrets Detection') {
            steps {
                script {
                    load 'scripts/secrets.groovy'
                }
            }
        }

        stage('Third-party Dependency Scan') {
            steps {
                script {
                    load 'scripts/dependencies.groovy'
                }
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: "${REPORT_DIR}/**", fingerprint: true
            }
        }
    }
}
