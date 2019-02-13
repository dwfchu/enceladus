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

package za.co.absa.enceladus.model

import java.time.ZonedDateTime
import za.co.absa.enceladus.model.versionedModel.VersionedModel
import za.co.absa.enceladus.model.menas.{Auditable, AuditTrailChange, AuditFieldName}

case class Schema(name: String,
    version: Int = 0,
    description: Option[String],

    dateCreated: ZonedDateTime = ZonedDateTime.now(),
    userCreated: String = null,

    lastUpdated: ZonedDateTime = ZonedDateTime.now(),
    userUpdated: String = null,

    disabled: Boolean = false,
    dateDisabled: Option[ZonedDateTime] = None,
    userDisabled: Option[String] = None,

    fields: List[SchemaField] = List()) extends VersionedModel with Auditable[Schema] {

  override def setVersion(value: Int): Schema = this.copy(version = value)
  override def setDisabled(disabled: Boolean): VersionedModel = this.copy(disabled = disabled)
  override def setLastUpdated(time: ZonedDateTime): VersionedModel = this.copy(lastUpdated = time)
  override def setDateCreated(time: ZonedDateTime): VersionedModel = this.copy(dateCreated = time)
  override def setUserCreated(user: String): VersionedModel = this.copy(userCreated = user)
  override def setUpdatedUser(user: String): VersionedModel = this.copy(userUpdated = user)
  override def setDescription(desc: Option[String]): VersionedModel = this.copy(description = desc)

  override def getAuditMessages(newRecord: Schema): Seq[AuditTrailChange] = {
    super.getPrimitiveFieldsAudit(newRecord,
      Seq(AuditFieldName("description", "Description"))) ++
      super.getSeqFieldsAudit(newRecord, AuditFieldName("fields", "Schema field"))
  }

}
