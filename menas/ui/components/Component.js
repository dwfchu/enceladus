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

sap.ui.define([
  "sap/ui/core/UIComponent"
  ],
  function (UIComponent) {
    "use strict";

    return UIComponent.extend("navigation", {
      metadata: {
        rootView : {
          viewName: "components.app",
          id: "rootView",
          type: "XML"
        },
        routing: {
          config: {
            routerClass: "sap.m.routing.Router",
            viewPath: "",
            controlId: "menasApp",
            controlAggregation: "detailPages",
            viewType: "XML"
          },
          routes: [
            {
              name: "root",
              // empty hash - normally the start page
              pattern: "",
              target: ""
            },
            {
              name: "home",
              pattern: "home",
              target: "home"
            },
            {
              name: "login",
              pattern: "login",
              target: "login"
            },
            {
              name: "runs",
              pattern: "runs/:dataset:/:version:/:id:",
              target: "runs"
            },
            {
              name: "schemas",
              pattern: "schema/:id:/:version:", // here id and version are optional
              target: "schemas"
            },
            {
              name: "datasets",
              pattern: "dataset/:id:/:version:",
              target: "dataset"
            },
            {
              name: "mappingTables",
              pattern: "mapping/:id:/:version:",
              target: "mappingTable"
            }
          ],
          targets: {
            login: {
              viewName: "components.login.loginDetail",
              viewLevel: 0,
              viewId: "loginDetailView"
            },
            home: {
              viewName: "components.home.landingPage",
              viewLevel: 1,
              viewId: "landingPage"              
            },
            runs: {
              viewName: "components.run.runDetail",
              viewLevel: 1,
              viewId: "runDetailView"
            },
            schemas: {
              viewName: "components.schema.schemaDetail",
              viewLevel: 1,
              viewId: "schemaDetailView"
            },
            dataset: {
              viewName: "components.dataset.datasetDetail",
              viewLevel: 1,
              viewId: "datasetDetailView"
            },
            mappingTable: {
              viewName: "components.mappingTable.mappingTableDetail",
              viewLevel: 1,
              viewId: "mappingTableDetailView"
            }
          }
        }
      },

      init: function () {
        UIComponent.prototype.init.apply(this, arguments);

        // Parse the current url and display the targets of the route that matches the hash
        this.getRouter().initialize();
      },
      busyIndicatorDelay: 0
    });
  }, /* bExport= */ true);
