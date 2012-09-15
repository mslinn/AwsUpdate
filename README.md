# AwsUpdate #

Responds to a git post-receive hook installed in BitBucket or GitHub to notify a Heroku app that files were changed, 
and then updates an S3 bucket with the modified files. `AwsUpdate` runs under Play 2 and requires Java 7.

This project is sponsored by [Micronautics Research Corporation](http://www.micronauticsresearch.com/)

## To Build ##

This project uses code from the [AwsMirror](https://github.com/mslinn/AwsMirror/) project, 
which provides a command-line program written in Scala and Java. `AwsMirror` requires Java 7, therefore 
`AwsUpdate` also requires Play 2 to run under Java 7.
In order for Play to access the `AwsMirror` project, you need to `sbt publish-local`, 
then create a symlink from the locally published project to Play:

    git clone git://github.com/mslinn/AwsMirror.git
    cd AwsMirror
    sbt publish-local
    cd ..
    git clone git://github.com/mslinn/AwsUpdate.git

For Linux and Mac, create a symlink:

    export PLAY_HOME ~/play-2.0.3
    ln -s ~/.ivy2/local/com.micronautics/ $PLAY_HOME/repository/local/com.micronautics

For Windows (even Cygwin) you must use [Sysinternals](http://technet.microsoft.com/en-us/sysinternals/bb842062) `junction` command:

    setx PLAY_HOME "C:\play-2.0.3"
    junction "%PLAY_HOME%/repository/local/com.micronautics" "%HOMEDRIVE%%HOMEPATH%/.ivy2/local/com.micronautics"

For all OSes, run `AwsUpdate` locally as follows:

    cd AwsUpdate
    play

## To Run ##

    git clone git@github.com:mslinn/AwsUpdate.git
    git remote add heroku git@heroku.com:smooth-stone-1114.git # use your Heroku app name here

Define two environment variables to hold your AWS access key and your AWS secret key:

    heroku config:add accessKey=34poslkflskeflsekjfl
    heroku config:add secretKey=asdfoif3r3wfw3wgagawgawgawgw3taw3tatefef

If you want to access a private repository on BitBucket, use basic auth credentials to
define two more environment variables:

    heroku config:add bbUserId=34poslkflskeflsekjfl
    heroku config:add bbPassword=asdfoif3r3wfw3wgagawgawgawgw3taw3tatefef

Deploy the project to Heroku:

    git push heroku master

The web app should now be up and running on Heroku. Open it in your browser with:

    heroku open

## Git Post-Receive Service Hooks ##

A Play route is dedicated to receiving updates from each remote git service (GitHub or BitBucket).
The associated controller will perform the following when complete:

 1. Accept a POST in JSON format from the remote git service describing the commit.
 2. Verify the POST to be a result of a valid commit.
 3. Read each of the committed files and store into a temporary directory.
 4. Push content files to AWS S3.

I had written a streaming copy utility from the Git repository to AWS S3 using NIO, but later discovered that AWS S3
needs to know the file size prior to initiating a transfer.
The only way I could discover the file size was to store each file locally :(
Let's hope the temporary disk space accessible from Heroku is big enough. 2GB per file would be ideal.
Not sure how many threads are available to the Heroku instance; I want to allocate as many threads as possible and
transfer files in parallel.

### GitHub WebHook URLs Hook ###
The Play 2 controller has not yet been written.

The GitHub WebHook URLs(0) service is what we need.
Go to Admin / Service Hooks and pick the first entry, then enter the URL to POST to.

The service description says:
"We’ll hit these URLs with POST requests when you push to us, passing along information about the push.
More information can be found in the [Post-Receive Guide](http://help.github.com/post-receive-hooks/).
The Public IP addresses for these hooks are: 207.97.227.253, 50.57.128.197, 108.171.174.178."

FYI, GitHub's [service hooks](https://github.com/mslinn/HerokuTomcatAwsS3/admin/hooks) are open source, written in Ruby.
They include user-written hooks into the public list.
Docs are [here](https://github.com/github/github-services).

### BitBucket POST Service ###
The Play 2 controller has not yet been written.

Each time files are pushed to BitBucket, a POST can originate from the repo and can go a designated URL.
For the details on the services included with Bitbucket, check out [BitBucket services](https://confluence.atlassian.com/display/BITBUCKET/Managing+bitbucket+Services).
This Heroku app works with the [POST service](https://confluence.atlassian.com/display/BITBUCKET/Setting+Up+the+bitbucket+POST+Service).
Basic authentication doesn't work for some of direct file routes; an internal ticket has been opened.
BitBucket's OAuth does not yet support authenticating against private git repositories.
