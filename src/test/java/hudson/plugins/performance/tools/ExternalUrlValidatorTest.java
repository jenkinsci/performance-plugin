package hudson.plugins.performance.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExternalUrlValidatorTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
            # url                               | isValid
            'http://www.jenkins.io'             | true
            'https://www.jenkins.io'            | true
            'ftp://www.jenkins.io'              | false
            'ftps://www.jenkins.io'             | false
            'javascript:alert(document.cookie)' | false
            'https://javascript:alert(1)'       | false
            ''                                  | false
            ' '                                 | false
                                                | false
            """, delimiter = '|')
    void isValidTest(final String url, final boolean isValid) {
        assertEquals(ExternalUrlValidator.isValid(url), isValid);
    }

}
