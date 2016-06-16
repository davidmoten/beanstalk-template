package app.util;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQSClient;

/**
 * Provides clients for Amazon services (like S3 and SQS) from the Sydney
 * region.
 */
public final class Amazon {

    private static final AWSCredentialsProvider CREDENTIALS = new SystemPropertiesCredentialsProvider();

    private static Region region() {
        return Region.getRegion(Regions.AP_SOUTHEAST_2);
    }

    public static AmazonSQSClient sqs(AWSCredentialsProvider awsCredentialsProvider,
            ClientConfiguration cc) {
        return new AmazonSQSClient(awsCredentialsProvider, cc).withRegion(region());
    }

    public static AmazonSQSClient sqs(AWSCredentialsProvider awsCredentialsProvider) {
        return sqs(awsCredentialsProvider, createDefaultClientConfiguration());
    }

    public static AmazonSQSClient sqs() {
        return sqs(CREDENTIALS);
    }

    public static AmazonS3Client s3(AWSCredentialsProvider awsCredentialsProvider,
            ClientConfiguration cc) {
        return new AmazonS3Client(awsCredentialsProvider, cc).withRegion(region());
    }

    public static AmazonS3Client s3(AWSCredentialsProvider awsCredentialsProvider) {
        return s3(awsCredentialsProvider, createDefaultClientConfiguration());
    }

    public static AmazonS3Client s3() {
        return s3(CREDENTIALS);
    }

    public static ClientConfiguration createDefaultClientConfiguration() {
        // will setup proxy based on system properties (https.proxyHost, etc.)
        return new ClientConfiguration();
    }

    public static ClientConfiguration proxy(String host, int port) {
        return new ClientConfiguration().withProxyHost(host).withProxyPort(port);
    }

}
