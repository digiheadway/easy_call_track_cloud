import 'package:flutter/material.dart';

class ResizableSwitch extends StatelessWidget {
  final bool value;
  final Function(bool value) onChanged;
  final double size;

  const ResizableSwitch(this.value, this.onChanged, this.size, {super.key});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
        height: size,
        child: FittedBox(
          fit: BoxFit.fill,
          child: Switch(
            activeColor: Colors.green,
            value: value,
            onChanged: onChanged,
          ),
        ));
  }
}
