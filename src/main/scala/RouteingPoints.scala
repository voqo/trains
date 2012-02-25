package com.github.voqo.train

import java.io.File
import java.io.OutputStreamWriter
import java.io.ByteArrayOutputStream

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.util.PDFTextStripper

object RouteingPoints {
  def main(args: Array[String]) {
    if (args.size != 1) {
      Log.error("Wrong number of arguments")
      System.exit(1)
    }

    val rps = parse(extract(args(0)))
    rps.map { println(_) }
  }

  /**
   * Takes the path of routeing_point_identifier.pdf and returns
   * each routeing line within that document
   */
  def extract(s: String): Seq[String] = {
    val sep = "---- PAGE ----\n"
    val doc = PDDocument.load(s)
    val stream = new ByteArrayOutputStream
    val output = new OutputStreamWriter(stream)
    val stripper = new PDFTextStripper("UTF-8") {
      override def startPage(page: PDPage) {
        output.write(sep)
      }
    }
    stripper.writeText(doc, output)
    output.flush
    stream.toString("UTF-8").split(sep).map { _.split("\n").drop(4) }.flatten.toList
  }

  def parse(lines: Seq[String]): Map[String, List[String]] = {
    val points = lines collect { case RouteingOrGroupPoint(s, p) => p } toSet

    (lines collect {
      case RouteingOrGroupPoint(s, p) => (s -> List(p))
      case line => parseLine(points, line)
    }).toMap
  }

  def parseLine(points: Set[String], line: String): (String, List[String]) = {
    val tokens: List[String] = unfold(line.toLowerCase) { ss =>
      points find (ss endsWith _) match {
        case Some(s) => Some(s, ss.substring(0, ss.size - s.size - 1))
        case None => if (ss.size == 0) None else Some(ss, "")
      }
    }
    (tokens.reverse.head, tokens.reverse.tail)
  }

  def unfold[S,T](init: T)(f: T => Option[(S, T)]): List[S] = f(init) match {
    case Some((item, remain)) => item :: unfold(remain)(f)
    case None => Nil
  }
}

object RouteingOrGroupPoint {
  def unapply(s: String): Option[(String, String)] = {
    import scala.util.matching.Regex.Match

    val matchers = List(("^(.*) Routeing Point$", 0, 0),
                        ("^([^a-z ]+( [^a-z ]+)*) (.*) Routeing Point Member$", 0, 2))
    (matchers map { case (pattern, k1, k2) =>
      pattern.r.findFirstMatchIn(s).map { m: Match =>
        val f = { n: Int => m.subgroups(n).toLowerCase }
        (f(k1), f(k2))
      }
    }) find (_.isDefined) map (_.get)
  }
}