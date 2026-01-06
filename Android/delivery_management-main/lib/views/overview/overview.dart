import 'dart:async';

import 'package:tiffincrm/components/refresh_strip.dart';
import 'package:tiffincrm/utils/extensions/display_date.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/components/dropdown_filter.dart';
import 'package:tiffincrm/models/order.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:flutter/material.dart';
import 'package:tiffincrm/utils/extensions/date_check.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/numbers.dart';
import 'package:tiffincrm/views/overview/raw_materials.dart';
import 'package:intl/intl.dart';

List<String> status = ["awaiting", "processing", "delivered"];

class Overview extends StatefulWidget {
  const Overview({super.key});

  @override
  State<Overview> createState() => _OverviewState();
}

class _OverviewState extends State<Overview>
    with AutomaticKeepAliveClientMixin<Overview> {
  bool _keepAlive = true;

  @override
  bool get wantKeepAlive => _keepAlive;
  List<Order> orders = [];
  App app = App();
  bool loading = true;
  String timeFilter = "Dinner";
  String currentStatus = "";
  ValueNotifier<DateTime> dataDownloadedAt =
      ValueNotifier<DateTime>(DateTime.now());

  Map<String, Map<String, int>> stat = {
    'Breakfast': {},
    'Lunch': {},
    'Dinner': {}
  };

  @override
  void initState() {
    app.viewUpdaters["overview"] = updateOrders;

    super.initState();

    Utility.tryCatch(() async {
      updateOrders(forceUpdate: false);
    }, onException: (_) {
      _keepAlive = false;
      updateKeepAlive();
    });
  }

  Future<void> updateOrders({bool forceUpdate = true}) async {
    setState(() {
      loading = true;
    });
    await app.getInitialData(forceUpdate: forceUpdate);
    orders = List<Order>.from(
        app.initialData["deliveries"].map((e) => Order.fromMap(e)));

    stat = app.getStats(orders);
    await app.setVendorStatus(orders, app.initialData['vendor_status']);
    timeFilter = app.selectedTime.isEmpty ? "All" : app.selectedTime;

    Map<String, List<int>> customersCount = {};

    for (MapEntry entry in app.initialData['leftOutLowBal'].entries) {
      stat[entry.key]!['low_balance'] = entry.value;
    }

    for (var order in app.initialData['paused']) {
      for (var key in [order['time'], 'All']) {
        if (order["is_active"] == "0") {
          stat[key]?['paused_orders'] = (stat[key]?['paused_orders'] ?? 0) + 1;
        }
        if (order["status"] == "paused") {
          customersCount[key] ??= [];
          customersCount[key]!.add(int.parse(order['client_id']));
        }
      }
    }

    for (String time in customersCount.keys) {
      stat[time]!['paused_customers'] = customersCount[time]!.toSet().length;
    }
    setState(() {
      loading = false;
    });
    dataDownloadedAt = ValueNotifier<DateTime>(DateTime.now());
  }

  Future<void> openDatePicker() async {
    await showDatePicker(
            context: context,
            firstDate: DateTime.now().subtract(const Duration(days: 180)),
            lastDate: DateTime.now().add(const Duration(days: 120)),
            initialDate: app.selectedDate)
        .then((value) async {
      if (value != null) {
        value = value.add(const Duration(hours: 6));
        app.selectedDate = value;
        app.selectedTime = "";
        await updateOrders();
        if (app.viewUpdaters.containsKey("delivery_list_view")) {
          await app.viewUpdaters["delivery_list_view"]!(forceUpdate: false);
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);

    currentStatus = app.vendorStatus[timeFilter] ?? "";

    return Scaffold(
        drawer: loading ? const SizedBox() : app.drawer,
        onDrawerChanged: (isOpened) {
          if (isOpened) {
            app.changeInDrawer = false;
          } else if (app.changeInDrawer) {
            updateOrders();
          }
        },
        appBar: AppBar(
          titleSpacing: 0,
          title: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                loading ? "Orders " : '$timeFilter orders ',
              ),
              InkWell(
                onTap: openDatePicker,
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    const Icon(Icons.calendar_month, size: 18),
                    const SizedBox(width: 5),
                    Text(
                      DateFormat.MMMd().format(app.selectedDate),
                      style: const TextStyle(fontSize: 16),
                    ),
                    const SizedBox(width: 15),
                  ],
                ),
              )
            ],
          ),
        ),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : RefreshIndicator(
                onRefresh: updateOrders,
                child: SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  child: Column(
                    children: [
                      ValueListenableBuilder(
                          valueListenable: dataDownloadedAt,
                          builder: (context, value, child) => RefreshStrip(
                                update: updateOrders,
                                dataDownloadedAt: value,
                              )),
                      Padding(
                        padding: const EdgeInsets.all(14.0),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Center(
                              child: dropdownFilter(
                                  ["Breakfast", "Lunch", "Dinner", "All"],
                                  timeFilter,
                                  "Time Slots",
                                  (p0) => {
                                        setState(() {
                                          timeFilter = p0!;
                                          if (p0 != "All") {
                                            app.selectedTime = p0;
                                          }
                                        })
                                      },
                                  width: MediaQuery.of(context).size.width,
                                  padding: 10),
                            ),
                            const SizedBox(height: 10),
                            dashboard(),
                            const SizedBox(height: 10),
                            RawMaterials(
                                key: Key(app.selectedDate.toString()),
                                orders,
                                timeFilter,
                                currentStatus),
                            const SizedBox(height: 10),
                            if (timeFilter != "All")
                              Center(
                                  child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Text(
                                      "${app.selectedDate.isToday ? "Today" : app.selectedDate.format('MMM dd')}'s $timeFilter Status:"),
                                  const SizedBox(width: 5),
                                  Image.asset("assets/images/status.png",
                                      color: ThemeColors.primary, width: 30),
                                  const SizedBox(width: 5),
                                  Text(
                                    "Orders ${currentStatus.replaceFirst("cancelled", "skipped").replaceFirst("awaiting", "To Process").replaceFirst("processing", "To Deliver").capitalize()}",
                                    style: const TextStyle(
                                        fontWeight: FontWeight.bold),
                                  ),
                                ],
                              )),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ));
  }

  Widget dashboard() {
    List<String> cardStatus = loading
        ? []
        : [
            if (timeFilter == "All" || currentStatus == "awaiting") "Awaiting",
            ...["Processing", "Delivered"].where((e) =>
                stat[timeFilter]![e.toLowerCase()] != null ||
                e == currentStatus),
            'Cancelled',
            'Paused Orders',
            'Paused Customers',
            'Low Balance',
            ...['Disputed', 'Wasted', 'Refund']
                .where((e) => stat[timeFilter]![e.toLowerCase()] != null),
            "Total Orders",
            "Total Customers"
          ];
    return SizedBox(
      width: MediaQuery.of(context).size.width,
      child: Card(
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(Numbers.borderRadius),
            side: const BorderSide(
              width: 0.2,
              color: ThemeColors.primary,
            )),
        child: SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 6),
            child: Row(
                mainAxisSize: MainAxisSize.max,
                mainAxisAlignment: MainAxisAlignment.center,
                children: cardStatus
                    .map(
                      (e) => Container(
                          padding: const EdgeInsets.symmetric(
                              vertical: 6, horizontal: 16),
                          decoration: const BoxDecoration(
                            border: Border(
                                right: BorderSide(
                                    width: 0.5, color: Color(0xFFE0E0E0))),
                            color: Colors.white,
                          ),
                          child: Row(
                            children: [
                              Text(
                                (stat[timeFilter]?[e
                                            .toLowerCase()
                                            .replaceAll(" ", "_")] ??
                                        0)
                                    .toString(),
                                textScaler: const TextScaler.linear(3),
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  color: cardStatus.indexOf(e) == 0
                                      ? ThemeColors.primary
                                      : const Color(0xFF3D3D3D),
                                ),
                              ),
                              const SizedBox(width: 10),
                              Text(
                                "${(e.contains("Total") || e.contains("Paused") || e.contains("Low Balance")) ? "" : "Orders\n"}${e.replaceFirst("Total Customers", "${timeFilter == "All" ? "Total" : timeFilter} Customers").replaceAll(" ", "\n").replaceAll("Awaiting", "To Process").replaceAll("Processing", "To Deliver")}",
                                style: TextStyle(
                                    color: cardStatus.indexOf(e) == 0
                                        ? ThemeColors.primary
                                        : const Color(0xFF3D3D3D),
                                    fontSize: 12),
                              ),
                            ],
                          )),
                    )
                    .toList()),
          ),
        ),
      ),
    );
  }
}
