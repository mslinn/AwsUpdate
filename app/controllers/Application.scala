package controllers

import play.api._
import play.api.mvc._
import java.io.File
import scala.collection.JavaConversions._
import com.micronautics.aws.bitBucket.BBCopy

object Application extends Controller {
  val tmpDir = new File(System.getProperty("java.io.tmpdir"))
  
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
  	            var result = commit.repoName + "\n"
  	            commit.filesToActions.keySet foreach { fileName =>
  	              result += fileName + ": " + commit.filesToActions.get(fileName) + "\n"
                  new BBCopy(tmpDir, commit, fileName).call() // todo use future
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