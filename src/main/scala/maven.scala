package org.scalatools.fresh

import java.io.FileInputStream

object MavenHelper {

  lazy val userHome = new java.io.File(System.getProperty("user.home"))
  /** Reads the given location for maven settings file and attempts to pull the given credentials from the file */
  def readMavenCredentials(repoName : String = "nexus.scala-tools.org",
                           fileLocation : java.io.File = new java.io.File(userHome, ".m2/settings.xml")) : Option[(String,String)] = {

    import xml._
    if(!fileLocation.exists()) {
      System.err.println("Cannot resolve nexus credentials! File not found: " + fileLocation)
      System.exit(1) // Bringing the Hammer of Death!
    }
    val input = new FileInputStream(fileLocation)
    try {
      val xml = XML.load(input)
      println("Got xml = " + xml)
      val results = for { server <- xml \ "servers" \ "server"
         if ((server \ "id").text) == repoName
      } yield Tuple2((server \ "username").text, (server \ "password").text)
      results.headOption
    } finally {
      input.close() // When will they accept my ARM lib!!!
    }


  }

}