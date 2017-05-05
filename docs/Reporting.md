# Performance Trend Reporting

## Features

## Jenkins GUI Configuration

Specify the report files, separated by semicolon. Plugin will autodetect file format for each file. You can use Jenkins globs like `**/*.jtl`.

You can configure the error percentage thresholds which would make the project unstable or failed or leave them blank to disable the feature.

## Using from Pipeline Scripts

You can use Pipleline Script builder to build groovy script piece from GUI. Additinally, Performance Plugin offers "perfReport" Groovy command that allows shorthand use to simply build report, it will autodetect source file format:
 
```groovy
perfReport 'result.csv'
```

Minimal command for old-style invocation is this:

```groovy
performanceReport parsers: [[$class: 'JMeterParser', glob: 'result.xml']], relativeFailedThresholdNegative: 1.2, relativeFailedThresholdPositive: 1.89, relativeUnstableThresholdNegative: 1.8, relativeUnstableThresholdPositive: 1.5
```
