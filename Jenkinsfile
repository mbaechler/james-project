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
        sh 'mvn -B clean package -DskipTests -am -pl backends-common/cassandra'
        stash(name: 'build', includes: '**/target/**')
      }
    }
    stage('run tests') {
      parallel {
        stage('run unit tests') {
          steps {
            node(label: '') {
              checkout scm
              sh 'mvn -B -am -pl backends-common/cassandra -Dtest=CassandraSchemaVersionDAOTest -DfailIfNoTests=false test'
              stash(name: 'testResults', includes: '**/surefire-reports/*.xml')
            }
          }
        }
        stage('build jpa-guice docker image') {
          steps {
            node(label: '') {
              checkout scm
              sh 'mvn -B -T4 package -DskipTests -am -pl server/container/guice/jpa-guice'
              archiveArtifacts(artifacts: 'server/container/guice/jpa-guice/target/james-server-jpa-guice.lib/**', fingerprint: true)
              archiveArtifacts(artifacts: 'server/container/guice/jpa-guice/target/james-server-jpa-guice.jar', fingerprint: true)
            }
          }
        }
      }
    }
    stage('create docker image') {
      agent { 
        dockerfile {
          dir 'dockerfiles/docker-client'
          args '-v /root/.m2:/root/.m2'
        }
      }
      steps {
        node(label: '') {
          unarchive(mapping: [
            'server/container/guice/jpa-guice/target/james-server-jpa-guice.jar': 'dockerfiles/run/guice/jpa/destination/james-server-jpa-guice.jar',
            'server/container/guice/jpa-guice/target/james-server-jpa-guice.lib/': 'dockerfiles/run/guice/jpa/destination/'
          ])
          sh 'cd dockerfiles/run/guice/jpa; docker build -t jpa-guice .'
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

