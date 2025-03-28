package io.github.zodh.processor.infrastructure.configuration;

import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.net.URI;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsVideoServiceConfig {

  @Setter
  @Value("${spring.cloud.aws.region.static}")
  private String region;

  @Setter
  @Value("${spring.cloud.aws.credentials.access-key}")
  private String accessKey;

  @Setter
  @Value("${spring.cloud.aws.credentials.secret-key}")
  private String secretKey;

  @Setter
  @Value("${spring.cloud.aws.credentials.session.token}")
  private String sessionToken;

  @Value("${video.status.update.queue-url}")
  public String queueUrl;

  @Bean
  public S3Client getClient() {
    AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(
        AwsSessionCredentials.create(accessKey, secretKey, sessionToken));
    return S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(awsCredentialsProvider)
        .build();
  }

  @Bean
  public S3Presigner getPreSigner() {
    AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(
        AwsSessionCredentials.create(accessKey, secretKey, sessionToken));
    return S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(awsCredentialsProvider)
        .s3Client(getClient())
        .build();
  }

  @Bean
  public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
    return SqsTemplate.builder()
        .sqsAsyncClient(sqsAsyncClient)
        .configure(o -> o.queueNotFoundStrategy(QueueNotFoundStrategy.FAIL))
        .build();
  }

  @Bean
  public SqsAsyncClient sqsAsyncClient() {
    AwsSessionCredentials credentials = AwsSessionCredentials.create(accessKey, secretKey,
        sessionToken);
    return SqsAsyncClient.builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .region(Region.of(region))
        .endpointOverride(URI.create(queueUrl))
        .build();
  }

  @Bean
  public SqsClient getSqsClient() {
    AwsSessionCredentials credentials = AwsSessionCredentials.create(accessKey, secretKey,
        sessionToken);
    return SqsClient.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .build();
  }

}
