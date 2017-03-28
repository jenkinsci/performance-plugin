package hudson.plugins.performance.reports;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Result;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import hudson.plugins.performance.constraints.AbsoluteConstraint;
import hudson.plugins.performance.constraints.ConstraintEvaluation;
import hudson.plugins.performance.constraints.RelativeConstraint;
import hudson.plugins.performance.reports.ConstraintReport;
import jenkins.model.Jenkins;
import hudson.plugins.performance.constraints.AbstractConstraint.Escalation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class, FilePath.class, AbstractBuild.class, Calendar.class})
public class ConstraintReportTest {

    @Mock
    ArrayList<ConstraintEvaluation> ceList;

    @Mock
    ConstraintEvaluation ce0;
    @Mock
    ConstraintEvaluation ce1;
    @Mock
    ConstraintEvaluation ce2;
    @Mock
    ConstraintEvaluation ce3;
    @Mock
    ConstraintEvaluation ce4;
    @Mock
    ConstraintEvaluation ce5;

    @Mock
    RelativeConstraint rc0;
    @Mock
    RelativeConstraint rc1;
    @Mock
    RelativeConstraint rc2;
    @Mock
    AbsoluteConstraint ac3;
    @Mock
    AbsoluteConstraint ac4;
    @Mock
    AbsoluteConstraint ac5;

    @Mock
    Date date;

    @Mock
    Jenkins jenkins;

    @Mock
    Calendar calendar;

    @Mock
    FilePath filePath;

    @Mock
    URI uri;

    @SuppressWarnings("rawtypes")
    @Mock
    AbstractBuild globBuild;

    //Result Messages
    String resultMsgRc0 = "Relative constraint failed! - Report: Result.xml \n" +
            "The constraint says: Average of all test cases must not be greater than 110 \n" +
            "Measured value for Average: 120 \n" +
            "Included builds: 3 builds \n" +
            "Escalation Level: Warning";
    String resultMsgRc1 = "Relative constraint successful! - Report: Result.xml \n" +
            "The constraint says: Average of all test cases must not be less than 1 \n" +
            "Measured value for Average: 120 \n" +
            "Included builds: 3 builds \n" +
            "Escalation Level: Warning";
    String resultMsgRc2 = "Relative constraint failed! - Report: Result.xml \n" +
            "The constraint says: Maximum of all test cases must not be greater than 990 \n" +
            "Measured value for Maximum: 1000 \n" +
            "Included builds: 3 builds \n" +
            "Escalation Level: Warning";
    String resultMsgAc3 = "Absolute constraint successful! - Report: Result.xml \n " +
            "The constraint says: Average of checkTickets must not be greater than 100 \n " +
            "Measured value for Average: 9 \n " +
            "Escalation Level: Error";
    String resultMsgAc4 = "Absolute constraint failed! - Report: Result.xml \n " +
            "The constraint says: Average of checkTickets must not be less than 100 \n " +
            "Measured value for Average: 9 \n " +
            "Escalation Level: Information";
    String resultMsgAc5 = "Absolute constraint successful! - Report: Result.xml \n " +
            "The constraint says: Average of checkTickets must not be equal than 9 \n " +
            "Measured value for Average: 9 \n " +
            "Escalation Level: Warning";

    @Before
    public void setUp() throws IOException, InterruptedException {
        when(ce0.getAbstractConstraint()).thenReturn(rc0);
        when(ce1.getAbstractConstraint()).thenReturn(rc1);
        when(ce2.getAbstractConstraint()).thenReturn(rc2);
        when(ce3.getAbstractConstraint()).thenReturn(ac3);
        when(ce4.getAbstractConstraint()).thenReturn(ac4);
        when(ce5.getAbstractConstraint()).thenReturn(ac5);

        when(rc0.getEscalationLevel()).thenReturn(Escalation.WARNING);
        when(rc1.getEscalationLevel()).thenReturn(Escalation.WARNING);
        when(rc2.getEscalationLevel()).thenReturn(Escalation.WARNING);
        when(ac3.getEscalationLevel()).thenReturn(Escalation.ERROR);
        when(ac4.getEscalationLevel()).thenReturn(Escalation.INFORMATION);
        when(ac5.getEscalationLevel()).thenReturn(Escalation.WARNING);

        when(rc0.getSuccess()).thenReturn(false);
        when(rc1.getSuccess()).thenReturn(true);
        when(rc2.getSuccess()).thenReturn(false);
        when(ac3.getSuccess()).thenReturn(true);
        when(ac4.getSuccess()).thenReturn(false);
        when(ac5.getSuccess()).thenReturn(true);

        when(rc0.getResultMessage()).thenReturn(resultMsgRc0);
        when(rc1.getResultMessage()).thenReturn(resultMsgRc1);
        when(rc2.getResultMessage()).thenReturn(resultMsgRc2);
        when(ac3.getResultMessage()).thenReturn(resultMsgAc3);
        when(ac4.getResultMessage()).thenReturn(resultMsgAc4);
        when(ac5.getResultMessage()).thenReturn(resultMsgAc5);

        when(globBuild.getNumber()).thenReturn(42);
        when(globBuild.getTimestamp()).thenReturn(calendar);

        filePath = PowerMockito.mock(FilePath.class);

        when(globBuild.getWorkspace()).thenReturn(filePath);
        when(filePath.toURI()).thenReturn(uri);
        when(uri.getPath()).thenReturn("test-jenkins-filepath/");

        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(jenkins);
        when(jenkins.getRootUrl()).thenReturn("test-jenkins-rooturl");
    }

