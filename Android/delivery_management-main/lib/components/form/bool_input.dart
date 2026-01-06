import 'package:flutter/material.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/components/form/resizable_switch.dart';

class BoolInput extends StatefulWidget {
  final FormInput input;
  const BoolInput(this.input, {super.key});

  @override
  State<BoolInput> createState() => _BoolInputState();
}

class _BoolInputState extends State<BoolInput> {
  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: widget.input.showInAppBar
          ? MainAxisAlignment.end
          : MainAxisAlignment.spaceBetween,
      children: [
        Row(
          children: [
            if (widget.input.prefixIcon != null)
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: widget.input.prefixIcon!,
              ),
            Text(
              widget.input.name,
              style: TextStyle(
                fontSize: 14.0,
                color: widget.input.showInAppBar ? Colors.white : Colors.black,
              ),
              textAlign: TextAlign.start,
            ),
            const SizedBox(width: 5),
          ],
        ),
        const SizedBox(width: 10),
        ResizableSwitch(widget.input.controller.text == "Yes", (val) {
          setState(() {
            widget.input.controller.text = val ? "Yes" : "No";
          });
        }, 30)
      ],
    );
  }
}
