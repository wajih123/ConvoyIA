package com.goweyy.convoyia.biller.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Stores PDF documents in MinIO (S3-compatible object storage).
 * Returns the public URL of the stored object.
 */
@Slf4j
@Service
public class DocumentStorageService {

    private final WebClient minioClient;

    @Value("${minio.bucket:convoyia-invoices}")
    private String bucket;

    @Value("${minio.base-url:http://minio:9000}")
    private String minioBaseUrl;

    public DocumentStorageService(WebClient.Builder builder,
            @Value("${minio.base-url:http://minio:9000}") String minioBaseUrl) {
        this.minioClient = builder.baseUrl(minioBaseUrl).build();
        this.minioBaseUrl = minioBaseUrl;
    }

    /**
     * Upload a PDF document to MinIO and return its accessible URL.
     *
     * @param objectKey e.g. "invoices/mission-123/client.pdf"
     * @param pdfBytes  raw PDF byte array
     * @return public URL string
     */
    public Mono<String> store(String objectKey, byte[] pdfBytes) {
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(pdfBytes);
        return minioClient.put()
                .uri("/{bucket}/{key}", bucket, objectKey)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(BodyInserters.fromDataBuffers(Mono.just(buffer)))
                .retrieve()
                .toBodilessEntity()
                .map(response -> minioBaseUrl + "/" + bucket + "/" + objectKey)
                .doOnSuccess(url -> log.info("Stored document at {}", url))
                .doOnError(e -> log.error("Failed to store document {}: {}", objectKey, e.getMessage()))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
