import 'package:flutter/material.dart';

import 'my_colors.dart';
import 'numbers.dart';

class Styles {
  static final ButtonStyle headerButton = TextButton.styleFrom(
    padding: const EdgeInsets.all(8),
    textStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
    backgroundColor: ThemeColors.primary,
    foregroundColor: Colors.white,
    shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Numbers.borderRadius)),
  );

  static const TextStyle textHeading =
      TextStyle(fontWeight: FontWeight.bold, fontSize: 22);

  static ButtonStyle mainButton = ElevatedButton.styleFrom(
      backgroundColor: ThemeColors.primary,
      foregroundColor: Colors.white,
      padding: const EdgeInsets.all(10.0),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(30),
      ),
      minimumSize: const Size.fromHeight(50),
      textStyle: const TextStyle(
          fontSize: 18, color: Colors.white, fontWeight: FontWeight.w500));

  static ButtonStyle normalButton = ElevatedButton.styleFrom(
      backgroundColor: ThemeColors.primary,
      foregroundColor: Colors.white,
      padding: const EdgeInsets.all(10.0),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(30),
      ),
      minimumSize: const Size.fromHeight(50),
      textStyle: const TextStyle(
          fontSize: 14, color: Colors.white, fontWeight: FontWeight.w500));

  static TextStyle buttonText = const TextStyle(
      fontSize: 16, fontWeight: FontWeight.bold, color: Color(0xff343434));

  static OutlineInputBorder inkwellBorder = OutlineInputBorder(
      borderRadius: BorderRadius.circular(Numbers.borderRadius));

  static RoundedRectangleBorder cardShape(bool selected) =>
      RoundedRectangleBorder(
          borderRadius: const BorderRadius.all(
            Radius.circular(Numbers.borderRadius),
          ),
          side: BorderSide(
              color: selected ? ThemeColors.primary : const Color(0xFFDEDEDE)));
}
