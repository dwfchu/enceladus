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

package za.co.absa.enceladus.menas.services

import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import za.co.absa.enceladus.menas.repositories.MonitoringMongoRepository

import scala.concurrent.Future

@Service
class MonitoringService @Autowired()(monitoringMongoRepository: MonitoringMongoRepository)
  extends ModelService(monitoringMongoRepository) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getMonitoringDataPoints(datasetName: String, startDate: String, endDate: String): Future[String] = {
    monitoringMongoRepository.getMonitoringDataPoints(datasetName, startDate, endDate).map(_.mkString("[", ",", "]"))
  }

}
