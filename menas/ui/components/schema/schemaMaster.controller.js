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
  "sap/ui/core/mvc/Controller",
  "sap/ui/core/Fragment"
], function (Controller, Fragment) {
  "use strict";

  return Controller.extend("components.schema.schemaMaster", {

    onInit: function () {
      this._eventBus = sap.ui.getCore().getEventBus();
      this._eventBus.subscribe("schemas", "list", this.list, this);

      this._model = sap.ui.getCore().getModel();
      this._router = sap.ui.core.UIComponent.getRouterFor(this);

      new SchemaDialogFactory(this, Fragment.load).getAdd();

      this._schemaService = new SchemaService(this._model, this._eventBus)
      this._searchField = this.byId("schemaSearchField")
    },

    list: function () {
      this._schemaService.getList(this.byId("masterPage"));
      this._schemaService.getSearchSuggestions(this._model, "schema")
    },

    onSearch: function(oEv) {
      this._schemaService.getList(this.byId("masterPage"), oEv.getSource().getValue())
    },

    _searchFilter: function(sValue) {
      return [new sap.ui.model.Filter([
          new sap.ui.model.Filter("name", function(sText) {
            return (sText || "").toUpperCase().indexOf(sValue.toUpperCase()) > -1;
          })], false)];
    },

    onSuggest: function(oEv) {
      let value = oEv.getParameter("suggestValue");
      let filters = [];
      if (value) {
        filters = this._searchFilter(value)
      }
      this._searchField.getBinding("suggestionItems").filter(filters);
      this._searchField.suggest();
    },

    onPressMasterBack: function () {
      this._eventBus.publish("nav", "back");
    },

    schemaSelected: function (oEv) {
      let selected = oEv.getParameter("listItem").data("id");
      this._router.navTo("schemas", {
        id: selected
      });
    }

  });
});
