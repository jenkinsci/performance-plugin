package hudson.plugins.performance.dsl


/*
<code>
taurusReport 'report_file.xml'
</code>

*/


// The call(body) method in any file in workflowLibs.git/vars is exposed as a
// method with the same name as the file.
def call(fileName) {
    performanceReport parsers: [[$class: 'TaurusParser', glob: fileName]],
            relativeFailedThresholdNegative: 0.0, relativeFailedThresholdPositive: 0.0,
            relativeUnstableThresholdNegative: 0.0, relativeUnstableThresholdPositive: 0.0
}

