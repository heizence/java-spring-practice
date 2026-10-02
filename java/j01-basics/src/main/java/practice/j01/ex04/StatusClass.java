package practice.j01.ex04;

/**
 * The five classes of HTTP status codes. The first digit of a code tells a client how to react, even when it
 * doesn't know the exact code.
 */
public enum StatusClass {
    /** 1xx: the request was received, and the server is still working on it. */
    INFORMATIONAL,
    /** 2xx: the request worked, for example 200 OK or 201 Created. */
    SUCCESS,
    /** 3xx: the client has to do something else, usually follow a redirect. */
    REDIRECTION,
    /** 4xx: the request is wrong, for example 404 Not Found. Sending it again won't help. */
    CLIENT_ERROR,
    /** 5xx: the server failed to handle a valid request. Trying again later may work. */
    SERVER_ERROR
}
