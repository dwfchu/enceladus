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

package za.co.absa.enceladus.migrations.framework.integration.data

import za.co.absa.enceladus.migrations.framework.migration._

class IntegrationTestData {
  object Migration0 extends MigrationBase with CollectionMigration {
    override val targetVersion: Int = 0

    createCollection("foo1")
    createCollection("foo2")
    createCollection("foo3")
    createIndex("foo1", IndexField("date", ASC) :: Nil)
  }

  object Migration1 extends MigrationBase with CollectionMigration with JsonMigration with CommandMigration {
    override val targetVersion: Int = 1

    renameCollection("foo2", "bar2")
    dropCollection("foo3")
    createCollection("bar1")
    createIndex("foo1", IndexField("name", ASC) :: Nil, unique = true)
    dropIndex("foo1", IndexField("date", ASC) :: Nil)

    transformJSON("foo1")(jsonIn => {
      jsonIn.replaceAll("Doodad", "Hickey")
    })

    runCommand("foo1") (versionedCollectionName => {
      s"""{ update: "$versionedCollectionName", updates: [ { q: { name: "Gizmo" }, u: { $$set: { date: "2018-10-10" } },
         | multi: false, upsert: false } ], ordered: true }""".stripMargin
    })
  }

  object Migration2 extends MigrationBase with CollectionMigration  {
    override val targetVersion: Int = 2
    createIndex("foo1", IndexField("name", ASC) :: IndexField("date", ASC) :: Nil)
  }

}
