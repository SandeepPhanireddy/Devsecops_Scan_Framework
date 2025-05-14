pipeline {
    agent any

    environment {
        SEMGREP_RULES = 'p/ci' // or use custom rules repo
        PYTM_MODEL = 'threat_model.py'
    }

    stages {

        stage('Checkout') {
            steps {
                git url: 'https://your.repo.url/app.git', branch: 'main'
            }
        }

        stage('SAST - Semgrep') {
            steps {
                sh '''
                    pip install semgrep
                    semgrep scan --config=${SEMGREP_RULES} --json > semgrep-report.json || true
                '''
                archiveArtifacts artifacts: 'semgrep-report.json'
            }
        }

        stage('Threat Modeling - PyTM') {
            steps {
                sh '''
                    pip install pytm
                    python ${PYTM_MODEL} > threat_model_output.txt || true
                '''
                archiveArtifacts artifacts: 'threat_model_output.txt'
            }
        }

        stage('Build') {
            steps {
                sh './build.sh' // Or Docker build, Maven, etc.
            }
        }

        stage('Compliance Scan - OpenSCAP') {
            steps {
                sh '''
                    sudo apt-get install -y openscap-scanner
                    oscap xccdf eval --profile xccdf_org.ssgproject.content_profile_cis \
                        --results results.xml --report report.html /usr/share/xml/scap/ssg/content/ssg-ubuntu.xml || true
                '''
                archiveArtifacts artifacts: 'report.html'
            }
        }

        stage('DAST - OWASP ZAP') {
            steps {
                sh '''
                    docker run -t owasp/zap2docker-stable zap-baseline.py \
                        -t http://your-app-url.com -r zap-report.html || true
                '''
                archiveArtifacts artifacts: 'zap-report.html'
            }
        }

        stage('SBOM + CVE Check - Syft + Grype') {
            steps {
                sh '''
                    curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sh -s -- -b . v0.93.0
                    ./syft dir:. -o json > sbom.json
                    curl -sSfL https://raw.githubusercontent.com/anchore/grype/main/install.sh | sh -s -- -b . v0.72.0
                    ./grype sbom:sbom.json -o table > cve-report.txt || true
                '''
                archiveArtifacts artifacts: 'cve-report.txt'
            }
        }

        stage('Post & Publish Reports') {
            steps {
                publishHTML([
                    reportName : 'Semgrep Report',
                    reportDir  : '.',
                    reportFiles: 'semgrep-report.json',
                    keepAll    : true
                ])
                publishHTML([
                    reportName : 'ZAP DAST Report',
                    reportDir  : '.',
                    reportFiles: 'zap-report.html',
                    keepAll    : true
                ])
                publishHTML([
                    reportName : 'Compliance Report',
                    reportDir  : '.',
                    reportFiles: 'report.html',
                    keepAll    : true
                ])
            }
        }
    }

    post {
        always {
            junit 'test-results.xml' // Optional: if running unit tests
            cleanWs()
        }
    }
}
