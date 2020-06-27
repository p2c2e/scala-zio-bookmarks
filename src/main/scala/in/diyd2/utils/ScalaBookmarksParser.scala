package in.diyd2.utils

import java.io.{BufferedWriter, File, FileWriter}

import org.jsoup.{Connection, Jsoup}
import zio.{Task, UIO, URIO, ZIO}

import scala.util.{Failure, Success, Try}

object ScalaBookmarksParser extends App {

  if (args.length != 1) {
    println("Usage: scala ../path/to/filename.jar <bookmarks-file.html>")
    sys.exit(1)
  }
  val bookmarksFilename = args(0)
//  val bookmarksFilename = "scala-zio-bookmarks/bookmarks.html"

  case class UrlStatus(url: String, code: Int, message: String)

  def parseUrl(url: String): Option[String] = {
    val regex = "(?i)^.*href=\"([^\"]*)\".*".r
    val result = regex.findFirstMatchIn(url)
    result match {
      case Some(x) =>
//        println(x.group(1))
        Some(x.group(1))
      case None =>
//        println("Nothing") // Non URL line ...
        None
    }
  }

  def writeFile(filename: String, lines: List[String], invalidLines: List[String]): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    var additions = false
    for (line <- lines) {
      bw.write(line + "\n")
      if (!additions && line.trim.equalsIgnoreCase("<DL><P>")) {
        bw.write("<DT><H3>INVALIDURLS</H3>\n<DL><p>\n")
        for (line2 <- invalidLines) bw.write(line2 + "\n")
        bw.write("</DL><p>\n")
        additions = true
      }
    }
    bw.close()
  }

  def checkUrl(href: String): UrlStatus = {
    val resp = Try(Jsoup.connect(href).method(Connection.Method.HEAD).execute())

    resp match {
      case Success(r) =>
//        println(r.statusCode() + " = " + r.statusMessage())
        UrlStatus(href, r.statusCode, r.statusMessage())
      case Failure(e) =>
//        println("ERROR: " + href + " " + e.getMessage)
        UrlStatus(href, 404, e.getMessage)
    }
  }

  def validLine(urls: List[String], line: String): Boolean = {
    //    !line.contains("<a ") || line.contains("valid=\"true\"")
    parseUrl(line) match {
      case Some(href) => urls.contains(href)
      case None => true
    }
  }

  val program = for {

    allLines <- Task(scala.io.Source.fromFile(bookmarksFilename)).bracket(bs => URIO.effectTotal(bs.close())) {
      bs =>
        Task.effect(bs.getLines().toList)
    }

    allUrls <- ZIO.foreach(allLines) {
      line => {
        UIO.succeed(parseUrl(line))
      }
    }
    checkedUrls <- ZIO.foreachParN(50)(allUrls.flatten) {
      url =>
        Task.effect(checkUrl(url))
    }

    validStatuses <- ZIO.filter(checkedUrls) {
      urlstatus => Task.succeed(urlstatus.code == 200)
    }

    validUrls <- Task.effect(validStatuses.map(s => s.url))

    validatedLines <- ZIO.foreach(allLines) {
      line => {
        UIO.succeed(Tuple2(validLine(validUrls, line), line))
      }
    }
    valid <- UIO.succeed(validatedLines.filter(tup => tup._1))
    invalid <- UIO.succeed(validatedLines.filter(tup => !tup._1))
    _ <- ZIO.effect({
      println("INVALID COUNT : " + invalid.length)
      val validLines = valid.map(tup => tup._2)
      val invalidLines = invalid.map(tup => tup._2)
      writeFile(bookmarksFilename+".out", validLines, invalidLines)
    })
  } yield ()

  zio.Runtime.default.unsafeRun(program)
}
