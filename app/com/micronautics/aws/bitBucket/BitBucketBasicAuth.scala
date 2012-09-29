package com.micronautics.aws.bitBucket

import BitBucketBasicAuth._
import com.codahale.jerkson.Json._
import com.micronautics.aws.S3
import java.io.InputStream
import java.util.{ArrayList, LinkedHashMap, Properties}
import org.apache.commons.io.IOUtils
import org.apache.http.{HttpEntity, HttpResponse}
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.DefaultHttpClient
import scala.collection.JavaConversions._


// TODO this raw port from Java could be made more functional

object BitBucketBasicAuth {
  /** Return the URL that can fetch file contents
    * @param path can be a filename or a directory path.
    *             If `path` is empty or ends with `/` the request is interpreted as a directory request and returns a list.
    * @see https://confluence.atlassian.com/display/BITBUCKET/Using+the+bitbucket+REST+APIs
    * @return <pre>{
        "node": "562344e0ae10",
        "path": "/",
        "directories": [
            "images",
            "javascripts",
            "stylesheets"
        ],
        "files": [
            {
                "size": 577,
                "path": "Readme",
                "timestamp": "2012-06-13 22:07:20",
                "utctimestamp": "2012-06-13 22:07:20+00:00",
                "revision": "4dd1b211d689"
            },
            {
                "size": 144855,
                "path": "index.html",
                "timestamp": "2012-08-12 02:06:43",
                "utctimestamp": "2012-08-12 02:06:43+00:00",
                "revision": "562344e0ae10"
            } ...
        ]
    }</pre> */
  def urlStrNode(ownerName: String, repoName: String, path: String): String =
    "https://api.bitbucket.org/1.0/repositories/" + ownerName.toLowerCase + "/" + repoName.toLowerCase + "/src/master/" + path

  def urlStrRaw(ownerName: String, repoName: String, path: String): String =
    "https://bitbucket.org/" + ownerName.toLowerCase + "/" + repoName.toLowerCase + "/raw/master/" + path

  /** Return URL that can fetch metadata about fileName
    * @see https://confluence.atlassian.com/display/BITBUCKET/Using+the+bitbucket+REST+APIs */
  def urlStrSrc(ownerName: String, repoName: String, fileName: String): String =
    "https://bitbucket.org/" + ownerName.toLowerCase + "/" + repoName.toLowerCase + "/src/master/" + fileName

  /** Return URL that can fetch metadata about fileName
    * This doc is WRONG: https://confluence.atlassian.com/display/BITBUCKET/Using+the+bitbucket+REST+APIs
    * @return <pre>{
        "repositories": [
            {
                "scm": "git",
                "has_wiki": true,
                "last_updated": "2012-09-23 05:00:05",
                "creator": null,
                "created_on": "2012-09-23 05:00:05",
                "owner": "mslinn",
                "logo": null,
                "email_mailinglist": "",
                "is_mq": false,
                "size": 580,
                "read_only": false,
                "fork_of": null,
                "mq_of": null,
                "followers_count": 1,
                "state": "available",
                "utc_created_on": "2012-09-23 03:00:05+00:00",
                "website": "",
                "description": "",
                "has_issues": true,
                "is_fork": false,
                "slug": "www.slinnbooks.com",
                "is_private": true,
                "name": "www.slinnbooks.com",
                "language": "",
                "utc_last_updated": "2012-09-23 03:00:05+00:00",
                "email_writers": true,
                "no_public_forks": false,
                "resource_uri": "/1.0/repositories/mslinn/www.slinnbooks.com"
            }, ...
        ],
        "user": {
            "username": "mslinn",
            "first_name": "Michael",
            "last_name": "Slinn",
            "is_team": false,
            "avatar": "https://secure.gravatar.com/avatar/d1f530945b209174d116ed37dc123a62?d=identicon&s=32",
            "resource_uri": "/1.0/users/mslinn"
        }</pre> */
  def urlRepositories(ownerName: String): String = "https://api.bitbucket.org/1.0/users/" + ownerName.toLowerCase
}

class BitBucketBasicAuth(val s3: S3) {
  /**@see https://github.com/fernandezpablo85/scribe-java/wiki/getting-started */
  var exception: Exception = null
  var userid: String = System.getenv("bbUserId")
  var password: String = System.getenv("bbPassword")
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

  /**
    * <p><tt>raw</tt> URL with filename  just returns the file contents:<br/>
    * <tt>curl --user user:password https://api.bitbucket.org/1.0/repositories/$owner/$repo/raw/master/$file</tt></p>
    * <p><tt>src</tt> URL without filename returns directory metadata in JSON format:<br/>
    * <tt>curl --user user:password https://api.bitbucket.org/1.0/repositories/$owner/$repo/src/master/$dir</tt> */
  def copyUrlToAWS(ownerName: String, repoName: String, fileName: String, bucketName: String, key: String) {
      val urlStrIn: String = urlStrSrc(ownerName, repoName, fileName)
      val payload = getUrlAsString(urlStrIn)
      val filesize = JSON.parseFileSize(payload, urlStrIn)
      val urlRawIn = urlStrNode(ownerName, repoName, fileName)
      s3.uploadStream(bucketName, key, getInputStream(urlRawIn), filesize)
  }

  /** @return directory metadata in JSON format or "Not Found"
    * @see https://confluence.atlassian.com/display/BITBUCKET/Using+the+bitbucket+REST+APIs */
  def dirMetadata(ownerName: String, repoName: String, fileName: String) = {
    def url(ownerName: String, repoName: String, dirName: String) =
      "https://api.bitbucket.org/1.0/repositories/" + ownerName.toLowerCase + "/" + repoName.toLowerCase + "/src/master/" + dirName

    val dirName = fileName.substring(0, if (fileName.contains("/")) fileName.lastIndexOf("/") else 0)
    val theUrl = url(ownerName, repoName, dirName)
    println("Fetching directory metadata from " + theUrl)
    val contents = getUrlAsString(theUrl)
//      if (contents.contains("<title>Someone kicked over the bucket, sadface &mdash; Bitbucket</title>")) {
//        getUrlAsString(theUrl)
//      } else
    if (null==contents) "Not Found" else contents
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
  def getUrlAsString(urlStr: String): String = IOUtils.toString(getInputStream(urlStr))

  def repoExists(repoName: String): Boolean = {
    val metadata = dirMetadata(userid, repoName, "")
    null != metadata && metadata != "Not Found"
  }

  def repositoryObjects(ownerName: String): List[BBRepository] = {
    val jsonStr: String = getUrlAsString(urlRepositories(ownerName))
    val json: LinkedHashMap[String, Any] = parse(jsonStr)
    if (json.size()==2) {
      val reposJson = json.get("repositories").asInstanceOf[ArrayList[LinkedHashMap[String, Any]]]
      reposJson.toList map { (x: LinkedHashMap[String, Any]) => BBRepository(x) }
    } else // badly formed JSON; perhaps invalid user name? TODO look into error handling
      Nil
  }
}
