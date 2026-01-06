import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/components/property_with_icon.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/components/form/form_view.dart';
import 'package:tiffincrm/components/form/order_inputs.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/models/order_template.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:flutter/material.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/tables.dart';

import '../../components/onboarding_no_item.dart';
import '../../utils/db.dart';

class OrderTemplates extends StatefulWidget {
  const OrderTemplates({super.key});

  @override
  State<OrderTemplates> createState() => _OrderTemplatesState();
}

class _OrderTemplatesState extends State<OrderTemplates> {
  List<String> times = ["Breakfast", "Lunch", "Dinner"];
  App app = App();
  bool loading = true;

  @override
  void initState() {
    app.changeInOrderTemplates = false;
    app.fetchOrderTemplates(update: true).then((_) async {
      await app.setVendor();
      if (!mounted) return;
      setState(() {
        loading = false;
      });
    });
    super.initState();
  }

  @override
  void dispose() {
    if (app.changeInOrderTemplates) {
      app.refreshOtherViews();
      app.changeInOrderTemplates = false;
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          titleSpacing: 0,
          title: const Text("Standard Meal Plans"),
          actions: [learnMoreButton("template")],
        ),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : SingleChildScrollView(
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Card(
                          child: ListTile(
                            onTap: () async {
                              List<String> orderItems =
                                  await app.getOrderItems();
                              dynamic formData = await AppRouter.navigateTo(
                                  MaterialPageRoute(builder: (context) {
                                return FormView(
                                    "Default Items",
                                    [
                                      FormInput("Items", "select_multiple",
                                          value: app.vendor.defaultMeal,
                                          options: orderItems,
                                          isRequired: false,
                                          prefixIcon: const Icon(
                                              Icons.add_circle_outline)),
                                    ],
                                    learnMoreTag: "default_items");
                              }));

                              if (formData == null) {
                                return;
                              }
                              await Database.update(
                                Tables.vendors,
                                app.vendorId,
                                {
                                  "default_meal": formData["Items"],
                                },
                              );
                              Utility.showMessage("Updated Successfully");
                              app.vendor.defaultMeal = formData["Items"];
                              setState(() {});
                            },
                            leading: const Icon(Icons.dining),
                            title: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Text("Default Items"),
                                const SizedBox(width: 6),
                                learnMoreIcon("default_items")
                              ],
                            ),
                            subtitle: Text(app.vendor.defaultMeal),
                            trailing: const Icon(Icons.chevron_right),
                          ),
                        ),
                        const SizedBox(height: 16),
                        SingleChildScrollView(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const SizedBox(height: 6),
                              if (app.orderTemplates.isEmpty)
                                onboardingNoItem(
                                  "Add a Standard Plan!",
                                  "Standard meal plans can be used to\nquickly add meals plans for your customers.",
                                  Icons.library_add,
                                  linkTag: "template",
                                )
                              else
                                const Padding(
                                  padding: EdgeInsets.only(left: 6, bottom: 6),
                                  child: Text("Standard Meal Plans",
                                      style: TextStyle(
                                          fontSize: 16,
                                          fontWeight: FontWeight.w500)),
                                ),
                              ...app.orderTemplates.map((template) => Card(
                                    child: ListTile(
                                      title: Text(template.name),
                                      subtitle: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                              "${template.items} (${app.currencySymbol}${template.price})"),
                                          Text("Repeat: ${template.repeat}"),
                                          getPropertyWithIcon(
                                              "${template.ordersLinked} order${template.ordersLinked > 1 ? "s" : ""} linked"
                                                  .toString(),
                                              Icons.dataset_linked),
                                        ],
                                      ),
                                      trailing: Row(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          IconButton(
                                              icon: const Icon(Icons.edit),
                                              onPressed: () async {
                                                if (template.ordersLinked > 0) {
                                                  if (!(await Utility.getConfirmation(
                                                      "Edit Standard Meal Plan?",
                                                      "Changes made to this plan will be applied on the ${template.ordersLinked} linked meal plans!"))) {
                                                    return;
                                                  }
                                                }
                                                await handleTemplateAddUpdate(
                                                    template);
                                                if (mounted) {
                                                  setState(() {});
                                                }
                                              }),
                                          IconButton(
                                              icon: const Icon(Icons.delete),
                                              onPressed: () async {
                                                await deleteOrder(template);
                                              }),
                                        ],
                                      ),
                                    ),
                                  ))
                            ],
                          ),
                        ),
                      ]),
                ),
              ),
        floatingActionButton: FloatingActionButton(
            child: const Icon(Icons.add),
            onPressed: () async {
              await handleTemplateAddUpdate();
              if (mounted) {
                setState(() {});
              }
            }));
  }

  Future<void> deleteOrder(OrderTemplate template) async {
    bool confirmed = await Utility.getConfirmation(
      "Delete ${template.name}",
      "Note: Deleting this standard meal plan will unlink meal plans linked to it!\n\nContinue to delete this standard meal plan?",
    );
    if (!confirmed) {
      return;
    }

    await Database.delete(Tables.orderTemplates, template.id);
    app.orderTemplates.removeWhere((element) => element.id == template.id);
    Utility.showMessage("Meal Plan Deleted Successfully");
    setState(() {});
  }
}

