import 'package:flutter/material.dart';
import 'package:tiffincrm/components/property_with_icon.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/phone_call.dart';

import '../../components/learn_more_button.dart';
import '../../models/customer.dart';
import '../../utils/db.dart';
import '../../utils/navigate.dart';
import '../../utils/utility.dart';
import '../../values/icons.dart';
import '../../values/tables.dart';

Widget extraTiffinPickup(
    List<String> selectedZones,
    String curStatus,
    App app,
    List<Customer> customerWithTiffins) {
  List<Customer> tiffinCustomers = customerWithTiffins.where((cx) {
    bool result = selectedZones.contains(cx.zone) ||
        ((app.role == "Admin" ||
                app.deliveryMan!.zones.isEmpty ||
                app.deliveryMan!.zones.contains(cx.zone)) &&
            selectedZones.isEmpty);
    return result;
  }).toList();

  if (tiffinCustomers.isEmpty || curStatus != "processing") {
    return const SizedBox();
  }
  return GestureDetector(
    onTap: () async {
      Map<int, int> tiffins = {};
      for (Customer cx in tiffinCustomers) {
        tiffins[cx.id] = cx.tiffinCounts;
      }

      await Utility.showBottomSheet(
        "Tiffins to Pickup",
        StatefulBuilder(
          builder: (context, setState) => SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: tiffinCustomers
                  .map(
                    (e) => Card(
                      child: Padding(
                        padding: const EdgeInsets.fromLTRB(10, 8, 0, 8),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            SizedBox(
                              width: MediaQuery.of(context).size.width - 170,
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    " ${e.name} ${e.status == "paused" ? "(Paused)" : ""}",
                                    style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                        overflow: TextOverflow.ellipsis),
                                  ),
                                  getPropertyWithIcon(
                                      e.address, MyIcons.address),
                                  getPropertyWithIcon(e.tiffinCounts.toString(),
                                      MyIcons.tiffinCounts),
                                  getPropertyWithIcon(
                                      "${app.currencySymbol}${e.balance}",
                                      MyIcons.balance),
                                ],
                              ),
                            ),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                PopupMenuButton(itemBuilder: (context) {
                                  return [
                                    PopupMenuItem(
                                      onTap: () async {
                                        await call(e.phone);
                                      },
                                      child: const Text("Call"),
                                    ),
                                    if (e.location != null)
                                      PopupMenuItem(
                                        onTap: () async {
                                          await navigate(e.location!);
                                        },
                                        child: const Text("Navigator"),
                                      ),
                                  ];
                                }),
                                Padding(
                                  padding: const EdgeInsets.only(right: 12),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      if (tiffins[e.id]! > 0)
                                        GestureDetector(
                                            onTap: () {
                                              setState(() {
                                                tiffins[e.id] =
                                                    tiffins[e.id]! - 1;
                                              });
                                            },
                                            child: const Icon(Icons.remove)),
                                      Padding(
                                        padding: const EdgeInsets.all(8.0),
                                        child: Text(tiffins[e.id].toString()),
                                      ),
                                      if (tiffins[e.id]! < e.tiffinCounts)
                                        GestureDetector(
                                            onTap: () {
                                              setState(() {
                                                tiffins[e.id] =
                                                    tiffins[e.id]! + 1;
                                              });
                                            },
                                            child: const Icon(Icons.add)),
                                      GestureDetector(
                                        onTap: () async {
                                          if (!(await Utility.getConfirmation(
                                            "Mark As Picked Up?",
                                            "Are you sure to mark ${tiffins[e.id]!} tiffins as picked up?",
                                          ))) {
                                            return;
                                          }

                                          await Database.update(
                                              Tables.clients, e.id, {
                                            "tiffin_counts": (e.tiffinCounts -
                                                    tiffins[e.id]!)
                                                .toString()
                                          });
                                          tiffins[e.id] =
                                              (e.tiffinCounts - tiffins[e.id]!);
                                          int index =
                                              customerWithTiffins.indexWhere(
                                                  (cx) => cx.id == e.id);
                                          e.tiffinCounts = tiffins[e.id]!;
                                          customerWithTiffins[index] = e;
                                          setState(() {});
                                        },
                                        child: const Icon(Icons.check),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ),
        ),
        infoTag: "offday_tiffin_collection",
      );
    },
    child: Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Flexible(
              child: Text(
                  "Tiffin Pickups from ${tiffinCustomers.length} Customer${tiffinCustomers.length > 1 ? "s" : ""} in this Zone!"),
            ),
            const SizedBox(
              width: 8,
            ),
            learnMoreIcon("offday_tiffin_collection", size: 20)
          ],
        ),
      ),
    ),
  );
}
