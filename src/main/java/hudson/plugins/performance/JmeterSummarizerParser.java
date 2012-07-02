package hudson.plugins.performance;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.SAXException;

import java.util.*;
import java.io.File;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Created by IntelliJ IDEA.
 * User: Agoley
 * Date: 06.02.2012
 * Time: 12:45:24
 * To change this template use File | Settings | File Templates.
 */
public class JmeterSummarizerParser extends PerformanceReportParser{


    @Extension
  public static class DescriptorImpl extends PerformanceReportParserDescriptor {
    @Override
    public String getDisplayName() {
      return "JmeterSummarizer";
    }
  }


   @DataBoundConstructor
  public JmeterSummarizerParser(String glob) {
      super(glob);
    }

    @Override
  public String getDefaultGlobPattern() {
    return "**/*.log";
  }

   @Override
   public Collection<PerformanceReport> parse(AbstractBuild<?, ?> build,
      Collection<File> reports, TaskListener listener)  {
      List<PerformanceReport> result = new ArrayList<PerformanceReport>();
      PrintStream logger = listener.getLogger();

      PerformanceSimpleCache sc= build.getProject().getAction(PerformanceProjectAction.class).simpleCache;

      for (File f : reports) {
         try {
           if (sc.getCache(f.getPath()) == null )  {

               final  PerformanceReport r = new PerformanceReport();
               r.setReportFileName(f.getName());

               logger.println("Performance: Parsing JMeterSummarizer report file " + f.getName());

               Scanner s = new Scanner(f);

               String key;
               String line;
               SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/mm/dd H:m:s");
               while ( s.hasNextLine() )  {
                   line=s.nextLine().replaceAll("="," ");

                   if (line.contains ("+"))   {
                       Scanner scanner= new Scanner(line);
                       HttpSample sample = new HttpSample();

                       String dateString= scanner.next()+" "+scanner.next();

                       sample.setDate(dateFormat.parse(dateString));

                       scanner.findInLine("jmeter.reporters.Summariser:");
                       key=scanner.next();
                       scanner.next();
                       // set SamplesCount
                       scanner.findInLine(key);
                       sample.setSummarizerSamples(scanner.nextLong());
                       // set response time
                       scanner.findInLine("Avg:");
                       sample.setDuration(scanner.nextLong());
                       sample.setSuccessful(true);
                       // set MIN
                       scanner.findInLine("Min:");
                       sample.setSummarizerMin(scanner.nextLong());
                       // set MAX
                       scanner.findInLine("Max:");
                       sample.setSummarizerMax(scanner.nextLong());
                       // set errors count
                       scanner.findInLine("Err:");
                       sample.setSummarizerErrors(scanner.nextInt());
                       //sample.setSummarizerErrors( Float.valueOf(scanner.next().replaceAll("[()%]","")));
                       sample.setUri(key);
                       r.addSample(sample);
                  }
               }
            sc.putCache(f.getPath(), r);   
           }
          result.add(sc.getCache(f.getPath()));

         }catch (FileNotFoundException e) {
          logger.println("File not found" + e.getMessage());
         }catch (SAXException e) {
          logger.println(e.getMessage());
         }catch (ParseException e) {

      }
      }

    return result;

   }

}
