import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:screenshot/screenshot.dart';
import 'package:share_plus/share_plus.dart';
import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/models/order.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/extensions/date_check.dart';
import 'package:tiffincrm/utils/extensions/display_date.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/numbers.dart';

class RawMaterials extends StatefulWidget {
  final List<Order> orders;
  final String timeFilter;
  final String currentStatus;
  final bool fromDeliveryScreen;
  const RawMaterials(this.orders, this.timeFilter, this.currentStatus,
      {super.key, this.fromDeliveryScreen = false});

  @override
  State<RawMaterials> createState() => _RawMaterialsState();
}

class _RawMaterialsState extends State<RawMaterials> {
  String action = "Prepare";
  App app = App();
  String vegFilter = "All";
  List<String> pieceItems = ["roti", "chapati"];
  ScreenshotController screenshotController = ScreenshotController();

  @override
  void initState() {
    action = widget.currentStatus == "awaiting" ? "Prepare" : "Pack";
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    Map<String, dynamic> data = {};
    List<String> possibleStatus = action == "Pack"
        ? ['processing']
        : [if (widget.currentStatus == "awaiting") "awaiting", "processing"];

    // get all order items for that time and filter
    List<Order> filteredOrders = widget.orders
        .where((element) =>
            (widget.timeFilter == 'All' || element.time == widget.timeFilter) &&
            possibleStatus.contains(element.status))
        .toList();

    // PREPARE ORDER COUNT
    Map<String, int> ordersCount = {"All": 0, "Veg": 0, "Non-Veg": 0};
    ordersCount["All"] = filteredOrders.length;
    for (var element in filteredOrders) {
      String key = element.isVegan ? "Veg" : "Non-Veg";
      ordersCount[key] = ordersCount[key]! + 1;
    }

    // PRPARE ITEM COUNT
    if (action == "Prepare") {
      List<String> allOrderItems = filteredOrders
          .where((element) => (vegFilter == "All" ||
              (vegFilter == "Veg" && element.isVegan) ||
              (vegFilter == "Non-Veg" && !element.isVegan)))
          .map((e) => e.items.split(", "))
          .expand((element) => element)
          .toList();

      // get item count
      for (String item in allOrderItems) {
        int count = 1;
        if (widget.currentStatus == "awaiting") {
          List<String> temp = item.split(" ");
          if (temp.length > 1 && int.tryParse(temp[0]) != null) {
            count = int.parse(temp[0]);
            item = temp.sublist(1).join(" ");
          }
        }
        item = item.trim().toLowerCase();
        data[item] = data[item] == null ? count : data[item]! + count;
      }

      data = Map.fromEntries(
          data.entries.toList()..sort((a, b) => b.value - a.value));
      for (String key in data.keys) {
        data[key] = data[key].toString() +
            (" ${pieceItems.contains(key) ? "pieces" : "servings"}");
      }
    } else {
      List<String> allOrderItems = filteredOrders
          .where((element) => (vegFilter == "All" ||
              (vegFilter == "Veg" && element.isVegan) ||
              (vegFilter == "Non-Veg" && !element.isVegan)))
          .map((e) => e.items)
          .toList();

      // get item count
      for (String items in allOrderItems) {
        List<String> temp =
            items.split(",").map((e) => e.trim().toLowerCase()).toList();
        temp.sort();
        items = temp.join(", ");
        data[items] = data[items] == null ? 1 : data[items]! + 1;
      }

      data = Map.fromEntries(
          data.entries.toList()..sort((a, b) => b.value - a.value));
      for (String key in data.keys) {
        data[key] = "${data[key]} tiffins";
      }
    }

    return Screenshot(
      controller: screenshotController,
      child: SingleChildScrollView(
        child: Card(
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(Numbers.borderRadius),
              side: const BorderSide(
                color: ThemeColors.primary,
              )),
          child: Padding(
            padding: const EdgeInsets.all(12.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (!widget.fromDeliveryScreen)
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      widget.timeFilter != "All" &&
                              ['awaiting', 'processing']
                                  .contains(widget.currentStatus)
                          ? Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      children: [
                                        Text(
                                            action == "Prepare"
                                                ? "Items to Prepare"
                                                : "Tiffins to Pack",
                                            style: const TextStyle(
                                                fontSize: 14,
                                                fontWeight: FontWeight.w500,
                                                color: ThemeColors.primary)),
                                        const SizedBox(width: 5),
                                        GestureDetector(
                                          onTap: () {
                                            setState(() {
                                              action = action == "Prepare"
                                                  ? "Pack"
                                                  : "Prepare";
                                            });
                                          },
                                          child: const Icon(
                                            Icons.change_circle_outlined,
                                            color: ThemeColors.primary,
                                          ),
                                        ),
                                        const SizedBox(width: 5),
                                      ],
                                    ),
                                    Text(
                                        "For ${app.selectedDate.isToday ? "Today" : app.selectedDate.format('MMM dd')}${widget.timeFilter == 'All' ? '' : "'s ${widget.timeFilter}"}",
                                        style: const TextStyle(fontSize: 12)),
                                  ],
                                ),
                                Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    InkWell(
                                        onTap: () async {
                                          try {
                                            Uint8List? image =
                                                await screenshotController
                                                    .capture();
                                            if (image == null) {
                                              Utility.showMessage(
                                                  "Failed to capture");
                                              return;
                                            }

                                            await Share.shareXFiles([
                                              XFile.fromData(image,
                                                  mimeType: 'image/png'),
                                            ], fileNameOverrides: [
                                              "items_to_${action.toLowerCase()}.png"
                                            ]);
                                          } catch (e) {
                                            Utility.showMessage(e.toString());
                                          }
                                        },
                                        child: const Icon(
                                          Icons.share_outlined,
                                          size: 20,
                                          color: ThemeColors.primary,
                                        )),
                                    const SizedBox(width: 10),
                                    learnMoreIcon("items_to_pack", size: 22)
                                  ],
                                )
                              ],
                            )
                          : const Text('All items',
                              style: TextStyle(
                                  fontSize: 14, color: ThemeColors.primary)),
                      const SizedBox(height: 10),
                    ],
                  ),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.start,
                    children: ["All", "Veg", "Non-Veg"]
                        .map((e) => Padding(
                              padding: const EdgeInsets.only(right: 8),
                              child: FilterChip(
                                visualDensity: VisualDensity.compact,
                                padding: const EdgeInsets.symmetric(
                                    vertical: 0, horizontal: 8),
                                labelPadding: const EdgeInsets.symmetric(
                                    vertical: 0, horizontal: 8),
                                label: Text(
                                  "${e == "All" ? "For All Orders" : e} (${ordersCount[e] ?? 0})",
                                  textScaler: const TextScaler.linear(0.8),
                                ),
                                shape: StadiumBorder(
                                    side: BorderSide(
                                        width: vegFilter == e ? 0.5 : 0,
                                        color: vegFilter == e
                                            ? const Color(0xFF3A3939)
                                            : Colors.grey)),
                                selected: vegFilter == e,
                                showCheckmark: false,
                                backgroundColor: Colors.grey.shade100,
                                selectedColor: Colors.grey.shade200,
                                labelStyle: TextStyle(
                                    fontWeight: FontWeight.w600,
                                    color: vegFilter == e
                                        ? const Color(0xFF3A3939)
                                        : Colors.grey),
                                onSelected: (_) {
                                  setState(() {
                                    vegFilter = e;
                                  });
                                },
                              ),
                            ))
                        .toList(),
                  ),
                ),
                data.isEmpty
                    ? Center(
                        child: Text(
                          action == "Pack"
                              ? "No orders processed yet!\nOnly processed orders will be shown here!"
                              : "No orders found!\nTry changing date or time",
                          textAlign: TextAlign.center,
                          style: const TextStyle(color: Colors.black45),
                        ),
                      )
                    : SingleChildScrollView(
                        child: Column(
                          children: data.entries
                              .map((e) => Container(
                                    padding: const EdgeInsets.all(10),
                                    decoration: const BoxDecoration(
                                        border: Border(
                                            bottom: BorderSide(
                                                color: Color(0xFFDEDEDE))),
                                        color: Colors.white),
                                    child: Row(
                                        mainAxisAlignment:
                                            MainAxisAlignment.spaceBetween,
                                        children: [
                                          Flexible(child: Text(e.key)),
                                          Text("x ${e.value}"),
                                        ]),
                                  ))
                              .toList(),
                        ),
                      ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
