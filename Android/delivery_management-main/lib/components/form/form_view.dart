import 'package:country_picker/country_picker.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:tiffincrm/components/form/bool_input.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/components/form/tags_input.dart';
import 'package:flutter/material.dart';
import 'package:textfield_tags/textfield_tags.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/numbers.dart';

import '../learn_more_button.dart';

class FormView extends StatefulWidget {
  final List<FormInput> inputs;
  final String title;
  final bool groupOptional;
  final bool showOptionalGroup;
  final String? learnMoreTag;
  const FormView(this.title, this.inputs,
      {this.groupOptional = false,
      this.showOptionalGroup = false,
      super.key,
      this.learnMoreTag});

  @override
  State<FormView> createState() => _FormViewState();
}

class _FormViewState extends State<FormView> {
  bool showOptionalGroup = false;
  static late GlobalKey<FormState> _formKey;

  @override
  initState() {
    _formKey = GlobalKey<FormState>();
    showOptionalGroup = widget.showOptionalGroup;
    super.initState();
  }

  @override
  void dispose() {
    for (var input in widget.inputs) {
      input.focusNode.dispose();
      if (input.controller != null) {
        input.controller.dispose();
      }
    }
    super.dispose();
  }

  InputDecoration getInputDecoration(FormInput input) {
    return InputDecoration(
        prefixIcon: input.prefixIcon,
        labelText: "${input.name}${input.isRequired ? "" : '(optional)'}",
        errorStyle: const TextStyle(color: Colors.redAccent, fontSize: 16.0),
        helperText: input.helperText.isNotEmpty ? input.helperText : null,
        hintStyle: const TextStyle(
          color: Colors.black45,
        ),
        hintText: input.hintText.isNotEmpty ? input.hintText : null,
        counterText: "",
        border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(Numbers.borderRadius)));
  }

  handleClose() async {
    if (MediaQuery.of(context).viewInsets.bottom != 0) {
      // Keyboard is visible.
      FocusManager.instance.primaryFocus?.unfocus();
      return;
    }
    try {
      getData(ensureChanged: true);
      bool confirmed = await Utility.getConfirmation(
        "Discard changed",
        "Are you sure you want to discard your changes?",
      );
      if (!confirmed) {
        return;
      }
    } catch (e, s) {
      if (e.toString() != "Exception: No change is made") {
        FirebaseCrashlytics.instance.recordError(e, s, reason: "handleClose");
      }
    }
    AppRouter.goBack();
  }

  Widget getInput(FormInput input) {
    if (input.showIf != null) {
      if (input.showIf!(getData()) == false) {
        return const SizedBox();
      }
    }

    return Padding(
        padding: EdgeInsets.only(bottom: input.showInAppBar ? 0 : 13, top: 0),
        child: input.type == "reorder_items"
            ? reorderableListview(input)
            : input.type == "weekdays"
                ? weekdaysSelect(input)
                : input.type == "country"
                    ? countryPicker(input)
                    : input.type == "bool"
                        ? BoolInput(input)
                        : input.type == "select"
                            ? input.canAddOption
                                ? selectWithAddOption(input)
                                : selectDropdown(input)
                            : input.type == "select_multiple"
                                ? input.canAddOption
                                    ? Stack(children: [
                                        StringAutoCompleteTags(input),
                                        Positioned(
                                            top: -3,
                                            left: 10,
                                            child: Container(
                                                color: Colors.white,
                                                child: Text(
                                                    input.name +
                                                        (input.isRequired
                                                            ? ""
                                                            : " (optional)"),
                                                    style: const TextStyle(
                                                        fontSize: 12))))
                                      ])
                                    : checkBoxSelect(input)
                                : normalInput(input));
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (bool didPop, dynamic result) async {
        if (didPop) return;
        await handleClose();
      },
      child: GestureDetector(
        onTap: () {
          FocusScope.of(context).requestFocus(FocusNode());
        },
        child: Scaffold(
          appBar: AppBar(
              backgroundColor: ThemeColors.primary,
              shadowColor: Colors.grey,
              automaticallyImplyLeading: false,
              title: Text(
                widget.title,
                style: const TextStyle(color: Colors.white),
                textAlign: TextAlign.center,
              ),
              actions: widget.learnMoreTag != null
                  ? [learnMoreButton(widget.learnMoreTag!)]
                  : widget.inputs
                      .where((input) => input.showInAppBar)
                      .map((input) => Container(
                          margin: const EdgeInsets.only(right: 10),
                          constraints: const BoxConstraints(maxWidth: 180),
                          child: Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 8),
                            child: Transform.scale(
                                scale: 1, child: getInput(input)),
                          )))
                      .toList()),
          body: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                height: MediaQuery.of(context).size.height -
                    MediaQuery.of(context).viewInsets.bottom -
                    100,
                child: SingleChildScrollView(
                  child: Form(
                    key: _formKey,
                    child: Padding(
                      padding: EdgeInsets.fromLTRB(18.0, 18, 18,
                          MediaQuery.of(context).viewInsets.bottom + 30),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.max,
                        children: List<Widget>.from([
                          const SizedBox(height: 10),
                          ...widget.inputs
                              .where((input) =>
                                  !input.showInAppBar &&
                                  (input.isRequired || !widget.groupOptional))
                              .map((input) => getInput(input)),
                          if (widget.groupOptional)
                            Padding(
                              padding: const EdgeInsets.only(bottom: 16),
                              child: TextButton(
                                  onPressed: () {
                                    setState(() {
                                      showOptionalGroup = !showOptionalGroup;
                                    });
                                  },
                                  child: Text(
                                    "${showOptionalGroup ? "Hide" : "Show"} More info",
                                    style: const TextStyle(
                                        color: ThemeColors.primary,
                                        fontWeight: FontWeight.normal),
                                  )),
                            ),
                          IgnorePointer(
                            ignoring: !showOptionalGroup,
                            child: Opacity(
                              opacity: showOptionalGroup ? 1 : 0,
                              child: Column(children: [
                                ...widget.inputs
                                    .where((input) =>
                                        !input.showInAppBar &&
                                        !input.isRequired &&
                                        widget.groupOptional)
                                    .map((input) => getInput(input)),
                              ]),
                            ),
                          ),
                          const SizedBox(height: 20),
                        ]),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
          bottomSheet: Padding(
            padding: MediaQuery.of(context).padding,
            child: Container(
              decoration: BoxDecoration(
                border: Border(
                    top: BorderSide(
                  color: Colors.grey.shade300,
                )),
                color: Colors.white,
              ),
              padding: const EdgeInsets.all(6),
              child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: <Widget>[
                    TextButton(
                      onPressed: handleClose,
                      child: const Text(
                        'Cancel',
                        style: TextStyle(
                          color: ThemeColors.primary,
                        ),
                        textScaler: TextScaler.linear(1.2),
                      ),
                    ),
                    TextButton(
                      onPressed: () {
                        Map data;
                        try {
                          data = getData(
                              ensureChanged: !widget.title.contains("Add"),
                              checkRequired: true);
                        } catch (e) {
                          Utility.showMessage(e.toString());
                          return;
                        }
                        Navigator.of(context).pop(data);
                      },
                      child: Text(
                        widget.title.contains("Add") ? 'Save' : 'Update',
                        style: const TextStyle(
                          color: ThemeColors.primary,
                        ),
                        textScaler: const TextScaler.linear(1.2),
                      ),
                    ),
                  ]),
            ),
          ),
        ),
      ),
    );
  }

  Map getData({bool checkRequired = false, bool ensureChanged = false}) {
    bool changed = false;
    Map data = {};
    for (var input in widget.inputs) {
      String value = "";
      if (input.type == "select_multiple" && input.canAddOption) {
        StringTagController stringTagController =
            input.controller as StringTagController;
        List<String> tags = stringTagController.getTags ?? [];
        if (stringTagController.getTextEditingController != null &&
            stringTagController.getTextEditingController!.text.isNotEmpty) {
          tags.add(stringTagController.getTextEditingController!.text.trim());
        }
        value = tags.map((e) => e.trim()).join(", ");
      } else if (input.type == "reorder_items") {
        value = input.options.join("##");
      } else {
        value = input.controller.text.trim();
      }

      if (input.isRequired && checkRequired) {
        if (value.isEmpty) {
          throw Exception("${input.name} is required");
        }
        if (input.type == "number" &&
            input.minValue != null &&
            (int.tryParse(value) ?? 0) < input.minValue!) {
          throw Exception(
              "${input.name} must be greater than or equal to ${input.minValue}");
        }
      }
      if (input.value != value) changed = true;
      data[input.name] = value;
    }

    if (!changed && ensureChanged) {
      // updating without changing
      throw Exception("No change is made");
    }
    return data;
  }

  Widget checkBoxSelect(FormInput input) {
    List<String> selectedOptions = input.controller.text
        .split(",")
        .where((element) => element != "" && element != ",")
        .toList();
    List<String> options = input.options
        .where((element) => element
            .toLowerCase()
            .contains(input.optionsSearchController!.value.text.toLowerCase()))
        .toList();

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  "Select ${input.name} (${selectedOptions.length}/${input.options.length})",
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.bold),
                ),
                if (input.options.isNotEmpty)
                  GestureDetector(
                    onTap: () {
                      if (input.controller.text.isEmpty) {
                        input.controller.text = input.options.join(",");
                      } else {
                        input.controller.text = "";
                      }
                      setState(() {});
                    },
                    child: Text(
                      "${input.controller.text.isEmpty ? "Select" : "Deselect"} All",
                      style: const TextStyle(
                          color: ThemeColors.primary,
                          fontWeight: FontWeight.w500),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 8),
            if (input.options.length > 10)
              TextField(
                controller: input.optionsSearchController,
                autofocus: true,
                style: const TextStyle(fontSize: 12),
                decoration: InputDecoration(
                  suffixIcon: Padding(
                    padding: const EdgeInsets.all(12.0),
                    child: Text("${options.length}/${input.options.length}"),
                  ),

                  // labelStyle: TextStyle(color: Colors.black45),
                  hintText: 'Search ${input.name}',
                ),
                onChanged: (value) {
                  setState(() {});
                },
              ),
            options.isEmpty
                ? const Padding(
                    padding: EdgeInsets.only(top: 8.0),
                    child: Text("No options available to select!"),
                  )
                : Container(
                    constraints: BoxConstraints(
                      maxHeight: MediaQuery.of(context).size.height * 0.5,
                    ),
                    child: SingleChildScrollView(
                      child: Column(
                          children: options.map((option) {
                        bool selected = selectedOptions.contains(option);
                        return GestureDetector(
                          onTap: () {
                            if (!selected) {
                              selectedOptions.add(option);
                            } else {
                              selectedOptions.remove(option);
                            }
                            input.controller.text = selectedOptions.join(",");
                            setState(() {});
                          },
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Flexible(child: Text(option)),
                              Checkbox(
                                value: selected,
                                onChanged: (value) {
                                  List<String> temp = input.controller.text
                                      .split(",")
                                      .where((element) =>
                                          element != "" && element != ",")
                                      .toList();

                                  if (value!) {
                                    temp.add(option);
                                  } else {
                                    temp.remove(option);
                                  }
                                  input.controller.text = temp.join(",");
                                  setState(() {});
                                },
                              )
                            ],
                          ),
                        );
                      }).toList()),
                    ),
                  ),
          ],
        ),
      ),
    );
  }

  Widget weekdaysSelect(FormInput input) {
    return Container(
      decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(
            color: Colors.black12,
          ),
          borderRadius: BorderRadius.circular(Numbers.borderRadius)),
      padding: const EdgeInsets.all(8),
      width: double.infinity,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(input.name),
          const SizedBox(height: 8),
          Wrap(
            spacing: 5,
            children: [0, 1, 2, 3, 4, 5, 6].map((e) {
              bool selected = input.controller.text.split(",")[e] == "1";
              return GestureDetector(
                onTap: () {
                  setState(() {
                    List<String> temp = input.controller.text.split(",");
                    temp[e] = temp[e] == "0" ? "1" : "0";
                    input.controller.text = temp.join(",");
                  });
                },
                child: Opacity(
                  opacity: selected ? 1.0 : 0.4,
                  child: Stack(
                    children: [
                      CircleAvatar(
                        radius: 22,
                        backgroundColor:
                            selected ? ThemeColors.primary : Colors.black45,
                        child: CircleAvatar(
                          radius: 19,
                          backgroundColor:
                              selected ? ThemeColors.primary : Colors.white,
                          child: Text(
                            ["M", "T", "W", "T", "F", "S", "S"][e],
                            style: TextStyle(
                                color: selected ? Colors.white : Colors.black,
                                fontSize: 16,
                                fontWeight: FontWeight.bold),
                          ),
                        ),
                      ),
                      if (selected)
                        const Positioned(
                          bottom: 0,
                          right: 0,
                          child: CircleAvatar(
                            radius: 9,
                            child: Icon(
                              Icons.check,
                              size: 16,
                              weight: 10,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 7),
          if (input.helperText.isNotEmpty)
            Text(
              input.helperText,
              style: const TextStyle(color: Colors.black54),
            )
        ],
      ),
    );
  }

  Widget countryPicker(FormInput input) {
    return ListTile(
      contentPadding: const EdgeInsets.only(left: 0, right: 0),
      leading: const Icon(Icons.flag),
      title: Text(input.name),
      trailing: InkWell(
        onTap: () {
          Utility.openCountryPicker((Country country) {
            setState(() {
              input.controller.text = country.toJson()['iso2_cc'];
            });
          });
        },
        child: Text(
            "${Utility.getCountryByCode(input.controller.text)} (${input.controller.text})"),
      ),
    );
  }

  Widget selectWithAddOption(FormInput input) {
    return DropdownButtonHideUnderline(
      child: Autocomplete<String>(
          fieldViewBuilder:
              (context, textEditingController, focusNode, onFieldSubmitted) =>
                  TextField(
                    focusNode: focusNode,
                    controller: textEditingController,
                    decoration: getInputDecoration(input),
                  ),
          onSelected: (option) {
            setState(() {
              input.controller.text = option;
            });
          },
          optionsViewBuilder: (context, onAutoCompleteSelect, options) {
            return Align(
              alignment: Alignment.topLeft,
              child: Material(
                  elevation: 4.0,
                  child: SizedBox(
                      width: MediaQuery.of(context).size.width - 40,
                      child: ListView.builder(
                          padding: EdgeInsets.zero,
                          shrinkWrap: true,
                          itemCount: options.length,
                          itemBuilder: (BuildContext context, int index) {
                            return ListTile(
                              onTap: () {
                                onAutoCompleteSelect(options.elementAt(index));
                              },
                              title: Text(
                                options.elementAt(
                                  index,
                                ),
                              ),
                            );
                          }))),
            );
          },
          initialValue: TextEditingValue(
            text: input.controller.text,
          ),
          optionsBuilder: (inputValue) {
            input.controller.text = inputValue.text;

            return input.options.where((element) =>
                element != "null" &&
                element != "" &&
                element
                    .toLowerCase()
                    .startsWith(inputValue.text.toLowerCase()));
          }),
    );
  }

  Widget normalInput(FormInput input) {
    return TextFormField(
      onTapOutside: (PointerDownEvent event) {
        FocusManager.instance.primaryFocus?.unfocus();
      },
      enabled: !input.readOnly,
      // focusNode: input.focusNode,
      readOnly: input.readOnly || input.type == "date",
      autofocus: input.autoFocus,
      inputFormatters: <TextInputFormatter>[
        if (input.type == "number") FilteringTextInputFormatter.digitsOnly,
        if (input.type == "price")
          FilteringTextInputFormatter.allow(RegExp("[0-9.]")),
      ],
      textCapitalization: TextCapitalization.sentences,
      scrollPadding:
          EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      controller: input.controller,
      keyboardType: input.keyboardType,
      maxLength: input.maxLength,
      onTap: () async {
        //     input.focusNode.requestFocus();
        if (input.type != "date") {
          return;
        }
        DateTime? date = await showDatePicker(
            context: context,
            initialDate: input.value.isEmpty
                ? DateTime.now()
                : DateFormat.yMMMd().parse(input.controller.text),
            firstDate: input.firstDate ?? DateTime.now(),
            lastDate: input.lastDate ?? DateTime.now());

        if (date != null) {
          input.controller.text = DateFormat.yMMMd().format(date);
          setState(() {});
        }
      },
      decoration: InputDecoration(
        isDense: input.showInAppBar,
        prefixIcon: input.prefixIcon,
        prefixIconConstraints:
            const BoxConstraints(minHeight: 36, minWidth: 40),
        floatingLabelBehavior: input.showInAppBar
            ? FloatingLabelBehavior.never
            : input.type == "date"
                ? FloatingLabelBehavior.always
                : FloatingLabelBehavior.auto,
        helperText: input.helperText.isNotEmpty ? input.helperText : null,
        hintStyle: const TextStyle(color: Colors.black45),
        hintText: input.hintText.isNotEmpty ? input.hintText : null,
        counterText: "",
        labelText: input.type == "date"
            ? input.name
            : "${input.name}${input.isRequired ? "" : ' (optional)'}",
      ),
    );
  }

  Widget selectDropdown(FormInput input) {
    return InputDecorator(
      decoration: getInputDecoration(input),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: input.controller.text,
          isDense: true,
          onChanged: (String? newValue) {
            if (input.onChange != null) {
              input.onChange!(newValue, widget.inputs);
            }
            setState(() {
              input.controller.text = newValue!;
            });
          },
          items: input.options.map((String value) {
            return DropdownMenuItem<String>(
              value: value,
              child: Text(
                value.replaceAll(
                  "_",
                  " ",
                ),
                style: const TextStyle(
                    fontWeight: FontWeight.w600,
                    color: ThemeColors.normalBlack),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }

  Widget reorderableListview(FormInput input) {
    return ReorderableListView.builder(
        physics: const ClampingScrollPhysics(),
        shrinkWrap: true,
        itemBuilder: (context, index) {
          return ListTile(
            key: Key('$index'),
            title: Text(input.options[index]),
            trailing: ReorderableDragStartListener(
                index: index, child: const Icon(Icons.drag_handle)),
          );
        },
        itemCount: input.options.length,
        onReorder: (int oldIndex, int newIndex) {
          setState(() {
            if (oldIndex < newIndex) {
              newIndex -= 1;
            }
            final String item = input.options.removeAt(oldIndex);
            input.options.insert(newIndex, item);
          });
        });
  }
}
