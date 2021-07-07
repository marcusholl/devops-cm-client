node() {

  stage('prepare') {
    checkout scm
  } 

  stage('build') {
    sh "mvn clean install findbugs:findbugs"
  }
}
