# Running Tests

## Features

## Jenkins GUI Configuration

## Pipeline

## Other Ways to Run Test in Jenkins (deprecated)

### Using jmeter in a 'MAVEN' project
Follow the instructions in this [article](http://www.theserverlabs.com/blog/?p=280&cpage=1)

### Using jmeter in an 'ANT' project
Although there are different ways to run jmeter tests, here is explained a method to run them using ant.

Once you have your build.xml ready to run jmeter, you can add your project to Jenkins as a Freestyle-project which uses ant, and configure it following the instructions above.

Finally run your project setting the property jmeter-home to the appropriate folder in your computer: `ant "-Djmeter-home=C:\jmeter" -f build.xml`

```xml
<project default="all">
  <!-- ant-jmeter.jar comes with jmeter, be sure this is the release you have -->
  <path id="ant.jmeter.classpath">
    <pathelement
       location="${jmeter-home}/extras/ant-jmeter-1.1.1.jar" />
  </path>
  <taskdef
    name="jmeter"
    classname="org.programmerplanet.ant.taskdefs.jmeter.JMeterTask"
    classpathref="ant.jmeter.classpath" />
  <target name="clean">
    <delete dir="results"/>
    <delete file="jmeter.log"/>
    <mkdir dir="results/jtl"/>
    <mkdir dir="results/html"/>
  </target>
  <target name="test" depends="clean">
    <jmeter
       jmeterhome="${jmeter-home}"
       resultlogdir="results/jtl">
      <testplans dir="test/jmeter" includes="*.jmx"/>
      <property name="jmeter.save.saveservice.output_format" value="xml"/>
    </jmeter>
  </target>
  <!-- This is not needed for the plugin, but it produces a nice html report
       which can be saved usin jenkins's archive artifact feature -->
  <target name="report" depends="test">
    <xslt
       basedir="results/jtl"
       destdir="results/html"
       includes="*.jtl"
       style="${jmeter-home}/extras/jmeter-results-detail-report_21.xsl"/>
  </target>
  <target name="all" depends="test, report"/>
</project>
```