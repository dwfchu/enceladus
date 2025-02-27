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

package za.co.absa.enceladus.conformance.interpreter.rules

import org.apache.spark.sql.DataFrame
import org.mockito.Mockito.{mock, when => mockWhen}
import org.scalatest.FunSuite
import za.co.absa.enceladus.conformance.CmdConfig
import za.co.absa.enceladus.conformance.interpreter.DynamicInterpreter
import za.co.absa.enceladus.dao.EnceladusDAO
import za.co.absa.enceladus.model.Dataset
import za.co.absa.enceladus.utils.testUtils.SparkTestBase
import za.co.absa.enceladus.utils.testUtils.LoggerTestBase
import org.slf4j.event.Level._


trait TestRuleBehaviors  extends FunSuite with SparkTestBase with LoggerTestBase {

  def conformanceRuleShouldMatchExpected(inputDf: DataFrame, inputDataset: Dataset, expectedJSON: String) {
    implicit val dao: EnceladusDAO = mock(classOf[EnceladusDAO])
    implicit val progArgs: CmdConfig = CmdConfig(reportDate = "2017-11-01")
    val experimentalMR = true
    val isCatalystWorkaroundEnabled = true
    val enableCF: Boolean = false

    mockWhen(dao.getDataset("Orders Conformance", 1)) thenReturn inputDataset
    mockWhen(dao.getDataset("Library Conformance", 1)) thenReturn inputDataset

    import spark.implicits._
    val conformed = DynamicInterpreter.interpret(inputDataset,
      inputDf,
      experimentalMR,
      isCatalystWorkaroundEnabled,
      enableCF).cache

    val conformedJSON = conformed.orderBy($"id").toJSON.collect().mkString("\n")

    if (conformedJSON != expectedJSON) {
      logger.error("EXPECTED:")
      logger.error(expectedJSON)
      logger.error("ACTUAL:")
      logger.error(conformedJSON)
      logger.error("DETAILS (Input):")
      logDataFrameContent(inputDf, ERROR)
      println("DETAILS (Conformed):")
      logDataFrameContent(conformed, ERROR)
      fail("Actual conformed dataset JSON does not match the expected JSON (see log).")
    }

  }

}
