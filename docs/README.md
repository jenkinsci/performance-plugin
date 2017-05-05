# Performance Plugin for Jenkins 

## About

Performance Plugin allows you to [run performance tests](RunTests.md) as build step of your Jenkins job, or [build reports](Reporting.md) from pre-existing test result files. 

Here's how example trend report looks like:
![](report_seclevel.png)

## Running Tests

Running tests is done with Taurus Tool and explained in detail on dedicated **[Test Running](RunTests.md)** doc page.


## Building Reports

Report building supports many formats from popular testing tools and explained in detail on dedicated **[Reporting](Reporting.md)** doc page.


## Links

- [Test Running](RunTests.md)
- [Reporting](Reporting.md)
- [Changelog](Changelog.md)
- [Usage Stats](stats.html)
- [Jenkins Plugins Entry](https://wiki.jenkins-ci.org/display/JENKINS/Performance+Plugin)


## Troubleshooting



If you get the error `java.lang.NoClassDefFoundError: Could not initialize class org.jfree.chart.JFreeChart` when the plugin generates the charts, is because you have running an XServer in the jenkins machine. Set the property `-Djava.awt.headless=true` when starting your servlet container. Note that this normally does not happen when running the embedded servlet container Jenkins is packaged with (Jetty).

https://groups.google.com/forum/#!topic/jenkinsci-users/o_Dr7Tn0i3U

## Compiling
To use the latest plugin release, you need to download, compile and install by hand. To do it, you need git, maven and java installed in your computer.
```bash
$ git clone https://github.com/jenkinsci/performance-plugin.git performance
$ cd performance
$ mvn package
$ cp target/performance.hpi <path_to_jenkins>/data/plugins
```
Remember to restart jenkins in order to use reload the plugin.
You could read more about plugins reading these pages :
- http://wiki.jenkins-ci.org/display/JENKINS/Checking+out+existing+plugins
- http://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial
- http://wiki.jenkins-ci.org/display/JENKINS/Hosting+Plugins