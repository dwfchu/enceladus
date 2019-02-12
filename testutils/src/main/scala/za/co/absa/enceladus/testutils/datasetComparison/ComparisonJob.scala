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

import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import za.co.absa.enceladus.testutils.exceptions._
import za.co.absa.enceladus.testutils.{DataframeReader, DataframeReaderOptions}

object ComparisonJob {
  private val log: Logger = LogManager.getLogger("enceladus.testutils.ComparisonJob")
  private val errorColumnName: String = "errCol"
  private val tmpColumnName: String = "tmp"

  private def renameColumns(dataSet: Dataset[Row], keys: Seq[String], prefix: String): DataFrame = {
    val renamedColumns = dataSet.columns.map { c =>
      if (keys.contains(c)) {
        dataSet(c)
      } else {
        dataSet(c).as(s"$prefix$c")
      }}

    dataSet.select(renamedColumns: _*)
  }

  private def getKeyBasedOutput(expectedMinusActual: Dataset[Row],
                        actualMinusExpected: Dataset[Row],
                        keys: Seq[String]): DataFrame = {
    val dfNewExpected = renameColumns(expectedMinusActual, keys, "expected_")
    val dfNewColumnsActual = renameColumns(actualMinusExpected, keys, "actual_")

    dfNewExpected.join(dfNewColumnsActual, keys,"full")
  }

  private def checkForDuplicateRows(actualDf: DataFrame, keys: Seq[String], path: String): Unit = {
    val duplicates = actualDf.groupBy(keys.head, keys.tail: _*).count().filter("`count` >= 2")
    if (duplicates.count() > 0) {
      duplicates.write.format("parquet").save(path)
      throw DuplicateRowsInDF(path)
    }
  }

  def main(args: Array[String]): Unit = {
    val cmd: CmdConfig = CmdConfig.getCmdLineArguments(args)
    implicit val dfReaderOptions: DataframeReaderOptions = DataframeReaderOptions(cmd.rawFormat,
                                                                                  cmd.rowTag,
                                                                                  cmd.csvDelimiter,
                                                                                  cmd.csvHeader,
                                                                                  cmd.fixedWidthTrimValues)
    val enableWholeStage = false //disable whole stage code gen - the plan is too long

    implicit val sparkSession: SparkSession = SparkSession.builder()
      .appName(s"Dataset comparison - '${cmd.newPath}' and '${cmd.refPath}'")
      .config("spark.sql.codegen.wholeStage", enableWholeStage)
      .getOrCreate()

    implicit val sc: SparkContext = sparkSession.sparkContext

    val expectedDfReader = new DataframeReader(cmd.refPath, None)
    val actualDfReader = new DataframeReader(cmd.newPath, None)
    val expectedDf = expectedDfReader.dataFrame
    val actualDf = actualDfReader.dataFrame
    val expectedSchema = expectedDfReader.getSchemaWithoutMetadata
    val actualSchema = actualDfReader.getSchemaWithoutMetadata

    if (cmd.keys.isDefined) { checkForDuplicateRows(actualDf, cmd.keys.get, cmd.outPath) }

    if (expectedSchema != actualSchema) {
      val diffSchema = actualSchema.diff(expectedSchema) ++ expectedSchema.diff(actualSchema)
      throw CmpJobSchemasDifferException(cmd.refPath, cmd.newPath, diffSchema)
    }

    val expectedMinusActual: Dataset[Row] = expectedDf.except(actualDf)
    val actualMinusExpected: Dataset[Row] = actualDf.except(expectedDf)

    val errorsPresent: Boolean = expectedMinusActual.count() != 0 || actualMinusExpected.count() != 0

    if (errorsPresent) {
      if (cmd.keys.isDefined) {
        handleKeyBasedDiff(cmd, expectedDf, expectedMinusActual, actualMinusExpected)
      } else {
        expectedMinusActual.write.format("parquet").save(s"${cmd.outPath}/expected_minus_actual")
        actualMinusExpected.write.format("parquet").save(s"${cmd.outPath}/actual_minus_expected")
      }

      throw CmpJobDatasetsDifferException(cmd.refPath, cmd.newPath, cmd.outPath, expectedDf.count(), actualDf.count())
    } else {
      log.info("Expected and actual datasets are the same.")
    }
  }

  private def handleKeyBasedDiff(cmd: CmdConfig,
                                 expectedDf: DataFrame,
                                 expectedMinusActual: Dataset[Row],
                                 actualMinusExpected: Dataset[Row]): Unit = {
    val flatteningFormula = Flatten.flattenSchema(expectedDf)
    val columns: Array[String] = expectedDf.select(flatteningFormula: _*).columns.filterNot(cmd.keys.get.contains)

    val flatExpectedMinusActual: DataFrame = expectedMinusActual
    val flatActualMinusExpected: DataFrame = actualMinusExpected.select(flatteningFormula: _*)

    val joinedData: DataFrame = getKeyBasedOutput(flatExpectedMinusActual, flatActualMinusExpected, cmd.keys.get)
    val joinedDataWithErrCol = joinedData.withColumn(errorColumnName, lit(Array[String]()))

    val dataWithErrors: DataFrame = columns.foldLeft(joinedDataWithErrCol) { (data, column) =>
      data.withColumnRenamed(errorColumnName, tmpColumnName)
        .withColumn(errorColumnName, concat(
          when(col(s"actual_$column") === col(s"expected_$column"), lit(Array[String]()))
          .otherwise(array(lit(column))), col(tmpColumnName)))
        .drop(tmpColumnName)
    }

    dataWithErrors.write.format("parquet").save(cmd.outPath)
  }
}
