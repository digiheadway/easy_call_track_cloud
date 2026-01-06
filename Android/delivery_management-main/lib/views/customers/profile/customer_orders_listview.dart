import 'package:intl/intl.dart';
import 'package:tiffincrm/components/bottomsheet_button.dart';
import 'package:tiffincrm/components/custom_listtile.dart';
import 'package:tiffincrm/components/onboarding_no_item.dart';
import 'package:tiffincrm/components/property_with_icon.dart';
import 'package:tiffincrm/components/form/form_view.dart';
import 'package:tiffincrm/components/form/order_inputs.dart';
import 'package:tiffincrm/components/veg_icon.dart';
import 'package:tiffincrm/models/customer.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/models/order_template.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:flutter/material.dart';
import 'package:tiffincrm/utils/extensions/date_check.dart';
import 'package:tiffincrm/utils/extensions/display_date.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/tables.dart';
import 'package:tiffincrm/views/customers/profile/add_meal_dialog.dart';

import '../../../models/order.dart';
import '../../../utils/app_router.dart';
import '../../../utils/db.dart';

class CustomerOrders extends StatefulWidget {
  final Customer customer;
  final Function onUpdate;
  const CustomerOrders(this.customer, this.onUpdate, {super.key});

  @override
  State<CustomerOrders> createState() => _CustomerOrdersState();
}

class _CustomerOrdersState extends State<CustomerOrders> {
  late Customer customer;
  List<Order> orders = [];
  List<String> times = ["Breakfast", "Lunch", "Dinner"];
  App app = App();
  bool loading = true;

  @override
  void initState() {
    customer = widget.customer;

    updateOrders(initial: true);

    super.initState();
  }

