package hudson.plugins.performance.dsl


/*
<code>
perfReport 'report_file.xml,report_csv_file.csv'
</code>

*/


def call(sourceDataFiles) {
    performanceReport sourceDataFiles: sourceDataFiles,
            relativeFailedThresholdNegative: 0.0, relativeFailedThresholdPositive: 0.0,
            relativeUnstableThresholdNegative: 0.0, relativeUnstableThresholdPositive: 0.0,
            errorFailedThreshold: -1,  errorUnstableThreshold: -1
}

