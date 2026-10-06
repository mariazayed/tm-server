package trademind.tm_server.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestSizeLimitFilterTest {

    private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(100);

    private static MockHttpServletRequest post(String uri, byte[] body, boolean declareLength) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri) {
            @Override
            public long getContentLengthLong() {
                return declareLength ? super.getContentLengthLong() : -1; // -1 = chunked, length unknown
            }
        };
        request.setRequestURI(uri);
        request.setContent(body);
        return request;
    }

    @Test
    void refusesADeclaredOversizeBodyWithoutReadingIt() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        filter.doFilter(post("/api/v1/invest/contributions/import", new byte[101], true), response,
                (req, res) -> chainCalled[0] = true);
        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("too large"));
        assertFalse(chainCalled[0]);
    }

    @Test
    void passesABodyAtTheLimitUnchanged() throws Exception {
        byte[] body = new byte[100];
        java.util.Arrays.fill(body, (byte) 'a');
        AtomicReference<byte[]> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(((HttpServletRequest) req).getInputStream().readAllBytes());
        filter.doFilter(post("/api/v1/invest/journal", body, true), new MockHttpServletResponse(), chain);
        assertArrayEquals(body, seen.get());
    }

    @Test
    void aChunkedBodyIsStoppedOnceItPassesTheLimit() throws Exception {
        FilterChain chain = (req, res) -> assertThrows(RequestTooLargeException.class,
                () -> ((HttpServletRequest) req).getInputStream().readAllBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(post("/api/v1/auth/login", new byte[500], false), response, chain);
        assertEquals(200, response.getStatus()); // the filter itself does not reject; the read fails
    }

    @Test
    void aSmallChunkedBodyIsFine() throws Exception {
        AtomicReference<byte[]> seen = new AtomicReference<>();
        FilterChain chain = (req, res) -> seen.set(((HttpServletRequest) req).getInputStream().readAllBytes());
        filter.doFilter(post("/api/v1/invest/rules", "{\"text\":\"x\"}".getBytes(), false), new MockHttpServletResponse(), chain);
        assertEquals("{\"text\":\"x\"}", new String(seen.get()));
    }

    @Test
    void otherRoutesAreLeftAlone() throws Exception {
        boolean[] chainCalled = {false};
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(post("/api/v1/trade", new byte[10_000], true), response, (req, res) -> chainCalled[0] = true);
        assertTrue(chainCalled[0]);
        assertEquals(200, response.getStatus());
    }

    @Test
    void readingByteByByteIsCountedToo() throws Exception {
        FilterChain chain = (req, res) -> {
            var in = ((HttpServletRequest) req).getInputStream();
            assertThrows(RequestTooLargeException.class, () -> {
                while (in.read() >= 0) {
                    // keep reading single bytes
                }
            });
        };
        filter.doFilter(post("/api/v1/invest/journal", new byte[500], false), new MockHttpServletResponse(), chain);
    }

    @Test
    void anIOExceptionTypeSoSpringWrapsItAsAnUnreadableBody() {
        assertTrue(IOException.class.isAssignableFrom(RequestTooLargeException.class));
    }
}