  updateOrders({bool initial = false}) async {
    dynamic data = await Database.get(Tables.realOrders,
        where: "client_id = ${customer.id} AND deleted = 0", silent: true);
    orders = List<Order>.from(data.map((item) => Order.fromMap(item)));
    orders
        .sort((a, b) => times.indexOf(a.time).compareTo(times.indexOf(b.time)));

    if (mounted) {
      setState(() {
        loading = false;
      });
    }
    if (initial == false) {
      widget.onUpdate();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        bottomSheet: bottomSheetButton(() async {
          await onAddOrderClick([customer.id]);
          updateOrders();
        }, "+ Add New Meal Plan"),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : orders.isEmpty
                ? onboardingNoItem(
                    "No Meal Plan Added Yet!",
                    "${customer.name}'s daily orders will be generated using meal plans you add here.",
                    Icons.shopping_cart_outlined,
                    linkTag: "meal_plan",
                  )
                : RefreshIndicator(
                    onRefresh: () => updateOrders(),
                    child: SingleChildScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      child: Padding(
                        padding: const EdgeInsets.only(bottom: 80),
                        child: Column(
                            children: orders
                                .map((order) => getOrderCard(order))
                                .toList()),
                      ),
                    ),
                  ));
  }

  Widget getOrderCard(Order order) {
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsets.only(left: 12, top: 8, bottom: 8),
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(width: 2),
            order.isVegan ? vegIcon : nonVegIcon,
            const SizedBox(width: 10),
            Text(
                "x${order.quantity} ${order.time} ${order.isActive ? "" : "(Inactive) "}${order.expiryDate != null && !order.expiryDate!.isInFuture ? "- Expired" : ""}",
                style: const TextStyle(fontWeight: FontWeight.w500)),
          ],
        ),
        subtitle: DefaultTextStyle(
          style: const TextStyle(fontSize: 14, color: Colors.black87),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 4),
              getPropertyWithIcon(order.items, Icons.shopping_cart_outlined),
              getPropertyWithIcon(
                  "${app.currencySymbol}${order.price} (x${order.quantity}=${order.price * order.quantity})",
                  Icons.sell_outlined),
              getPropertyWithIcon(order.repeat, Icons.event),
              order.templateName.isNotEmpty
                  ? getPropertyWithIcon(order.templateName, Icons.link_outlined)
                  : getPropertyWithIcon("Customised Plan", Icons.tune),
              getPropertyWithIcon(order.note, Icons.note),
              order.startFrom != null && order.expiryDate != null
                  ? getPropertyWithIcon(
                      "From ${order.startFrom!.format('dd MMM')} To ${order.expiryDate!.format('dd MMM')}",
                      Icons.date_range)
                  : Column(children: [
                      if (order.startFrom != null)
                        getPropertyWithIcon(
                            "${order.startFrom!.isInFuture ? "Will Start" : "Started"} From ${order.startFrom!.format('dd MMM')}",
                            Icons.date_range),
                      if (order.expiryDate != null)
                        getPropertyWithIcon(
                            "${order.expiryDate!.isInFuture ? "Will Exipre" : "Expired"} On ${order.expiryDate!.format('dd MMM')}",
                            Icons.date_range),
                    ]),
              const SizedBox(height: 5),
              Text(
                "${order.createdOn.format('dd MMM, hh:mm a')} | ID: ${order.id}",
                style: const TextStyle(color: Colors.grey),
              ),
            ],
          ),
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            InkWell(
                onTap: () async {
                  await Database.update(Tables.orders, order.id, {
                    "is_active": order.isActive ? "0" : "1",
                  });
                  order.isActive = !order.isActive;
                  Utility.showMessage(
                      "Meal Plan ${order.isActive ? "Activated" : "Paused"} Successfully");
                  setState(() {});
                  widget.onUpdate();
                },
                child: order.isActive
                    ? const Icon(Icons.pause)
                    : const Icon(Icons.play_arrow)),
            InkWell(
                onTap: () {
                  moreOptionsClicked(order);
                },
                child: const Icon(Icons.more_vert)),
            const SizedBox(width: 5),
          ],
        ),
      ),
    );
  }

  moreOptionsClicked(Order order) async {
    int oldQuantity = order.quantity;
    await Utility.showBottomSheet(
        "",
        StatefulBuilder(
          builder: (context, dialogSetState) => Column(
            children: [
              Text("Meal Plan #${order.id}",
                  style: const TextStyle(fontWeight: FontWeight.w500)),
              const SizedBox(height: 10),
              CustomListTile(
                isProperty: true,
                leadingIcon: Icons.update,
                title: "Update Start Date",
                onTap: () async {
                  DateTime? value = await showDatePicker(
                      context: context,
                      firstDate: DateTime(1999),
                      lastDate: DateTime(2099),
                      initialDate: order.startFrom);

                  if (value == null ||
                      (order.startFrom != null &&
                          value.isSameDate(order.startFrom!))) {
                    return;
                  }
                  order.startFrom = value;
                  await Database.update(Tables.orders, order.id,
                      {"start_from": value.toIso8601String()});
                  dialogSetState(() {});
                },
                actions: [
                  order.startFrom != null
                      ? Text(order.startFrom!.format('dd MMM'))
                      : Container()
                ],
              ),
              CustomListTile(
                leadingIcon: Icons.update,
                title: "Set Expiry Date",
                isProperty: true,
                onTap: () async {
                  DateTime? value = await showDatePicker(
                      context: context,
                      firstDate: DateTime(1999),
                      lastDate: DateTime(2099),
                      initialDate: order.expiryDate);

                  if (value == null ||
                      (order.expiryDate != null &&
                          value.isSameDate(order.expiryDate!))) {
                    return;
                  }
                  order.expiryDate = value;
                  await Database.update(Tables.orders, order.id,
                      {"expiry_date": value.toIso8601String()});
                  dialogSetState(() {});
                },
                actions: [
                  order.expiryDate != null
                      ? Text(order.expiryDate!.format('dd MMM'))
                      : Container()
                ],
              ),
              CustomListTile(
                title: "Update Quantity",
                leadingIcon: Icons.layers,
                actions: [
                  if (order.quantity > 1)
                    IconButton(
                        onPressed: () {
                          order.quantity--;
                          dialogSetState(() {});
                        },
                        icon: const Icon(Icons.remove)),
                  Text(order.quantity.toString()),
                  IconButton(
                      onPressed: () {
                        order.quantity++;
                        dialogSetState(() {});
                      },
                      icon: const Icon(Icons.add)),
                ],
              ),
              const SizedBox(height: 20),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Expanded(
                    child: CustomListTile(
                      leadingIcon: Icons.edit,
                      title: "Edit Meal Plan",
                      onTap: () async {
                        if (order.templateName.isNotEmpty) {
                          if (!(await Utility.getConfirmation("Edit Meal Plan?",
                              "Editing this meal plan will unlink it from the standard meal plan!\n\nChanging anything in standard meal plan will not affect this meal plan.\n\nContinue to edit this meal plan?"))) {
                            return;
                          }
                        }

                        await handleOrderAddUpdate(order, customer.id);
                        updateOrders();
                        AppRouter.goBack();
                      },
                    ),
                  ),
                  Expanded(
                    child: CustomListTile(
                      leadingIcon: Icons.delete,
                      title: "Delete",
                      onTap: () async {
                        await deleteOrder(order);
                        AppRouter.goBack();
                        Utility.showMessage("Meal Plan Deleted Successfully");
                      },
                    ),
                  ),
                ],
              ),
            ],
          ),
        ));

    if (order.quantity != oldQuantity) {
      await Database.update(
          Tables.orders, order.id, {"quantity": order.quantity.toString()});
      Utility.showMessage("Meal Plan Updated Successfully");
    }
    setState(() {});
  }

  Future<void> deleteOrder(Order order) async {
    bool confirmed = await Utility.getConfirmation(
      "Delete Meal Plan",
      "Are you sure you want to delete this meal plan?",
    );
    if (!confirmed) {
      return;
    }

    // ORDERS are not actually deleted because their deliveries shouldn't be deleted
    await Database.update(
        Tables.orders, order.id, {"deleted": "1", "is_active": "0"});
    Utility.showMessage("Meal Plan Deleted Successfully");
    updateOrders();
  }
}

