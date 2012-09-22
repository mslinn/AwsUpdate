package com.micronautics.aws.bitBucket

import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.map.ObjectMapper
import scala.collection.JavaConversions._

import java.io.IOException

// TODO this raw port from Java could be made more functional

/**
  * payload looks like:
  * <pre>{"repository": {"website": "https://github.com/mslinn/AwsUpdate",
  *                       "fork": false,
  *                       "name": "AwsUpdateTest",
  *                       "scm": "git",
  *                       "absolute_url": "/mslinn/awsupdatetest/",
  *                       "owner": "mslinn",
  *                       "slug": "awsupdatetest",
  *                       "is_private": false},
  *                       "commits": [{"node": "64d92b2400cc",
  *                       "files": [{"type": "modified", "file": "empty.html"}],
  *                       "branch": "master",
  *                       "utctimestamp": "2012-09-22 16:05:36+00:00",
  *                       "author": "mslinn",
  *                       "timestamp": "2012-09-22 18:05:36",
  *                       "raw_node": "64d92b2400cc4798e77802a4f60dc62871b89cc7",
  *                       "parents": ["fd2a0f821000"],
  *                       "raw_author": "Mike Slinn <mslinn@mslinn.com>",
  *                       "message": "testing\n",
  *                       "size": -1,
  *                       "revision": null}],
  *                       "canon_url": "https://bitbucket.org",
  *                       "user": "mslinn"}</pre>
  */
object JSON {

    def parseCommit(payload: String): Commit = {
        var commit = new Commit()
        if (payload==null || payload.length()==0)
            return commit

        val mapper = new ObjectMapper()
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        var rootNode: JsonNode = try {
            mapper.readValue(payload, classOf[JsonNode])
        } catch {
          case e: IOException =>
        	System.err.println(e.getMessage())
            return commit
            null
        }

        val repositoryNode: JsonNode = rootNode.path("repository")
        commit = Commit(repositoryNode.path("repoName").getTextValue(),
                        repositoryNode.path("owner").getTextValue())

        val commitsNode  = rootNode.path("commits")
        commitsNode.getElements foreach { commitNode =>
            val filesNode: JsonNode = commitNode.path("filesToActions")
            filesNode foreach { fileNode: JsonNode =>
                val fileName   = fileNode.path("file").getTextValue()
                val fileAction = fileNode.path("type").getTextValue() // Possible types are: added, modified, removed
                commit.filesToActions.put(fileName, fileAction)
            }
        }
        commit
    }

    /** Search payload for size of specified file.
     * <p><tt>raw</tt> URL with filename  just returns the file contents:<br/>
     * <tt>curl --user user:password https://api.bitbucket.org/1.0/repositories/$owner/$repo/raw/master/$file</tt></p>
     * <p><tt>src</tt> URL without filename returns directory metadata in JSON format:<br/>
     * <tt>curl --user user:password https://api.bitbucket.org/1.0/repositories/$owner/$repo/src/master/$dir</tt>
     * Returns:</p>
     * <pre>{
    "node": "b51cd557430b",
    "path": "books/futures/",
    "directories": [
        "_notes",
        "images",
        "presentations",
        "private",
        "videos"
    ],
    "files": [
        {
            "size": 9030,
            "path": "books/futures/changes.jsp",
            "timestamp": "2012-05-27 09:19:01",
            "utctimestamp": "2012-05-27 09:19:01+00:00",
            "revision": "bbea1bb40bce"
        },
       {
            "size": 6828,
            "path": "books/futures/toc.jsp",
            "timestamp": "2012-05-27 09:19:01",
            "utctimestamp": "2012-05-27 09:19:01+00:00",
            "revision": "bbea1bb40bce"
        }
    ]
}</pre>

<p>Used to return:</p>
     * <pre>{
         "node": "b51cd557430b",
         "path": "dir1/",
         "directories": [
             "dir2",
             "dir3"
         ],
         "filesToActions": [
             {
                 "size": 68081,
                 "path": "dir1/blah blah blah.pdf",
                 "timestamp": "2012-05-27 09:19:01",
                 "utctimestamp": "2012-05-27 09:19:01+00:00",
                 "revision": "bbea1bb40bce"
             },
             {
                 "size": 12498,
                 "path": "dir1/blah.properties",
                 "timestamp": "2012-05-27 09:19:01",
                 "utctimestamp": "2012-05-27 09:19:01+00:00",
                 "revision": "bbea1bb40bce"
             }
         ]</pre>
     <p>Note that the directory is included as a prefix for each file</p> */
    def parseFileSize(payload: String, fileName: String): Int = {
        val mapper = new ObjectMapper()
        var rootNode: JsonNode = null
        try {
            rootNode = mapper.readValue(payload, classOf[JsonNode])
        } catch {
          case e: IOException =>
            System.err.println(e.getMessage())
            return 0
        }

        val path  = rootNode.path("path").getTextValue()
        val filesNode = rootNode.path("files") // was filesToActions, not files for directory info; is this field named differently when a commit is pushed?
        filesNode foreach { fileNode =>
            val filePath = fileNode.path("path").getTextValue()
            if (fileName.compareTo(filePath)==0)
                return fileNode.path("size").getIntValue()
        }
        0
    }
}
