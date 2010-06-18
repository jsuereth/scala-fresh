package dispatch.webdav;


//import org.apache.jackrabbit.webdav.client.methods.{PutMethod => WebDavPut}
import org.apache.http.HttpEntity
import org.apache.http.client.methods._
import org.apache.http.entity._
import java.io.OutputStream
import _root_.dispatch._
import Http._

//import org.apache.commons.httpclient.methods.{RequestEntity, PutMethod => HttpPut, ByteArrayRequestEntity, InputStreamRequestEntity}

object WebDAV {


  implicit def addDAV(request : Request) = new {
    def PUT[T : StreamableBinaryData](data : T) = request.next {
      val put = new HttpPut
      put.setEntity(implicitly[StreamableBinaryData[T]].makeEntity(data))
      Request.mimic(put) _
    }
  }

}


// Typeclass for streamable binary data
trait StreamableBinaryData[T] {
  def makeEntity(data : T) : HttpEntity
}

/** Defines objects that we can bind into a request (PUT or POST). */
object StreamableBinaryData {
  implicit val byteArrayEntity = new StreamableBinaryData[Array[Byte]] {
    override def makeEntity(data : Array[Byte]) : HttpEntity = new ByteArrayEntity(data)
  }
  implicit val inputStreamEntity = new StreamableBinaryData[java.io.InputStream] {
    override def makeEntity(data : java.io.InputStream) : HttpEntity = new InputStreamEntity(data, -1)
  }
  implicit val fileEntity = new StreamableBinaryData[java.io.File] {
    override def makeEntity(data : java.io.File) : HttpEntity = {
      val input = new java.io.FileInputStream(data)
      // SHould be using ARM!!!
      try {
        new InputStreamEntity(input, -1)
      } finally {
        input.close();
      }
    }
  }  
}