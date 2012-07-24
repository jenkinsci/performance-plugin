package hudson.plugins.performance;

import org.apache.commons.collections.map.LRUMap;

import java.io.*;
import java.util.Date;
import java.util.Arrays;

import hudson.model.AbstractBuild;


/**
 * Created by IntelliJ IDEA.
 * User: AGoley
 * Date: 28.06.2012
 * Time: 11:58:21
 * To change this template use File | Settings | File Templates.
 */
public  final class PerformanceSimpleCache {
    private  LRUMap cache;
    private  long filesSize;

    public PerformanceSimpleCache(){
        filesSize =0;
        cache=new LRUMap();
    }

    public PerformanceReport getCache(String path) {
        if (!cache.containsKey(path)) {
            return null;
        } else {
            return (PerformanceReport) cache.get(path);
        }
    }

    public void putCache(File f, PerformanceReport report) throws IOException {
        if (f.length() < 20000000) {
            if (filesSize > 500000000) {
                cache.isScanUntilRemovable();
                File removedFile = new File ((String) cache.lastKey());
                filesSize=filesSize-removedFile.length();
            }
            cache.put(f.getPath(), report);
            System.out.println("File "+f.getPath()+ " added to cache");
            filesSize=filesSize+f.length();
        }
    }

    public PerformanceReport deSerializeObject(File serializedReports) throws IOException,ClassNotFoundException{
        FileInputStream in = null;
        ObjectInputStream sPerfReport = null;
        try {
            in = new FileInputStream (serializedReports);
            sPerfReport = new ObjectInputStream(in);
        return (PerformanceReport) sPerfReport.readObject();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.getMessage();
                }
            }
            if (sPerfReport != null) {
                try {
                    sPerfReport.close();
                } catch (IOException e) {
                    e.getMessage();
                }
            }
        }
    }

    public void serializeObject(PerformanceReport report, String path) throws IOException{
        FileOutputStream out = null;
        ObjectOutputStream sPerfReport = null;
        try {
            out = new FileOutputStream(path + ".serialized");
            sPerfReport = new ObjectOutputStream(out);
            sPerfReport.writeObject(report);
            sPerfReport.flush();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.getMessage();    
                }
            }
            if (sPerfReport != null) {
                try {
                    sPerfReport.close();
                } catch (IOException e) {
                    e.getMessage();
                }

            }
        }
    }

    public PerformanceReport getReportFromCache(PerformanceSimpleCache sc, File f, PrintStream logger ) {
    
        try {
            if (sc.getCache(f.getPath()) != null )  {
               return sc.getCache(f.getPath());
            } else {
                File dir = new File(f.getParent());
                File[] serializedReports = dir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name){
                        return name.contains(".serialized");
                    }
            });

            if (serializedReports.length > 0) {
                PerformanceReport p = sc.deSerializeObject(Arrays.asList(serializedReports).get(0));
                sc.putCache(f,p);
                return p;
            }
        }
        } catch (FileNotFoundException e) {
            logger.println("File not found" + e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.println("Class not found" + e.getMessage());
        } catch (IOException e) {
            logger.println(e.getMessage());
        }
        return null;
        }
}
