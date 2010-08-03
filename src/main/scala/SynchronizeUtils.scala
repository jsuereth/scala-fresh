package synchronize

import dispatch.Http
import dispatch.Request
import org.apache.http.client.methods.{HttpPut, HttpRequestBase}
import org.apache.http.entity.InputStreamEntity
import org.apache.http.params.HttpProtocolParams
import java.io.{InputStream,OutputStream,File,FileInputStream}

/**
 * Type trait representing an item that has a name.
 */
trait NamedItem[T] {
  def name(item : T) : String
  def debugName(item : T) : String
}

/**
 * Defines the interface for reading from a particular location.
 */
trait SynchronizeFromItem[T] extends NamedItem[T] {
  /** Returns whether or not this item is a directory (i.e. has children) */
  def isDirectory(item : T) : Boolean
  /** Returns all the children of this item */
  def children(item : T) : Seq[T]
  /** Returns the contents of this item as a java.io.InputStream of bytes
   * Also includes the length and mime type. 
   */
  def content(item : T) : InputStream
}

object SynchronizeFromItem {
  implicit val file = JavaFileLike
}

object SynchronizeToItem {
  implicit val file = JavaFileLike
  implicit val request = WebDavSynchronizeTo
}

/**
 * This type trait defines the interface for locations that can be synchronized to.
 */
trait SynchronizeToItem[T] extends NamedItem[T] {
  /**
   * Constructs a (possible "null-object") reference to a child item using a parent item.
   */
  def child(parent : T, name : String) : T
  /** Creates all directories needed for this reference to be a directory */
  def mkdirs(item : T) : Unit
  /** Writes the content provided by an input stream to this item */
  def writeContent(item : T, otherContent : InputStream) : Unit
}

/**
 * Defines an interface for things that are like files for our synchronization code.
 */
trait FileLike[T] extends SynchronizeFromItem[T] with SynchronizeToItem[T]

/** Implements FileLike for java.io.File */
object JavaFileLike extends FileLike[File] {
  override def name(file : File) = file.getName()
  override def debugName(file : File) = file.getAbsolutePath
  override def isDirectory(file : File) = file.isDirectory()
  override def children(directory : File) = directory.listFiles() //TODO - Lift null
  override def child(parent : File, name : String) = new java.io.File(parent, name)
  override def mkdirs(file : File) : Unit = file.mkdirs()
  override def content(file : File) = {
    //val parser = new net.sf.jmimemagic.Magic()
    //val mimeType = net.sf.jmimemagic.Magic.getMagicMatch(file, true).getMimeType
    new FileInputStream(file) //, file.length, mimeType)
  }
  override def writeContent(file : File, otherContent : InputStream) = {
    // TODO - Auto close input stream? yes...
    val bufferedOutput = new java.io.BufferedOutputStream(new java.io.FileOutputStream(file))
    StreamUtil.writeStream(otherContent, bufferedOutput)
  }
}

object StreamUtil {
  def writeStream(input : InputStream, output : OutputStream) : Unit = {
     try {
      val bufferedInput = new java.io.BufferedInputStream(input)
      val buffer = new Array[Byte](512)
      var ready : Int = 0
      ready = bufferedInput.read(buffer)
      while(ready != -1) {
        if(ready > 0) {
          output.write(buffer, 0, ready)
        }
        ready = bufferedInput.read(buffer)
      }
    } finally {
      input.close()
      output.close()
    }
  }
}

// Synchronize implementation for dispatch requests and webdav-ish stuff.
object WebDavSynchronizeTo extends SynchronizeToItem[Request] {
  import Http._
  override def name(request : Request) = {
    val path = request.to_uri.getPath
    path.lastIndexOf('/') match {
      case -1 => path
      case x => path.substring(x+1)
    }
  }
  override def debugName(request : Request) = request.to_uri.toASCIIString
  override def child(parent : Request, name : String) = parent / name
  override def mkdirs(directory : Request) = () // We think we can get away with this
  override def writeContent(request : Request, otherContent : InputStream) = {
    // Write entire file to a buffer -> TODO - this is kind of horrible for big files!
    val bufferStream = new java.io.ByteArrayOutputStream
    StreamUtil.writeStream(otherContent, bufferStream)
    val put = request << bufferStream.toString("UTF-8")
    // Construct a new PUT request with the input stream as the content
    /*val put = request next {
        val m = new HttpPut
        val e = new InputStreamEntity(otherContent, length)
        e.setContentType(mimeType)
        m setEntity e
        HttpProtocolParams.setUseExpectContinue(m.getParams, false)
        Request.mimic(m)_
    }*/
    // Write the content out and print the resulting HTML...
    Http(put >>> System.out)
  }
}

// Utility to synchronize files
object SynchUtil {
  /* synchronizes two items. */
  private def synchronizeItem[F : SynchronizeFromItem, T : SynchronizeToItem](from : F, to : T) : Unit = {
    val fromHelper = implicitly[SynchronizeFromItem[F]]
    val toHelper = implicitly[SynchronizeToItem[T]]
    val inputStream = implicitly[SynchronizeFromItem[F]].content(from)
    Console.println("Synchronizing [" + fromHelper.debugName(from) + "] to [" + toHelper.debugName(to) + "] ")// + length + " bytes of type: " + mimeType)
    implicitly[SynchronizeToItem[T]].writeContent(to,inputStream)
  }


  /** This method synchronizes from one location to another.  It always copies all data */
  def synchronizeOneWay[F : SynchronizeFromItem, T : SynchronizeToItem](from : F, to : T) : Unit = {
    val fromHelper = implicitly[SynchronizeFromItem[F]]
    val toHelper = implicitly[SynchronizeToItem[T]]
    // Helper method to determine if we're synching directories or files.
    def synchronizeHelper(f : F, t : T) : Unit = {
      if(fromHelper.isDirectory(f)) {
          toHelper.mkdirs(t)
          synchDirectory(f,t)
        } else {
          synchronizeItem(f,t)
        }
    }

    // TODO - redesign this to be higher-order and far more flexible in *not* synching every time!
    def synchDirectory(fromDir : F, toDir : T) : Unit = {
      Console.println("Synchronizing [" + fromHelper.debugName(from) + "] to [" + toHelper.debugName(to) + "]")
      for(file1 <- fromHelper.children(fromDir)) {
        val file2 = toHelper.child(toDir, fromHelper.name(file1))
        synchronizeHelper(file1, file2)
      }
    }
    synchronizeHelper(from, to)
  }
}