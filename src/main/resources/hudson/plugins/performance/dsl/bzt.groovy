package hudson.plugins.performance.dsl


/*
<code>
bzt test1.yml test2.yml
</code>

*/


def call(params) {
    performanceTest generatePerformanceTrend: true, params: params, useBztExitCode: true, useSystemSitePackages: true
}
