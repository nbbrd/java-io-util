module nbbrd.io.http {

    requires static org.jspecify;
    requires static lombok;
    requires static nbbrd.design;

    requires nbbrd.io.base;

    exports nbbrd.io.http;
    exports nbbrd.io.http.ext;
}