Future<void> handleTemplateAddUpdate([OrderTemplate? template]) async {
  App app = App();
  String defaultOrder = app.vendor.defaultMeal;
  List<String> options = await app.getOrderItems();
  options.addAll(app.orderTemplates
      .map((e) => e.items.split(", ").map((e) => e.trim()))
      .expand((element) => element));
  options.addAll(app.vendor.defaultMeal.split(", ").map((e) => e.trim()));
  String type = template != null ? template.type : 'repeat';
  String frequency = template == null ? "1,1,1,1,1,1,1" : template.frequency;
  if (type == "one_time") {
    type = "One Time Order";
  } else if (frequency.contains("0")) {
    type = "On Specific Days";
  } else {
    type = "Daily";
  }

  List<FormInput> orderForm = [
    FormInput("Meal Plan Name", "text",
        value: template != null ? template.name : "",
        prefixIcon: const Icon(Icons.folder)),
    ...orderInputs({
      "type": type,
      "frequency": frequency,
      "time": template != null ? template.time : "Breakfast",
      "price": template != null ? template.price.toString() : "",
      "items": template != null ? template.items : defaultOrder,
      "item_options": options.toSet().toList(),
      "is_veg": (template == null ? true : template.isVegan) ? "Yes" : "No"
    })
  ];

  dynamic formData =
      await AppRouter.navigateTo(MaterialPageRoute(builder: (context) {
    return FormView(
      "${template == null ? "Add" : "Update"} Meal Plan",
      orderForm,
    );
  }));
  if (formData == null) {
    return;
  }

  Map<String, String> data = {
    "template_name": formData["Meal Plan Name"],
    "time": formData["Time Slot"],
    "items": formData["Items"],
    "price": formData["Price Per Order"],
    "type": formData["Repeat"] == "One Time Order" ? "one_time" : "repeat",
    "is_veg": formData["Veg Order?"] == "Yes" ? "1" : "0",
    "vendor_id": app.vendorId.toString(),
    "frequency": formData["Repeat"] == "Daily"
        ? "1,1,1,1,1,1,1"
        : formData["Select day(s)"],
  };
  if (template != null) {
    await Database.update(Tables.orderTemplates, template.id, data);
  } else {
    await Database.add(Tables.orderTemplates, data);
    await Logger.logFirebaseEvent("template_added", {});
  }

  Utility.showMessage(
      "Meal Plan ${template == null ? "Added" : "Updated"} Successfully");
  await app.fetchOrderTemplates(update: true);
}
