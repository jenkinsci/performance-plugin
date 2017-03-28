package hudson.plugins.performance.cookie;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.kohsuke.stapler.Ancestor;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CookieHandlerTest {

    @Test
    public void testCookieHandler() throws Exception {
        CookieHandler cookieHandler = new CookieHandler("testCookie");

        final List<Ancestor> list = new ArrayList<Ancestor>();
        list.add(new AncestorImpl("url1"));
        list.add(new AncestorImpl("url2"));
        list.add(new AncestorImpl("url3"));
        list.add(new AncestorImpl("url4"));

        final Cookie cookie = cookieHandler.create(list, "Something value");

        final String testValue1 = cookieHandler.getValue(new Cookie[] {cookie});
        assertEquals("Something value", testValue1);

        String testValue2 = cookieHandler.getValue(new Cookie[] {new Cookie("otherCookie", "some  other value")});
        assertEquals(StringUtils.EMPTY, testValue2);
    }

    private static class AncestorImpl implements Ancestor {
        private final String url;

        public AncestorImpl(String url) {
            this.url = url;
        }
        public Object getObject() {
            return null;
        }
        public String getUrl() {
            return url;
        }
        public String getRestOfUrl() {
            return url;
        }
        public String getNextToken(int n) {
            return null;
        }
        public String getFullUrl() {
            return url;
        }
        public String getRelativePath() {
            return url;
        }
        public Ancestor getPrev() {
            return null;
        }
        public Ancestor getNext() {
            return null;
        }
    }
}