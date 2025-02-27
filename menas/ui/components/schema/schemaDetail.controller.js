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
  "sap/ui/core/Fragment",
  "sap/m/MessageToast",
  "sap/m/MessageItem",
  "sap/m/MessageBox",
  "components/AuditTrail"
], function (Controller, Fragment, MessageToast, MessageItem, MessageBox, AuditTrail) {
  "use strict";

  return Controller.extend("components.schema.schemaDetail", {

    onInit: function () {
      this._model = sap.ui.getCore().getModel();
      this._router = sap.ui.core.UIComponent.getRouterFor(this);
      this._router.getRoute("schemas").attachMatched(function (oEvent) {
        let args = oEvent.getParameter("arguments");
        this.routeMatched(args);
      }, this);

      new SchemaDialogFactory(this, Fragment.load).getEdit();

      this._eventBus = sap.ui.getCore().getEventBus();
      this._eventBus.subscribe("schemas", "updated", this.onEntityUpdated, this);

      this._schemaService = new SchemaService(this._model, this._eventBus);
      this._schemaTable = new SchemaTable(this);

      const auditTable = this.byId("auditTrailTable");
      const auditUtils = new AuditTrail(auditTable);
      auditUtils.applyTableUtils();
    },

    onEntityUpdated: function (sTopic, sEvent, oData) {
      this._model.setProperty("/currentSchema", oData);
      this.load();
    },

    usedInNavTo: function (oEv) {
      let source = oEv.getSource();
      sap.ui.core.UIComponent.getRouterFor(this).navTo(source.data("collection"), {
        id: source.data("name"),
        version: source.data("version")
      });
    },

    auditVersionPress: function (oEv) {
      let oSrc = oEv.getSource();
      let oRef = oSrc.data("menasRef");
      this._router.navTo("schemas", {
        id: oRef.name,
        version: oRef.version
      });
    },

    onRemovePress: function (oEv) {
      let currentSchema = this._model.getProperty("/currentSchema");

      MessageBox.show("This action will remove all versions of the schema definition. \nAre you sure?", {
        icon: MessageBox.Icon.WARNING,
        title: "Are you sure?",
        actions: [MessageBox.Action.YES, MessageBox.Action.NO],
        onClose: (oAction) => {
          if (oAction === "YES") {
            this._schemaService.disable(currentSchema.name);
          }
        }
      });
    },

    onExportPress: function (oEv) {
      const schema = this._model.getProperty("/currentSchema");
      new SchemaRestDAO().downloadSchema(schema.name, schema.version)
        .then((data, status, request) => {
          const mimeType = request.getResponseHeader("mime-type");
          const blob = new Blob([data], {type: mimeType});
          const extension = this._getFileExtension(mimeType);
          const filename = `${schema.name}-v${schema.version}.${extension}`;
          if (window.navigator && window.navigator.msSaveOrOpenBlob) {
            window.navigator.msSaveOrOpenBlob(blob, filename);
          } else {
            const event = document.createEvent('MouseEvents'),
              element = document.createElement('a');

            element.download = filename;
            element.href = window.URL.createObjectURL(blob);
            element.dataset.downloadurl = [mimeType, element.download, element.href].join(':');
            event.initEvent('click', true, false);
            element.dispatchEvent(event);
          }
        })
        .fail(() => {
          sap.m.MessageToast.show(`No file uploaded for schema: "${schema.name}", version: ${schema.version}`)
        })
    },

    _getFileExtension: function (mimeType) {
      switch (mimeType) {
        case "application/json":
          return "json";
        case "application/octet-stream":
          return "cob"; //copybook
        default:
          return "";
      }
    },

    routeMatched: function (oParams) {
      if (Prop.get(oParams, "id") === undefined) {
        this._schemaService.getTop().then(() => this.load())
      } else if (Prop.get(oParams, "version") === undefined) {
        this._schemaService.getLatestByName(oParams.id).then(() => this.load())
      } else {
        this._schemaService.getByNameAndVersion(oParams.id, oParams.version).then(() => this.load())
      }
      this.byId("schemaIconTabBar").setSelectedKey("info");
    },

    handleUploadPress: function (oParams) {
      if (this.validateSchemaFileUplaod()) {
        const oFileUpload = this.byId("fileUploader");
        sap.ui.core.BusyIndicator.show();
        // include CSRF token here - it may have changed between sessions
        oFileUpload.removeAllHeaderParameters();
        oFileUpload.addHeaderParameter(new sap.ui.unified.FileUploaderParameter({
          name: "X-CSRF-TOKEN",
          value: localStorage.getItem("csrfToken")
        }));
        oFileUpload.upload();
      }
    },

    handleUploadProgress: function (oParams) {
      console.log((oParams.getParameter("loaded") / oParams.getParameter("total")) * 100)
    },

    handleUploadComplete: function (oParams) {
      sap.ui.core.BusyIndicator.hide();
      let status = oParams.getParameter("status");

      if (status === 500) {
        const sSchemaType = this.byId("schemaFormatSelect").getSelectedItem().getText();

        MessageBox.error(`Failed to upload new schema. Ensure that the file is a valid ${sSchemaType} schema and try again.`)
      } else if (status === 201) {
        this.byId("fileUploader").setValue(undefined);
        MessageToast.show("Schema successfully uploaded.");
        let oData = JSON.parse(oParams.getParameter("responseRaw"));
        model.setProperty("/currentSchema", oData);
        this.load();
        // update the list as well - may be cheaper in future to update locally
        this._eventBus.publish("schemas", "list");
        // nav back to info
        this.byId("schemaIconTabBar").setSelectedKey("info");
      } else if (status === 401 || status === 403) {
        GenericService.clearSession("Session has expired");
      }
    },

    validateSchemaFileUplaod: function () {
      let isOk = true;
      let oSchemaFileUpload = this.byId("fileUploader").getValue();
      if (oSchemaFileUpload === "") {
        this.byId("fileUploader").setValueState(sap.ui.core.ValueState.Error);
        this.byId("fileUploader").setValueStateText("File Name cannot be empty, Please select a file");
        isOk = false;
      }
      return isOk;
    },

    load: function () {
      const currentSchema = this._model.getProperty("/currentSchema");
      this.byId("info").setModel(new sap.ui.model.json.JSONModel(currentSchema), "schema");
      this._schemaTable.model = this._model.getProperty("/currentSchema")
      const auditTable = this.byId("auditTrailTable");
      this._schemaService.getAuditTrail(currentSchema.name, auditTable);
    }

  });
});
