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

class ConformanceRuleDialog {

  constructor(controller) {
    this._model = sap.ui.getCore().getModel();

    const eventBus = sap.ui.getCore().getEventBus();
    this._datasetService = new DatasetService(this.model, eventBus);
    this._mappingTableService = new MappingTableService(this.model, eventBus);
    this._controller = controller;
    this._addJoinConditionDialog = new JoinConditionDialogFactory(this.controller, sap.ui.core.Fragment.load).getDialog();
    this._addConcatColumnDialog = new ConcatenationColumnDialogFactory(this.controller, sap.ui.core.Fragment.load).getDialog();
    this._ruleFormFragmentFactory = new ConformanceRuleFormFragmentFactory(this);
    this._ruleForms = new ConformanceRuleFormRepository(this);
    this._rules = this._ruleForms.all;

    this.model.setProperty("/rules", this.rules);
    this.model.setProperty("/dataTypes", this._ruleForms.byType("CastingConformanceRule").dataTypes);
  }

  get controller() {
    return this._controller;
  }

  get model() {
    return this._model;
  }

  get datasetService() {
    return this._datasetService;
  }

  get mappingTableService() {
    return this._mappingTableService;
  }

  get addJoinConditionDialog() {
    return this._addJoinConditionDialog;
  }

  get addConcatColumnDialog() {
    return this._addConcatColumnDialog;
  }

  get ruleFormFactory() {
    return this._ruleFormFragmentFactory;
  }

  get ruleForms() {
    return this._ruleForms;
  }

  get rules() {
    return this._rules;
  }

  onBeforeOpen() {
    this._dialog = this.controller.byId("upsertConformanceRuleDialog");
    this._datasetSchemaFieldSelector = new ConformanceRuleSchemaFieldSelector(this, this._dialog);
    this._targetAttributeSelector = new TargetAttributeFieldSelector(this, this._dialog);
    this._ruleForm = this.controller.byId("ruleForm");
    this.mappingTableService.getList(this._dialog);
    if (this.model.getProperty("/newRule/isEdit")) {
      this.showFormFragment(this.model.getProperty("/newRule/_t"));
    } else {
      this.model.setProperty("/newRule/_t", this.rules[0].ruleType);
      this.showFormFragment(this.rules[0].ruleType);
    }
    this._dialog.setEscapeHandler(() => this.onClosePress());
    this.resetRuleValidation();
  }

  onAfterOpen() {
    const newRule = this.model.getProperty("/newRule");
    if (newRule.isEdit && this.ruleForms.byType(newRule._t).hasSchemaFieldSelector) {
      this.preselectSchemaFieldSelector(newRule._t);
    }
  }

  onClosePress() {
    this.resetRuleForm();
    this._dialog.close();
  }

  onRuleSubmit() {
    let currentDataset = this.model.getProperty("/currentDataset");
    let newRule = $.extend(true, {}, this.model.getProperty("/newRule"));
    this.beforeSubmitChanges(newRule);
    this.resetRuleValidation();
    if (this.ruleForms.byType(newRule._t).isValid(newRule, this.controller._transitiveSchemas, currentDataset.conformance)) {
      if (this.model.getProperty("/newRule/isEdit")) {
        this.updateRule(currentDataset, newRule);
      } else {
        this.addRule(currentDataset, newRule);
      }
      this.onClosePress();
    }
  }

  onAddConcatColumn() {
    const datasetSchema = this._dialog.getModel("schema").oData;
    this.addConcatColumnDialog.setSchema(datasetSchema);
    this.addConcatColumnDialog.onAddPress();
  }

  onAddJoinCondition() {
    this.addJoinConditionDialog.onAddPress();
  }

  onMappingTableSelect(oEv) {
    this.resetTargetAttribute();
    this.resetJoinConditions();

    let mappingTableName = oEv.getParameter("selectedItem").getKey();
    this.selectMappingTable(mappingTableName);
  }

