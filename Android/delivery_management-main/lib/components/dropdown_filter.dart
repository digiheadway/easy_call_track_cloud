import 'package:flutter/material.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/numbers.dart';

Widget dropdownFilter(List<String> values, String? defaultValue, String label,
    Function(String?) onChanged,
    {double width = 116,
    double padding = 4,
    double textScale = 0.7,
    bool underline = false,
    Color textColor = const Color(0xFF666666),
    Map<String, String> valuesWithLabels = const {}}) {
  List<DropdownMenuItem<String>> items = valuesWithLabels.isEmpty
      ? values.map((String value) {
          return DropdownMenuItem<String>(
              value: value,
              child: Text(
                value == "All" ? "All $label" : value,
                maxLines: 1,
                style: TextStyle(
                    fontWeight: FontWeight.w600,
                    overflow: TextOverflow.ellipsis,
                    color: value == defaultValue
                        ? ThemeColors.primary
                        : const Color(0xFF444444)),
                textScaler: const TextScaler.linear(0.7),
              ));
        }).toList()
      : valuesWithLabels.entries
          .map((e) => DropdownMenuItem<String>(
              value: e.key,
              child: Text(e.value == "All" ? "All $label" : e.value,
                  maxLines: 1,
                  style: TextStyle(
                      fontWeight: FontWeight.w600,
                      overflow: TextOverflow.ellipsis,
                      color: e.key == defaultValue
                          ? ThemeColors.primary
                          : const Color(0xFF444444)),
                  textScaler: TextScaler.linear(textScale))))
          .toList();

  Widget dropDown = DropdownButton<String>(
      iconEnabledColor: textColor,
      selectedItemBuilder: (context) {
        return valuesWithLabels.isEmpty
            ? values
                .map((e) => Text(e == "All" ? "All $label" : e,
                    style: TextStyle(
                        overflow: TextOverflow.ellipsis,
                        color: textColor,
                        fontWeight: FontWeight.w600),
                    textScaler: TextScaler.linear(textScale)))
                .toList()
            : valuesWithLabels.entries
                .map((e) => Text(e.value == "All" ? "All $label" : e.value,
                    style: TextStyle(
                        overflow: TextOverflow.ellipsis,
                        color: textColor,
                        fontWeight: FontWeight.w600),
                    textScaler: TextScaler.linear(textScale)))
                .toList();
      },
      value: defaultValue,
      borderRadius: BorderRadius.circular(Numbers.borderRadius),
      hint: Text(label),
      isDense: true,
      isExpanded: true,
      padding: const EdgeInsets.only(left: 6),
      onChanged: onChanged,
      items: items);

  return SizedBox(
    width: width,
    child: underline
        ? dropDown
        : InputDecorator(
            decoration: InputDecoration(
              isDense: true,
              contentPadding: EdgeInsets.all(padding),
            ),
            child: DropdownButtonHideUnderline(child: dropDown),
          ),
  );
}
