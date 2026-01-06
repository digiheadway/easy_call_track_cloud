import 'dart:async';

import 'package:flutter/gestures.dart';
import 'package:tiffincrm/components/bottomsheet_button.dart';
import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/components/refresh_strip.dart';
import 'package:tiffincrm/components/view_customize_form.dart';
import 'package:tiffincrm/models/customer.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/utils/extensions/date_check.dart';
import 'package:tiffincrm/utils/extensions/display_date.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';
import 'package:tiffincrm/components/form/form_view.dart';
import 'package:tiffincrm/utils/phone_call.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/tables.dart';
import 'package:tiffincrm/components/dropdown_filter.dart';
import 'package:tiffincrm/utils/navigate.dart';
import 'package:tiffincrm/models/order.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/views/customers/profile/profile_view.dart';
import 'package:flutter/material.dart';

import 'package:tiffincrm/utils/html2pdf.dart' deferred as html2pdf;
import 'package:intl/intl.dart';
import 'package:tiffincrm/views/deliveries/order_card.dart';
import 'package:tiffincrm/views/overview/raw_materials.dart';

import '../../components/custom_filter_chip.dart';
import '../../components/onboarding_no_item.dart';
import '../../values/styles.dart';
import 'extra_tiffin_pickups.dart';

class DeliveryList extends StatefulWidget {
  const DeliveryList({super.key});

  @override
  State<DeliveryList> createState() => _DeliveriesState();
}

