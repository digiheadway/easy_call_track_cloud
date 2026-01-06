import 'dart:async';

import 'package:tiffincrm/components/bottomsheet_button.dart';
import 'package:tiffincrm/components/card_chip.dart';
import 'package:tiffincrm/components/custom_listtile.dart';
import 'package:tiffincrm/components/property_with_icon.dart';
import 'package:tiffincrm/components/refresh_strip.dart';
import 'package:tiffincrm/components/view_customize_form.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/icons.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/styles.dart';
import 'package:tiffincrm/values/tables.dart';
import 'package:tiffincrm/views/customers/profile/customer_orders_listview.dart';
import 'package:tiffincrm/components/form/form_view.dart';
import 'package:tiffincrm/components/veg_icon.dart';
import 'package:tiffincrm/models/customer.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/values/numbers.dart';
import 'package:tiffincrm/views/customers/profile/profile_view.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:flutter/material.dart';

import '../../components/custom_filter_chip.dart';
import '../../components/learn_more_button.dart';
import '../../components/onboarding_no_item.dart';
import '../../utils/db.dart';

class CustomerList extends StatefulWidget {
  const CustomerList({super.key});

  @override
  State<CustomerList> createState() => _CustomersState();
}

class _CustomersState extends State<CustomerList>
    with AutomaticKeepAliveClientMixin<CustomerList> {
  bool _keepAlive = true;

  @override
  bool get wantKeepAlive => _keepAlive;
  Map<String, List<String>> values = {
    "Zones": [],
    "Time Slots": ["Breakfast", "Lunch", "Dinner"],
    "Labels": [],
    "Sort By": [
      "Newest",
      "Oldest",
      "Name (A-Z)",
      "Name (Z-A)",
      "Lowest Balance",
      "Highest Balance"
    ],
    "Balance": [
      "-5000 and below",
      "-5000 to -1000",
      "-1000 to -100",
      "-100 to 100",
      "100 to 500",
      "500 to 1000",
      "1000 to 2000",
      "2000 to 5000",
      "5000 and above"
    ],
    "Status": [
      "Low Balance",
      "Vegetarian",
      "Non Vegetarian",
      "Active",
      "Paused",
      "Blocked",
      "Inactive Meals",
      "Customized Meals"
    ]
  };
  App app = App();
  List<Customer> filteredCustomers = [];
  bool loaded = false;
  ValueNotifier<DateTime> dataDownloadedAt =
      ValueNotifier<DateTime>(DateTime.now());
  bool isSearching = false;
  late TextEditingController _searchFilterController;
  Map<String, int> stats = {};
  List<int> selectedIds = [];
  Map<String, List<String>> selectedFilters = {
    "Zones": [],
    "Time Slots": [],
    "Labels": [],
    "Status": [],
    "Sort By": ["Newest"],
    "Balance": []
  };

  Timer? _timer;
  bool longPressed = false;

  void _startTimer(int customerId) {
    _timer = Timer(const Duration(milliseconds: 100), () {
      if (!selectedIds.contains(customerId)) {
        selectedIds.add(customerId);
      }
      longPressed = true;
      setState(() {});
    });
  }

  void _cancelTimer() {
    if (_timer != null) {
      _timer!.cancel();
    }
    longPressed = false;
    setState(() {});
  }

  @override
  void initState() {
    openLearnMore("customer", firstTime: true);
    super.initState();

    app.viewUpdaters["customers_list_view"] = updateClients;
    _searchFilterController = TextEditingController();

    Utility.tryCatch(() async {
      updateClients();
      await app.setVendor();
      if (app.vendor.trackTiffinboxes) {
        values["Sort By"]
            ?.addAll(["Lowest Tiffin Counts", "Highest Tiffin Counts"]);
      }
    }, onException: (_) {
      _keepAlive = false;
      updateKeepAlive();
    });
  }

  Future<void> updateClients() async {
    setState(() {
      loaded = false;
    });
    await app.fetchCustomers(update: true);

    setState(() {
      loaded = true;
      selectedIds = [];
    });
    dataDownloadedAt = ValueNotifier<DateTime>(DateTime.now());
  }

  @override
  void dispose() {
    if (_timer != null) {
      _timer!.cancel();
    }
    super.dispose();
  }

  setFilterData() {
    values["Zones"] =
        List<String>.from(app.customers.map((e) => e.zone).toSet());
    values["Labels"] = List<String>.from(app.customers
        .map((e) => e.tags.split(", "))
        .expand((e) => e)
        .where((element) => element.isNotEmpty)
        .toSet());
    filteredCustomers = app.customers.where((element) {
      return ((selectedFilters["Status"]!.isEmpty ||
                  selectedFilters["Status"]!.contains("Low Balance") &&
                      element.hasLowBalance) ||
              (selectedFilters["Status"]!.contains("Vegetarian") &&
                  element.isVegan) ||
              (selectedFilters["Status"]!.contains("Non Vegetarian") &&
                  !element.isVegan) ||
              selectedFilters["Status"]!
                  .contains(element.status.capitalize()) ||
              (selectedFilters["Status"]!.contains("Inactive Meals") &&
                  element.orders.contains("Inactive")) ||
              (selectedFilters["Status"]!.contains("Customized Meals") &&
                  element.orders.contains("Customized"))) &&
          element
              .toMap()
              .toString()
              .toLowerCase()
              .contains(_searchFilterController.text.toLowerCase()) &&
          (selectedFilters["Time Slots"]!.isEmpty ||
              selectedFilters["Time Slots"]!
                  .any((e) => element.orders.contains(e))) &&
          (selectedFilters["Balance"]!.isEmpty ||
              selectedFilters["Balance"]!
                  .any((e) => e == getRange(element.balance))) &&
          (selectedFilters["Zones"]!.isEmpty ||
              selectedFilters["Zones"]!.contains(element.zone)) &&
          (selectedFilters["Labels"]!.isEmpty ||
              selectedFilters["Labels"]!.any((e) => element.tags.contains(e)));
    }).toList();
  }

  void prepareStats() {
    stats = {};
    stats = {
      "Low Balance":
          app.customers.where((element) => element.hasLowBalance).length,
      "Vegetarian": app.customers.where((element) => element.isVegan).length
    };
    stats["Non Vegetarian"] = app.customers.length - stats["Vegetarian"]!;
    for (var customer in app.customers) {
      String status = customer.status.capitalize();

      stats[status] = stats[status] == null ? 1 : stats[status]! + 1;
      customer.orders
          .replaceAll(RegExp(r"\([A-Za-z, ]*\)"), "")
          .split(", ")
          .map((e) => e.trim())
          .toSet()
          .forEach((time) {
        stats[time] = stats[time] == null ? 1 : stats[time]! + 1;
      });
      customer.tags.split(", ").forEach((tag) {
        stats[tag] = stats[tag] == null ? 1 : stats[tag]! + 1;
      });

      if (customer.orders.contains("Inactive")) {
        stats["Inactive Meals"] =
            stats["Inactive Meals"] == null ? 1 : stats["Inactive Meals"]! + 1;
      }
      if (customer.orders.contains("Customized")) {
        stats["Customized Meals"] = stats["Customized Meals"] == null
            ? 1
            : stats["Customized Meals"]! + 1;
      }

      stats[customer.zone] =
          stats[customer.zone] == null ? 1 : stats[customer.zone]! + 1;

      String curRange = getRange(customer.balance);
      stats[curRange] = stats[curRange] == null ? 1 : stats[curRange]! + 1;
    }
  }

  String getRange(num cxBalance) {
    String curRange = "";
    for (String balance in values["Balance"]!) {
      List<String> options = balance.split(" ");
      if (balance.contains("and below")) {
        if (cxBalance <= num.parse(options[0])) {
          curRange = balance;
          break;
        }
      } else if (balance.contains("and above")) {
        if (cxBalance >= num.parse(options[0])) {
          curRange = balance;
          break;
        }
      } else if (balance.contains("to")) {
        List<String> options = balance.split(" to ");
        if (cxBalance >= num.parse(options[0]) &&
            cxBalance <= num.parse(options[1])) {
          curRange = balance;
          break;
        }
      }
    }
    return curRange;
  }

  Widget getFilters() {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 0, vertical: 6),
        child: Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          spacing: 7,
          children: [
            const SizedBox(width: 0),
            Text(
              "${filteredCustomers.length} Results",
              textAlign: TextAlign.center,
              textScaler: const TextScaler.linear(0.8),
            ),
            ...["Sort By", "Zones", "Status", "Time Slots", "Balance", "Labels"]
                .map((e) => CustomFilterChip(
                      isSelected: e == "Sort By"
                          ? selectedFilters[e]![0] != "Newest"
                          : selectedFilters[e]!.isNotEmpty,
                      label: e,
                      optionCounts: stats,
                      multipleSelect: !["Sort By"].contains(e),
                      labelWithCount:
                          "$e (${selectedFilters[e]!.isEmpty ? values[e]!.length : selectedFilters[e]!.length}/${values[e]!.length})",
                      options: values[e]!,
                      selectedOptions: selectedFilters[e]!,
                      updateState: ({bool selected = false}) {
                        if (selectedFilters[e]!.isEmpty && e == "Sort By") {
                          selectedFilters[e]!.add("Newest");
                        }
                        setState(() {});
                      },
                    ))
          ],
        ),
      ),
    );
  }

  void sortCustomer() {
    app.customers.sort((a, b) {
      dynamic valA = a.id;
      dynamic valB = b.id;
      bool reverse = false;
      if (selectedFilters["Sort By"]!.isNotEmpty) {
        String sortBy = selectedFilters["Sort By"]![0];
        if (sortBy.contains("Balance")) {
          valA = a.balance;
          valB = b.balance;
        }
        if (sortBy.contains("Name")) {
          valA = a.name;
          valB = b.name;
        }
        if (sortBy.contains("Tiffin Counts")) {
          valA = a.tiffinCounts;
          valB = b.tiffinCounts;
        }
        if (["Newest", "Name (Z-A)", "Highest Balance", "Highest Tiffin Counts"]
            .contains(sortBy)) {
          reverse = true;
        }
      }
      return reverse ? valB.compareTo(valA) : valA.compareTo(valB);
    });
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    // 35
    // Future.delayed(Duration(seconds: 2), () {
    //   Database.fcmNotify(
    //           //35
    //           "fVgvI5xKQUS6tg3ELhPBrp:APA91bEZS9Y-siP2qb0a8IcRG8uKg2V8uSBAAZgW09H216RkuD_ZPM1zruQMnZ0yEePSiGFXtmzEnVndPmdq2eyMa9Kc28-SGVDn7Je8c8lmBu-qA629XdE"
    //           // "d4cpQZVFT5e25Rz8YlgZWt:APA91bH8ofEswc3GzyiVD22Ezj00w0zGpTCm69cop_UPLSXskD3zVCAwplRlXCt_x_njWH9YXAUCO8Yo_OCfx-QgpKkhgAZPaHNYjUyOd625I_C3kVIaATA"
    //           )
    //       .then(print);
    // });

    if (loaded) {
      prepareStats();
      sortCustomer();
      setFilterData();
    }
    return PopScope(
      canPop: true,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) return;

        if (isSearching) {
          setState(() {
            _searchFilterController.clear();
            isSearching = false;
          });
          return;
        }

        if (selectedIds.isNotEmpty) {
          setState(() {
            selectedIds = [];
          });
          return;
        }
      },
      child: Scaffold(
        drawer: isSearching || selectedIds.isNotEmpty ? null : app.drawer,
        appBar: AppBar(
          bottom: loaded || app.customers.isEmpty
              ? null
              : const PreferredSize(
                  preferredSize: Size(double.infinity, 1),
                  child: LinearProgressIndicator()),
          titleSpacing: isSearching ? 10 : 0,
          title: selectedIds.isNotEmpty
              ? Row(
                  children: [
                    IconButton(
                      icon: const Icon(Icons.arrow_back),
                      onPressed: () {
                        setState(() {
                          selectedIds = [];
                        });
                      },
                    ),
                    Text("Selected ${selectedIds.length} Customer(s)"),
                  ],
                )
              : !isSearching
                  ? const Text("Customers")
                  : SizedBox(
                      height: 35,
                      child: Row(
                        children: [
                          GestureDetector(
                            child: const Icon(Icons.arrow_back),
                            onTap: () {
                              setState(() {
                                _searchFilterController.clear();
                                isSearching = false;
                              });
                            },
                          ),
                          const SizedBox(width: 20),
                          Expanded(
                            child: TextField(
                              controller: _searchFilterController,
                              autofocus: true,
                              style: const TextStyle(fontSize: 12),
                              decoration: const InputDecoration(
                                suffixIcon: Icon(Icons.search),

                                contentPadding: EdgeInsets.fromLTRB(8, 0, 8, 0),
                                // labelStyle: TextStyle(color: Colors.black45),
                                hintText: 'Search Customer Details',
                              ),
                              onChanged: (value) {
                                setState(() {});
                              },
                            ),
                          ),
                        ],
                      ),
                    ),
          actions: selectedIds.isNotEmpty
              ? [
                  IconButton(
                      onPressed: () async {
                        if (!(await Utility.getConfirmation("Delete Selected",
                            "Are you sure you want to delete selected customers?"))) {
                          return;
                        }
                        await Database.deleteMany(Tables.clients, selectedIds);
                        Utility.showMessage("Clients Deleted Successfully");
                        updateClients();
                      },
                      icon: const Icon(Icons.delete)),
                  IconButton(
                      onPressed: () async {
                        app.userInProfile = true;
                        app.changeInProfile = false;
                        await onAddOrderClick(selectedIds);
                        if (app.changeInProfile) {
                          await updateClients();
                          app.refreshOtherViews();
                        }
                        app.userInProfile = false;
                        app.changeInProfile = false;
                      },
                      icon: const Icon(Icons.format_list_bulleted_add)),
                ]
              : isSearching
                  ? null
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
                          "Customize Card Info",
                          "Download Backup",
                          if (app.customers.isNotEmpty) "Customer Analytics",
                          "Import Bulk Data",
                          "Learn More",
                        ].map((e) {
                          return PopupMenuItem(
                              value: e,
                              child: Text(e),
                              onTap: () async {
                                if (e == "Import Bulk Data") {
                                  await openLearnMore("import_data");
                                } else if (e == "Customize Card Info") {
                                  await showViewCustomizeForm(
                                      context,
                                      [
                                        "address",
                                        "note",
                                        "veg_symbol",
                                        "labels",
                                        if (app.vendor.trackTiffinboxes)
                                          "tiffin_box_to_collect"
                                      ],
                                      "customer");

                                  setState(() {});
                                } else if (e == "Customer Analytics") {
                                  await AppRouter.navigateTo(
                                      "/customer_analytics");
                                } else if (e == "Learn More") {
                                  openLearnMore("customer");
                                } else {
                                  await app.downloadCustomers();
                                }
                              });
                        }).toList();
                      })
                    ],
        ),
        body: RefreshIndicator(
          onRefresh: updateClients,
          child: Column(
            children: [
              ValueListenableBuilder(
                  valueListenable: dataDownloadedAt,
                  builder: (context, value, child) {
                    return RefreshStrip(
                      update: updateClients,
                      dataDownloadedAt: value,
                    );
                  }),
              if (loaded &&
                  app.customers.isNotEmpty &&
                  app.prefs.getBool("hide_customer_list_info_card") != true)
                Padding(
                  padding: const EdgeInsets.only(left: 8, right: 8, top: 8),
                  child: CustomListTile(
                    leadingIcon: Icons.info,
                    title:
                        "Click and hold on any customer to select multiple customers!",
                    actions: [
                      IconButton(
                          onPressed: () async {
                            await app.prefs
                                .setBool("hide_customer_list_info_card", true);
                            setState(() {});
                          },
                          icon: const Icon(Icons.close))
                    ],
                  ),
                ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(left: 10, right: 10, top: 0),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.start,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (app.customers.isNotEmpty) getFilters(),
                      Expanded(
                        child: !loaded && filteredCustomers.isEmpty
                            ? const Center(child: CircularProgressIndicator())
                            : loaded && app.customers.isEmpty
                                ? onboardingNoItem(
                                    "Add Your Customers!",
                                    "To Get Started With TiffinCRM!",
                                    Icons.person_add_outlined,
                                    linkTag: "customer",
                                    button: bottomSheetButton(
                                        openContactsPicker, "+ Add Customer",
                                        width:
                                            MediaQuery.of(context).size.width *
                                                0.5),
                                  )
                                : filteredCustomers.isEmpty
                                    ? const Center(
                                        child: Text("No customers found!"),
                                      )
                                    : ListView.builder(
                                        padding:
                                            const EdgeInsets.only(bottom: 80),
                                        shrinkWrap: true,
                                        // Providing a restorationId allows the ListView to restore the
                                        // scroll position when a user leaves and returns to the app after it
                                        // has been killed while running in the background.
                                        physics:
                                            const AlwaysScrollableScrollPhysics(),

                                        restorationId: 'customersListView',
                                        itemCount: filteredCustomers.length,
                                        itemBuilder:
                                            (BuildContext context, int index) {
                                          final customer =
                                              filteredCustomers[index];

                                          return getCustomerCard(customer);
                                        },
                                      ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        floatingActionButton:
            selectedIds.isNotEmpty || isSearching || app.customers.isEmpty
                ? null
                : FloatingActionButton(
                    onPressed: openContactsPicker,
                    child: const Icon(Icons.add),
                  ),
      ),
    );
  }

  Widget getCustomerCard(Customer customer) {
    List<String> tags = [
      if (customer.tags.isNotEmpty) ...customer.tags.split(", "),
      if (customer.hasLowBalance) "Low Balance"
    ];
    return Opacity(
      opacity: customer.status == "active" ? 1.0 : 0.6,
      child: InkWell(
        customBorder: Styles.inkwellBorder,
        onTap: () async {},
        onTapDown: (_) {
          _startTimer(customer.id);
        },
        onTapUp: (_) async {
          if (selectedIds.isEmpty) {
            openProfilePage(customer);
          } else if (!longPressed) {
            if (selectedIds.contains(customer.id)) {
              selectedIds.remove(customer.id);
            } else {
              selectedIds.add(customer.id);
            }
            setState(() {});
          }
          _cancelTimer();
        },
        child: Card(
            shape: RoundedRectangleBorder(
                borderRadius: const BorderRadius.all(
                  Radius.circular(Numbers.borderRadius),
                ),
                side: BorderSide(
                    color: selectedIds.contains(customer.id)
                        ? ThemeColors.primary
                        : const Color(0xFFDEDEDE))),
            child: ListTile(
              title: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: MediaQuery.of(context).size.width * 0.65,
                    child: IconTheme(
                      data: const IconThemeData(color: Color(0xFF424242)),
                      child: DefaultTextStyle.merge(
                        style: const TextStyle(color: Color(0xFF424242)),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Flexible(
                              child: Text(
                                customer.displayName,
                                textScaler: const TextScaler.linear(1.2),
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            if (customer.status != "active")
                              Text(" (${customer.status.capitalize()})"),
                            const SizedBox(width: 6),
                            if (customer.isVegan &&
                                (app.prefs
                                        .getBool("show_customer_veg_symbol") ??
                                    true))
                              vegIcon
                          ],
                        ),
                      ),
                    ),
                  ),
                  Expanded(
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.account_balance_wallet_outlined,
                          color: customer.balance < 0
                              ? const Color.fromARGB(255, 202, 13, 13)
                              : null,
                          size: 19,
                        ),
                        const SizedBox(width: 3),
                        Flexible(
                          child: Text(
                            app.currencySymbol +
                                (customer.balance < 10000
                                    ? customer.balance.toString()
                                    : "${customer.balance ~/ 1000}k"),
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: customer.balance < 0
                                  ? const Color.fromARGB(255, 202, 13, 13)
                                  : null,
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(height: 6),
                  getPropertyWithIcon(customer.zone, MyIcons.zone),
                  if (app.prefs.getBool("show_customer_address") ?? true)
                    getPropertyWithIcon(customer.address, MyIcons.address),
                  getPropertyWithIcon(customer.orders, MyIcons.orders),
                  if (app.prefs.getBool("show_customer_note") ?? true)
                    getPropertyWithIcon(customer.note, MyIcons.customerNote),
                  if (app.vendor.trackTiffinboxes &&
                      (app.prefs
                              .getBool("show_customer_tiffin_box_to_collect") ??
                          true) &&
                      customer.tiffinCounts > 0)
                    getPropertyWithIcon("${customer.tiffinCounts} tiffins",
                        MyIcons.tiffinCounts),
                  if (app.prefs.getBool("show_customer_labels") ?? true)
                    cardChips(tags, textColor: (String label) {
                      if (label == "Low Balance") {
                        return const Color.fromARGB(255, 202, 13, 13);
                      }
                      return const Color(0xFF424242);
                    }),
                ],
              ),
            )),
      ),
    );
  }

  Future<void> openProfilePage(Customer customer,
      {int initialIndex = 0}) async {
    await app.setVendor();
    app.changeInProfile = false;
    app.userInProfile = true;
    await AppRouter.navigateTo(MaterialPageRoute(builder: (context) {
      return CustomerProfile(
        customer.id,
        initialIndex: initialIndex,
      );
    }));

    // check mounted
    if (mounted) {
      setState(() {});
    }

    if (app.changeInProfile) {
      app.refreshOtherViews();
    }
    app.userInProfile = false;
  }

  void openContactsPicker() async {
    Map<String, String>? result = await AppRouter.navigateTo("/contacts");
    setState(() {});

    if (result == null) {
      return;
    }

    if (!result.containsKey("action") ||
        !["addFromSearch", "openProfile"].contains(result["action"])) {
      return;
    }

    if (result["action"] == "openProfile") {
      Iterable<Customer> customersWithNewPhone =
          app.customers.where((e) => e.phone == result["value"]);
      if (customersWithNewPhone.isNotEmpty) {
        await openProfilePage(customersWithNewPhone.first, initialIndex: 1);
      }
      return;
    }

    Map<String, dynamic> options = {
      "zones": values["Zones"]!
          .where((element) => element != "Zone Not Set!")
          .toList(),
      "Labels": values["Labels"]
    };

    if (int.tryParse(result["value"]!) == null) {
      options['name'] = result["value"];
    } else {
      options['phone'] = result["value"];
    }
    try {
      bool added = await handleClientAddUpdate(null, options);
      if (added) {
        Utility.showMessage("Client Added Successfully");
        await openProfilePage(app.customers.last, initialIndex: 1);
      }
    } catch (e) {
      if (e.toString().contains("unique_phone")) {
        Utility.showMessage("Error: Phone already exists");
      } else {
        Utility.showMessage("Error: ${e.toString()}");
      }
    }
  }
}

Future<bool> handleClientAddUpdate(
    Customer? customer, Map<String, dynamic> options) async {
  App app = App();
  if (!options.containsKey("zones")) {
    List<dynamic> optionsData = await Database.get(Tables.customersView,
        fields: "UNIQUE zone, tags", where: {"vendor_id": app.vendorId});
    options['zones'] =
        optionsData.map((x) => x['zone'].toString()).toSet().toList();
    options['tags'] = optionsData
        .map((x) => x['tags'].toString().split(", "))
        .expand((x) => x)
        .toSet()
        .toList();
  }
  List<FormInput> customerForm = [
    FormInput("Full Name", "text",
        value: customer == null ? (options["name"] ?? "") : customer.name,
        prefixIcon: const Icon(Icons.person)),
    FormInput("Phone", "phone",
        value: customer == null ? options["phone"] ?? "" : customer.phone),
    FormInput("Zone/Area", "select",
        value: (customer == null || customer.zone == "Zone Not Set!")
            ? ""
            : customer.zone,
        canAddOption: true,
        options: options["zones"] ?? [],
        isRequired: false,
        hintText: "e.g. landmark, location tag",
        prefixIcon: const Icon(Icons.map_outlined),
        tagsLimit: 1),
    FormInput(
      "Address",
      "text",
      value: customer == null ? "" : customer.address,
      isRequired: false,
      prefixIcon: const Icon(Icons.location_on_outlined),
    ),
    FormInput("Vegetarian?", "bool",
        showInAppBar: true,
        value: customer == null
            ? "Yes"
            : customer.isVegan
                ? "Yes"
                : "No"),
    FormInput("Labels", "select_multiple",
        value: customer == null ? "" : customer.tags,
        isRequired: false,
        hintText: "e.g. Premium Customer",
        options: options["Labels"] ?? [],
        prefixIcon: const Icon(Icons.label_outline_rounded)),
    FormInput("Note", "text",
        value: customer == null ? "" : customer.note,
        isRequired: false,
        hintText: "e.g delivery instruction, specific note"),
    FormInput("Alternative Phone", "phone",
        value: customer == null ? "" : (customer.phone2 ?? ""),
        isRequired: false),
  ];

  dynamic formData = await AppRouter.navigateTo(MaterialPageRoute(
      builder: (context) => FormView(
            "${customer == null ? "Add" : "Update"} Customer",
            customerForm,
            groupOptional: true,
            showOptionalGroup: customer != null ? true : false,
          )));

  if (formData == null) {
    return false;
  }
  Map<String, String> data = {
    "name": formData["Full Name"],
    "phone": formData["Phone"],
    "address": formData["Address"],
    "phone2": formData["Alternative Phone"],
    "note": formData["Note"],
    "tags": formData["Labels"],
    "zone": formData["Zone/Area"],
    "is_vegan": formData["Vegetarian?"] == "Yes" ? "1" : "0",
    "vendor_id": app.vendorId.toString(),
  };

  int customerId;
  if (customer != null) {
    await Database.update(Tables.clients, customer.id, data);
    customerId = customer.id;
  } else {
    data["created_on"] = DateTime.now().toIso8601String();
    data["auth_token"] = app.generateRandomToken();
    customerId = await Database.add(Tables.clients, data);
  }

  Customer newCustomer =
      (await app.getCustomers(where: {"id": customerId})).first;

  if (customer != null) {
    int index =
        app.customers.indexWhere((element) => element.id == customer.id);
    app.customers[index] = newCustomer;
  } else {
    app.customers.add(newCustomer);
  }

  return true;
}
