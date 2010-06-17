import sbt._


// Am I fresh enough for you!
class FreshProject(info : ProjectInfo) extends DefaultProject(info) {
  val dispatch = "net.databinder" %% "dispatch-http" % "0.7.4"
  val slf4j_nop = "org.slf4j" % "slf4j-nop" % "1.6.0"
  val jackrabbit_webdav = "org.apache.jackrabbit" % "jackrabbit-webdav" % "2.1.0" 
}