class _DeliveriesState extends State<DeliveryList>
    with TickerProviderStateMixin, AutomaticKeepAliveClientMixin<DeliveryList> {
  List<Order> orders = [];
  App app = App();
  String learnTag = "order";

  bool _keepAlive = true;

  @override
  bool get wantKeepAlive => _keepAlive;
  bool loaded = false;
  bool isSearching = false;
  Map<String, int> stat = {};
  List<Customer> customerWithTiffins = [];
  List<int> selected = [];
  List<String> status = ["awaiting", "processing", "delivered"];
  List<String> zones = [];
  List<Order> _filteredOrders = [];
  List<int> shownClientIds = [];
  ValueNotifier<DateTime> dataDownloadedAt =
      ValueNotifier<DateTime>(DateTime.now());

  Map<String, String> filters = {
    "time": "Breakfast",
    "status": "",
    "veg": "",
    "search": "",
  };
  Map<String, List<String>> selectedFilters = {
    "Zones": [],
    "Exclude Reasons": [],
    "Labels": []
  };
  Map<String, Map<String, int>> counts = {};

  List<String> times = ["Breakfast", "Lunch", "Dinner"];
  late TabController tabController;

  List<String> getFlow(String status) {
    List<String> statues = [
      "processing",
      "awaiting",
      "cancelled",
      "delivered",
      "disputed",
      "wasted"
    ];
    List<List<String>> flow = [
      ["delivered", "wasted", "disputed", "refund"], // processing
      ["processing", "cancelled"], // awaiting
      ["awaiting"], // cancelled
      ["wasted", "disputed", "refund"], // delivered
      ["refund", "delivered", "wasted"], // disputed
      ["delivered", "disputed", "refund"], // wasted
    ];

    if (!app.canProcessOrdersToday) {
      return [status == "awaiting" ? "cancelled" : "awaiting"];
    } else {
      List<String> result = flow[statues.indexOf(status)];
      if (app.role != "Admin" && result.contains("refund")) {
        result.remove("refund");
      }
      return result;
    }
  }

  Map<String, String> actionNames = {
    "cancelled": "Mark as Cancelled",
    "awaiting": "Undo Cancellation",
    "processing": "Start Processing",
    "delivered": "Mark as Delivered",
    "disputed": "Raise Dispute",
    "wasted": "Mark as Wasted",
    "refund": "Raise Refund",
  };

  @override
  void initState() {
    if (app.role != "Admin") {
      learnTag = "delivery_man_order";
    }
    openLearnMore(learnTag, firstTime: true);
    app.viewUpdaters["delivery_list_view"] = updateDeliveries;
    status = [if (app.role == "Admin") "awaiting", "processing", "delivered"];
    tabController = TabController(length: status.length, vsync: this);

    Utility.tryCatch(() async {
      updateDeliveries(forceUpdate: false);
    }, onException: (_) {
      _keepAlive = false;
      updateKeepAlive();
    });

    super.initState();
  }

  @override
  void dispose() {
    tabController.dispose();
    super.dispose();
  }

  updateStats() {
    counts = {"Labels": {}, "Zones": {}, "Exclude Reasons": {}};
    stat = {
      "awaiting": 0,
      "delivered": 0,
      "cancelled": 0,
      "Breakfast": 0,
      "processing": 0,
      "Lunch": 0,
      "Dinner": 0,
      "Veg": 0,
      "Non-Veg": 0,
    };

    for (Order order
        in orders.where((element) => element.time == filters['time'])) {
      stat[order.status] =
          stat[order.status] == null ? 1 : stat[order.status]! + 1;
      stat[order.time] = stat[order.time]! + 1;

      if (order.status == filters["status"]) {
        // SET Tags
        order.customerTags.split(", ").forEach((tag) {
          if (tag.isNotEmpty) {
            counts["Labels"]![tag] = counts["Labels"]![tag] == null
                ? 1
                : (counts["Labels"]![tag]!) + 1;
          }
        });

        // SET Zones

        if (order.status == "excluded") {
          counts["Exclude Reasons"]![order.excludeReason] =
              counts["Exclude Reasons"]![order.excludeReason] == null
                  ? 1
                  : counts["Exclude Reasons"]![order.excludeReason]! + 1;
        }

        if (filters["status"] != "excluded" ||
            selectedFilters["Exclude Reasons"]!.isEmpty ||
            selectedFilters["Exclude Reasons"]!.contains(order.excludeReason)) {
          if (filters["veg"] == "" ||
              (filters["veg"] == "Veg" && order.isVegan) ||
              (filters["veg"] == "Non-Veg" && !order.isVegan)) {
            counts["Zones"]![order.zone] = counts["Zones"]![order.zone] == null
                ? 1
                : (counts["Zones"]![order.zone]!) + 1;
          }

          if (selectedFilters["Zones"]!.isEmpty ||
              selectedFilters["Zones"]!.contains(order.zone)) {
            if (order.isVegan) {
              stat["Veg"] = stat["Veg"]! + 1;
            } else {
              stat["Non-Veg"] = stat["Non-Veg"]! + 1;
            }
          }
        }
      }
    }
  }

  Future<void> getTiffinPickups() async {
    if (shownClientIds.isEmpty) {
      return;
    }
    customerWithTiffins = await app.getCustomers(
        where:
            "vendor_id = ${app.vendor.id} AND id NOT IN (${shownClientIds.join(",")}) AND tiffin_counts > 0");
    setState(() {});
  }

  Future<void> updateDeliveries(
      {bool forceUpdate = true, bool manualRefresh = false}) async {
    setState(() {
      selected = [];
      loaded = false;
    });

    await app.getInitialData(forceUpdate: forceUpdate);
    await app.setVendor();
    orders = List<Order>.from(
        app.initialData["deliveries"].map((e) => Order.fromMap(e)));
    shownClientIds = orders
        .where((order) => !["cancelled", "excluded"].contains(order.status))
        .map((order) => order.clientId)
        .toSet()
        .toList();
    if (app.role != "Admin") {
      orders = orders.where((element) => element.status != "awaiting").toList();
    }

    status = {
      ...["awaiting", "processing", "delivered"],
      ...orders.map((e) => e.status).where((e) => e != "excluded"),
      ...["excluded"]
    }.toList();

    await app.setVendorStatus(orders, app.initialData['vendor_status']);

    // keep only order times in B,L,D order
    times = ["Breakfast", "Lunch", "Dinner"];
    if (orders.isNotEmpty) {
      Set<String> uniqueOrderTimes = orders.map((e) => e.time).toSet();
      times = times.toSet().intersection(uniqueOrderTimes).toList();
    }

    filters["time"] = times[0];
    if (app.selectedTime.isNotEmpty && times.contains(app.selectedTime)) {
      filters["time"] = app.selectedTime;
    }

    // SET Status
    if (filters["status"]!.isEmpty || !status.contains(filters["status"])) {
      filters["status"] = app.vendorStatus[filters["time"]] ?? status[0];
      if (!status.contains(filters["status"])) {
        status.add(filters["status"]!);
      }
    }

    if (app.role != "Admin") {
      status.remove("awaiting");
      status.remove("cancelled");
      status.remove("excluded");
      if (!manualRefresh) {
        filters["status"] = "processing";
      }
    }

    // SET Zones
    List<String> orderZones = [];
    for (var order in orders) {
      if (order.zone.isEmpty) {
        continue;
      }
      if (order.status != "excluded") {
        orderZones.add(order.zone);
      }
    }

    orderZones = orderZones.toSet().toList();

    // app.zones contains all zones
    // keep zones from app.zones whose orders are available
    zones = {
      if (orderZones.contains("Zone Not Set!")) "Zone Not Set!",
      ...app.zones.where((zone) => orderZones.contains(zone)),
    }.toList();

    if (app.role != "Admin" && app.deliveryMan!.zones.isNotEmpty) {
      zones =
          zones.where((zone) => app.deliveryMan!.zones.contains(zone)).toList();
    }

    // Sort Orders
    orders
        .sort((a, b) => zones.indexOf(a.zone).compareTo(zones.indexOf(b.zone)));

    // Prepare Tab Controller
    tabController = TabController(
        length: status.length,
        vsync: this,
        initialIndex: status.indexOf(filters["status"]!));
    tabController.addListener(() {
      if (!mounted) return;
      setState(() {
        selected = [];
        if ([filters["status"], status[tabController.index]]
            .contains("excluded")) {
          selectedFilters["Zones"] = [];
        }
        filters["status"] = status[tabController.index];
      });
    });

    // Finalize Update
    setState(() {
      loaded = true;
    });

    dataDownloadedAt = ValueNotifier<DateTime>(DateTime.now());

    if (app.viewUpdaters.containsKey("overview") && forceUpdate) {
      app.viewUpdaters["overview"]!(forceUpdate: false);
    }

    if (app.vendor.trackTiffinboxes) {
      getTiffinPickups();
    }
  }

  Future<void> openDatePicker() async {
    showDatePicker(
            context: context,
            firstDate: DateTime.now().subtract(const Duration(days: 180)),
            lastDate: DateTime.now().add(const Duration(days: 120)),
            initialDate: app.selectedDate)
        .then((value) async {
      if (value != null) {
        value = value.add(DateTime.now().timeZoneOffset);
        app.selectedDate = value;
        await updateDeliveries();
      }
    });
  }

  Widget infoCards(String curStatus) {
    return (["awaiting", "processing"].contains(curStatus) &&
            app.prefs.getBool("hide_${curStatus}_card") != true &&
            app.role == "Admin")
        ? Card(
            color:
                curStatus == "awaiting" ? Colors.amber[100] : Colors.teal[100],
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  SizedBox(
                    width: MediaQuery.of(context).size.width - 100,
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.info),
                        const SizedBox(width: 10),
                        Flexible(
                          child: Text.rich(
                            overflow: TextOverflow.clip,
                            TextSpan(
                              children: [
                                TextSpan(
                                  text: curStatus == "awaiting"
                                      ? "Processing these orders will deduct balance from customers' wallets."
                                      : "These orders are also visible to delivery man. ",
                                ),
                                const WidgetSpan(
                                  child: SizedBox(width: 5),
                                ),
                                TextSpan(
                                  recognizer: TapGestureRecognizer()
                                    ..onTap = () {
                                      openLearnMore(learnTag);
                                    },
                                  text: 'Show More',
                                  style: const TextStyle(
                                    decoration: TextDecoration.underline,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        )
                      ],
                    ),
                  ),
                  GestureDetector(
                    onTap: () {
                      app.prefs.setBool("hide_${curStatus}_card", true);
                      setState(() {});
                    },
                    child: const Icon(Icons.close),
                  )
                ],
              ),
            ),
          )
        : const SizedBox();
  }

  Widget getFilters(String curStatus, List<Order> filteredOrders) {
    bool canSelectAll = filteredOrders.isNotEmpty &&
        (!["refund", "excluded"].contains(filters["status"]));
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Wrap(
        crossAxisAlignment: WrapCrossAlignment.center,
        spacing: 10,
        children: [
          const SizedBox(width: 0),
          InkWell(
            onTap: () {
              if (!canSelectAll) return;

              if (selected.isEmpty) {
                selected = filteredOrders.map((e) => e.id).toList();
              } else {
                selected = [];
              }
              setState(() {});
            },
            child: Text(
              canSelectAll
                  ? "${selected.isEmpty ? "Select All" : "Deselect All"} (${filteredOrders.length})"
                  : "${filteredOrders.length} Results",
              textAlign: TextAlign.center,
              style: TextStyle(
                  color: canSelectAll ? ThemeColors.primary : null,
                  fontWeight: FontWeight.w600),
              textScaler: const TextScaler.linear(0.9),
            ),
          ),
          ...[
            if (curStatus == "excluded") "Exclude Reasons",
            "Zones",
            "Labels",
            "Veg",
            "Non-Veg"
          ].map((e) {
            List<String> options = (counts[e] ?? {}).keys.toList();
            List<String> selectedOptions = selectedFilters[e] ?? [];

            bool filterSelected = e.contains("Veg")
                ? e == filters["veg"]
                : selectedOptions.isNotEmpty;
            return CustomFilterChip(
              isSelected: filterSelected,
              label: e,
              optionCounts: counts[e] ?? {},
              labelWithCount:
                  "$e (${e.contains("Veg") ? (stat[e] ?? 0) : "${filterSelected ? selectedOptions.length : options.length}/${options.length}"})",
              options: options,
              selectedOptions: selectedOptions,
              updateState: ({bool selected = false}) {
                if (e.contains("Veg")) {
                  filters["veg"] = selected ? e : "";
                }
                setState(() {});
              },
            );
          })
        ],
      ),
    );
  }

  Widget getView(String curStatus, List<Order> filteredOrders) {
    filteredOrders =
        filteredOrders.where((e) => e.status == curStatus).toList();

    return Column(
      mainAxisSize: MainAxisSize.max,
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        ValueListenableBuilder(
            valueListenable: dataDownloadedAt,
            builder: (context, value, child) => RefreshStrip(
                  update: () => updateDeliveries(manualRefresh: true),
                  dataDownloadedAt: value,
                )),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(left: 8.0, right: 8, top: 8),
            child: RefreshIndicator(
              onRefresh: () => updateDeliveries(manualRefresh: true),
              child: Column(
                mainAxisSize: MainAxisSize.max,
                children: [
                  infoCards(curStatus),
                  if (orders.isNotEmpty) getFilters(curStatus, filteredOrders),
                  const SizedBox(height: 4),
                  Expanded(
                    child: ListView.builder(
                      physics: const AlwaysScrollableScrollPhysics(),
                      shrinkWrap: true,
                      key: Key(filters.toString()),
                      padding: const EdgeInsets.only(bottom: 0),
                      itemCount: filteredOrders.isEmpty
                          ? 1
                          : filteredOrders.length + 1,
                      itemBuilder: (BuildContext context, int index) {
                        if (orders.isEmpty) {
                          return curStatus == "awaiting"
                              ? onboardingNoItem(
                                  "No Orders To Process Yet!",
                                  "Orders will be automatically generated from meal plans\nof your customers and shown here for each time slot.",
                                  Icons.food_bank,
                                  linkTag: learnTag)
                              : curStatus == "processing"
                                  ? onboardingNoItem(
                                      "No Orders To Deliver Yet!",
                                      "Processed orders will be shown here to deliver!",
                                      Icons.delivery_dining_outlined,
                                      linkTag: learnTag)
                                  : onboardingNoItem(
                                      "No Orders Delivered Yet!",
                                      "Delivered orders will be shown here!",
                                      Icons.delivery_dining_outlined,
                                      linkTag: learnTag);
                        }
                        if (filteredOrders.isEmpty) {
                          return Center(
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                extraTiffinPickup(
                                    selectedFilters["Zones"]!,
                                    filters["status"]!,
                                    app,
                                    customerWithTiffins),
                                const SizedBox(height: 10),
                                Text(app.role == "Admin"
                                    ? "No $curStatus orders found!"
                                    : "No deliveries found!"),
                              ],
                            ),
                          );
                        }
                        if (index == 0) {
                          return extraTiffinPickup(selectedFilters["Zones"]!,
                              filters["status"]!, app, customerWithTiffins);
                        } else {
                          var item = filteredOrders[index - 1];
                          return OrderCard(
                            order: item,
                            curStatus: curStatus,
                            isSelected: selected.contains(item.id),
                            onSelect: () {
                              setState(() {
                                if (selected.contains(item.id)) {
                                  selected.remove(item.id);
                                } else {
                                  selected.add(item.id);
                                }
                              });
                            },
                            menuItems: [
                              PopupMenuButton(
                                padding: const EdgeInsets.only(left: 18),
                                itemBuilder: (context) => [
                                  if (!["refund", "excluded"]
                                      .contains(curStatus))
                                    ...getFlow(curStatus)
                                        .map((e) => PopupMenuItem(
                                            child: Text(actionNames[e]!),
                                            onTap: () {
                                              handleDelivery([e], [item.id]);
                                            })),
                                  if (app.role == "Admin")
                                    PopupMenuItem(
                                      child:
                                          const Text("View Customer Profile"),
                                      onTap: () async {
                                        await app.fetchCustomers();
                                        if (!context.mounted) return;
                                        await Navigator.of(context).push(
                                            MaterialPageRoute(
                                                builder: (context) =>
                                                    CustomerProfile(
                                                        item.clientId)));
                                        await updateDeliveries();
                                      },
                                    ),
                                  if (item.location != null)
                                    PopupMenuItem(
                                      child: const Text("Navigate"),
                                      onTap: () async {
                                        try {
                                          navigate(item.location!);
                                        } catch (e) {
                                          Utility.showMessage(e.toString());
                                        }
                                      },
                                    ),
                                  PopupMenuItem(
                                    child: const Text("Call"),
                                    onTap: () async {
                                      await call(item.customerPhone);
                                    },
                                  )
                                ],
                              ),
                              ["awaiting", "cancelled", "processing"]
                                      .contains(curStatus)
                                  ? TextButton(
                                      style: TextButton.styleFrom(
                                          padding:
                                              const EdgeInsets.only(right: 12)),
                                      onPressed: () {
                                        handleDelivery(
                                            curStatus == "awaiting"
                                                ? ["cancelled"]
                                                : [getFlow(curStatus)[0]],
                                            [item.id]);
                                      },
                                      child: Row(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          Icon(
                                            curStatus == "awaiting"
                                                ? Icons.cancel
                                                : curStatus == "processing"
                                                    ? Icons.check
                                                    : Icons.undo,
                                            size: 16,
                                            color: ThemeColors.primary,
                                          ),
                                          const SizedBox(width: 5),
                                          Text(
                                            curStatus == "awaiting"
                                                ? "Cancel"
                                                : curStatus == "processing"
                                                    ? "Deliver"
                                                    : "Undo Cancellation",
                                            style: const TextStyle(
                                                color: ThemeColors.primary),
                                          ),
                                        ],
                                      ))
                                  : Container(),
                            ],
                          );
                        }
                      },
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
        curStatus == "awaiting" &&
                filteredOrders.isNotEmpty &&
                app.canProcessOrdersToday &&
                selected.isEmpty
            ? bottomSheetButton(
                () async {
                  bool processed = await handleDelivery(
                      ["processing"], filteredOrders.map((e) => e.id).toList());
                  if (!processed) return;

                  await Database.updateMany(
                      'vendors_status',
                      {
                        'vendor_id': app.vendorId,
                        'time': filters["time"]!,
                        'date': app.selectedDate.toString().split(" ")[0]
                      },
                      {'status': "processing"},
                      silent: true);
                  filters["status"] = "processing";
                  app.askForReview();
                },
                "Process ${filteredOrders.length > 1 ? "All " : ""}${filteredOrders.length} Order(s)",
                width: double.infinity,
                bottomMargin: 0,
              )
            : const SizedBox(),
      ],
    );
  }

  Future<void> printLabels() async {
    String htmlContent = """
                      <style>
                          div {
                            padding: 10px;
                            border: 1px solid black;
                            border-radius: 10px;
                            margin: 10px;
                            min-width: 300px;
                            position: relative;
                          }
      
                          span {
                            position: absolute;
                            right: 10px;
                            bottom: 10px;
                          }
      
                          section {
                            width: 1200px;
                            display: flex;
                            flex-direction: row;
                            flex-wrap: wrap;
                          }
                      </style>
                      <section>""";
    for (var order
        in _filteredOrders.where((e) => e.status == filters["status"])) {
      htmlContent += """
         <div>
        <h3>${app.vendor.businessName.isNotEmpty ? app.vendor.businessName : "Your Business Name"}</h3>
        <p><b>Name:</b> ${order.customerName}</p>
        <p><b>Zone:</b> ${order.zone.isEmpty ? "Zone Not Set" : order.zone}</p>
        <p><b>Phone:</b> ${order.customerPhone}</p>
        <p><b>Meal:</b> ${order.items}</p>
        <p><b>Remark:</b> ${order.note}</p>
        <span>${app.selectedDate.format("dd-MM-yyyy")}</span>
         </div>
      """;
    }

    htmlContent += """</section>""";
    await html2pdf.loadLibrary();
    await html2pdf.html2pdf(htmlContent, "labels");
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);

    _filteredOrders = orders.where((element) {
      return element.time == filters["time"] &&
          (filters["veg"] == "" ||
              (filters["veg"] == "Veg" && element.isVegan) ||
              (filters["veg"] == "Non-Veg" && !element.isVegan)) &&
          (selectedFilters["Zones"]!.contains(element.zone) ||
              selectedFilters["Zones"]!.isEmpty) &&
          (filters["status"] != "excluded" ||
              selectedFilters["Exclude Reasons"]!.isEmpty ||
              selectedFilters["Exclude Reasons"]!
                  .contains(element.excludeReason)) &&
          (selectedFilters["Labels"]!.isEmpty ||
              selectedFilters["Labels"]!
                  .any((e) => element.customerTags.contains(e))) &&
          element
              .toMap()
              .toString()
              .toLowerCase()
              .contains(filters["search"]!.toLowerCase());
    }).toList();
    updateStats();

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) return;

        if (selected.isNotEmpty) {
          setState(() {
            selected = [];
          });
          return;
        }

        if (isSearching) {
          setState(() {
            isSearching = false;
          });
          return;
        }

        if (app.role != "Admin") {
          AppRouter.exit();
        }
      },
      child: Scaffold(
        resizeToAvoidBottomInset: false,
        drawer: isSearching || selected.isNotEmpty
            ? null
            : loaded
                ? app.drawer
                : app.role == "Admin"
                    ? const Text("")
                    : null,
        onDrawerChanged: (isOpened) {
          if (isOpened) {
            app.changeInDrawer = false;
          } else if (app.changeInDrawer) {
            updateDeliveries();
          }
        },
        appBar: prepareAppBar(),
        body: !loaded
            ? const Center(child: CircularProgressIndicator())
            : TabBarView(
                physics: const NeverScrollableScrollPhysics(),
                controller: tabController,
                children:
                    status.map((e) => getView(e, _filteredOrders)).toList()),
      ),
    );
  }

  Future<bool> handleDelivery(
      List<String> possibleStatus, List<int> orderIds) async {
    String status = possibleStatus[0];
    Order firstOrder = orders.where((e) => e.id == orderIds[0]).first;
    bool trackTiffinCounts = app.vendor.trackTiffinboxes &&
        orderIds.length == 1 &&
        status == "delivered";
    int customerTiffins = firstOrder.customerTiffinCounts;
    int tiffinsPicked = 1;
    bool tiffinDropped = true;
    bool confirmed = await Utility.getConfirmation(
        (possibleStatus.length == 1)
            ? status == "awaiting"
                ? "Undo Cancellation"
                : actionNames[status]!
            : "Choose action!",
        "Are you sure to continue?",
        okText: "Confirm",
        contentWidget: possibleStatus.length != 1
            ? StatefulBuilder(
                builder: (context, setState) => Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    const Text(
                        "Choose the action to be performe on selected orders"),
                    const SizedBox(height: 10),
                    dropdownFilter(possibleStatus, status, 'Action',
                        (String? p0) {
                      setState(() {
                        status = p0 ?? "";
                      });
                    },
                        width: double.infinity,
                        padding: 8,
                        valuesWithLabels: Map.fromEntries(possibleStatus
                            .map((e) => MapEntry(e, actionNames[e] ?? "")))),
                  ],
                ),
              )
            : trackTiffinCounts
                ? SizedBox(
                    width: MediaQuery.of(context).size.width * 0.8,
                    child: StatefulBuilder(
                      builder: (context, setState) => Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Card(
                            child: Padding(
                              padding: const EdgeInsets.all(8.0),
                              child: Column(
                                children: [
                                  Text(
                                    "Tiffin boxes to Collect: $customerTiffins",
                                    style: const TextStyle(
                                      fontSize: 14,
                                    ),
                                    textAlign: TextAlign.center,
                                  ),
                                  const SizedBox(height: 20),
                                  Row(
                                    mainAxisAlignment:
                                        MainAxisAlignment.spaceBetween,
                                    children: [
                                      const Text("Left 1 Tiffin box"),
                                      SizedBox(
                                        height: 30,
                                        child: FittedBox(
                                          fit: BoxFit.fill,
                                          child: Switch(
                                              value: tiffinDropped,
                                              onChanged: (value) {
                                                setState(() {
                                                  tiffinDropped = value;
                                                });
                                              }),
                                        ),
                                      )
                                    ],
                                  ),
                                  const SizedBox(height: 5),
                                  Row(
                                    mainAxisAlignment:
                                        MainAxisAlignment.spaceBetween,
                                    children: [
                                      const Text("Tiffin boxes Picked Up: "),
                                      Card(
                                        child: Row(
                                          children: [
                                            GestureDetector(
                                                onTap: () {
                                                  setState(() {
                                                    if (tiffinsPicked > 0) {
                                                      tiffinsPicked--;
                                                    }
                                                  });
                                                },
                                                child: const Padding(
                                                  padding: EdgeInsets.symmetric(
                                                      horizontal: 5),
                                                  child: Icon(
                                                    Icons.remove,
                                                    size: 20,
                                                  ),
                                                )),
                                            const SizedBox(width: 5),
                                            Text(
                                              tiffinsPicked.toString(),
                                              style: const TextStyle(
                                                  fontSize: 20,
                                                  fontWeight: FontWeight.bold),
                                            ),
                                            const SizedBox(width: 5),
                                            GestureDetector(
                                                onTap: () {
                                                  setState(() {
                                                    if (tiffinsPicked <=
                                                        customerTiffins) {
                                                      tiffinsPicked++;
                                                    }
                                                  });
                                                },
                                                child: const Padding(
                                                  padding: EdgeInsets.symmetric(
                                                      horizontal: 5),
                                                  child: Icon(
                                                    Icons.add,
                                                    size: 20,
                                                  ),
                                                )),
                                          ],
                                        ),
                                      )
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 10),
                          const Text("Are you sure to mark as delivered?"),
                        ],
                      ),
                    ),
                  )
                : app.vendor.trackTiffinboxes &&
                        orderIds.length > 1 &&
                        status == "delivered"
                    ? const Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Card(
                              child: Padding(
                            padding: EdgeInsets.all(8.0),
                            child: Text("Tiffin boxes: Dropped 1, Picked 1"),
                          )),
                          SizedBox(height: 10),
                          Text("Are you sure to mark as delivered?"),
                        ],
                      )
                    : null);
    if (confirmed == false) {
      return false;
    }

    try {
      if (["cancelled", "processing"].contains(status)) {
        await addDeliveries(orderIds, status, date: app.selectedDate);
      } else if (status == "awaiting") {
        await deleteDeliveries(orderIds, date: app.selectedDate);
      } else {
        await updateDeliveriesStatus(orderIds, status, date: app.selectedDate);
      }
    } catch (e) {
      if (e.toString().contains("Duplicate entry")) {
        Utility.showMessage(
            "Some order(s) are already $status\nTry Again After Refreshing!");
        return false;
      }
    }

    String message = status;
    if (message == "processing") {
      message = "Processed";
    } else if (message == "awaiting") {
      message = "Cancellation Undo";
    }
    Utility.showMessage("Orders $message Successfully");

    if (trackTiffinCounts) {
      await Database.update(Tables.clients, firstOrder.clientId, {
        "tiffin_counts":
            (customerTiffins + (tiffinDropped ? 1 : 0) - tiffinsPicked.toInt())
                .toString(),
      });
    }

    await updateDeliveries();

    // Handle Notifications
    if (["delivered", "cancelled"].contains(status) && orderIds.isNotEmpty) {
      app.showNotification(
        orders
            .where((e) => orderIds.contains(e.id))
            .map((e) => e.clientId)
            .toList(),
        "${filters["time"]} has been $status!",
      );
    }

    // Update customers_view/revenue_view if action involve transaction (refunded/processed)
    if (["refund", "processing"].contains(status)) {
      for (var view in ["revenue_view", "customers_list_view"]) {
        if (app.viewUpdaters.containsKey(view)) {
          app.viewUpdaters[view]!();
        }
      }
    }
    return true;
  }

  deleteDeliveries(List<int> selected, {required DateTime date}) async {
    return await Database.request({
      'table': 'deliveries',
      'where':
          "order_id IN (${selected.join(",")}) AND date = '${date.toString().split(" ")[0]}'",
      'action': 'delete',
      'limit': selected.length
    });
  }

  Future<void> addDeliveries(List<int> orderIds, String status,
      {DateTime? date}) async {
    if (date == null || date.isToday) {
      date = DateTime.now();
    } else {
      date = DateTime.parse(app.getDateTime(date.toString().split(" ")[0]));
    }
    await Database.addMany(
        Tables.deliveries,
        orderIds
            .map((orderId) => {
                  "order_id": orderId,
                  "hash": "${orderId}_${date.toString().split(" ")[0]}",
                  "status": status,
                  "timestamp": date.toString().split(".")[0]
                })
            .toList());
  }

  Future<void> updateDeliveriesStatus(List<int> orderIds, String status,
      {DateTime? date}) async {
    date ??= DateTime.now();
    await Database.updateMany(
        "deliveries",
        "order_id IN (${orderIds.join(",")}) AND date = '${date.toString().split(" ")[0]}'",
        {"status": status});
  }

  Future<void> _handleReorderZones() async {
    FormInput zonesInput = FormInput(
      "Zones",
      "reorder_items",
      value: app.zones.join("##"),
      options: app.zones,
    );
    dynamic formData =
        await AppRouter.navigateTo(MaterialPageRoute(builder: (context) {
      return FormView(
        "Reorder Zones",
        [zonesInput],
        learnMoreTag: "reorder_zones",
      );
    }));

    if (formData == null) {
      return;
    }

    await app.updateVendor({"zones_in_order": formData["Zones"]});
    await app.setVendor(update: true);
    Utility.showMessage("Zones Reordered Successfully!");
    setState(() {});
    await updateDeliveries();
  }

  AppBar prepareAppBar() {
    return AppBar(
      toolbarHeight: 45,
      titleSpacing: isSearching || app.role != "Admin" ? 10 : 0,
      bottom: TabBar(
        padding: const EdgeInsets.all(0),
        isScrollable: true,
        controller: tabController,
        tabAlignment: TabAlignment.start,
        tabs: status
            .map((e) => Tab(
                text:
                    "${e == "awaiting" ? "To Process" : e == "processing" ? "To Deliver" : e == "disputed" ? "Problem Reported" : e.capitalize()} (${stat[e] ?? 0})"))
            .toList(),
      ),
      title: isSearching
          ? SizedBox(
              height: 35,
              child: Row(
                children: [
                  GestureDetector(
                    child: const Icon(Icons.arrow_back),
                    onTap: () {
                      setState(() {
                        filters["search"] = "";
                        isSearching = false;
                      });
                    },
                  ),
                  const SizedBox(width: 20),
                  Expanded(
                    child: TextField(
                      autofocus: true,
                      style: const TextStyle(fontSize: 12),
                      decoration: const InputDecoration(
                        suffixIcon: Icon(Icons.search),
                        contentPadding: EdgeInsets.fromLTRB(8, 0, 8, 0),
                        // labelStyle: TextStyle(color: Colors.black45),
                        hintText: 'Search Orders',
                      ),
                      onChanged: (value) {
                        setState(() {
                          filters["search"] = value;
                        });
                      },
                    ),
                  ),
                ],
              ),
            )
          : selected.isNotEmpty
              ? Row(
                  mainAxisAlignment: MainAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(
                        visualDensity: VisualDensity.compact,
                        onPressed: () => setState(() => selected = []),
                        icon: const Icon(Icons.arrow_back)),
                    Text("${selected.length} Selected"),
                  ],
                )
              : times.isNotEmpty
                  ? dropdownFilter(
                      times,
                      filters["time"]!,
                      "Time",
                      (newValue) => {
                            setState(() {
                              filters["time"] = newValue ?? "Breakfast";
                              app.selectedTime = filters["time"]!;
                              selected = [];
                            })
                          },
                      underline: true,
                      textScale: 0.9,
                      textColor: Colors.white,
                      padding: 20,
                      width: 100)
                  : const SizedBox(),
      actions: isSearching
          ? [
              if (filters['status'] != "refund" && selected.isNotEmpty)
                PopupMenuButton(
                  padding: const EdgeInsets.only(left: 18),
                  itemBuilder: (context) => getFlow(filters['status']!)
                      .map((e) => PopupMenuItem(
                          child: Text(actionNames[e]!),
                          onTap: () {
                            handleDelivery([e], selected);
                          }))
                      .toList(),
                ),
            ]
          : selected.isNotEmpty
              ? [
                  TextButton(
                      style: Styles.headerButton,
                      onPressed: () {
                        handleDelivery(
                            [getFlow(filters['status']!)[0]], selected);
                      },
                      child:
                          Text(actionNames[getFlow(filters['status']!)[0]]!)),
                  const SizedBox(width: 4),
                  if (getFlow(filters['status']!).length > 1)
                    TextButton(
                        style: Styles.headerButton,
                        onPressed: () {
                          handleDelivery(
                              getFlow(filters['status']!).sublist(1), selected);
                        },
                        child: getFlow(filters['status']!).length == 2
                            ? Text(actionNames[getFlow(filters['status']!)[1]]!)
                            : const Row(
                                children: [
                                  Text("Other"),
                                  Icon(Icons.arrow_drop_down, size: 18),
                                ],
                              )),
                  const SizedBox(
                    width: 5,
                  )
                ]
              : [
                  IconButton(
                    icon: const Icon(Icons.search),
                    onPressed: () {
                      setState(() {
                        isSearching = true;
                      });
                    },
                  ),
                  PopupMenuButton(itemBuilder: (context) {
                    return [
                      "Change Date (${DateFormat.MMMd().format(app.selectedDate)})",
                      "Print Tiffin Labels",
                      "Customize Card Info",
                      if (app.role == "Admin" && app.zones.isNotEmpty)
                        "Reorder Zones",
                      "Tiffins To Deliver",
                      "Learn More",
                      if (app.role != "Admin")
                        "Logout (${app.deliveryMan!.name})"
                    ].map((e) {
                      return PopupMenuItem(
                          value: e,
                          child: Text(e),
                          onTap: () async {
                            if (e == "Print Tiffin Labels") {
                              await printLabels();
                            } else if (e == "Customize Card Info") {
                              await showViewCustomizeForm(
                                  context,
                                  [
                                    "items",
                                    "address",
                                    "balance",
                                    "customer_note",
                                    if (app.vendor.trackTiffinboxes)
                                      "tiffin_box_to_collect",
                                    "order_note",
                                    "veg_symbol",
                                    "customer_labels"
                                  ],
                                  "delivery");

                              setState(() {});
                            } else if (e == "Reorder Zones") {
                              await _handleReorderZones();
                            } else if (e == "Tiffins To Deliver") {
                              await Utility.showBottomSheet(
                                "Tiffins To Deliver",
                                RawMaterials(
                                  orders,
                                  filters["time"]!,
                                  "processing",
                                  fromDeliveryScreen: true,
                                ),
                                infoTag: "tiffins_to_deliver_in_orders",
                              );
                            } else if (e.contains("Change Date")) {
                              openDatePicker();
                            } else if (e.contains("Logout")) {
                              await app.logout();
                            } else if (e == "Learn More") {
                              openLearnMore(learnTag);
                            }
                          });
                    }).toList();
                  })
                ],
    );
  }
}
