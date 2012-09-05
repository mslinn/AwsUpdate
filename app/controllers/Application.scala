package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
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
  	            val result = commit.repoName + "\n" + commit.ownerName + "\n"
	            Ok(views.html.index("Got commit from BitBucket repo :" + result))
	          }

	        case _ =>
	          BadRequest("payload list was empty")
	      }

	    case None =>
	      BadRequest("No POST data found")
	  }
  }
}