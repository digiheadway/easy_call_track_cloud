import 'package:flutter/material.dart';
import 'package:tiffincrm/values/my_colors.dart';

Widget hintCard(String boldText, String normalText) {
  return Flexible(
    child: RichText(
        text: TextSpan(
      text: "$boldText: ",
      style: const TextStyle(
        color: ThemeColors.primary,
        fontWeight: FontWeight.bold,
      ),
      children: [
        TextSpan(
          style: const TextStyle(
              color: Color(0xFF323232), fontWeight: FontWeight.normal),
          text:
              normalText,
        )
      ],
    )),
  );
}
