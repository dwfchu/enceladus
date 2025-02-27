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

package za.co.absa.enceladus.conformance.interpreter

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StructType
import za.co.absa.enceladus.conformance.CmdConfig
import za.co.absa.enceladus.conformance.interpreter.DynamicInterpreter.getExplosionOptimizedSteps
import za.co.absa.enceladus.dao.EnceladusDAO
import za.co.absa.enceladus.model.conformanceRule.ConformanceRule
import za.co.absa.enceladus.model.{Dataset => ConfDataset}

/** Holds everything that is needed in between dynamic conformance interpreter stages */
case class InterpreterContext (
                                schema: StructType,
                                conformance: ConfDataset,
                                experimentalMappingRule: Boolean,
                                isCatalystWorkaroundEnabled: Boolean,
                                enableControlFramework: Boolean,
                                jobShortName: String,
                                spark: SparkSession,
                                dao: EnceladusDAO,
                                progArgs: CmdConfig
                              )
