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
                  new BBCopy(tmpDir, commit, fileName).call() // TODO use future
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