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
        sh 'mvn -B clean package -DskipTests'
        stash(name: 'build', includes: '**/target/**')
      }
    }
    stage('run some tests') {
      parallel {
        stage('run some tests') {
          steps {
            sh 'mvn -B -Dtest=MemoryGetMessagesMethodTest -DfailIfNoTests=false '
          }
        }
        stage('run some other tests') {
          steps {
            sh 'mvn -B test -Dtest=MemorySetMessagesMethodCucumberTest -DfailIfNoTests=false'
          }
        }
      }
    }
  }
}