  selectMappingTable(sMappingTableId) {
    this.mappingTableService
      .getAllVersions(sMappingTableId, sap.ui.getCore().byId("mappingTableVersionSelect"))
      .then(data => {
        const latestVersion = data[data.length - 1].version;
        this.model.setProperty("/newRule/mappingTableVersion", latestVersion);
        this.selectMappingTableVersion(sMappingTableId, latestVersion)
      });
  }

  onMTVersionSelect(oEv) {
    this.resetTargetAttribute();
    this.resetJoinConditions();

    let mappingTableId = this.model.getProperty("/newRule/mappingTable");
    let mappingTableVersion = this.model.getProperty("/newRule/mappingTableVersion");
    this.selectMappingTableVersion(mappingTableId, mappingTableVersion);
  }

  selectMappingTableVersion(mappingTableId, mappingTableVersion) {
    new MappingTableRestDAO().getByNameAndVersionSync(mappingTableId, mappingTableVersion).then(mappingTable => {
      const schemaRestDAO = new SchemaRestDAO();
      schemaRestDAO.getByNameAndVersionSync(mappingTable.schemaName, mappingTable.schemaVersion).then(mappingTableSchema => {
        this.addJoinConditionDialog.setMappingTableSchema(mappingTableSchema);
        if (this.model.getProperty("/newRule/_t") === "MappingConformanceRule") {
          const model = new sap.ui.model.json.JSONModel(mappingTableSchema);
          model.setSizeLimit(5000);
          this._dialog.setModel(model, "mappingTableSchema");
        }
      });
      const datasetSchema = this._dialog.getModel("schema").oData;
      this.addJoinConditionDialog.setDatasetSchema(datasetSchema);
    });
  }

  resetTargetAttribute() {
    this._targetAttributeSelector.reset(this._ruleForm);
    this.model.setProperty("/newRule/targetAttribute", null);
  }

  resetJoinConditions() {
    this._addJoinConditionDialog.reset();
    this.model.setProperty("/newRule/newJoinConditions", []);
  }

  onRuleSelect() {
    this.resetRuleForm();
    this.showFormFragment(this.model.getProperty("/newRule/_t"));
    this.resetRuleValidation();
  }

  onDeleteConcatColumn(oEv) {
    let sBindPath = oEv.getParameter("listItem").getBindingContext().getPath();
    let toks = sBindPath.split("/");
    let inputColumnIndex = parseInt(toks[toks.length - 1]);
    let oldInputColumns = this.model.getProperty("/newRule/inputColumns");

    let newInputColumns = oldInputColumns.filter((_, index) => index !== inputColumnIndex);
    this.model.setProperty("/newRule/inputColumns", newInputColumns);
  }

  onDeleteJoinCondition(oEv) {
    let sBindPath = oEv.getParameter("listItem").getBindingContext().getPath();
    let toks = sBindPath.split("/");
    let inputColumnIndex = parseInt(toks[toks.length - 1]);
    let oldInputColumns = this.model.getProperty("/newRule/newJoinConditions");

    let newInputColumns = oldInputColumns.filter((_, index) => index !== inputColumnIndex);
    this.model.setProperty("/newRule/newJoinConditions", newInputColumns);
  }

  onJoinConditionSelect(oEv) {
    const item = oEv.getSource();
    const datasetField = item.data("datasetField");
    const mappingTableField = item.data("mappingTableField");
    const index = item.getParent().indexOfItem(item);

    this.addJoinConditionDialog.onEditPress(index, datasetField, mappingTableField);
  }

  onConcatSelect(oEv) {
    const item = oEv.getSource();
    const concatField = item.data("concatField");
    const index = item.getParent().indexOfItem(item);

    this.addConcatColumnDialog.onEditPress(index, concatField);
  }

