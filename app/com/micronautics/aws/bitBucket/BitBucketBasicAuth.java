package com.micronautics.aws.bitBucket;

import com.micronautics.aws.S3;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BitBucketBasicAuth {
    public Exception exception;
    String userid;
    String password;
    S3 s3;

    /**@see https://github.com/fernandezpablo85/scribe-java/wiki/getting-started */
    public BitBucketBasicAuth(S3 s3) {
        userid = System.getenv("userid");
        password = System.getenv("password");
        InputStream inputStream = null;
        try {
            if (userid==null && password==null) {
                inputStream = getClass().getClassLoader().getResourceAsStream("BBCredentials.properties");
                Properties prop = new Properties();
                prop.load(inputStream);
                userid = prop.getProperty("userid");
                password = prop.getProperty("password");
            }
        } catch (Exception ex) {
            exception = ex;
        } finally {
            if (inputStream!=null) try {
                inputStream.close();
            } catch (Exception ignored) {}
        }
        this.s3 = s3;
    }

    /** Return the contents of the file at urlStr as an inputStream */
    public InputStream getInputStream(String urlStr) throws IOException {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(urlStr);
        httpGet.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(userid, password), "UTF-8", false));
        HttpResponse httpResponse = httpClient.execute(httpGet);
        HttpEntity responseEntity = httpResponse.getEntity();
        return responseEntity.getContent();
    }

    /** Return the contents of the file at urlStr as a String */
    public String getUrlAsString(String urlStr) throws IOException {
        return IOUtils.toString(getInputStream(urlStr));
    }

    /** Return the URL that can fetch file contents */
    public String urlStrRaw(String ownerName, String repoName, String fileName) {
      return "https://bitbucket.org/" + ownerName + "/" + repoName + "/raw/master/" + fileName;
    }

    /** Return URL that can fetch metadata about fileName */
    public String urlStrSrc(String ownerName, String repoName, String fileName) {
      return "https://bitbucket.org/" + ownerName + "/" + repoName + "/src/master/" + fileName;
    }

    /**
     * <p><tt>raw</tt> URL with filename  just returns the file contents:<br/>
     * <tt>curl --user user:password https://api.bitbucket.org/1.0/repositories/$owner/$repo/raw/master/$file</tt></p>
     * <p><tt>src</tt> URL without filename returns directory metadata in JSON format:<br/>
     * <tt>curl --user user:password https://api.bitbucket.org/1.0/repositories/$owner/$repo/src/master/$dir</tt> */
    public void copyUrlToAWS(String ownerName, String repoName, String fileName, String bucketName, String key) throws IOException {
        String urlStrIn = urlStrSrc(ownerName, repoName, fileName);
        String payload = getUrlAsString(urlStrIn);
        int filesize = JSON.parseFileSize(payload, urlStrIn);
        String urlRawIn = urlStrRaw(ownerName, repoName, fileName);
        s3.uploadStream(bucketName, key, getInputStream(urlRawIn), filesize);
    }
}
