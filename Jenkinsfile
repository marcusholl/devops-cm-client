@Library('piper-lib-os') _

node() {

  stage('prepare') {
    checkout scm
  } 

  stage('build') {
    sh "mvn clean install findbugs:findbugs"
  }

  stage('publish') {
    checksPublishResults script: this, findbugs: true
  }
}
