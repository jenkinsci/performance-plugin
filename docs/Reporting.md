<small>[<< Back to main page](./)</small>
# Performance Trend Reporting

## Features
Performance Report post-build step allows you to generate reports from popular testing tools. Jenkins will generate graphic charts with the trend report of performance and robustness.

It includes the feature of setting the final build status as good, unstable or failed, based on the reported error percentage.

Report formats supported:
- Taurus Tool's [Final Stats XML](http://gettaurus.org/docs/Reporting/?utm_source=jenkins&utm_medium=link&utm_campaign=wiki#BlazeMeter-Reporter) - through it, you can publish summaries from JMeter, Gatling, Grinder, Siege, ab, Selenium and many others
- [Apache JMeter](https://jmeter.apache.org/) CSV and XML format, also its Summarizer log output
- [JUnit](https://junit.org/junit5/) format (used by SoapUI for example)
- Twitter's [Iago](https://github.com/twitter/iago)
- [wrk](https://github.com/wg/wrk)
- [HPE LoadRunner](https://www.microfocus.com/en-us/products/loadrunner-load-testing/overview)

## Browsing Reports

As soon as you have configured Jenkins to run this post-build action and launched a first build, you'll notice that a new entry is appearing in the left pane: Performance Trend.

If you just have one report file, the graph of this reports will appear on the main page.

If you have more than one report file, you have to click on "Performance Trend" and the graphs will appear.

![](report_trend.jpg)


The link: Filter trend data in the Performance Trend Page, will permit you to configure the graphs. When you click on it, you obtain the graph configuration menu.

This configuration will be saved in a cookie named performance. The default configuration is: "Show all the builds".

![](report_filter.jpg)
         
         
The link Last report in the Performance Trend Page, give us the detailed information of each report for the last build.

You can access to the data of old builds pushing on the new menu entry of each build named Performance report.

![](report_single.jpg)


In the performance trend page, the links to Trend report shows a report with the history data of each build.


![](report_trend_table.jpg)


## Jenkins GUI Configuration

If you are using GUI to configure Jenkins jobs, start with adding "Publish Pefrormance test result report" item into your post-build actions:

![](report_step_choice.png)

Specify the report files into "Source data files" field, separate multiple entries with semicolon. Plugin will autodetect file format for each file. You can use Jenkins globs like `**/*.jtl`.

You can configure the error percentage thresholds and the relative percentage thresholds which would make the project unstable or failed or set them to `-1` to disable the feature:

![](report_constraints.png)

## Using from Pipeline Scripts

You can use Pipleline Script builder to build groovy script piece from GUI. Additinally, Performance Plugin offers "perfReport" Groovy command that allows shorthand use to simply build report, it will autodetect source file format:
 
### Minimal configuration 
```groovy
perfReport 'result.csv'
```

### More advanced configuration
```groovy
perfReport filterRegex: '', relativeFailedThresholdNegative: 1.2, relativeFailedThresholdPositive: 1.89, relativeUnstableThresholdNegative: 1.8, relativeUnstableThresholdPositive: 1.5, sourceDataFiles: 'results.csv'
```

### Minimal command for old-style invocation is this:

Minimal command for old-style invocation is this:

```groovy
performanceReport parsers: [[$class: 'JMeterParser', glob: 'result.xml']], relativeFailedThresholdNegative: 1.2, relativeFailedThresholdPositive: 1.89, relativeUnstableThresholdNegative: 1.8, relativeUnstableThresholdPositive: 1.5
```
