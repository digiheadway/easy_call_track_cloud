import 'package:flutter/services.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:intl/intl.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:tiffincrm/components/card_chip.dart';
import 'package:tiffincrm/components/custom_listtile.dart';
import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/components/property_with_icon.dart';
import 'package:tiffincrm/components/form/resizable_switch.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/extensions/formatted_number.dart';
import 'package:tiffincrm/utils/extensions/string_ext.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/navigate.dart';
import 'package:tiffincrm/models/customer.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/phone_call.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/icons.dart';
import 'package:tiffincrm/values/strings.dart';
import 'package:tiffincrm/values/tables.dart';
import 'package:tiffincrm/views/customers/customers_listview.dart';
import 'package:tiffincrm/views/customers/profile/delivery_cancel_view.dart';
import 'package:tiffincrm/views/customers/profile/customer_orders_listview.dart';
import 'package:tiffincrm/views/customers/profile/set_location.dart';
import 'package:tiffincrm/views/customers/profile/transactions_list_view.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../utils/db.dart';

class CustomerProfile extends StatefulWidget {
  final int customerId;
  final int initialIndex;
  const CustomerProfile(this.customerId, {this.initialIndex = 0, super.key});

  @override
  State<CustomerProfile> createState() => _CustomerProfileState();
}

class _CustomerProfileState extends State<CustomerProfile> {
  App app = App();
  late Customer customer;
  @override
  void initState() {
    customer = app.customers.firstWhere((e) => e.id == widget.customerId);
    super.initState();
  }

  Future<void> _handleClientDelete(int id, {bool archive = true}) async {
    bool confirmed = await Utility.getConfirmation(
      "${archive ? "Archive" : "Delete"} Client",
      "Are you sure?",
    );
    if (!confirmed) {
      return;
    }
    if (archive) {
      await Database.update(Tables.clients, id, {"deleted": "1"});
    } else {
      await Database.delete(Tables.clients, id);
    }
    Utility.showMessage(
        "Client ${archive ? "Archived" : "Deleted"} Successfully");
    await Logger.logFirebaseEvent("client_${archive ? "archived" : "deleted"}",
        {"client_id": id.toString()});

    // remove from list
    app.customers.removeWhere((cx) => cx.id == id);
    if (!mounted) {
      return;
    }
    AppRouter.goBack();
  }

  Future<void> _setLocation() async {
    PermissionStatus status = await Permission.locationWhenInUse.request();
    if (status != PermissionStatus.granted) {
      Utility.showMessage("Permission Denied");
      openAppSettings();
      return;
    }

    LatLng? latLng = await AppRouter.navigateTo(
        MaterialPageRoute(builder: (context) => SetLocationView(customer)));
    if (latLng == null) {
      return;
    }

    await customer.update({"lat_lng": "${latLng.latitude},${latLng.longitude}"},
        successMessage: "Location Updated Successfully");
    setState(() {
      customer.location = latLng;
    });
  }

  Future<void> _editCustomer() async {
    bool updated = await handleClientAddUpdate(customer, {});
    if (updated) {
      customer = app.customers.firstWhere((e) => e.id == customer.id);
      Utility.showMessage("Client Updated Successfully");
      if (mounted) {
        setState(() {});
      }
    }
  }

  Widget getButtons() {
    return PopupMenuButton(itemBuilder: (context) {
      return [
        "Edit Customer",
        "Archive Customer",
        "Delete Customer",
        "${customer.status == "blocked" ? "Unblock" : "Block"} Customer"
      ].map((e) {
        return PopupMenuItem(
            onTap: () async {
              if (e == "Edit Customer") {
                await _editCustomer();
              } else if (e == "Archive Customer") {
                await _handleClientDelete(customer.id, archive: true);
              } else if (e == "Delete Customer") {
                await _handleClientDelete(customer.id, archive: false);
              } else if (e.contains("lock Customer")) {
                await _toggleCustomerStatus();
              }
            },
            child: Text(e));
      }).toList();
    });
  }

