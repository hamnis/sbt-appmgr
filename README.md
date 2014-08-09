sbt-appmgr
==========

Packages a distribution as an appmgr [app](https://trygvis.io/projects/appmgr/app.html).

## Requirements
 sbt 0.13.x

## Installation

Add the following lines to PROJECT_DIR/project/plugin.sbt

```scala
addSbtPlugin("net.hamnaberg.sbt" % "sbt-appmgr" % "0.3.1")
```

This is what an application zip archive looks like:

```
/app.config
/root
/root/bin
/root/bin/my-app
/root/lib/
/hooks/
/hooks/post-install
```
