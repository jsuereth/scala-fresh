package org.scalatools.fresh

import java.io.File
import dispatch._
import Http._

object ResolveConfig {
  private val url_prefix = "http://nexus.scala-tools.org/content/sites/scala-fresh/"  

  private val resourceMap = Map("sbt.boot.properties" -> "sbt/sbt.boot.properties")


  def resolve_artifacts(buildName : String, outputDirectory : File) : Unit = {
    val http = new Http
    // Ensures a parent directory is available to place a file inside.
    def ensureParentExists(file : File) = {
      val parent = file.getParentFile
      if(!parent.exists) {
        parent.mkdirs()
      }
      file
    }
    // Resolves a relative url into a given path.
    def resolveFile(url : String, path : File) {
      val outputStream = new java.io.FileOutputStream(ensureParentExists(path));
      try {
        http( url_prefix / buildName / url >>> outputStream )
      } finally {
        outputStream.close()
      }
    }
    for( (url, relativeLocation) <- resourceMap) resolveFile(url, new File(outputDirectory, relativeLocation))
  }
}