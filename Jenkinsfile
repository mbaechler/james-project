pipeline {
  agent {
    docker {
      image 'maven:3.5.2-jdk-8'
      args '-v /root/.m2:/root/.m2'
    }
    
  }
  stages {
    stage('build') {
      steps {
        sh 'mvn -B clean package -DskipTests -am -pl core'
        stash(name: 'build', includes: '**/target/**')
      }
    }
    stage('run some tests') {
      parallel {
        stage('run some tests') {
          steps {
            ws('foo') {
              sh 'mvn -B test -Dtest=MailAddressTest -DfailIfNoTests=false '
              stash(name: 'testResults', allowEmpty: true, includes: '**/surefire-reports/*.xml')
          }
        }
        stage('run some other tests') {
          steps {
            ws('bar') {
              sh 'mvn -B test -Dtest=UserTest -DfailIfNoTests=false'
            }
          }
        }
      }
    }
    stage('junit') {
      steps {
        unstash 'testResults'
        junit(testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true)
      }
    }
  }
}