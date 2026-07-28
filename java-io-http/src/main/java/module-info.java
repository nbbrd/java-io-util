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

    uses nbbrd.io.http.HttpFactory;
    provides nbbrd.io.http.HttpFactory with
            nbbrd.io.http.UrlConnectionHttpFactory,
            nbbrd.io.http.OkHttpHttpFactory,
            nbbrd.io.http.CurlHttpFactory;
}
