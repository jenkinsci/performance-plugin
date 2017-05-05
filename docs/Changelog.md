<small>[<< Back to main page](./)</small>
# Changelog 

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