import 'package:flutter/material.dart';

class ThemeColors {
  static const Color primary = Color(0xff2559da);
  // Color.fromARGB(255, 55, 108, 207);
  // static const Color primary = Color(0xFF5546AF);
  // static const Color primary = Color.fromARGB(255, 77, 53, 213);

  static const LinearGradient primaryGradient =
      LinearGradient(colors: [Color(0xFF6078FE), Color(0xFF4B13E9)]);

  static const Color borderColor = Color(0xFFDEDEDE);
  static const Color normalBlack = Color(0xFF313131);
  //  linear(from, to, colors) Color(linear-gradient(90deg, #6078FE 0%, #4B13E9 100%));

  static Color get(String name) {
    return _get[name] ?? Colors.black;
  }

  static final Map<String, Color> _get = {
    "order_delivered": Colors.green,
    "order_refund": Colors.red,
    "expenses": Colors.purple,
    "cash_added": Colors.blue,
  };
}
