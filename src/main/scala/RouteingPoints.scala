package com.github.voqo.train

import java.io.File

object RouteingPoints {
  val targetfile = "routeing_points.pdf"

  def main(args: Array[String]) {
    if (args.size != 1) {
      Log.error("Wrong number of arguments. Please supply the source directory.")
      System.exit(1)
    }

    val rps = fromPDF(new File(args(0), targetfile))
    rps.map { case (s, rs) => println("%s: %s" format (s, rs mkString ", ")) }
  }

  def fromPDF(f: File) = {
    parse(PDFReader.getPages(f) flatMap { _.split("\n").drop(1) })
  }

  /**
   * Extract Routeing Point to Map mappings
   *
   * This relies upon each Map name being two characters, and the map names being
   * listed in alphabetical order. Thus we can chomp off two-character names as long
   * as they're ascending and thus avoid capturing the JN on the end of WILLESDEN JN.
   */
  private def parse(lines: Seq[String]) = {
    for (line <- lines) yield {
      val tokens = line split " "
      val ms: List[String] = tokens.foldRight(List[String]()) { (token, acc) =>
        if (token.size == 2 && (acc.isEmpty || token < acc.head)) {
          token :: acc
        } else acc
      }
      val s = tokens take (tokens.size - ms.size) mkString " "
      (s, ms)
    }
  }
}