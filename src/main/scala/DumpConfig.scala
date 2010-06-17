package org.scalatools.fresh

import java.io.File
import dispatch._
import Http._

object DumpConfig {
  private val url_prefix = "http://nexus.scala-tools.org/content/sites/scala-fresh/"
  
  // Lame method to dump SBT Ouptut!
  def makeConfig(scalaVersion : String, staging_repo_url : String) = {
     """[scala]
  version: """ + scalaVersion + """
# classifiers: sources

[app]
  org: org.scala-tools.sbt
  name: sbt
  version: read(sbt.version)
  class: sbt.xMain
  components: xsbti
  cross-versioned: true

[repositories]
  local
  maven-local
  scala-fresh-current: """ + staging_repo_url + """
  scala-fresh-staging: http://nexus.scala-tools.org/service/local/staging/deploy/maven2/
  sbt-db: http://databinder.net/repo/, [organization]/[module]/[revision]/[type]s/[artifact](-[classifier]).[ext]
  maven-central
  scala-tools-releases
  scala-tools-snapshots

[boot]
 directory: project/boot
 properties: project/build.properties
 prompt-create: Project does not exist, create new project?
 prompt-fill: true
 quick-option: true

[log]
 level: info

[app-properties]
 project.name: quick=set(test), new=prompt(Name), fill=prompt(Name)
 project.organization: new=prompt(Organization)
 project.version: quick=set(1.0), new=prompt(Version)[1.0], fill=prompt(Version)[1.0]
 def.scala.version: quick=set(""" + scalaVersion + """), new=set(""" + scalaVersion + """), fill=set(""" + scalaVersion + """)
 build.scala.versions: quick=set(2.7.7), new=prompt(Scala version)[2.7.7], fill=prompt(Scala version)[2.7.7]
 sbt.version: quick=set(0.7.2), new=prompt(sbt version)[0.7.2], fill=prompt(sbt version)[0.7.2]
 project.scratch: quick=set(true)
 project.initialize: quick=set(true), new=set(true)
"""
  }
  def dumpToUrl(buildName : String, fileName : String, contents : String) = {
    import dispatch._
    import Http._
    val http = new Http
    val req = :/("nexus.scala-tools.org") / "content/sites/scala-fresh" / buildName / fileName
    val post = req << contents
    val rauth = post as ("hudson", "TODO - Pull auth from hudson auth file in home directory!")
    http(rauth >>> System.out)
  }
}