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

package za.co.absa.enceladus.testutils.datasetComparison

import org.scalatest.FunSuite
import za.co.absa.enceladus.testutils.DataframeReaderOptions

class ConfigSuite extends FunSuite {

  private val newPath = "/tmp/standardized_out"
  private val refPath = "/tmp/reference_data"
  private val outPath = "/tmp/out_data"
  private val delimiter = ";"
  private val rowTag = "Alfa"
  private val csvFormat = "csv"
  private val parquetFormat = "parquet"
  private val xmlFormat = "xml"
  private val fixedWithdFormat = "fixed-width"

  test("Parquest file") {
    val cmdConfig = CmdConfig.getCmdLineArguments(
      Array(
        "--raw-format", parquetFormat,
        "--new-path", newPath,
        "--ref-path", refPath,
        "--out-path", outPath,
        "--keys", "id"
      )
    )

    assert(cmdConfig.rawFormat == parquetFormat)
    assert(cmdConfig.newPath == newPath)
    assert(cmdConfig.refPath == refPath)
    assert(cmdConfig.outPath == outPath)
    assert(cmdConfig.keys == Some(Seq("id")))
  }

  test("Csv with default header") {
    val cmdConfig = CmdConfig.getCmdLineArguments(
      Array(
        "--raw-format", csvFormat,
        "--delimiter", delimiter,
        "--new-path", newPath,
        "--ref-path", refPath,
        "--out-path", outPath,
        "--keys", "id,alfa"
      )
    )

    assert(cmdConfig.rawFormat == csvFormat)
    assert(cmdConfig.csvDelimiter == Option(delimiter))
    assert(cmdConfig.csvHeader == Option(false))
    assert(cmdConfig.newPath == newPath)
    assert(cmdConfig.refPath == refPath)
    assert(cmdConfig.outPath == outPath)
    assert(cmdConfig.keys == Some(Seq("id","alfa")))
  }

  test("Csv with header") {
    val cmdConfig = CmdConfig.getCmdLineArguments(
      Array(
        "--raw-format", csvFormat,
        "--delimiter", ";",
        "--header", "true",
        "--new-path", newPath,
        "--ref-path", refPath,
        "--out-path", outPath
      )
    )

    assert(cmdConfig.rawFormat == csvFormat)
    assert(cmdConfig.csvDelimiter == Option(delimiter))
    assert(cmdConfig.csvHeader == Option(true))
    assert(cmdConfig.newPath == newPath)
    assert(cmdConfig.refPath == refPath)
    assert(cmdConfig.outPath == outPath)
  }


  test("XML file") {
    val cmdConfig = CmdConfig.getCmdLineArguments(
      Array(
        "--raw-format", xmlFormat,
        "--row-tag", rowTag,
        "--new-path", newPath,
        "--ref-path", refPath,
        "--out-path", outPath
      )
    )

    assert(cmdConfig.rawFormat == xmlFormat)
    assert(cmdConfig.rowTag == Option(rowTag))
    assert(cmdConfig.newPath == newPath)
    assert(cmdConfig.refPath == refPath)
    assert(cmdConfig.outPath == outPath)
  }

  test("Fixed-width file don't trim value") {
    val cmdConfig = CmdConfig.getCmdLineArguments(
      Array(
        "--raw-format", fixedWithdFormat,
        "--new-path", newPath,
        "--ref-path", refPath,
        "--out-path", outPath
      )
    )

    assert(cmdConfig.rawFormat == fixedWithdFormat)
    assert(cmdConfig.fixedWidthTrimValues == Option(false))
    assert(cmdConfig.newPath == newPath)
    assert(cmdConfig.refPath == refPath)
    assert(cmdConfig.outPath == outPath)
  }

  test("Fixed-width file trim values") {
    val cmdConfig = CmdConfig.getCmdLineArguments(
      Array(
        "--raw-format", fixedWithdFormat,
        "--trim-values", "true",
        "--new-path", newPath,
        "--ref-path", refPath,
        "--out-path", outPath
      )
    )

    assert(cmdConfig.rawFormat == fixedWithdFormat)
    assert(cmdConfig.fixedWidthTrimValues == Option(true))
    assert(cmdConfig.newPath == newPath)
    assert(cmdConfig.refPath == refPath)
    assert(cmdConfig.outPath == outPath)
  }
}
