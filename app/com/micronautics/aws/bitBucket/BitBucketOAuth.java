package com.micronautics.aws.bitBucket;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Abandoned because BitBucket does not yet support OAuth for file access.
 * Might revisit when BB gets this together. */
public class BitBucketOAuth {
    public Exception exception;
    String accessKey;
    String secretKey;
    OAuthService oAuthService;

    /**@see https://github.com/fernandezpablo85/scribe-java/wiki/getting-started */
    public BitBucketOAuth() throws IOException {
        accessKey = System.getenv("bbAccessKey");
        secretKey = System.getenv("bbSecretKey");
        try {
            if (accessKey==null && secretKey==null) {
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("BBCredentials.properties");
                Properties prop = new Properties();
                prop.load(inputStream);
                accessKey = prop.getProperty("accessKey");
                secretKey = prop.getProperty("secretKey");
            }

            oAuthService = new ServiceBuilder()
                           .provider(BitBucketApi.class)
                           .apiKey(accessKey)
                           .apiSecret(secretKey)
                           .debug() // comment this out when it works
                           .build();
        } catch (Exception ex) {
            exception = ex;
        }
    }

    public Response request(String urlStr) {
        OAuthRequest request = new OAuthRequest(Verb.POST, urlStr);
        request.addHeader("referer", "http://slinnbooks.com/");
        oAuthService.signRequest(new Token("", ""), request);
        Response response = request.send();
        System.out.println(response.getBody());
        return response;
    }
}
