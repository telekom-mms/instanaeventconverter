#!/usr/bin/env groovy

pipeline {
    agent { label 'docker-build' }

    stages {
        stage('build library') {
            steps {
                script {
                    buildJavaProject()
                }
            }
        }
    }
    post {
        always {
            node('master') {
                step([$class: 'Mailer', notifyEveryUnstableBuild: true, sendToIndividuals: false, recipients:
                        'kay.koedel@telekom.de'
                ])
            }
        }
    }
}
