import nbbrd.io.http.HttpClientFactory;
import nbbrd.io.http.curl.CurlHttpClientFactory;
import nbbrd.io.http.okhttp.OkHttpHttpClientFactory;
import nbbrd.io.http.urlconnection.UrlConnectionHttpClientFactory;

module nbbrd.io.http {

    requires static org.jspecify;
    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;

    requires nbbrd.io.base;
    requires static nbbrd.io.curl;
    requires static okhttp3;

    exports nbbrd.io.http;
    exports nbbrd.io.http.ext;
    exports nbbrd.io.http.okhttp;
    exports nbbrd.io.http.urlconnection;
    exports nbbrd.io.http.curl;

    uses HttpClientFactory;
    provides HttpClientFactory with
            UrlConnectionHttpClientFactory,
            OkHttpHttpClientFactory,
            CurlHttpClientFactory;
}
