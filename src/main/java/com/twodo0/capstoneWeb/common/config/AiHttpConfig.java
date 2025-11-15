package com.twodo0.capstoneWeb.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiHttpConfig {
    @Bean
    WebClient aiWebClient(WebClient.Builder builder, AiProperties props){
        return builder
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .protocol(HttpProtocol.HTTP11)
                                .wiretap(true)
                ))
                .build();
    }
}
