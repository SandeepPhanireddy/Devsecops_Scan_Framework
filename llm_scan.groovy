pipeline {
  agent any

  environment {
    // Credentials in Jenkins > Credentials > (Kind: Secret text)
    HF_TOKEN            = credentials('HF_TOKEN')
    EMAIL_RECIPIENTS    = credentials('EMAIL_RECIPIENTS')
    // Comma-separated list of HF model IDs to scan
    HUGGINGFACE_MODELS  = 'gpt2,mosaicml/mpt-7b-instruct'
    // Optional: limit probes (default runs all)
    GARAK_PROBES        = ''
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

    stage('Download LLMs from Hugging Face') {
      agent {
        docker {
          image 'python:3.12-slim'
          args  '-e HF_TOKEN'
        }
      }
      steps {
        sh '''
          pip install --no-cache-dir huggingface-hub
          python3 - <<EOF
import os
from huggingface_hub import snapshot_download

models = os.environ["HUGGINGFACE_MODELS"].split(",")
for m in models:
    print(f"📥 Downloading model: {m}")
    snapshot_download(
      repo_id=m,
      cache_dir="hf_models",
      token=os.environ["HF_TOKEN"]
    )
EOF
        '''
      }
    }

    stage('Garak Vulnerability Scans') {
      agent {
        docker {
          image 'python:3.12-slim'
          args  '-e HF_TOKEN'
        }
      }
      steps {
        sh '''
          pip install --no-cache-dir garak
        '''
        script {
          // Loop through each model ID
          def models = env.HUGGINGFACE_MODELS.split(',')
          for (m in models) {
            // sanitize filename
            def safeName = m.replaceAll('[^A-Za-z0-9_\\-]','_')
            sh """
              echo "🔎 Scanning ${m} with Garak"
              garak \
                --model_type huggingface \
                --model_name ${m} \
                ${env.GARAK_PROBES ? "--probes ${env.GARAK_PROBES}" : ""} \
                --format json \
                --output garak_${safeName}_report.json
            """
            archiveArtifacts artifacts: "garak_${safeName}_report.json", fingerprint: true
          }
        }
      }
    }
  }

  post {
    success {
      mail to: "${EMAIL_RECIPIENTS}",
           subject: " [${JOB_NAME} #${BUILD_NUMBER}] LLM Scans Passed",
           body: """
           All Garak scans completed successfully!

             • Build: ${env.BUILD_URL}
             • Models: ${HUGGINGFACE_MODELS}

           Reports are archived as build artifacts.
           """
    }
    unstable {
      mail to: "${EMAIL_RECIPIENTS}",
           subject: " [${JOB_NAME} #${BUILD_NUMBER}] LLM Scans Warnings",
           body: """
           Some probes flagged potential issues:

             • Build: ${env.BUILD_URL}
             • Models: ${HUGGINGFACE_MODELS}

           Please review the JSON reports in the artifacts.
           """
    }
    failure {
      mail to: "${EMAIL_RECIPIENTS}",
           subject: " [${JOB_NAME} #${BUILD_NUMBER}] LLM Scan Pipeline Failed",
           body: """
           The pipeline failed before all scans could complete.

             • Build: ${env.BUILD_URL}

           Check the console output and any generated reports.
           """
    }
    always {
      echo "📬 Notifications sent to ${EMAIL_RECIPIENTS}"
    }
  }
}
