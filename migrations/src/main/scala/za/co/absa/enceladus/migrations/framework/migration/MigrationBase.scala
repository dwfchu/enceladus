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

package za.co.absa.enceladus.migrations.framework.migration

import org.apache.log4j.{LogManager, Logger}
import za.co.absa.enceladus.migrations.framework.MigrationUtils
import za.co.absa.enceladus.migrations.framework.dao.DocumentDb

/**
  * This is the base abstract class each concrete migration should inherit from.
  *
  * The base class does not do much by itself. It is only responsible for cloning
  * each collection before any transformation is applied.
  *
  * Each migration should use different migration strategies as mixins. For example,
  * in order to create an instance of JSON to JSON transformer migration you need
  * to define a migration like this:
  *
  * {{{
  *   object MigrationTo1 extends MigrationBase with JsonMigration {...}
  * }}}
  */
abstract class MigrationBase extends Migration {
  private val log: Logger = LogManager.getLogger(this.getClass)

  def execute(db: DocumentDb, collectionNames: Seq[String]): Unit = {
    collectionNames.foreach(collection => cloneCollection(db, collection))
  }

  def validate(collectionNames: Seq[String]): Unit = {}

  /**
    * Clones a collection from one version to another. E.g. from 'schema_v1' to 'schema_v2'.
    */
  private def cloneCollection(db: DocumentDb, collectionName: String): Unit = {
    val sourceCollection = MigrationUtils.getVersionedCollectionName(collectionName, targetVersion - 1)
    val targetCollection = MigrationUtils.getVersionedCollectionName(collectionName, targetVersion)
    log.info(s"Cloning a collection $sourceCollection -> $targetCollection")
    db.cloneCollection(sourceCollection, targetCollection)
  }
}
