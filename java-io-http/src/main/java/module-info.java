import nbbrd.io.http.curl.CurlHttpFactory;
import nbbrd.io.http.okhttp.OkHttpHttpFactory;
import nbbrd.io.http.urlconnection.UrlConnectionHttpFactory;

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

    uses nbbrd.io.http.HttpFactory;
    provides nbbrd.io.http.HttpFactory with
            UrlConnectionHttpFactory,
            OkHttpHttpFactory,
            CurlHttpFactory;
}
