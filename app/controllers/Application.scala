package controllers

import play.api._
import play.api.mvc._
import java.io.File
import scala.collection.JavaConversions._
import com.micronautics.aws.bitBucket.BBCopy
import com.micronautics.aws.S3File
import scala.collection.mutable

object Application extends Controller {
  lazy val tmpDir = new File(System.getProperty("java.io.tmpdir")) // TODO use /var/tmp on Linux instead?
  val s3Files = mutable.Map.empty[String, S3File]

  def index = Action {
    Ok(views.html.index("AwsUpdate is ready."))
  }

  /** data looks like:
    * <pre>Map(payload -> List({"repository": {"website": "https://github.com/mslinn/AwsUpdate", "fork": false, "name": "AwsUpdateTest", "scm": "git", "absolute_url": "/mslinn/awsupdatetest/", "owner": "mslinn", "slug": "awsupdatetest", "is_private": false}, "commits": [{"node": "64d92b2400cc", "files": [{"type": "modified", "file": "empty.html"}], "branch": "master", "utctimestamp": "2012-09-22 16:05:36+00:00", "author": "mslinn", "timestamp": "2012-09-22 18:05:36", "raw_node": "64d92b2400cc4798e77802a4f60dc62871b89cc7", "parents": ["fd2a0f821000"], "raw_author": "Mike Slinn <mslinn@mslinn.com>", "message": "testing\n", "size": -1, "revision": null}], "canon_url": "https://bitbucket.org", "user": "mslinn"}))</pre>
    */
  def acceptBB = Action { implicit request =>
	  request.body.asFormUrlEncoded match {
	    case Some(data) =>
	      data.get("payload") match {
	      	case Some(payloadList) =>
	      	  if (payloadList.size!=1) {
	      	    BadRequest("payload list length was " + payloadList.size)
              } else {
  	            val payload = payloadList.head
  	            val commit = com.micronautics.aws.bitBucket.JSON.parseCommit(payload)
  	            val s3 = commit.repoName
  	            var result = commit.repoName + "\n"
  	            commit.filesToActions.keySet foreach { fileName =>
  	              result += fileName + ": " + commit.filesToActions.get(fileName) + "\n"
                  try {
                    val bbCopy = BBCopy(tmpDir, commit, fileName)
                    bbCopy.call() // TODO use future
                  } catch {
                    case ex =>
                      Console.err.println(ex.getMessage)
                      PreconditionFailed(ex.getMessage)
                  }
  	            }
	            Ok(views.html.index("Got commit from BitBucket repo: " + result))
	          }

	        case _ =>
	          BadRequest("payload list was empty")
	      }

	    case None =>
	      BadRequest("No POST data found")
	  }
  }
}