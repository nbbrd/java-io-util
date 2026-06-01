package internal.io.http;

import nbbrd.design.StaticFactoryMethod;

// https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
public enum HttpResponseType {

    UNKNOWN, INFORMATIONAL, SUCCESSFUL, REDIRECTION, CLIENT_ERROR, SERVER_ERROR;

    @StaticFactoryMethod
    public static HttpResponseType ofResponseCode(int code) {
        switch (code / 100) {
            case 1:
                return INFORMATIONAL;
            case 2:
                return SUCCESSFUL;
            case 3:
                return REDIRECTION;
            case 4:
                return CLIENT_ERROR;
            case 5:
                return SERVER_ERROR;
            default:
                return UNKNOWN;
        }
    }
}
