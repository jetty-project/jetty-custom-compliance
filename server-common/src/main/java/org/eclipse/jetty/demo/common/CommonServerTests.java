package org.eclipse.jetty.demo.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;

import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.util.IO;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class CommonServerTests
{
    public HttpClient httpClient;

    @BeforeEach
    public void initHttpClient()
    {
        httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }

    public abstract URI getServerBaseURI();

    /**
     * Test of CookieCompliance.Violation.ESCAPE_IN_QUOTES (the escaped {@code \(} and {@code \)} chars)
     */
    @Test
    public void testCookieEscapedParensInQuotes() throws IOException, InterruptedException
    {
        URI destURI = getServerBaseURI().resolve("/cookie/");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(destURI)
            .GET()
            .header("Cookie", "key=\"a\\(b\\)c\"")
            .build();
        HttpResponse<InputStream> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode(), is(200));
        OptionalLong violationCount = response.headers().firstValueAsLong("X-Compliance-Violations");
        if (violationCount.isPresent())
            assertThat("Violation Count", violationCount.getAsLong(), Matchers.lessThanOrEqualTo(0L));

        Properties props = readResponseAsProperties(response);
        assertThat("cookies.length", props.getProperty("cookies.length"), is("1"));
        assertThat("cookie[key]", props.getProperty("cookie.key"), is("a(b)c"));
    }

    /**
     * Test of CookieCompliance.Violation.BAD_QUOTES (no ending quote)
     */
    @Test
    public void testCookieBadQuotesNoEndQuote() throws IOException, InterruptedException
    {
        URI destURI = getServerBaseURI().resolve("/cookie/");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(destURI)
            .GET()
            .header("Cookie", "key=\"abc") // no ending quote
            .build();
        HttpResponse<InputStream> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode(), is(200));
        OptionalLong violationCount = response.headers().firstValueAsLong("X-Compliance-Violations");
        if (violationCount.isPresent())
            assertThat("Violation Count", violationCount.getAsLong(), Matchers.lessThanOrEqualTo(0L));

        Properties props = readResponseAsProperties(response);
        assertThat("cookies.length", props.getProperty("cookies.length"), is("1"));
        assertThat("cookie[key]", props.getProperty("cookie.key"), is("\"abc"));
    }

    /**
     * Test of CookieCompliance.Violation.BAD_QUOTES (escaped dquote {@code \"})
     */
    @Test
    public void testCookieBadQuotesEscapedQuotes() throws IOException, InterruptedException
    {
        URI destURI = getServerBaseURI().resolve("/cookie/");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(destURI)
            .GET()
            .header("Cookie", "key=\\\"abc\\\"") // escaped dquotes
            .build();
        HttpResponse<InputStream> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode(), is(200));
        OptionalLong violationCount = response.headers().firstValueAsLong("X-Compliance-Violations");
        if (violationCount.isPresent())
            assertThat("Violation Count", violationCount.getAsLong(), Matchers.lessThanOrEqualTo(0L));

        Properties props = readResponseAsProperties(response);
        assertThat("cookies.length", props.getProperty("cookies.length"), is("1"));
        assertThat("cookie[key]", props.getProperty("cookie.key"), is("\\\"abc\\\""));
    }

    /**
     * Test of CookieCompliance.Violation.SPECIAL_CHARS_IN_QUOTES (existence of comma {@code ,})
     */
    @Test
    public void testCookieSpecialCharsInQuotes() throws IOException, InterruptedException
    {
        URI destURI = getServerBaseURI().resolve("/cookie/");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(destURI)
            .GET()
            .header("Cookie", "key=\"a,b,c\"") // comma in quotes
            .build();
        HttpResponse<InputStream> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode(), is(200));
        OptionalLong violationCount = response.headers().firstValueAsLong("X-Compliance-Violations");
        if (violationCount.isPresent())
            assertThat("Violation Count", violationCount.getAsLong(), Matchers.lessThanOrEqualTo(0L));

        Properties props = readResponseAsProperties(response);
        assertThat("cookies.length", props.getProperty("cookies.length"), is("1"));
        assertThat("cookie[key]", props.getProperty("cookie.key"), is("a,b,c"));
    }

    /**
     * Test of CookieCompliance.Violation.INVALID_COOKIES (existence of dquote in name {@code "})
     */
    @Test
    public void testCookieInvalid() throws IOException, InterruptedException
    {
        URI destURI = getServerBaseURI().resolve("/cookie/");
        HttpRequest request = HttpRequest.newBuilder()
            .uri(destURI)
            .GET()
            .header("Cookie", "\"key\"=abc") // name has dquotes
            .build();
        HttpResponse<InputStream> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode(), is(200));
        OptionalLong violationCount = response.headers().firstValueAsLong("X-Compliance-Violations");
        if (violationCount.isPresent())
            assertThat("Violation Count", violationCount.getAsLong(), Matchers.lessThanOrEqualTo(0L));

        Properties props = readResponseAsProperties(response);
        assertThat("cookies.length", props.getProperty("cookies.length"), is("null")); // should have no valid cookies
    }

    /**
     * Test of HttpCompliance.Violation.DUPLICATE_HOST_HEADERS
     */
    @Test
    public void testDuplicateHostHeadersSame() throws IOException
    {
        URI serverURI = getServerBaseURI();

        try (Socket client = new Socket(serverURI.getHost(), serverURI.getPort()))
        {
            String rawRequest = "GET /cookie/ HTTP/1.1\r\n" +
                "Host: " + serverURI.getAuthority() + "\r\n" +
                "Host: " + serverURI.getAuthority() + "\r\n" + // duplicate
                "Connection: close\r\n" +
                "\r\n";

            OutputStream out = client.getOutputStream();
            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String rawResponse = IO.toString(client.getInputStream());

            HttpTester.Response response = HttpTester.parseResponse(rawResponse);
            assertThat(response.getStatus(), is(400)); // duplicate `Host` headers not supported
        }
    }

    /**
     * Test of HttpCompliance.Violation.DUPLICATE_HOST_HEADERS
     */
    @Test
    public void testDuplicateHostHeadersDifferent() throws IOException
    {
        URI serverURI = getServerBaseURI();

        try (Socket client = new Socket(serverURI.getHost(), serverURI.getPort()))
        {
            String rawRequest = "GET /cookie/ HTTP/1.1\r\n" +
                "Host: " + serverURI.getAuthority() + "\r\n" +
                "Host: abc.example.com:8080\r\n" + // duplicate (different name)
                "Connection: close\r\n" +
                "\r\n";

            OutputStream out = client.getOutputStream();
            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String rawResponse = IO.toString(client.getInputStream());

            HttpTester.Response response = HttpTester.parseResponse(rawResponse);
            assertThat(response.getStatus(), is(400)); // duplicate `Host` headers not supported
        }
    }

    /**
     * Test of HttpCompliance.Violation.DUPLICATE_HOST_HEADERS
     */
    @Test
    public void testUnsafeHostHeader() throws IOException
    {
        URI serverURI = getServerBaseURI();

        try (Socket client = new Socket(serverURI.getHost(), serverURI.getPort()))
        {
            String rawRequest = "GET /cookie/ HTTP/1.1\r\n" +
                "Host: " + serverURI.getHost() + ":-10\r\n" + // unsafe port
                "Connection: close\r\n" +
                "\r\n";

            OutputStream out = client.getOutputStream();
            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            String rawResponse = IO.toString(client.getInputStream());

            HttpTester.Response response = HttpTester.parseResponse(rawResponse);
            assertThat(response.getStatus(), is(400)); // unsafe `Host` header not supported
        }
    }

    private Properties readResponseAsProperties(HttpResponse<InputStream> response) throws IOException
    {
        Properties props = new Properties();
        props.load(response.body());
        return props;
    }
}
