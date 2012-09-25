package com.micronautics.aws.bitBucket

import java.util.{LinkedHashMap, Date}
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.DateTimeFormat

/**
  * @author Mike Slinn
  */

object BBRepository {
  private val fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  /** @param json <pre>            {
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
              }</pre> */
  def apply(json: LinkedHashMap[String, Any]): BBRepository = {
    val name = json.get("name").asInstanceOf[String]
    val lastUpdated = fmt.parseDateTime(json.get("last_updated").asInstanceOf[String]) // what time zone is this in?
    //val lastUpdated = new DateTime(DateTimeZone.getDefault.convertUTCToLocal(dateTime.getMillis))
    val owner = json.get("owner").asInstanceOf[String]
    val website = json.get("website").asInstanceOf[String]
    val isPrivate = json.get("is_private").asInstanceOf[Boolean]
    new BBRepository(name, lastUpdated, owner, website, isPrivate)
  }
}

class BBRepository(val repoName: String,
                   val lastUpdated: DateTime,
                   val owner: String,
                   val website: String,
                   val isPrivate: Boolean) extends Ordered[BBRepository] {
  def compare(that: BBRepository) = this.repoName compare that.repoName
}
