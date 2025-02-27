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

package za.co.absa.enceladus.samples

import org.apache.spark.sql.types._
import za.co.absa.enceladus.model.Dataset
import za.co.absa.enceladus.model.conformanceRule.NegationConformanceRule

object NegationRuleSamples {

  val schema = StructType(
    Array(
      StructField("byte", ByteType),
      StructField("short", ShortType),
      StructField("integer", IntegerType),
      StructField("long", LongType),
      StructField("float", FloatType),
      StructField("double", DoubleType),
      StructField("decimal", DecimalType(32, 2)),
      StructField("date", DateType)
    )
  )

  val negateByte = NegationConformanceRule(order = 1, outputColumn = "ConformedByte", controlCheckpoint = false, inputColumn = "byte")
  val negateShort = NegationConformanceRule(order = 2, outputColumn = "ConformedShort", controlCheckpoint = false, inputColumn = "short")
  val negateInt = NegationConformanceRule(order = 3, outputColumn = "ConformedInt", controlCheckpoint = false, inputColumn = "integer")
  val negateLong = NegationConformanceRule(order = 4, outputColumn = "ConformedLong", controlCheckpoint = false, inputColumn = "long")
  val negateFloat = NegationConformanceRule(order = 5, outputColumn = "ConformedFloat", controlCheckpoint = false, inputColumn = "float")
  val negateDouble = NegationConformanceRule(order = 6, outputColumn = "ConformedDouble", controlCheckpoint = false, inputColumn = "double")
  val negateDecimal = NegationConformanceRule(order = 7, outputColumn = "ConformedDecimal", controlCheckpoint = false, inputColumn = "decimal")

  val dataset = Dataset(name = "Test Name", version = 1, hdfsPath = "", hdfsPublishPath = "",
    schemaName = "Test Name", schemaVersion = 1,
    conformance = List(negateByte, negateShort, negateInt, negateLong, negateFloat, negateDouble, negateDecimal))

  object Positive {
    val data: Seq[String] = Seq(
      s"""{ "byte": 1,
         |  "short": 1,
         |  "integer": 1,
         |  "long": 1,
         |  "float": 1.0,
         |  "double": 1.0,
         |  "decimal": 1.0,
         |  "date": "2018-06-11"}""".stripMargin)

    val conformedJSON = """{"byte":1,"short":1,"integer":1,"long":1,"float":1.0,"double":1.0,"decimal":1.00,"date":"2018-06-11","errCol":[],"ConformedByte":-1,"ConformedShort":-1,"ConformedInt":-1,"ConformedLong":-1,"ConformedFloat":-1.0,"ConformedDouble":-1.0,"ConformedDecimal":-1.00}"""
  }

  object Negative {
    val data: Seq[String] = Seq(
      s"""{ "byte": -1,
         |  "short": -1,
         |  "integer": -1,
         |  "long": -1,
         |  "float": -1.0,
         |  "double": -1.0,
         |  "decimal": -1.0,
         |  "date": "2018-06-11"}""".stripMargin)

    val conformedJSON = """{"byte":-1,"short":-1,"integer":-1,"long":-1,"float":-1.0,"double":-1.0,"decimal":-1.00,"date":"2018-06-11","errCol":[],"ConformedByte":1,"ConformedShort":1,"ConformedInt":1,"ConformedLong":1,"ConformedFloat":1.0,"ConformedDouble":1.0,"ConformedDecimal":1.00}"""
  }

  object Zero {
    val data: Seq[String] = Seq(
      s"""{ "byte": 0,
         |  "short": 0,
         |  "integer": 0,
         |  "long": 0,
         |  "float": 0.0,
         |  "double": 0.0,
         |  "decimal": 0.0,
         |  "date": "2018-06-11"}""".stripMargin)

    val conformedJSON = """{"byte":0,"short":0,"integer":0,"long":0,"float":0.0,"double":0.0,"decimal":0.00,"date":"2018-06-11","errCol":[],"ConformedByte":0,"ConformedShort":0,"ConformedInt":0,"ConformedLong":0,"ConformedFloat":0.0,"ConformedDouble":0.0,"ConformedDecimal":0.00}"""
  }

  object Min {
    val data: Seq[String] = Seq(
      s"""{ "byte": ${Byte.MinValue},
         |  "short": ${Short.MinValue},
         |  "integer": ${Int.MinValue},
         |  "long": ${Long.MinValue},
         |  "float": ${Float.MinValue},
         |  "double": ${Double.MinValue},
         |  "decimal": 0,
         |  "date": "2018-06-11"}""".stripMargin)

    val conformedJSON = """{"byte":-128,"short":-32768,"integer":-2147483648,"long":-9223372036854775808,"float":-3.4028235E38,"double":-1.7976931348623157E308,"decimal":0.00,"date":"2018-06-11","errCol":[{"errType":"confNegError","errCode":"E00004","errMsg":"Conformance Error - Negation of numeric type with minimum value overflows and remains unchanged","errCol":"ConformedByte","rawValues":["-128"],"mappings":[]},{"errType":"confNegError","errCode":"E00004","errMsg":"Conformance Error - Negation of numeric type with minimum value overflows and remains unchanged","errCol":"ConformedShort","rawValues":["-32768"],"mappings":[]},{"errType":"confNegError","errCode":"E00004","errMsg":"Conformance Error - Negation of numeric type with minimum value overflows and remains unchanged","errCol":"ConformedInt","rawValues":["-2147483648"],"mappings":[]},{"errType":"confNegError","errCode":"E00004","errMsg":"Conformance Error - Negation of numeric type with minimum value overflows and remains unchanged","errCol":"ConformedLong","rawValues":["-9223372036854775808"],"mappings":[]}],"ConformedByte":0,"ConformedShort":0,"ConformedInt":0,"ConformedLong":0,"ConformedFloat":3.4028234663852886E38,"ConformedDouble":1.7976931348623157E308,"ConformedDecimal":0.00}"""
  }

  object Max {
    val data: Seq[String] = Seq(
      s"""{ "byte": ${Byte.MaxValue},
         |  "short": ${Short.MaxValue},
         |  "integer": ${Int.MaxValue},
         |  "long": ${Long.MaxValue},
         |  "float": ${Float.MaxValue},
         |  "double": ${Double.MaxValue},
         |  "decimal": 0,
         |  "date": "2018-06-11"}""".stripMargin)

    val conformedJSON = """{"byte":127,"short":32767,"integer":2147483647,"long":9223372036854775807,"float":3.4028235E38,"double":1.7976931348623157E308,"decimal":0.00,"date":"2018-06-11","errCol":[],"ConformedByte":-127,"ConformedShort":-32767,"ConformedInt":-2147483647,"ConformedLong":-9223372036854775807,"ConformedFloat":-3.4028234663852886E38,"ConformedDouble":-1.7976931348623157E308,"ConformedDecimal":0.00}"""
  }

  object Null {
    val data: Seq[String] = Seq(
      s"""{ "byte": null,
         |  "short": null,
         |  "integer": null,
         |  "long": null,
         |  "float": null,
         |  "double": null,
         |  "decimal": null,
         |  "date": null}""".stripMargin
    )

    // `.toJSON()` discards null-valued fields
    val conformedJSON = """{"errCol":[]}"""
  }

}