    @Test
    public void happyPathWithoutConstraintLog() throws IOException, InterruptedException {

        ceList = new ArrayList<ConstraintEvaluation>();
        ceList.add(ce0);
        ceList.add(ce1);
        ceList.add(ce2);
        ceList.add(ce3);
        ceList.add(ce4);
        ceList.add(ce5);

        ConstraintReport result = new ConstraintReport(ceList, globBuild, false);

        assertEquals(6, result.getAllConstraints());
        assertEquals(3, result.getAbsoluteConstraints());
        assertEquals(3, result.getRelativeConstraints());
        assertEquals(3, result.getSuccessfulConstraints());
        assertEquals(3, result.getViolatedConstraints());
        assertEquals(0, result.getViolatedError());
        assertEquals(2, result.getViolatedUnstable());
        assertEquals(1, result.getViolatedInformation());
        assertEquals(result.getAllConstraints(), result.getSuccessfulConstraints() + result.getViolatedConstraints());
        assertEquals(result.getAllConstraints(), result.getAbsoluteConstraints() + result.getRelativeConstraints());
        assertEquals(result.getViolatedConstraints(), result.getViolatedError() + result.getViolatedUnstable() + result.getViolatedInformation());
        assertEquals(Result.UNSTABLE, result.getBuildResult());
        assertEquals(42, result.getBuildNumber());
    }

    @Test
    public void happyPathWithConstraintLog() throws IOException, InterruptedException {
        ceList = new ArrayList<ConstraintEvaluation>();
        ceList.add(ce0);
        ceList.add(ce1);
        ceList.add(ce2);
        ceList.add(ce3);
        ceList.add(ce4);
        ceList.add(ce5);

        ConstraintReport result = new ConstraintReport(ceList, globBuild, true);
        File f = result.getPerformanceLog();
        //checking existence of file
        assertTrue(f.exists());

        String s = result.getLoggerMsgAdv();
        //checking content of loggerMsg
        assertTrue(s.contains("Number of all constraints: 6\nRelative constraints: 3\nAbsolute constraints: 3\nSuccessful constraints: 3\nViolated constraints: 3\n->INFORMATION: 1\n->UNSTABLE: 2\n->ERROR: 0\n-------------- \n----------------------------------------------------------- \nEvaluating all relative constraints! \n-------------- \nRelative constraint failed! - Report: Result.xml \nThe constraint says: Average of all test cases must not be greater than 110 \nMeasured value for Average: 120 \nIncluded builds: 3 builds \nEscalation Level: Warning\n-------------- \nRelative constraint successful! - Report: Result.xml \nThe constraint says: Average of all test cases must not be less than 1 \nMeasured value for Average: 120 \nIncluded builds: 3 builds \nEscalation Level: Warning\n-------------- \nRelative constraint failed! - Report: Result.xml \nThe constraint says: Maximum of all test cases must not be greater than 990 \nMeasured value for Maximum: 1000 \nIncluded builds: 3 builds \nEscalation Level: Warning\n-------------- \nEvaluating all absolute constraints! \n-------------- \nAbsolute constraint successful! - Report: Result.xml \n The constraint says: Average of checkTickets must not be greater than 100 \n Measured value for Average: 9 \n Escalation Level: Error\n-------------- \nAbsolute constraint failed! - Report: Result.xml \n The constraint says: Average of checkTickets must not be less than 100 \n Measured value for Average: 9 \n Escalation Level: Information\n-------------- \nAbsolute constraint successful! - Report: Result.xml \n The constraint says: Average of checkTickets must not be equal than 9 \n Measured value for Average: 9 \n Escalation Level: Warning\n-------------- \nThe highest escalation: Warning! The build will be marked as UNSTABLE\n\n"));

        // remove 'null' directory and including folders
        f.delete();
        f.getParentFile().delete();
        f.getParentFile().getParentFile().delete();
    }

}
