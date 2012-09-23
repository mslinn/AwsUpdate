package com.micronautics.aws.bitBucket

import java.io.IOException
import java.io.InputStream
import java.util.Properties
import com.micronautics.aws.S3
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.DefaultHttpClient


// TODO this raw port from Java could be made more functional

class BitBucketBasicAuth(val s3: S3) {
    /**@see https://github.com/fernandezpablo85/scribe-java/wiki/getting-started */
    var exception: Exception = null
    var userid: String = System.getenv("userid")
    var password: String = System.getenv("password")
    var inputStream: InputStream = null

    try {
        if (userid==null && password==null) {
            //println("BitBucketBasicAuth: env vars not set, looking for BBCredentials.properties")
            inputStream = getClass().getClassLoader().getResourceAsStream("BBCredentials.properties")
          if (inputStream==null)
              throw new Exception()
            var prop: Properties = new Properties()
            prop.load(inputStream)
            userid   = prop.getProperty("userid")
            password = prop.getProperty("password")
          if (userid==null && password==null)
              throw new Exception()
        }
    } catch {
      case ex: Exception =>
        if (inputStream==null || inputStream.available==0)
          exception = new Exception("Environment variables not defined and BBCredentials.properties not found, so cannot authenticate against BitBucket")
        else
          exception = ex
    } finally {
        if (inputStream!=null) try {
            inputStream.close()
        } catch {
          case _ =>
        }
    }

    /** Return the contents of the file at urlStr as an inputStream */
    def getInputStream(urlStr: String): InputStream = {
        val httpClient = new DefaultHttpClient()
        val httpGet = new HttpGet(urlStr)
        httpGet.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(userid, password), "UTF-8", false))
        val httpResponse: HttpResponse = httpClient.execute(httpGet)
        val responseEntity: HttpEntity = httpResponse.getEntity()
        responseEntity.getContent()
    }

    /** Return the contents of the file at urlStr as a String */
    def getUrlAsString(urlStr: String): String =
        IOUtils.toString(getInputStream(urlStr))

    /** Return the URL that can fetch file contents */
    def urlStrRaw(ownerName: String, repoName: String, fileName: String): String =
      "https://bitbucket.org/" + ownerName + "/" + repoName + "/raw/master/" + fileName

    /** Return URL that can fetch metadata about fileName */
    def urlStrSrc(ownerName: String, repoName: String, fileName: String): String =
      "https://bitbucket.org/" + ownerName + "/" + repoName + "/src/master/" + fileName

    def dirMetadata(ownerName: String, repoName: String, fileName: String) = {
      def url(ownerName: String, repoName: String, dirName: String) =
        "https://api.bitbucket.org/1.0/repositories/" + ownerName + "/" + repoName + "/src/master/" + dirName

	  val contents = getUrlAsString(url(ownerName, repoName, fileName))
	  if (contents.contains("<title>Someone kicked over the bucket, sadface &mdash; Bitbucket</title>")) {
	    val dirName = fileName.substring(0, fileName.lastIndexOf("/"))
	    getUrlAsString(url(ownerName, repoName, dirName))
	  } else
	    contents
    }

    /**
     * <p><tt>raw</tt> URL with filename  just returns the file contents:<br/>
     * <tt>curl --user user:password https://api.bitbucket.org/1.0/repositories/$owner/$repo/raw/master/$file</tt></p>
     * <p><tt>src</tt> URL without filename returns directory metadata in JSON format:<br/>
     * <tt>curl --user user:password https://api.bitbucket.org/1.0/repositories/$owner/$repo/src/master/$dir</tt> */
    def copyUrlToAWS(ownerName: String, repoName: String, fileName: String, bucketName: String, key: String) {
        val urlStrIn: String = urlStrSrc(ownerName, repoName, fileName)
        val payload = getUrlAsString(urlStrIn)
        val filesize = JSON.parseFileSize(payload, urlStrIn)
        val urlRawIn = urlStrRaw(ownerName, repoName, fileName)
        s3.uploadStream(bucketName, key, getInputStream(urlRawIn), filesize)
    }
}
