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


class EntityDialog {

  constructor(oDialog, entityService, oController) {
    this._oDialog = oDialog;
    this._entityService = entityService;
    this._oController = oController;
  }

  get oDialog() {
    return this._oDialog;
  }

  get entityService() {
    return this._entityService;
  }

  get oController() {
    return this._oController;
  }

  submit() {
    let newEntity = this.oDialog.getModel("entity").oData;
    if (!newEntity.isEdit && newEntity.name && typeof (newEntity.nameUnique) === "undefined") {
      // need to wait for the service call
      setTimeout(this.submit.bind(this), 500);
      return;
    }

    if (this.isValid(newEntity)) {
      // send and update UI
      if (newEntity.isEdit) {
        this.entityService.update(newEntity);
      } else {
        this.entityService.create(newEntity);
      }
      this.cancel(); // close & clean up
    }
  }

  cancel() {
    this.resetValueState();
    this.oDialog.close();
  }

}

class DatasetDialog extends EntityDialog {

  constructor(oDialog, datasetService, schemaService, oController) {
    super(oDialog, datasetService, oController);
    this._schemaService = schemaService;
    oController.byId("newDatasetAddButton").attachPress(this.submit, this);
    oController.byId("newDatasetCancelButton").attachPress(this.cancel, this);
    oController.byId("newDatasetName").attachChange(this.onNameChange, this);
  }

  get schemaService() {
    return this._schemaService;
  }

  resetValueState() {
    this.oController.byId("newDatasetName").setValueState(sap.ui.core.ValueState.None);
    this.oController.byId("newDatasetName").setValueStateText("");

    this.oController.byId("schemaVersionSelect").setValueState(sap.ui.core.ValueState.None);
    this.oController.byId("schemaVersionSelect").setValueStateText("");
  }

  isValid(oDataset) {
    this.resetValueState();

    let hasValidName = EntityValidationService.hasValidName(oDataset, "Dataset",
      this.oController.byId("newDatasetName"));
    let hasValidSchema = EntityValidationService.hasValidSchema(oDataset, "Dataset",
        this.oController.byId("schemaVersionSelect"));
    let hasValidRawHDFSPath = this.oController.byId("newDatasetRawHDFSBrowser").validate();
    let hasValidPublishHDFSPath = hasValidRawHDFSPath ? this.oController.byId("newDatasetPublishHDFSBrowser").validate() : false;

    return hasValidName && hasValidSchema && hasValidPublishHDFSPath && hasValidRawHDFSPath;
  }

  onNameChange() {
    let sName = this.oDialog.getModel("entity").getProperty("/name");
    if (GenericService.isValidEntityName(sName)) {
      DatasetService.hasUniqueName(sName, this.oDialog.getModel("entity"));
    } else {
      this.oDialog.getModel("entity").setProperty("/nameUnique", true);
    }
  }

  onSchemaSelect(oEv) {
    let sSchemaId = oEv.getParameter("selectedItem").getKey();
    this.schemaService.getAllVersions(sSchemaId, this.oController.byId("schemaVersionSelect"),
      this.oDialog.getModel("entity"), "/schemaVersion");
  }

  cancel() {
    sap.ui.getCore().getModel().setProperty("/currentSchemaVersions", undefined);
    super.cancel();
  }

}

class AddDatasetDialog extends DatasetDialog {

  onPress() {
    this.oDialog.setBusy(true);
    this.oDialog.open();

    this.schemaService.getList(this.oDialog).then(oData => {
      this.oDialog.setModel(new sap.ui.model.json.JSONModel({
        name: "",
        description: "",
        schemaName: "",
        schemaVersion: "",
        hdfsPath: "/",
        hdfsPublishPath: "/",
        isEdit: false,
        title: "Add"
      }), "entity");

      this.oDialog.setBusy(false);
    })
  }

}

class EditDatasetDialog extends DatasetDialog {

  onPress() {
    this.oDialog.setBusy(true);
    this.oDialog.open();

    this.schemaService.getList(this.oDialog).then(() => {
      let current = this.oController._model.getProperty("/currentDataset");

      current.isEdit = true;
      current.title = "Edit";
      this.schemaService.getAllVersions(current.schemaName, this.oController.byId("schemaVersionSelect"));

      this.oDialog.setModel(new sap.ui.model.json.JSONModel(jQuery.extend(true, {}, current)), "entity");
      this.oDialog.setBusy(false);
    });
  }

}

class SchemaDialog extends EntityDialog {

