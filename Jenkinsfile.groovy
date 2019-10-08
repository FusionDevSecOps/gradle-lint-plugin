pipeline {
    agent {
        node {
            label 'master'
        }
    }
    // options {
    //     skipDefaultCheckout true
    //     timeout(time: 3, unit: 'HOURS')
    //     timestamps()
    //     buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
    // }
    stages {
        stage('Build') {
            steps {
                script {
                    try {
                    sh './gradlew --continue clean build '
                    }catch (Exception e) {
                    sh 'echo "${e}"'
                    }
                }
            }
        }
        stage('Test') {
            steps {
                script {
                    try {
                     // sh 'sudo ./gradlew test --scan'
                    // sh 'sudo ./gradlew test --ignoreFailures=true'
                    sh './gradlew test --continue'


                    }catch (Exception e) {
                    sh 'echo "${e}"'
                    }

                }
            }
        }
        stage('Code Coverage') {
            steps {
                script {
                    sh './gradlew jacocoTestReport'

                }
            }
        }
        stage('Static code analysis') {
            steps {
                script {
                    sh label: '', script: '''find . -name *.java > javaFiles.txt
                        sed -ne \'s/$/, &/p\' javaFiles.txt > javaFilesClean.txt
                        cat javaFilesClean.txt | tr -d " \\t\\n\\r" > JavaWhiteSpace.text
                       /home/administrator/pmd-bin-6.17.0/bin/run.sh pmd -filelist JavaWhiteSpace.text -f xml -cache cache.xml -R rulesets/java/quickstart.xml -reportfile build/pmd.xml -failOnViolation false
                        '''

                    sh label: '', script: '/home/administrator/pmd-bin-6.17.0/bin/run.sh cpd --minimum-tokens 10 --filelist JavaWhiteSpace.text --format xml --language java --failOnViolation false > build/cpd.xml'


                }
            }
        }
        stage('SonarQube') {
            steps {
                script {
                    try {
                        sh './gradlew sonarqube -Dsonar.host.url=http://192.168.50.36:9000 -Dsonar.login=admin -Dsonar.password=admin'
                    }catch (Exception e) {
                    sh 'echo "${e}"'
                    }
                }
            }
        }
        stage('Archival') {
            steps {
                publishHTML (target: [
                             allowMissing: true,
                             alwaysLinkToLastBuild: false,
                             keepAll: true,
                             reportDir: 'build/jacocoHtml',
                             reportFiles: 'index.html',
                             reportName: "Coverage Report"])
                publishHTML (target: [
                             allowMissing: true,
                             alwaysLinkToLastBuild: false,
                             keepAll: true,
                             reportDir: 'build/reports/tests/test',
                             reportFiles: 'index.html',
                             reportName: "Test Report"])

            }
        }

    }
       post {
        always {
            recordIssues enabledForFailure: true, tools: [
                    cpd(pattern: 'build/cpd.xml'),
                    pmdParser(pattern: 'build/pmd.xml')]

        }

    }
}
