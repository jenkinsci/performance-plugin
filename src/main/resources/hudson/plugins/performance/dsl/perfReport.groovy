package hudson.plugins.performance.dsl

import hudson.plugins.performance.parsers.ParserDetector

/*
<code>
perfReport 'report_file.xml'
</code>

*/


def call(fileName) {
    String className = ParserDetector.detect(pwd() + '/' + fileName)

    performanceReport parsers: [[$class: className, glob: fileName]],
            relativeFailedThresholdNegative: 0.0, relativeFailedThresholdPositive: 0.0,
            relativeUnstableThresholdNegative: 0.0, relativeUnstableThresholdPositive: 0.0
}

