import 'package:flutter/material.dart';
import 'package:tiffincrm/components/veg_icon.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';
import 'package:tiffincrm/utils/utility.dart';

Future<void> showViewCustomizeForm(
    BuildContext context, List<String> properties, String entity) async {
  App app = App();
  Map<String, IconData> icons = {
    "balance": Icons.account_balance_wallet_outlined,
    "items": Icons.shopping_cart_outlined,
    "address": Icons.location_on_outlined,
    "order_note": Icons.notes_outlined,
    "customer_note": Icons.three_p_outlined,
    "note": Icons.three_p_outlined,
    "customer_labels": Icons.label_outlined,
    "labels": Icons.label_outlined,
    "tiffin_box_to_collect": Icons.shopping_bag
  };
  await Utility.showBottomSheet("Customize Card Info",
      StatefulBuilder(builder: (context, setState) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text("Select data to show on $entity card:"),
                const SizedBox(height: 10),
                ...properties.map((e) => CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    dense: true,
                    title: Row(
                      children: [
                        e == "veg_symbol" ? vegIcon : Icon(icons[e], size: 16),
                        const SizedBox(width: 5),
                        Text(e.replaceAll("_", " ").capitalize()),
                      ],
                    ),
                    value: app.prefs.getBool("show_${entity}_$e") ?? true,
                    onChanged: (value) {
                      setState(() {});
                      app.prefs.setBool("show_${entity}_$e", value!);
                    }))
              ],
            ),
          ),
        ],
      ),
    );
  }),
      topRightButton: TextButton(
          onPressed: () {
            Navigator.pop(context);
          },
          child: const Text(
            "Save",
            style: TextStyle(fontSize: 20),
          )));
}
