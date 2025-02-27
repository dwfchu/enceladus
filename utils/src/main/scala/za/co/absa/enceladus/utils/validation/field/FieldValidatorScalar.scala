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

package za.co.absa.enceladus.utils.validation.field

import org.apache.spark.sql.types._
import za.co.absa.enceladus.utils.validation.{ValidationError, ValidationIssue}

import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

/**
  * Scalar types schema validation against default value
  */
object FieldValidatorScalar extends FieldValidator {
  override def validateStructField(field: StructField): Seq[ValidationIssue] = {
    try {
      val defaultName = "default"
      if (field.metadata contains defaultName) {
        val default = field.metadata.getString(defaultName)
        field.dataType match {
          case _: BooleanType => default.toBoolean
          case _: ByteType => default.toByte
          case _: ShortType => default.toShort
          case _: IntegerType => default.toInt
          case _: LongType => default.toLong
          case _: FloatType =>
            val a: Float = default.toFloat
            if (a == Float.NaN) {
              throw new NumberFormatException(s"The Float value '$default' is NaN")
            }
            if (a.isInfinity) {
              throw new NumberFormatException(s"The Float value '$default' is infinite or out of range")
            }
          case _: DoubleType =>
            val a = default.toDouble
            if (a == Double.NaN) {
              throw new NumberFormatException(s"The Double value '$default' is NaN")
            }
            if (a.isInfinity) {
              throw new NumberFormatException(s"The Double value '$default' is infinite or out of range")
            }
          case v: DecimalType =>
            val a = BigDecimal(default)
            if (a.precision > v.precision || a.scale > v.scale) {
              throw new NumberFormatException(s"The Decimal(${v.precision},${v.scale}) value '$default' is " +
                s"out of range with precision=${a.precision} and scale=${a.scale} ")
            }
          case _ => true //can return anything, value is not important
        }
      }
      Nil
    }
    catch {
      case NonFatal(e) => Seq(ValidationError(e.toString))
    }
  }
}
