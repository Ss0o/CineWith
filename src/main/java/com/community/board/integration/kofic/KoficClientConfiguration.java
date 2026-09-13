package com.community.board.integration.kofic;

import com.community.board.movie.kofic.KoficClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KoficProperties.class)
public class KoficClientConfiguration {

    @Bean
    KoficClient koficClient(KoficProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout() == null ? Duration.ofSeconds(2) : properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout() == null ? Duration.ofSeconds(5) : properties.readTimeout());

        return new KoficRestClient(RestClient.builder()
                .baseUrl(properties.baseUrl() == null ? "https://www.kobis.or.kr/kobisopenapi/webservice/rest" : properties.baseUrl())
                .requestFactory(requestFactory)
                .build(), properties);
    }
}
