package trademind.tm_server.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import trademind.tm_server.constants.AppConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Caps request bodies on the investing and login routes. {@code @RequestBody String} would otherwise
 * read an arbitrarily large upload into memory. A declared Content-Length over the limit is refused
 * up front with 413; bodies sent without a length (chunked) are counted as they are read and the
 * read fails once the limit is passed.
 */
@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long limit;

    public RequestSizeLimitFilter(@Value("${app.max-body-bytes:2097152}") long limit) {
        this.limit = limit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith(AppConstants.URL_INVEST) || path.startsWith(AppConstants.URL_AUTH));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > limit) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(
                    "{\"error\":\"Request body is too large\"}".getBytes(StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(new LimitedRequest(request, limit), response);
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private final long limit;
        private ServletInputStream stream;

        LimitedRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) stream = new CountingStream(super.getInputStream(), limit);
            return stream;
        }
    }

    private static final class CountingStream extends ServletInputStream {
        private final ServletInputStream in;
        private final long limit;
        private long read;

        CountingStream(ServletInputStream in, long limit) {
            this.in = in;
            this.limit = limit;
        }

        private void count(long n) throws IOException {
            if (n > 0) {
                read += n;
                if (read > limit) throw new RequestTooLargeException(limit);
            }
        }

        @Override
        public int read() throws IOException {
            int b = in.read();
            if (b >= 0) count(1);
            return b;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            int n = in.read(buffer, off, len);
            count(n);
            return n;
        }

        @Override
        public boolean isFinished() {
            return in.isFinished();
        }

        @Override
        public boolean isReady() {
            return in.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            in.setReadListener(listener);
        }
    }
}