  constructor(oDialog, schemaService, oController) {
    super(oDialog, schemaService, oController);
    oController.byId("newSchemaAddButton").attachPress(this.submit, this);
    oController.byId("newSchemaCancelButton").attachPress(this.cancel, this);
    oController.byId("newSchemaName").attachChange(this.onNameChange, this);
  }

  resetValueState() {
    this.oController.byId("newSchemaName").setValueState(sap.ui.core.ValueState.None);
    this.oController.byId("newSchemaName").setValueStateText("");
  }

  isValid(oSchema) {
    this.resetValueState();

    let hasValidName = EntityValidationService.hasValidName(oSchema, "Schema",
      this.oController.byId("newSchemaName"));

    return hasValidName;
  }

  onNameChange() {
    let sName = this.oDialog.getModel("entity").getProperty("/name");
    if (GenericService.isValidEntityName(sName)) {
      SchemaService.hasUniqueName(sName, this.oDialog.getModel("entity"))
    } else {
      this.oDialog.getModel("entity").setProperty("/newSchema/nameUnique", true)
    }
  }

}

class AddSchemaDialog extends SchemaDialog {

  onPress() {
    this.oDialog.setModel(new sap.ui.model.json.JSONModel({
      name: "",
      description: "",
      isEdit: false,
      title: "Add"
    }), "entity");

    this.oDialog.open();
  }

}

class EditSchemaDialog extends SchemaDialog {

  onPress() {
    let current = this.oController._model.getProperty("/currentSchema");
    current.isEdit = true;
    current.title = "Edit";

    this.oDialog.setModel(new sap.ui.model.json.JSONModel(jQuery.extend(true, {}, current)), "entity");
    this.oDialog.open();
  }

}


class MappingTableDialog extends EntityDialog {

  constructor(oDialog, mappingTableService, schemaService, oController) {
    super(oDialog, mappingTableService, oController);
    this._schemaService = schemaService;
    oController.byId("newMappingTableAddButton").attachPress(this.submit, this);
    oController.byId("newMappingTableCancelButton").attachPress(this.cancel, this);
    oController.byId("newMappingTableName").attachChange(this.onNameChange, this);
  }

  get schemaService() {
    return this._schemaService;
  }

  resetValueState() {
    this.oController.byId("newMappingTableName").setValueState(sap.ui.core.ValueState.None);
    this.oController.byId("newMappingTableName").setValueStateText("");

    this.oController.byId("schemaVersionSelect").setValueState(sap.ui.core.ValueState.None);
    this.oController.byId("schemaVersionSelect").setValueStateText("");
  }

  isValid(oMT) {
    this.resetValueState();

    let hasValidName = EntityValidationService.hasValidName(oMT, "Mapping Table",
      this.oController.byId("newMappingTableName"));
    let hasValidSchema = EntityValidationService.hasValidSchema(oMT, "Mapping Table",
      this.oController.byId("schemaVersionSelect"));
    let hasValidHDFSPath = this.oController.byId("addMtHDFSBrowser").validate();

    return hasValidName && hasValidSchema && hasValidHDFSPath;
  }

  onNameChange() {
    let sName = this.oDialog.getModel("entity").getProperty("/name");
    if (GenericService.isValidEntityName(sName)) {
      MappingTableService.hasUniqueName(sName, this.oDialog.getModel("entity"));
    } else {
      this.oDialog.getModel("entity").setProperty("/nameUnique", true);
    }
  }
}

class AddMappingTableDialog extends MappingTableDialog {

  onPress() {
    this.oDialog.setBusy(true);
    this.oDialog.open();

    this.schemaService.getList(this.oDialog).then(oData => {
      this.oDialog.setModel(new sap.ui.model.json.JSONModel({
        name: "",
        description: "",
        schemaName: "",
        schemaVersion: "",
        hdfsPath: "/",
        isEdit: false,
        title: "Add"
      }), "entity");

      this.oDialog.setBusy(false);
    })
  }

}

class EditMappingTableDialog extends MappingTableDialog {

  onPress() {
    this.oDialog.setBusy(true);
    this.oDialog.open();

    this.schemaService.getList(this.oDialog).then(() => {
      const current = this.oController._model.getProperty("/currentMappingTable");

      current.isEdit = true;
      current.title = "Edit";
      this.schemaService.getAllVersions(current.schemaName, this.oController.byId("schemaVersionSelect"));

      this.oDialog.setModel(new sap.ui.model.json.JSONModel(jQuery.extend(true, {}, current)), "entity");
      this.oDialog.setBusy(false);
    });
  }

}
