import 'package:flutter/material.dart';

Widget cardChips(List<String> labels,
    {Function? bgColor, Function? textColor}) {
  labels = labels.where((label) => label.isNotEmpty).toList();
  if (labels.isEmpty) return const SizedBox();
  return Padding(
      padding: const EdgeInsets.only(top: 8.0),
      child: Row(
          children: labels
              .map((label) => Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Chip(
                      backgroundColor: bgColor != null
                          ? bgColor(label)
                          : const Color(0xFFFAF9FF),
                      visualDensity: VisualDensity.compact,
                      labelPadding: const EdgeInsets.symmetric(
                          vertical: 0, horizontal: 4.0),
                      label: Text(
                        label,
                        textScaler: const TextScaler.linear(0.8),
                        style: TextStyle(
                            color: textColor != null
                                ? textColor(label)
                                : const Color(0xFF424242)),
                      ),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14.0),
                          side: const BorderSide(
                            width: 0.3,
                            color: Color(0xFFBBBBBB),
                          )),
                    ),
                  ))
              .toList()));
}
