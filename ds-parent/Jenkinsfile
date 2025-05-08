pipeline {
    agent { label 'DS agent' }

    stages {
        stage('Build') {
            steps {
                script {
                    // Checkout the source code from the repository
                    checkout scm
                }
                // Execute Maven build
                sh 'mvn clean install'
            }
        }
    }

    post {
        success {
            echo 'Build completed successfully.'
        }
        failure {
            echo 'Build failed.'
        }
    }
}
