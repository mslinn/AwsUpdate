# AwsUpdate #

Responds to a git post-receive hook installed in BitBucket or GitHub to notify a Heroku app that files were changed, and then updates an S3 bucket with the modified files. `AwsUpdate` runs under Play 2 and requires Java 7.

This project was sponsored by [Micronautics Research Corporation](http://www.micronauticsresearch.com/)

## To Build ##

This project uses code from the [AwsMirror](https://github.com/mslinn/AwsMirror/) project, 
which provides a command-line program written in Scala and Java. `AwsMirror` requires Java 7, therefore `AwsUpdate` also requires Play 2 to run under Java 7.
In order for Play to access the `AwsMirror` project, you need to `sbt publish-local`, then create a symlink from the locally published project to Play:

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
