@Library('piper-lib-os') _

node() {

  stage('prepare') {
    checkout scm
  } 

  stage('build') {
    sh 'mvn clean install findbugs:findbugs checkstyle:checkstyle'
  }

  stage('publish') {
    checksPublishResults script: this, findbugs: true, checkstyle: true
  }
}
