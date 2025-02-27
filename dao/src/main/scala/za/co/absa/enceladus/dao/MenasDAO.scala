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

package za.co.absa.enceladus.dao

import za.co.absa.enceladus.model._
import za.co.absa.atum.model._


import scala.util.Try

/** Trait for Menas API DAO. */
trait MenasDAO {

  /**
    * Stores a new Run object in the database by sending REST request to Menas
    *
    * @param run A Run object
    * @return The unique id of newly created Run object or encapsulated exception
    */
  def storeNewRunObject(run: Run): Try[String]

  /**
    * Updates control measure object of the specified run
    *
    * @param uniqueId An unique id of a Run object
    * @param controlMeasure Control Measures
    * @return true if Run object is successfully updated
    */
  def updateControlMeasure (uniqueId: String,
                            controlMeasure: ControlMeasure): Boolean

  /**
    * Updates status of the specified run
    *
    * @param uniqueId An unique id of a run object
    * @param runStatus Status of a run object
    * @return true if Run object is successfully updated
    */
  def updateRunStatus (uniqueId: String,
                       runStatus: RunStatus): Boolean

  /**
    * Updates spline reference of the specified run
    *
    * @param uniqueId An unique id of a Run object
    * @param splineRef Spline Reference
    * @return true if Run object is successfully updated
    */
  def updateSplineReference (uniqueId: String,
                             splineRef: SplineReference): Boolean

  /**
    * Stores a new Run object in the database by loading control measurements from
    * _INFO file accompanied by output data
    *
    * @param uniqueId An unique id of a Run object
    * @param checkpoint A checkpoint to be appended to the database
    * @return true if Run object is successfully updated
    */
  def appendCheckpointMeasure (uniqueId: String,
                               checkpoint: Checkpoint): Boolean

}
