import 'package:flutter/material.dart';
import 'package:textfield_tags/textfield_tags.dart';

class FormInput {
  String name;
  String type;
  String value;
  int maxLength;
  int? minValue;
  int? maxValue;
  String hintText;
  String helperText;
  bool autoFocus = false;
  List<String> options;
  bool readOnly = false;
  bool quantitativeTags = false;
  int tagsLimit;
  DateTime? firstDate = DateTime.now();
  DateTime? lastDate = DateTime.now();
  bool canAddOption = false;
  bool showInAppBar = false;
  TextInputType keyboardType;
  bool isRequired;
  bool Function(dynamic data)? showIf = (data) => true;
  FocusNode focusNode = FocusNode();
  TextEditingController? optionsSearchController;
  Widget? prefixIcon;
  void Function(dynamic newValue, List<FormInput>)? onChange;
  dynamic controller = TextEditingController();
  FormInput(
    this.name,
    this.type, {
    this.value = "",
    this.keyboardType = TextInputType.text,
    this.maxLength = 200,
    this.minValue,
    this.maxValue,
    this.canAddOption = true,
    this.isRequired = true,
    this.options = const [],
    this.tagsLimit = 10,
    this.hintText = "",
    this.helperText = "",
    this.prefixIcon,
    this.showIf,
    this.firstDate,
    this.readOnly = false,
    this.lastDate,
    this.onChange,
    this.showInAppBar = false,
    this.autoFocus = false,
  }) {
    if (value.isNotEmpty) {
      controller.text = value;
    }

    options = options
        .map((e) => e.trim())
        .toSet()
        .where((e) => e.isNotEmpty && e != "null")
        .toList();

    if (prefixIcon == null) {
      if (name.toLowerCase().contains("name")) {
        prefixIcon = const Icon(Icons.person);
      } else if (name.toLowerCase().contains("note")) {
        prefixIcon = const Icon(Icons.notes_outlined);
      } else if (name.toLowerCase().contains("credit")) {
        prefixIcon = const Icon(Icons.payment);
      } else if (name.toLowerCase().contains("email")) {
        prefixIcon = const Icon(Icons.email);
        keyboardType = TextInputType.emailAddress;
      }
      if (type == "phone") {
        prefixIcon = const Icon(Icons.phone);
      }
    }

    if (name == "Items") {
      quantitativeTags = true;
    }

    if (type == "phone") {
      keyboardType = TextInputType.phone;
    } else if (type == "bool") {
      // isRequired = false;
    } else if (type == "number") {
      keyboardType = TextInputType.number;
    } else if (type == "price") {
      keyboardType = const TextInputType.numberWithOptions(decimal: true);
    } else if (type == "select_multiple" && canAddOption) {
      controller = StringTagController();
    } else if (type == "select_multiple" && !canAddOption) {
      optionsSearchController = TextEditingController();
    }
  }
}
