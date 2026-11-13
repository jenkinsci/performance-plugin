package hudson.plugins.performance.tools;

import java.net.URI;
import java.util.List;

public class ExternalUrlValidator {

    private static final List<String> VALID_PROTOCOLS = List.of("http", "https");

    public static final boolean isValid(final String url) {
        try {
            final String protocol = URI.create(url).toURL().getProtocol();
            return VALID_PROTOCOLS.contains(protocol);
        } catch (final Exception e) {
            return false;
        }
    }

}
