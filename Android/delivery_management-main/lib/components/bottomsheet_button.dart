import 'package:flutter/material.dart';
import 'package:tiffincrm/values/my_colors.dart';


Widget bottomSheetButton(Function() onclick, String text,
    {Color bgColor = Colors.transparent,
    double? width = double.infinity,
    double borderRadius = 30,
    EdgeInsets? insetPadding,
    double bottomMargin = 10}) {
  return Container(
    padding: const EdgeInsets.all(8),
    width: width,
    // width,
    color: bgColor,
    margin: EdgeInsets.only(bottom: bottomMargin),
    child: TextButton(
      style: TextButton.styleFrom(
          padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
          backgroundColor: ThemeColors.primary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(borderRadius),
          )),
      onPressed: onclick,
      child: Text(
        text,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 16,
          fontWeight: FontWeight.bold,
        ),
      ),
    ),
  );
}
