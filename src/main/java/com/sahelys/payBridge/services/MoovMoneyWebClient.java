package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.enums.EHuaweiEndpointType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Real HTTP call to Huawei's CPS SOAP API. Uses its own, dedicated {@link RestClient} -- not
 * the shared bean {@code PaymentCallbackDeliveryService} uses for outbound client callbacks --
 * so the TLS-verification bypass below can never leak into calls to arbitrary client-supplied
 * URLs.
 */
@Component
@Slf4j
public class MoovMoneyWebClient {

    @Value("${momo.base-url}")
    private String baseUrl;

    @Value("${momo.payment-endpoint}")
    private String asyncEndpoint;

    @Value("${momo.sync-endpoint}")
    private String syncEndpoint;

    /**
     * Temporary integration setting matching the real _MATERIALS/soapcall-momo-prod.sh
     * script's own "curl -k" -- Moov's endpoint doesn't present a certificate the JDK's
     * default trust store accepts. Defaults to false (skip verification) to match that
     * script; set MOOV_TLS_VERIFY=true once Moov's certificate is properly trusted.
     *
     * <p>"curl -k" skips two independent checks: certificate trust (handled below by the
     * trust-all SSLContext) AND hostname/endpoint identification (the SAN-vs-connected-host
     * match). {@code java.net.http.HttpClient} enforces the latter by default regardless of
     * the installed TrustManager -- confirmed live against Moov's real endpoint
     * (172.16.52.14), whose certificate has no SAN entries at all, failing with "No subject
     * alternative names present" even with the trust-all context in place. Both must be
     * disabled together to actually match curl -k.
     */
    @Value("${momo.tls-verify:false}")
    private boolean tlsVerify;

    private RestClient restClient;

    @PostConstruct
    private void init() {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5));

        if (!tlsVerify) {
            httpClientBuilder.sslContext(trustAllSslContext());
            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("");
            httpClientBuilder.sslParameters(sslParameters);
        }

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClientBuilder.build());
        int READ_TIME_OUT_SEC = 15;
        requestFactory.setReadTimeout(Duration.ofSeconds(READ_TIME_OUT_SEC));

        restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public String sendRequest(EHuaweiEndpointType endpoint, String xmlString) {
        String url = switch (endpoint) {
            case ASYNC -> baseUrl + asyncEndpoint;
            case SYNC -> baseUrl + syncEndpoint;
        };

        log.info("Calling Huawei SOAP url : {}", url);
        log.info("SOAP Request\n{}", redactCredentials(xmlString));

        String response = restClient.post()
                                    .uri(url)
                                    .contentType(MediaType.valueOf("text/xml;charset=utf-8"))
                                    .body(xmlString)
                                    .retrieve()
                                    .body(String.class);

        log.info("SOAP Response\n{}", response);
        return response;
    }

    /**
     * Never logs the real Moov integration password -- it appears twice in the built XML
     * (req:Password, req:SecurityCredential), both from the same momo.password value. Only
     * affects what's written to the log; the real value still goes out on the wire unchanged.
     */
    private String redactCredentials(String xmlString) {
        return xmlString.replaceAll("(?s)<req:Password>.*?</req:Password>", "<req:Password>[REDACTED]</req:Password>")
                        .replaceAll("(?s)<req:SecurityCredential>.*?</req:SecurityCredential>", "<req:SecurityCredential>[REDACTED]</req:SecurityCredential>");
    }

    /**
     * Trust-all SSLContext -- equivalent to curl -k, matching the real integration script.
     * Scoped to this class's own client only (see class javadoc).
     */
    private static SSLContext trustAllSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build trust-all SSLContext for Moov integration", ex);
        }
    }
}
