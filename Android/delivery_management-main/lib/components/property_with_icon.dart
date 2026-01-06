import 'package:flutter/material.dart';

Widget getPropertyWithIcon(String text, IconData icon,
    {double fontSize = 14,
    FontWeight fontWeight = FontWeight.normal,
    Color textColor = Colors.black}) {
  if (text.isEmpty) return const SizedBox();

  return Padding(
    padding: const EdgeInsets.only(top: 6),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 18),
        const SizedBox(width: 6),
        Flexible(
          child: Text(
            text,
            style: TextStyle(
              fontSize: fontSize,
              fontWeight: fontWeight,
              color: textColor,
            ),
            softWrap: true,
          ),
        ),
      ],
    ),
  );
}
