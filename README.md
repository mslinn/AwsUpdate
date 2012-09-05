# AwsUpdate #

This project was sponsored by [Micronautics Research Corporation](http://www.micronauticsresearch.com/)

## To Build ##

This project uses code from the [AwsMirror](https://github.com/mslinn/AwsMirror/) project, 
which provides a command-line program written in Scala and Java. `AwsMirror` requires Java 7, therefore `AwsUpdate` also requires Java 7.
In order for Play to access the `AwsMirror` project, you need to `sbt publish-local`, then create a symlink from the locally published project to Play:

    git clone git://github.com/mslinn/AwsMirror.git
    cd AwsMirror
    sbt publish-local
    ln -s ~/.ivy2/local/com.micronautics/ /opt/play-2.0.3/repository/local/com.micronautics
    git clone git://github.com/mslinn/AwsUpdate.git
    play
