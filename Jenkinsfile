pipeline {
  agent {
    docker {
      image 'maven:3.5.2-jdk-8'
    }
    
  }
  stages {
    stage('') {
      steps {
        sh 'mvn -B clean package -DskipTests'
        stash(name: 'build', includes: 'target')
      }
    }
  }
}