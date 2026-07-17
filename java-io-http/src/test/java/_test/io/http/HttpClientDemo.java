package _test.io.http;

import nbbrd.design.Demo;
import nbbrd.io.http.*;
import nbbrd.io.net.MediaType;
import nl.altindag.ssl.SSLFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HttpClientDemo {

    @Demo
    public static void main(String[] args) throws IOException {
        SSLFactory ssl = SSLFactory
                .builder()
                .withDefaultTrustMaterial()
                .withSystemTrustMaterial()
                .build();

        List<HttpClient> clients = new ArrayList<>();
        clients.add(UrlConnectionHttpClient
                .builder()
                .hostnameVerifier(ssl.getHostnameVerifier())
                .sslSocketFactory(ssl.getSslSocketFactory())
                .build());
        clients.add(OkHttpHttpClient
                .builder()
                .hostnameVerifier(ssl.getHostnameVerifier())
                .sslSocketFactory(ssl.getSslSocketFactory())
                .followRedirects(false)
                .build());

        List<URI> uris = new ArrayList<>();
        uris.add(URI.create("https://www.nbb.be"));
        uris.add(URI.create("https://data-api.ecb.europa.eu/service/dataflow/all/all/latest"));

        for (URI uri : uris) {
            System.out.println(uri);
            for (HttpClient client : clients) {
                System.out.println(String.format(Locale.ROOT, "%21s", client.getDescription()) + ": " + run(client, uri));
            }
            System.out.println();
        }
    }

    private static Report run(HttpClient client, URI query) throws IOException {
        try (HttpResponse response = client.send(HttpRequest.builder().query(query).build())) {
            return new Report(
                    response.getStatusCode(),
                    response.getContentType(),
                    response.getHeaders().getMap().size(),
                    response.getBodyAsString().length()
            );
        }
    }

    @lombok.Value
    private static class Report {
        int status;
        MediaType type;
        int headers;
        long body;
    }
}
