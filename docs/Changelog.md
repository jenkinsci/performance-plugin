<small>[<< Back to main page](./)</small>
# Changelog

## v3.17 (17th of June, 2019)
- FIX: Fix ineffective report cache [JENKINS-57997](https://issues.jenkins-ci.org/browse/JENKINS-57997)
- FIX: Remove redundant report loading

## v3.16 (28th of March, 2019)
- ADD: Add ability to select which transactions/samplers are included in report through regular expression. Developed by [Philippe M.](https://github.com/pmouawad) and sponsored by [Ubik Load Pack](https://ubikloadpack.com)
- FIX: Switch from table to entry [JENKINS-55787](https://issues.jenkins-ci.org/browse/JENKINS-55787)
- FIX: java 9 compatibility

## v3.15 (29th of January, 2019)
- ADD: network traffic when parsing JMeter CSV. Developed by [Philippe M.](https://github.com/pmouawad) and sponsored by [Ubik Load Pack](https://ubikloadpack.com) 

## v3.14 (14th of December, 2018)
- FIX: StackOverflowError for jobs with 2k+ builds

## v3.13 (1st of November, 2018)
- FIX: set default values for percentiles
- ADD: baseline build for Performance Trend

## v3.12 (11th of September, 2018)
- FIX: pass envVars from pipeline script to Taurus
- FIX: search report files using Ant pattern
- ADD: JUnit timestamp parsing
- ADD: a safe division where division by 0 can occur

## v3.11 (25th of July, 2018)
- ADD: write results xml file to disk for standard mode output
- FIX: Support [JEP-200](https://jenkins.io/blog/2018/01/13/jep-200/)

## v3.10 (1st of June, 2018)
- FIX: relative thresholds

## v3.9 (1st of June, 2018)
- FIX: parse Taurus tool parameters;
- FIX: hide Trend/URI graphs if report contains only summary info
- FIX: division by zero error

## v3.8 (13th of April, 2018)
- ADD: charts to `Performance report` page
- FIX: relative constraint failure on first build. Contributed by [Till Neunast](https://github.com/tilln)

## v3.7 (skipped)

## v3.6 (14th of March, 2018)
- Support [JEP-200](https://jenkins.io/blog/2018/01/13/jep-200/)
 
## v3.5 (29th of January, 2018)
- FIX: Display Performance Report Per Test Case
- FIX: NullPointerException in ConstraintFactory
- FIX: NumberFormatException in RelativeConstraint
- FIX: RelativeConstraint.tolerance and AbsoluteConstraint.value fields in UI
- ADD: move some options to Advanced
- ADD: choosing display percentiles

## v3.4 (12th of December, 2017)
- FEATURE: add LoadRunner parser. Contributed by [Till Neunast](https://github.com/tilln)
- FEATURE: add option to write a JUnit report file to the job's workspace
- FEATURE: add summary table of failed constraints for Publisher log in expert mode
- FEATURE: add Descriptor Symbols for nicer Constraints BuildStep syntax
- FEATURE: add 90th percentile difference in publisher summary table
- FIX: OutOfMemoryException when parsing huge XML report ([JENKINS-47808](https://issues.jenkins-ci.org/browse/JENKINS-47808)). Contributed by [Julien Coste](https://github.com/jcoste-orange)
- FIX: size column width logging of relative comparision results
- FIX: publisher JUnit output to Slave workspace

## v3.3 (21th of August, 2017)
- FEATURE: install 'bzt' from URL or path
- FEATURE: add option to exclude response time of errored samples ([JENKINS-45288](https://issues.jenkins-ci.org/browse/JENKINS-45288))
- FIX: summarizer parser for JMeter 3.2 
- FIX: does not present graphs while job running 
- FIX: wrong time values for uri reports 
- FIX: unused `failBuildIfNoResultFile` flag 
- FIX: always enabled `modePerformancePerTestCase` 
- FIX: remove unused `modeRelativeThresholds` flag ([JENKINS-39050](https://issues.jenkins-ci.org/browse/JENKINS-39050))
- FIX: wrong calculation of Average Throughput ([JENKINS-44410](https://issues.jenkins-ci.org/browse/JENKINS-44410))
- FIX: recognize JUnit file format which wrote as single line ([JENKINS-45723](https://issues.jenkins-ci.org/browse/JENKINS-45723))
- FIX: dependency that require code v2.62+
- FIX: show only chart legend ([JENKINS-45539](https://issues.jenkins-ci.org/browse/JENKINS-45539))
- FIX: virtualenv error in job which contains spaces in name
- FIX: publisher for more than one sourceDataFiles ([JENKINS-46046](https://issues.jenkins-ci.org/browse/JENKINS-46046)). Contributed by [Till Neunast](https://github.com/tilln)
- FIX: logging for expert criteria works with pipeline without throwing exceptions. Contributed by [Till Neunast](https://github.com/tilln)

## v3.2 (14th of July, 2017)
- FIX: Absolute path in Publisher ([JENKINS-45119](https://issues.jenkins-ci.org/browse/JENKINS-45119))
- FIX: Comparison to baseline
- FEATURE: Add `Always use virtualenv` option
- FIX: Changing build status with default comparison option
- FIX: Split params for build step
- FIX: Saving RelativeUnstableThresholdNegative. Contributed by [Märt Bakhoff](https://github.com/mbakhoff)
- FIX: add PerformanceProjectAction only to runs, which contain PerformanceBuildAction 

## v3.1 (2nd of June, 2017)
- FEATURE: Snippet Generator generates nice and simple pipeline scripts for Performance Test & Performance Publisher. Contributed by [Andrew Bayer](https://github.com/abayer)
- FEATURE: Add option to choose graphed metric. Contributed by [Märt Bakhoff](https://github.com/mbakhoff)
- FEATURE: Add option to choose bzt version
- FIX: Showing overall report link in pipeline mode

## v3.0

- FEATURE: add build step to run performance test
- FIX: compact table stats to fit into screen
- FIX: fix FileNotFound error
- FIX: use proper CSV reader to read multiline and quoted CSV values
- FIX: Pipeline Snippet Generator shows broken config

## v2.2
- FEATURE: make plugin to autodetect input file formats
- FEATURE: add "perfReport" Groovy command
- FIX: revive broken "constraints" gui, improve GUI display

## v2.1
- FEATURE: add support for Taurus Final Stats XML
- FIX: improve CPU and memory usage
- FIX: datetime format parsing and couple of NPEs

## v2.0
- FEATURE: Make it pipeline compatible 
- FIX: Testcase trend charts display the values for last sample

## v1.15
- FEATURE: Implemented absolute and relative constraints
- FIX: Sort correctly on date values
- FIX: Fix for uri column value not shown as link
- FIX: Use 0 for time when no value is provided in Junit test result
- FIX: Fix for Junit parser not handling errors
- FIX: Fix for wrong index when calculating 90Line
- FIX: Fix number format exception
- FIX: Fix for Jmeter summariser parser not showing correct data
- FIX: Fix for parsing jtl with CSV data

## v1.14
- FEATURE: Add checkbox in job config in order to choose build status when result performance file are not present
- IMPROVEMENT: Make csv fields case insensitive
- IMPROVEMENT: Cleaned up English localization
- IMPROVEMENT: Compatible with flexible publisher
- FIX: Rounding error on aggregated throughput
- FIX: Fix conversion failure with locale using comma as decimal separator

## v1.13
- FEATURE: Added variable checking and fail to filesystem check on files
- FIX: Allow parameter substitution in report files
- FIX: Add class name to URIs
- FIX: Many improvements in performance parsing files and caching results
- FIX: Compare only against last success builds

## v1.10
- FIX: Cache preprocessed JMeter Reports to avoid performance issues.
- FEATURE: Added comparison between builds
- FIX: UI bug always showing same values regardless of what was saved.
- FEATURE: Average response time thresholds per jtl file.
- FIX: Corrected a bug where the 'All URIs' was just displaying the last entry again.
- FEATURE: Added some useful metrics to the summary details table. JMeter sends size of the response which can be useful to calculate bandwidth usage for perf tests.

## v1.9
- FIX: don't use ; as separator in cookie value
- FEATURE: added csv parser
- FEATURE: added response time trend graph for selected build
- FEATURE: builds trends for responce time
- FEATURE: consider the time for each test case in a test suite
- FEATURE: simple cache added
- FEATURE: new response time graphs selected build and uri report
- FEATURE: new graphs for response time trends
- FEATURE: parse JMeter summarizer files

## v1.8
- FIX: parsing results of long running tests
- FIX: differences not shown for old builds
- FEATURE: more information columns in the report map
- CHANGES: use negative values to indicate no threshold (this allows to use 0% thresholds)
- FEATURE: graphs available on the reports
- FEATURE: url parameter (buildCount) to control the number of builds to display
- FEATURE: get a larger image when clicking on a graph

## v1.7
- FIX: Unstable test set final build incorrectly when a previous test failed.
- FIX: JENKINS-9655, didn't parse JUnit reports correctly (patch: Attila-Mihaly)

## v1.6
- Fix JMeter parser when nested xml tags are in the report.

## v1.5
- Now computes median and 90% Line in jmeter test results.

## v1.4
- Just a control version published after migrating the plugin to gitHub infrastructure.

## v1.3
- Formalized an extension point to define custom parsers so as it should be easier add new parsers.
- JMeter and JUnit parsers have been split in different classes.
- Added a new Trend report.
- Fixed an NPE when a build was failed (JENKINS-5224, JENKINS-6908)

## v1.2
- Support for Ant-FileSet pattern to search  report files.
- Improved css.
- Localized UI elements
- Added Spanish translation

## v1.0
- First release, moved code from JMeter plugin  v.0.3.0 to Performance v.1.0.
- Added ability to parse junit xml report files.
