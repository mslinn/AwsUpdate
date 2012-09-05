package com.micronautics.aws.bitBucket;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

/** Abandoned because BitBucket does not yet support OAuth for file access.
 * Might revisit when BB gets this together. */
public class BitBucketApi extends DefaultApi10a {
    // oauth_token is bogus, but there is probably a parameter that gets passed in here
    private static final String AUTHORIZE_URL = "https://bitbucket.org/api/1.0/oauth/authenticate/";
    private static final String REQUEST_TOKEN_RESOURCE = "https://bitbucket.org/api/1.0/oauth/request_token/";
    private static final String ACCESS_TOKEN_RESOURCE = "https://bitbucket.org/api/1.0/oauth/access_token/";

    @Override
    public String getAccessTokenEndpoint() {
        return ACCESS_TOKEN_RESOURCE;
    }

    @Override
    public String getRequestTokenEndpoint() {
        return REQUEST_TOKEN_RESOURCE;
    }

    @Override
    public String getAuthorizationUrl(Token requestToken) {
        return AUTHORIZE_URL;
    }

    public static class SSL extends BitBucketApi {
        @Override
        public String getAccessTokenEndpoint() {
            return ACCESS_TOKEN_RESOURCE;
        }

        @Override
        public String getRequestTokenEndpoint() {
            return REQUEST_TOKEN_RESOURCE;
        }
    }

    /**
     * BitBucketOAuth 'friendlier' authorization endpoint for OAuth.
     * <p/>
     * Uses SSL.
     */
    public static class Authenticate extends SSL {
        private static final String AUTHENTICATE_URL = "https://bitbucket.org/api/1.0/oauth/authenticate/";

        @Override
        public String getAuthorizationUrl(Token requestToken) {
            return AUTHENTICATE_URL;
        }
    }

    /**
     * Just an alias to the default (SSL) authorization endpoint.
     * <p/>
     * Need to include this for symmetry with 'Authenticate' only.
     */
    public static class Authorize extends SSL {
    }
}
