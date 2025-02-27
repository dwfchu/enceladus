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

package za.co.absa.enceladus.menas.controllers

import org.springframework.web.bind.annotation._
import org.springframework.beans.factory.annotation.Autowired
import za.co.absa.enceladus.menas.services.OozieService
import java.util.concurrent.CompletableFuture
import org.springframework.http.HttpStatus
import za.co.absa.enceladus.menas.models.OozieCoordinatorStatus

@RestController
@RequestMapping(Array("/api/oozie"))
class OozieController @Autowired() (oozieService: OozieService) extends BaseController {
  import za.co.absa.enceladus.menas.utils.implicits._

  @GetMapping(path = Array("/isEnabled"))
  @ResponseStatus(HttpStatus.OK)
  def isOozieEnabled: Boolean = oozieService.isOozieEnabled

  @GetMapping(path = Array("/coordinatorStatus/{id}"))
  def getCoordinatorStatus(@PathVariable id: String): CompletableFuture[OozieCoordinatorStatus] = oozieService.getCoordinatorStatus(id)  
  
}
