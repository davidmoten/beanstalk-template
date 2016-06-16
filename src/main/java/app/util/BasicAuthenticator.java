package app.util;

import java.io.IOException;
import java.util.Properties;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import com.amazonaws.services.s3.AmazonS3Client;
import com.github.davidmoten.servlet.security.BasicAuthentication;
import com.google.common.base.Preconditions;

public final class BasicAuthenticator {

    public static boolean isAuthorized(HttpServletRequest req, AmazonS3Client s3, String bucket,
            Supplier<String> passwordHashKey) {
        BasicAuthentication auth = BasicAuthentication.from(req);
        return com.github.davidmoten.servlet.security.AwsS3Authentication
                .isAuthorized(auth.username(), auth.password(), s3, bucket, passwordHashKey);
    }

    public static boolean isAuthorized(HttpServletRequest req, AmazonS3Client s3, String bucket) {
        return isAuthorized(req, s3, bucket, () -> getPasswordHashKeyFromApplicationProperties());
    }

    public static String getPasswordHashKeyFromApplicationProperties() {
        Properties p = new Properties();
        try {
            p.load(BasicAuthenticator.class.getResourceAsStream("/application.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String result = p.getProperty("password.hash.key");
        Preconditions.checkNotNull(result);
        return result;
    }

}
