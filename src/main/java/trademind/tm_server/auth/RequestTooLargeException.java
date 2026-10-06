package trademind.tm_server.auth;

import java.io.IOException;

/** Thrown while reading a request body that has grown past the allowed size. */
public class RequestTooLargeException extends IOException {
    public RequestTooLargeException(long limit) {
        super("Request body is larger than " + limit + " bytes");
    }
}
