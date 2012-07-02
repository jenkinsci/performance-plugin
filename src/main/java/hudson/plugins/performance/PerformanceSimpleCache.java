package hudson.plugins.performance;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: AGoley
 * Date: 28.06.2012
 * Time: 11:58:21
 * To change this template use File | Settings | File Templates.
 */
public  class PerformanceSimpleCache {

    private  Map<String, PerformanceReport> cache ;

    public PerformanceSimpleCache(){
        cache=new HashMap<String, PerformanceReport>();
    }

    public PerformanceReport getCache (String path){
        if ( !cache.containsKey(path)) {
            return null;
        } else {
            return cache.get(path);
        }
    }

    public void putCache (String path, PerformanceReport report){
        cache.put(path, report);
    }

}
