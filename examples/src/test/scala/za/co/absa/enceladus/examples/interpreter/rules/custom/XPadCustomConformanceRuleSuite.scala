/*
 * Copyright 2018-2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.enceladus.examples.interpreter.rules.custom

import org.apache.spark.sql
import org.apache.spark.sql.DataFrame
import org.scalatest.FunSuite
import za.co.absa.enceladus.conformance.CmdConfig
import za.co.absa.enceladus.conformance.interpreter.DynamicInterpreter
import za.co.absa.enceladus.dao.{EnceladusDAO, EnceladusRestDAO}
import za.co.absa.enceladus.model.Dataset
import za.co.absa.enceladus.utils.testUtils.SparkTestBase

case class XPadTestInputRow(intField: Int, stringField: Option[String])
case class XPadTestOutputRow(intField: Int, stringField: Option[String], targetField: String)
object XPadTestOutputRow {
  def apply(input: XPadTestInputRow, targetField: String): XPadTestOutputRow = XPadTestOutputRow(input.intField, input.stringField, targetField)
}

class LpadCustomConformanceRuleSuite extends FunSuite with SparkTestBase {
  import spark.implicits._

  implicit val progArgs: CmdConfig = CmdConfig() // here we may need to specify some parameters (for certain rules)
  implicit val dao: EnceladusDAO = EnceladusRestDAO // you may have to hard-code your own implementation here (if not working with menas)
  val experimentalMR = true
  val isCatalystWorkaroundEnabled = true
  val enableCF: Boolean = false

  test("String values") {
    val input: List[XPadTestInputRow] = List(
      XPadTestInputRow(1,Some("Short")),
      XPadTestInputRow(2,Some("This is long")),
      XPadTestInputRow(3,None)
    )
    val inputData: DataFrame = spark.createDataFrame(input)
    val conformanceDef: Dataset =  Dataset(
      name = "Test string values",
      version = 0,
      hdfsPath = "/a/b/c",
      hdfsPublishPath = "/publish/a/b/c",

      schemaName = "Not really used here",
      schemaVersion = 9999,

      conformance = List(
        LPadCustomConformanceRule(order = 0, outputColumn = "targetField", controlCheckpoint = false, inputColumn = "stringField", len = 8, pad = "~")
      )
    )

    val outputData: sql.DataFrame = DynamicInterpreter.interpret(conformanceDef,
      inputData,
      experimentalMR,
      isCatalystWorkaroundEnabled,
      enableCF)

    val output: List[XPadTestOutputRow] = outputData.as[XPadTestOutputRow].collect().toList
    val expected: List[XPadTestOutputRow] = (input zip List("~~~Short", "This is long", "~~~~~~~~")).map(x => XPadTestOutputRow(x._1, x._2))

    assert(output === expected)
  }

  test("Integer value") {
    val input: Seq[XPadTestInputRow] = Seq(
      XPadTestInputRow(7, Some("Agent")),
      XPadTestInputRow(42, None),
      XPadTestInputRow(100000, Some("A lot"))
    )
    val inputData: DataFrame = spark.createDataFrame(input)
    val conformanceDef: Dataset =  Dataset(
      name = "Test integer value",
      version = 0,
      hdfsPath = "/a/b/c",
      hdfsPublishPath = "/publish/a/b/c",

      schemaName = "Not really used here",
      schemaVersion = 9999,

      conformance = List(
        LPadCustomConformanceRule(order = 0, outputColumn = "targetField", controlCheckpoint = false, inputColumn = "intField", len = 3, pad = "0")
      )
    )

    val outputData: sql.DataFrame = DynamicInterpreter.interpret(conformanceDef,
      inputData,
      experimentalMR,
      isCatalystWorkaroundEnabled,
      enableCF)

    val output: Seq[XPadTestOutputRow] = outputData.as[XPadTestOutputRow].collect().toSeq
    val expected: Seq[XPadTestOutputRow] = (input zip Seq("007", "042", "100000")).map(x => XPadTestOutputRow(x._1, x._2))

    assert(output === expected)
  }

  test("Multicharacter pad") {
    val input: List[XPadTestInputRow] = List(
      XPadTestInputRow(1,Some("abcdefgh")),
      XPadTestInputRow(2,Some("$$$")),
      XPadTestInputRow(3,None)
    )
    val inputData: DataFrame = spark.createDataFrame(input)
    val conformanceDef: Dataset =  Dataset(
      name = "Test multicharacter pad",
      version = 0,
      hdfsPath = "/a/b/c",
      hdfsPublishPath = "/publish/a/b/c",

      schemaName = "Not really used here",
      schemaVersion = 9999,

      conformance = List(
        LPadCustomConformanceRule(order = 0, outputColumn = "targetField", controlCheckpoint = false, inputColumn = "stringField", len = 10, pad = "123")
      )
    )

    val outputData: sql.DataFrame = DynamicInterpreter.interpret(conformanceDef,
      inputData,
      experimentalMR,
      isCatalystWorkaroundEnabled,
      enableCF)

    val output: List[XPadTestOutputRow] = outputData.as[XPadTestOutputRow].collect().toList
    val expected: List[XPadTestOutputRow] = (input zip List("12abcdefgh", "1231231$$$", "1231231231")).map(x => XPadTestOutputRow(x._1, x._2))

    assert(output === expected)
  }

  test("Negative length") {
    val input: List[XPadTestInputRow] = List(
      XPadTestInputRow(1,Some("A")),
      XPadTestInputRow(2,Some("AAAAAAAAAAAAAAAAAAAA")),
      XPadTestInputRow(3,None)
    )
    val inputData: DataFrame = spark.createDataFrame(input)
    val conformanceDef: Dataset =  Dataset(
      name = "Test negative length",
      version = 0,
      hdfsPath = "/a/b/c",
      hdfsPublishPath = "/publish/a/b/c",

      schemaName = "Not really used here",
      schemaVersion = 9999,

      conformance = List(
        LPadCustomConformanceRule(order = 0, outputColumn = "targetField", controlCheckpoint = false, inputColumn = "stringField", len = -5, pad = ".")
      )
    )

    val outputData: sql.DataFrame = DynamicInterpreter.interpret(conformanceDef,
      inputData,
      experimentalMR,
      isCatalystWorkaroundEnabled,
      enableCF)

    val output: List[XPadTestOutputRow] = outputData.as[XPadTestOutputRow].collect().toList
    val expected: List[XPadTestOutputRow] = (input zip List("A", "AAAAAAAAAAAAAAAAAAAA", "")).map(x => XPadTestOutputRow(x._1, x._2))

    assert(output === expected)
  }
}


class RpadCustomConformanceRuleSuite extends FunSuite with SparkTestBase {


  import spark.implicits._

  implicit val progArgs: CmdConfig = CmdConfig()
  implicit val dao: EnceladusDAO = EnceladusRestDAO
  val experimentalMR = true
  val isCatalystWorkaroundEnabled = true
  val enableCF: Boolean = false

  test("String values") {
    val input: List[XPadTestInputRow] = List(
      XPadTestInputRow(1,Some("Short")),
      XPadTestInputRow(2,Some("This is long")),
      XPadTestInputRow(3,None)
    )
    val inputData: DataFrame = spark.createDataFrame(input)
    val conformanceDef: Dataset =  Dataset(
      name = "Test string values",
      version = 0,
      hdfsPath = "/a/b/c",
      hdfsPublishPath = "/publish/a/b/c",

      schemaName = "Not really used here",
      schemaVersion = 9999,

      conformance = List(
        RPadCustomConformanceRule(order = 0, outputColumn = "targetField", controlCheckpoint = false, inputColumn = "stringField", len = 8, ".")
      )
    )

    val outputData: sql.DataFrame = DynamicInterpreter.interpret(conformanceDef,
      inputData,
      experimentalMR,
      isCatalystWorkaroundEnabled,
      enableCF)

    val output: List[XPadTestOutputRow] = outputData.as[XPadTestOutputRow].collect().toList
    val expected: List[XPadTestOutputRow] = (input zip List("Short...", "This is long", "........")).map(x => XPadTestOutputRow(x._1, x._2))

    assert(output === expected)
  }

  test("Integer value") {
    val input: Seq[XPadTestInputRow] = Seq(
      XPadTestInputRow(1, Some("Cent")),
      XPadTestInputRow(42, None),
      XPadTestInputRow(100000, Some("A lot"))
    )
    val inputData: DataFrame = spark.createDataFrame(input)
    val conformanceDef: Dataset =  Dataset(
      name = "Test integer value",
      version = 0,
      hdfsPath = "/a/b/c",
      hdfsPublishPath = "/publish/a/b/c",

      schemaName = "Not really used here",
      schemaVersion = 9999,

      conformance = List(
        RPadCustomConformanceRule(order = 0, outputColumn = "targetField", controlCheckpoint = false, inputColumn = "intField", len = 3, pad = "0")
      )
    )

    val outputData: sql.DataFrame = DynamicInterpreter.interpret(conformanceDef,
      inputData,
      experimentalMR,
      isCatalystWorkaroundEnabled,
      enableCF)

    val output: Seq[XPadTestOutputRow] = outputData.as[XPadTestOutputRow].collect().toSeq
    val expected: Seq[XPadTestOutputRow] = (input zip Seq("100", "420", "100000")).map(x => XPadTestOutputRow(x._1, x._2))

    assert(output === expected)
  }

  test("Multicharacter pad") {
    val input: List[XPadTestInputRow] = List(
      XPadTestInputRow(1,Some("abcdefgh")),
      XPadTestInputRow(2,Some("$$$")),
      XPadTestInputRow(3,None)
    )
    val inputData: DataFrame = spark.createDataFrame(input)
    val conformanceDef: Dataset =  Dataset(
      name = "Test multicharacter pad",
      version = 0,
      hdfsPath = "/a/b/c",
      hdfsPublishPath = "/publish/a/b/c",

      schemaName = "Not really used here",
      schemaVersion = 9999,

      conformance = List(
        RPadCustomConformanceRule(order = 0, outputColumn = "targetField", controlCheckpoint = false, inputColumn = "stringField", len = 10, pad = "123")
      )
    )

    val outputData: sql.DataFrame = DynamicInterpreter.interpret(conformanceDef,
      inputData,
      experimentalMR,
      isCatalystWorkaroundEnabled,
      enableCF)

    val output: List[XPadTestOutputRow] = outputData.as[XPadTestOutputRow].collect().toList
    val expected: List[XPadTestOutputRow] = (input zip List("abcdefgh12", "$$$1231231", "1231231231")).map(x => XPadTestOutputRow(x._1, x._2))

    assert(output === expected)
  }

  test("Negative length") {
    val input: List[XPadTestInputRow] = List(
      XPadTestInputRow(1,Some("A")),
      XPadTestInputRow(2,Some("AAAAAAAAAAAAAAAAAAAA")),
      XPadTestInputRow(3,None)
    )
    val inputData: DataFrame = spark.createDataFrame(input)
    val conformanceDef: Dataset =  Dataset(
      name = "Test negative length",
      version = 0,
      hdfsPath = "/a/b/c",
      hdfsPublishPath = "/publish/a/b/c",

      schemaName = "Not really used here",
      schemaVersion = 9999,

      conformance = List(
        RPadCustomConformanceRule(order = 0, outputColumn = "targetField", controlCheckpoint = false, inputColumn = "stringField", len = -5, pad = "#")
      )
    )

    val outputData: sql.DataFrame = DynamicInterpreter.interpret(conformanceDef,
      inputData,
      experimentalMR,
      isCatalystWorkaroundEnabled,
      enableCF)

    val output: List[XPadTestOutputRow] = outputData.as[XPadTestOutputRow].collect().toList
    val expected: List[XPadTestOutputRow] = (input zip List("A", "AAAAAAAAAAAAAAAAAAAA", "")).map(x => XPadTestOutputRow(x._1, x._2))

    assert(output === expected)
  }
}
