package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.enums.EHuaweiEndpointType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;

/**
 * Real HTTP call to Huawei's CPS SOAP API. Uses its own, dedicated {@link RestClient} -- not
 * the shared bean {@code PaymentCallbackDeliveryService} uses for outbound client callbacks --
 * so the TLS-verification bypass below can never leak into calls to arbitrary client-supplied
 * URLs.
 *
 * <p>Built on Apache HttpClient 5, not {@code java.net.http.HttpClient}: the JDK client's
 * {@code SSLParameters.setEndpointIdentificationAlgorithm("")} does not reliably disable
 * hostname verification in practice -- confirmed live against Moov's real endpoint, which
 * kept failing with "No subject alternative names present" even with that set. Apache
 * HttpClient 5's {@link NoopHostnameVerifier} is the well-established, actually-reliable way
 * to fully replicate what {@code curl -k} does (skip both certificate trust and hostname
 * verification), which is exactly what the real
 * {@code _MATERIALS/soapcall-momo-prod.sh} script relies on.
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
     * script's own "curl -k". Defaults to false (skip verification) to match that script; set
     * MOOV_TLS_VERIFY=true once Moov's certificate is properly trusted and hostnamed.
     */
    @Value("${momo.tls-verify:false}")
    private boolean tlsVerify;

    private RestClient restClient;

    @PostConstruct
    private void init() {
        RequestConfig requestConfig = RequestConfig.custom()
                                                   .setConnectTimeout(Timeout.ofSeconds(5))
                                                   .setResponseTimeout(Timeout.ofSeconds(15))
                                                   .build();

        CloseableHttpClient httpClient = tlsVerify
                                         ? HttpClients.custom().setDefaultRequestConfig(requestConfig).build()
                                         : HttpClients.custom()
                                                      .setConnectionManager(trustAllConnectionManager())
                                                      .setDefaultRequestConfig(requestConfig)
                                                      .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
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
     * Trust-all SSLContext + NoopHostnameVerifier -- equivalent to curl -k, matching the real
     * integration script. Scoped to this class's own connection manager only (see class
     * javadoc).
     */
    private static PoolingHttpClientConnectionManager trustAllConnectionManager() {
        try {
            SSLContext sslContext = SSLContextBuilder.create()
                                                     .loadTrustMaterial((chain, authType) -> true)
                                                     .build();

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            return PoolingHttpClientConnectionManagerBuilder.create()
                                                            .setSSLSocketFactory(sslSocketFactory)
                                                            .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build trust-all connection manager for Moov integration", ex);
        }
    }
}
