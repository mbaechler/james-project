pipeline {
  agent {
    docker {
      image 'maven:3.5.2-jdk-8'
      args '-v /root/.m2:/root/.m2'
    }
    
  }
  stages {
    stage('error') {
      steps {
        sh 'mvn -B clean package -DskipTests'
        stash(name: 'build', includes: 'target')
      }
    }
  }
}