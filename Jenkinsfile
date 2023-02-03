properties([
    buildDiscarder(logRotator(numToKeepStr: '10')),
    disableConcurrentBuilds(abortPrevious: true)
])

node('maven-11') {
    stage('Checkout') {
        infra.checkoutSCM()
    }

    stage('Build') {
        timeout(30) {
            def args = ['clean', 'install', '-Dmaven.test.skip=true', '-Dmaven.javadoc.skip=true', '-Dset.changelist']
            infra.runMaven(args, 11)
        }
    }

    stage('Archive') {
        // junit '**/target/surefire-reports/TEST-*.xml'
        infra.prepareToPublishIncrementals()
    }
}

infra.maybePublishIncrementals()