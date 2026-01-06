import 'package:flutter/material.dart';
import 'package:tiffincrm/components/form/form_input.dart';

List<FormInput> orderInputs(dynamic defaultData) {
  List<String> times = ["Breakfast", "Lunch", "Dinner"];

  List<String> keysRequired = [
    "time",
    "type",
    "frequency",
    "price",
    "items",
    "is_veg",
    "item_options"
  ];
  for (var key in keysRequired) {
    if (defaultData[key] == null) {
      assert(false, "Missing $key in defaultData");
    }
  }
  return [
    FormInput("Repeat", "select",
        value: defaultData["type"],
        canAddOption: false, onChange: (newValue, inputs) {
      var freq = [1, 1, 1, 1, 1, 1, 1];
      if (newValue == "One Time Order") {
        freq = [0, 0, 0, 0, 0, 0, 0];
        freq[DateTime.now().weekday - 1] = 1;
      } else if (newValue == "On Specific Days") {
        freq = [1, 0, 0, 0, 1, 0, 0];
      }
      for (FormInput input in inputs) {
        if (input.name == "Select day(s)") {
          input.controller.text = freq.join(",");
        } else if (input.name == "Repeat") {
          input.helperText = newValue == "One Time Order"
              ? "Order will be delivered once on first available day."
              : newValue == "On Specific Days"
                  ? "Order will repeat on selected days."
                  : "";
        }
      }
    },
        options: ["Daily", "On Specific Days", "One Time Order"],
        helperText: defaultData["type"] == "One Time Order"
            ? "Order will be delivered once on first available day."
            : defaultData["type"] == "On Specific Days"
                ? "Orders will repeat on selected days."
                : "",
        prefixIcon: const Icon(Icons.repeat)),
    FormInput(
      "Select day(s)",
      "weekdays",
      value: defaultData["frequency"],
      showIf: (data) {
        return data["Repeat"] != "Daily";
      },
    ),
    FormInput("Time Slot", "select",
        value: defaultData["time"],
        options: times,
        canAddOption: false,
        prefixIcon: const Icon(Icons.schedule_outlined)),
    FormInput("Price Per Order", "price",
        value: defaultData["price"],
        prefixIcon: const Icon(Icons.sell, size: 20)),
    FormInput("Items", "select_multiple",
        value: defaultData["items"],
        options: defaultData["item_options"],
        hintText: "Add new item",
        prefixIcon: const Icon(Icons.add_circle_outline)),
    FormInput("Veg Order?", "bool",
        showInAppBar: true,
        value: defaultData["is_veg"],
        helperText: "To categorise order items on home screen!"),
  ];
}
