import 'package:flutter/material.dart';

Widget vegIcon = Container(
  width: 12,
  height: 12,
  padding: const EdgeInsets.all(1),
  decoration: BoxDecoration(border: Border.all(color: Colors.green, width: 1)),
  child: const Icon(
    Icons.circle,
    size: 8,
    color: Colors.green,
  ),
);

Widget nonVegIcon = Container(
  width: 12,
  height: 12,
  padding: const EdgeInsets.all(1),
  decoration: BoxDecoration(border: Border.all(color: Colors.red, width: 1)),
  child: const Icon(
    Icons.circle,
    size: 8,
    color: Colors.red,
  ),
);
