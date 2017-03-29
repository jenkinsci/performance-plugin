package hudson.plugins.performance.dsl


/*
<code>
taurusReport 'report_file.xml'
</code>

*/


def call(fileName) {
    performanceReport parsers: [[$class: 'TaurusParser', glob: fileName]],
            relativeFailedThresholdNegative: 0.0, relativeFailedThresholdPositive: 0.0,
            relativeUnstableThresholdNegative: 0.0, relativeUnstableThresholdPositive: 0.0
}

