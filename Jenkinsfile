properties([
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds(abortPrevious: true)
])

node('linux-amd64') {
    stage('Checkout') {
        infra.checkoutSCM()
    }

    stage('Build') {
        withEnv(['PATH+LOCAL=/home/jenkins/.local/bin']) {
            sh 'pip install --upgrade pip'
            sh 'pip install -r requirements.txt'
            
            def args = ['clean', 'install', '-Dset.changelist']
            infra.runMaven(args, 11)
        }
    }

    stage('Archive') {
        junit '**/target/surefire-reports/TEST-*.xml'
        infra.prepareToPublishIncrementals()
    }
}

infra.maybePublishIncrementals()