  onSchemaFieldSelect(oEv) {
    let ruleType = this._model.getProperty("/newRule/_t");

    switch (ruleType) {
      case "MappingConformanceRule":
        this._targetAttributeSelector.onSchemaFieldSelect(oEv, "/newRule/targetAttribute");
        break;
      case "DropConformanceRule":
        this._datasetSchemaFieldSelector.onSchemaFieldSelect(oEv, "/newRule/outputColumn");
        break;
      default:
        this._datasetSchemaFieldSelector.onSchemaFieldSelect(oEv, "/newRule/inputColumn");
    }
  }

  preselectSchemaFieldSelector(ruleType) {
    switch (ruleType) {
      case "MappingConformanceRule":
        this._targetAttributeSelector.preselectSchemaFieldSelector(this.model.getProperty("/newRule/targetAttribute"));
        break;
      case "DropConformanceRule":
        this._datasetSchemaFieldSelector.preselectSchemaFieldSelector(this.model.getProperty("/newRule/outputColumn"), ruleType);
        break;
      default:
        this._datasetSchemaFieldSelector.preselectSchemaFieldSelector(this.model.getProperty("/newRule/inputColumn"), ruleType);
    }
  }

  beforeShowFragmentChanges() {
    let currentRule = this.model.getProperty("/newRule");
    let newRule = currentRule;

    if (!newRule.isEdit) {
      newRule = (({title, isEdit, order, _t}) => ({title, isEdit, order, _t}))(currentRule);
    }

    if (currentRule._t === "MappingConformanceRule") {
      if (!currentRule.isEdit) {
        newRule.newJoinConditions = [];
        newRule.mappingTable = this._dialog.getModel("mappingTables").oData[0]._id;
        newRule.mappingTableVersion = this._dialog.getModel("mappingTables").oData[0].latestVersion;
      } else {
        let oAttributeMappings = newRule.attributeMappings;
        let aNewJoinConditions = [];
        for (let key in oAttributeMappings) {
          aNewJoinConditions.push({
            mappingTableField: key,
            datasetField: oAttributeMappings[key]
          });
        }
        newRule.newJoinConditions = aNewJoinConditions;
      }
      this.mappingTableService.getAllVersions(newRule.mappingTable, sap.ui.getCore().byId("mappingTableVersionSelect"));
      this.selectMappingTableVersion(newRule.mappingTable, newRule.mappingTableVersion);
    }

    if (!newRule.isEdit && newRule.order === undefined) {
      newRule.order = this.model.getProperty("/currentDataset").conformance.length;
    }

    this.model.setProperty("/newRule", newRule);
  }

  beforeSubmitChanges(newRule) {
    if (newRule._t === "MappingConformanceRule") {
      newRule.attributeMappings = {};
      newRule.newJoinConditions.map(function (joinCondition) {
        newRule.attributeMappings[joinCondition.mappingTableField] = joinCondition.datasetField
      });
      delete newRule.joinConditions;
    }
  }

  addRule(currentDataset, newRule) {
    currentDataset.conformance = RuleUtils.insertRule(currentDataset.conformance, newRule);
    sap.ui.getCore().getEventBus().publish("conformance", "updated", currentDataset.conformance);
    this.datasetService.update(currentDataset);
  }

  updateRule(currentDataset, newRule) {
    currentDataset.conformance[newRule.order] = newRule;
    sap.ui.getCore().getEventBus().publish("conformance", "updated", currentDataset.conformance);
    this.datasetService.update(currentDataset);
  }

  showFormFragment(sFragmentName) {
    let aFragment = this.ruleFormFactory.getFormFragment(sFragmentName);

    aFragment.forEach(oElement =>
      this._ruleForm.addContent(oElement)
    );
    this.beforeShowFragmentChanges();
  }

  resetRuleForm() {
    this._datasetSchemaFieldSelector.reset(this._ruleForm);
    this._ruleForm.removeAllContent();
  }

  resetRuleValidation() {
    const newRule = this.model.getProperty("/newRule");
    this.ruleForms.byType(newRule._t).reset();
  }

}
