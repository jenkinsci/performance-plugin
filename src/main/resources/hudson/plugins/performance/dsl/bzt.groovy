package hudson.plugins.performance.dsl


/*
<code>
bzt test1.yml test2.yml
</code>

*/


def call(params) {
    performanceTest params: params
}