  Future<void> _manageSetCreditUpdate() async {
    FormInput input = FormInput(
      "Credit Limit",
      "number",
      value:
          customer.creditLimit == null ? "" : customer.creditLimit.toString(),
    );
    Utility.showBottomSheet("Set Credit Limit", const SizedBox(),
        inputDescription:
            "This limit will be used as the minimum balance this customer can have.",
        showMoreTag: "credit_limit",
        isForm: true,
        inputs: [input], onValueChanged: () async {
      String newValue = input.controller.text.trim();
      await customer.update({
        "credit_limit": newValue.isEmpty ? "NULL" : newValue,
      }, successMessage: "Credit Limit Set Successfully");
      setState(() {
        customer.creditLimit = newValue.isEmpty ? null : int.parse(newValue);
      });
    });
  }

  Future<void> updateCustomer() async {
    customer = (await app.getCustomers(where: {"id": widget.customerId}))[0];
    app.customers[app.customers.indexWhere((e) => e.id == customer.id)] =
        customer;
    if (mounted) {
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    Widget? lowBalStripe;
    if (customer.orders.isNotEmpty) {
      if ((customer.balance - customer.allOrderPrice) <
          customer.minPossibleBalance) {
        lowBalStripe = Container(
          padding: const EdgeInsets.all(2),
          width: double.infinity,
          color: Colors.red[400],
          child: Text(
              "Low Balance to Process ${customer.hasLowBalance ? "Any" : "Some"} Deliveries",
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 16, color: Colors.white)),
        );
      }
    }
    return DefaultTabController(
      initialIndex: widget.initialIndex,
      length: customer.orders.isEmpty ? 3 : 4,
      child: Scaffold(
        appBar: AppBar(
          titleSpacing: 0,
          title: Text(customer.displayName),
          actions: [getButtons()],
          bottom: TabBar(
              isScrollable: true,
              tabAlignment: TabAlignment.start,
              tabs: [
                "Profile",
                "Meal Plans (${customer.orders.isEmpty ? "0" : customer.orders.split(", ").length})",
                "Transactions (${app.currencySymbol}${customer.balance.format()})",
                if (customer.orders.isNotEmpty) "Upcoming Orders"
              ].map((e) => Tab(text: e)).toList()),
        ),
        body: Padding(
          padding:
              EdgeInsets.only(bottom: MediaQuery.of(context).padding.bottom),
          child: TabBarView(
            children: [
              overview(lowBalStripe ?? const SizedBox()),
              Stack(
                children: [
                  Padding(
                    padding: EdgeInsets.fromLTRB(
                        8.0, lowBalStripe != null ? 35 : 8, 8, 8),
                    child: CustomerOrders(customer, updateCustomer),
                  ),
                  lowBalStripe ?? const SizedBox(),
                ],
              ),
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: CustomerTransactions(customer, updateCustomer),
              ),
              if (customer.orders.isNotEmpty) DeliveryCancelView(customer)
            ],
          ),
        ),
      ),
    );
  }

  Widget overview(Widget lowBalStripe) {
    return RefreshIndicator(
      onRefresh: () async {
        Utility.showLoaderDialog();
        await updateCustomer();
        Utility.closeCurrentDialog();
      },
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        child: Column(
          children: [
            lowBalStripe,
            Padding(
                padding: const EdgeInsets.all(8.0),
                child: Column(children: [
                  const SizedBox(height: 8.0),
                  Text(
                      "Customer (${customer.id}) Created On ${DateFormat('dd MMM, hh:mm a').format(customer.createdOn!)}",
                      style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w300,
                          color: Colors.grey)),
                  const SizedBox(height: 8.0),
                  GestureDetector(
                    onTap: _editCustomer,
                    child: Card(
                      child: Padding(
                        padding: const EdgeInsets.all(12.0),
                        child: SizedBox(
                          width: MediaQuery.of(context).size.width,
                          child: Stack(children: [
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                getPropertyWithIcon(
                                    customer.zone, MyIcons.zone),
                                getPropertyWithIcon(
                                    customer.address, MyIcons.address),
                                if (customer.phone2 != null)
                                  getPropertyWithIcon(
                                      customer.phone2.toString(),
                                      MyIcons.phone2),
                                getPropertyWithIcon(
                                    customer.note, MyIcons.customerNote),
                                if (customer.status == "blocked")
                                  getPropertyWithIcon(
                                      customer.status.capitalize(),
                                      MyIcons.status),
                                getPropertyWithIcon(
                                    customer.canBeNotified ? "On" : "Off",
                                    MyIcons.notifications),
                                cardChips(customer.tags.split(",")),
                              ],
                            ),
                            Positioned(
                              top: 0,
                              right: 0,
                              child: GestureDetector(
                                onTap: _editCustomer,
                                child: const Icon(Icons.edit_outlined),
                              ),
                            )
                          ]),
                        ),
                      ),
                    ),
                  ),
                  CustomListTile(
                    leadingIcon: MyIcons.phone,
                    title: customer.phone,
                    actions: [
                      IconButton(
                          onPressed: () async {
                            await call(customer.phone);
                          },
                          icon: const Icon(Icons.call)),
                      IconButton(
                          onPressed: () async {
                            Uri uri = Uri.parse(Strings.whatsappLink
                                .replaceFirst("__text__", "")
                                .replaceFirst("__phone__",
                                    "${customer.phone.startsWith("+") ? "" : app.dialCode}${customer.phone}"));
                            await launchUrl(uri);
                          },
                          icon: Image.asset(
                            "assets/images/whatsapp.png",
                            width: 22,
                          )),
                    ],
                  ),
                  if (customer.status != "blocked")
                    CustomListTile(
                      title:
                          "All Upcoming Deliveries (${customer.status.capitalize()})",
                      actions: [
                        ResizableSwitch(customer.status == "active",
                            (value) async {
                          await customer.update({
                            "status": customer.status == "active"
                                ? "paused"
                                : "active"
                          }, successMessage: "Status Updated Successfully");
                          setState(() {
                            customer.status = customer.status == "active"
                                ? "paused"
                                : "active";
                          });
                        }, 30)
                      ],
                    ),
                  CustomListTile(
                      title:
                          "Balance (${app.currencySymbol}${customer.balance})",
                      actions: [
                        IconButton(
                            onPressed: () async {
                              int amountAdded =
                                  await addCustomerBalance(customer);

                              customer.balance += amountAdded;
                              if (mounted) {
                                setState(() {});
                              }
                            },
                            icon: const Text("+Add Balance",
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                )))
                      ]),
                  if (app.vendor.trackTiffinboxes)
                    CustomListTile(
                        title: "Tiffin box To Collect",
                        learnMoretag: "tiffin_collection",
                        actions: [
                          IconButton(
                              onPressed: () async {
                                _updateTiffinCount(customer, -1);
                              },
                              icon: const Icon(Icons.remove)),
                          Text(
                            "${customer.tiffinCounts}",
                            style: const TextStyle(
                                fontWeight: FontWeight.bold, fontSize: 20),
                          ),
                          IconButton(
                              onPressed: () async {
                                _updateTiffinCount(customer, 1);
                              },
                              icon: const Icon(Icons.add)),
                        ]),
                  ...["Credit Limit", "Location"].map(
                    (e) {
                      String value = ((e == "Credit Limit"
                                  ? customer.creditLimit
                                  : customer.location) ??
                              "")
                          .toString();
                      return Card(
                          child: ListTile(
                              leading: Icon(MyIcons.get(e), size: 25),
                              title: Row(
                                children: [
                                  Text(e, style: const TextStyle(fontSize: 14)),
                                  const SizedBox(width: 5),
                                  learnMoreIcon(e == "Credit Limit"
                                      ? "credit_limit"
                                      : "customer_location")
                                ],
                              ),
                              trailing: InkWell(
                                onTap: () async {
                                  if (e == "Credit Limit") {
                                    await _manageSetCreditUpdate();
                                  } else {
                                    await _setLocation();
                                  }
                                },
                                child: SizedBox(
                                  width:
                                      MediaQuery.of(context).size.width * 0.4,
                                  child: Row(
                                      mainAxisAlignment: MainAxisAlignment.end,
                                      // mainAxisSize: MainAxisSize.min,
                                      children: e == "Credit Limit"
                                          ? [
                                              Text(value.isEmpty
                                                  ? "Default Limit Applied"
                                                  : "${app.currencySymbol}$value"),
                                              const Icon(Icons.chevron_right),
                                            ]
                                          : [
                                              value.isEmpty
                                                  ? const Text("Not Set")
                                                  : IconButton(
                                                      onPressed: () async {
                                                        Utility.tryCatch(
                                                            () async {
                                                          navigate(customer
                                                              .location!);
                                                        });
                                                      },
                                                      icon: const Icon(Icons
                                                          .navigation_rounded),
                                                    ),
                                              const Icon(Icons.chevron_right),
                                            ]),
                                ),
                              )));
                    },
                  ),
                  if (app.vendor.canCustomerLogin)
                    CustomListTile(
                      title: "Direct Login Link",
                      learnMoretag: "direct_login_link",
                      actions: [
                        IconButton(
                            onPressed: () async {
                              await _shareLoginLink();
                            },
                            icon: const Icon(Icons.share))
                      ],
                    ),
                  if (customer.totalDeliveries.isNotEmpty)
                    CustomListTile(
                      title:
                          "Deliveries in last 30 days: ${customer.totalDeliveries}",
                    ),
                  const SizedBox(height: 10),
                  Center(
                    child: TextButton(
                        onPressed: () async {
                          bool added = await onAddOrderClick([customer.id]);
                          if (added) {
                            await updateCustomer();
                          }
                        },
                        child: const Text("Add New Meal Plan",
                            style: TextStyle(
                                fontWeight: FontWeight.bold, fontSize: 16))),
                  ),
                ])),
          ],
        ),
      ),
    );
  }

  Future<void> _updateTiffinCount(Customer customer, int offset) async {
    int newCount = customer.tiffinCounts + offset;
    if (newCount < 0) {
      newCount = 0;
    }
    await customer.update(
      {"tiffin_counts": newCount.toString()},
      successMessage: "Tiffin count updated successfully",
    );
    customer.tiffinCounts = newCount;
    if (!mounted) {
      return;
    }
    setState(() {});
  }

  Future<void> _shareLoginLink() async {
    if (customer.authToken.isEmpty) {
      customer.authToken = app.generateRandomToken();
      await Database.update(
          Tables.clients, customer.id, {"auth_token": customer.authToken});
    }

    String directLink = "https://imeals.in/?direct_login=${customer.authToken}";
    String text = Strings.whatsappLink
        .replaceFirst("__text__",
            "You can login to ${app.vendor.displayName}'s web app to manage your delivery and balances, get notifications etc.\n\nUse the link below to automatically login with your account.\n\n$directLink")
        .replaceFirst("__phone__", app.getPhoneForWhatsapp(customer.phone));
    await Clipboard.setData(ClipboardData(text: directLink));

    Utility.tryCatch(() async {
      await launchUrl(Uri.parse(text));
    }, errorMessage: "Unable to open link");
  }

  Future<void> _toggleCustomerStatus() async {
    String newStatus = customer.status == "blocked" ? "active" : "blocked";
    await customer.update(
      {"status": newStatus},
      successMessage: "Status Set Successfully",
    );
    setState(() {
      customer.status = newStatus;
    });
  }
}