Future<bool> onAddOrderClick(List<int> customerIds) async {
  App app = App();
  await app.fetchOrderTemplates();

  return await Utility.showBottomSheet(
          "", AddMealDialog(customerIds: customerIds),
          height:
              MediaQuery.of(navigatorKey.currentContext!).size.height * 0.8) ??
      false;
}

Future<bool> handleOrderAddUpdate(Order? order, int customerId,
    {OrderTemplate? template, bool isVegan = true}) async {
  App app = App();
  template ??= OrderTemplate(-1, "Default", 0, app.vendor.defaultMeal,
      type: "repeat",
      isVegan: isVegan,
      frequency: "1,1,1,1,1,1,1",
      time: "Breakfast");

  List<String> options = await app.getOrderItems();
  options.addAll(app.vendor.defaultMeal.split(", ").map((e) => e.trim()));

  String type = order != null ? order.type : template.type;
  String frequency = order != null ? order.frequency : template.frequency;
  if (type == "one_time") {
    type = "One Time Order";
  } else if (frequency.contains("0")) {
    type = "On Specific Days";
  } else {
    type = "Daily";
  }

  List<FormInput> orderForm = [
    if (order == null)
      FormInput("Start From", "date",
          value: DateFormat.yMMMd().format(DateTime.now()),
          isRequired: false,
          firstDate: DateTime.now().subtract(const Duration(days: 30)),
          lastDate: DateTime.now().add(const Duration(days: 10)),
          prefixIcon: const Icon(Icons.calendar_today)),
    ...orderInputs({
      "type": type,
      "frequency": frequency,
      "time": order != null ? order.time : template.time,
      "price":
          order != null ? order.price.toString() : template.price.toString(),
      "items": order != null ? order.items : template.items,
      "item_options": options.toSet().toList(),
      "is_veg":
          (order == null ? template.isVegan : order.isVegan) ? "Yes" : "No",
    }),
    FormInput("Labels", "select_multiple",
        value: order == null ? "" : order.labels,
        isRequired: false,
        hintText: "e.g. 12:00-12:30, Special",
        options: [],
        prefixIcon: const Icon(Icons.label_outline_rounded)),
    FormInput(
      "Note",
      "text",
      value: order != null ? order.note : "",
      isRequired: false,
      hintText: "e.g. Roti without ghee",
      helperText: "Shown in deliveries.",
    ),
  ];
  dynamic formData =
      await AppRouter.navigateTo(MaterialPageRoute(builder: (context) {
    return FormView(
      "${order == null ? "Add" : "Update"} Meal Plan",
      orderForm,
      groupOptional: true,
    );
  }));

  if (formData == null) {
    return false;
  }

  Map<String, String> data = {
    "time": formData["Time Slot"],
    "items": formData["Items"],
    "price": formData["Price Per Order"],
    "note": formData["Note"],
    "type": formData["Repeat"] == "One Time Order" ? "one_time" : "repeat",
    "is_veg": formData["Veg Order?"] == "Yes" ? "1" : "0",
    "is_active": "1",
    "labels": formData["Labels"],
    "client_id": customerId.toString(),
    "frequency": formData["Repeat"] == "Daily"
        ? "1,1,1,1,1,1,1"
        : formData["Select day(s)"],
  };
  if (order != null) {
    data["template_id"] = "-1";
    await Database.update(Tables.orders, order.id, data);
  } else {
    data["start_from"] =
        DateFormat.yMMMd().parse(formData["Start From"]).toIso8601String();

    data["created_on"] = DateTime.now().toIso8601String();
    await Database.add(Tables.orders, data);
    await Logger.logFirebaseEvent("order_added", {});
  }
  Utility.showMessage(
      "Meal Plan ${order == null ? "Added" : "Updated"} Successfully");
  return true;
}
