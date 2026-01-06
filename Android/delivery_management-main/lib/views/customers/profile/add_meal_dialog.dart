import 'package:flutter/material.dart';
import 'package:tiffincrm/components/custom_filter_chip.dart';
import 'package:tiffincrm/models/customer.dart';
import 'package:tiffincrm/models/order_template.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/styles.dart';
import 'package:tiffincrm/values/tables.dart';
import 'package:tiffincrm/views/customers/profile/customer_orders_listview.dart';
import 'package:tiffincrm/views/sidebar/order_template_listview.dart';

import '../../../components/learn_more_button.dart';
import '../../../components/onboarding_no_item.dart';
import '../../../values/numbers.dart';

class AddMealDialog extends StatefulWidget {
  final List<int> customerIds;
  const AddMealDialog({super.key, required this.customerIds});

  @override
  State<AddMealDialog> createState() => _AddMealDialogState();
}

class _AddMealDialogState extends State<AddMealDialog> {
  App app = App();
  List<OrderTemplate> selected = [];
  List<String> selectedFilters = [];
  List<String> times = [];
  List<String> repeats = [];
  Map<String, int> counts = {};

  @override
  Widget build(BuildContext context) {
    times = app.orderTemplates.map((e) => e.time).toSet().toList();
    repeats = app.orderTemplates.map((e) => e.repeat).toSet().toList();

    // set counts
    counts = {};
    for (OrderTemplate e in app.orderTemplates) {
      counts[e.time] = (counts[e.time] ?? 0) + 1;
      counts[e.repeat] = (counts[e.repeat] ?? 0) + 1;
      if (e.isVegan) {
        counts["Veg"] = (counts["Veg"] ?? 0) + 1;
      } else {
        counts["Non-Veg"] = (counts["Non-Veg"] ?? 0) + 1;
      }
    }

    return SizedBox(
      height: MediaQuery.of(context).size.height * 0.75,
      child: SingleChildScrollView(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            SizedBox(
              height: MediaQuery.of(context).size.height * 0.75 - 50,
              child: SingleChildScrollView(
                  child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(right: 20),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                const Text("Add Meal Plan",
                                    style: TextStyle(
                                        fontSize: 18,
                                        fontWeight: FontWeight.bold)),
                                const SizedBox(width: 6),
                                learnMoreIcon("meal_plan")
                              ],
                            ),
                            const Text("Choose from Standard Meal Plans!"),
                          ],
                        ),
                      ),
                      GestureDetector(
                          onTap: () async {
                            await AppRouter.navigateTo("/order_templates");

                            setState(() {
                              selected = [];
                            });
                          },
                          child:
                              const Icon(Icons.settings, color: Colors.black))
                    ],
                  ),
                  const SizedBox(height: 20),
                  if (app.orderTemplates.isEmpty)
                    onboardingNoItem(
                      "Add a Standard Plan!",
                      "Standard meal plans can be used to\nquickly add meals plans for your customers.",
                      Icons.library_add,
                      linkTag: "template",
                    ),
                  if (app.orderTemplates.length > 6)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: Row(
                          children: [
                            ...times,
                            "Veg",
                            "Non-Veg",
                            ...repeats,
                          ].map((e) {
                            return Padding(
                              padding: const EdgeInsets.only(right: 8),
                              child: CustomFilterChip(
                                  isSelected: selectedFilters.contains(e),
                                  selectedOptions: const [],
                                  options: const [],
                                  label: e,
                                  labelWithCount: "$e (${counts[e] ?? 0})",
                                  optionCounts: const {},
                                  updateState: ({bool selected = false}) {
                                    setState(() {
                                      if (selected) {
                                        selectedFilters.add(e);
                                      } else {
                                        selectedFilters.remove(e);
                                      }
                                    });
                                  }),
                            );
                          }).toList(),
                        ),
                      ),
                    ),
                  ...app.orderTemplates
                      .where((e) =>
                          selectedFilters.isEmpty ||
                          (!selectedFilters.any((e) => times.contains(e)) ||
                                  selectedFilters.contains(e.time)) &&
                              (!selectedFilters
                                      .any((e) => repeats.contains(e)) ||
                                  selectedFilters.contains(e.repeat)) &&
                              (!selectedFilters.any((e) => e.contains("Veg")) ||
                                  selectedFilters
                                      .contains(e.isVegan ? "Veg" : "Non-Veg")))
                      .map(
                        (e) => GestureDetector(
                          onTap: () {
                            if (selected.contains(e)) {
                              selected.remove(e);
                            } else {
                              selected.add(e);
                            }
                            setState(() {});
                          },
                          child: Card(
                            color: Colors.grey.shade100,
                            shape: RoundedRectangleBorder(
                                borderRadius: const BorderRadius.all(
                                  Radius.circular(Numbers.borderRadius),
                                ),
                                side: BorderSide(
                                    color: selected.contains(e)
                                        ? ThemeColors.primary
                                        : const Color(0xFFDEDEDE))),
                            child: ListTile(
                              contentPadding: const EdgeInsets.symmetric(
                                  horizontal: 12, vertical: 8),
                              visualDensity: VisualDensity.compact,
                              dense: true,
                              title: Text(
                                "${e.name}${e.repeat == "One Time Order" ? " (One Time)" : ""}",
                                style: const TextStyle(fontSize: 14),
                              ),
                              trailing: Text(
                                "${e.time} (${app.currencySymbol}${e.price})",
                                style: const TextStyle(fontSize: 12),
                              ),
                            ),
                          ),
                        ),
                      ),
                  Opacity(
                    opacity: selected.isNotEmpty ? 0.4 : 1.0,
                    child: Card(
                      color: Colors.grey.shade100,
                      child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(
                              horizontal: 12, vertical: 8),
                          visualDensity: VisualDensity.compact,
                          dense: true,
                          onTap: () async {
                            if (selected.isNotEmpty) {
                              return;
                            }
                            await handleTemplateAddUpdate();
                            if (mounted) {
                              setState(() {});
                            }
                          },
                          title: const Text(
                            "-- Create New Standard Meal Plan!",
                            style: TextStyle(fontSize: 14),
                          ),
                          trailing: const Icon(Icons.add)),
                    ),
                  ),
                ],
              )),
            ),
            selected.isNotEmpty
                ? Center(
                    child: ElevatedButton(
                        style: Styles.normalButton,
                        onPressed: onAddBtnClicked,
                        child: Text(
                            "Add Selected (${selected.length}) Meal Plan")),
                  )
                : widget.customerIds.length == 1
                    ? Align(
                        alignment: Alignment.bottomCenter,
                        child: Center(
                          child: TextButton(
                              onPressed: () async {
                                Customer customer = app.customers.firstWhere(
                                    (e) => e.id == widget.customerIds.first);
                                bool done = await handleOrderAddUpdate(
                                    null, customer.id,
                                    isVegan: customer.isVegan);
                                if (done) {
                                  AppRouter.goBack(true);
                                }
                              },
                              child: const Text("Create Customized Meal Plan")),
                        ),
                      )
                    : const SizedBox.shrink(),
          ],
        ),
      ),
    );
  }

  void onAddBtnClicked() async {
    // create order from template
    bool confirmed = await Utility.getConfirmation(
      "Are you sure?",
      "Add selected (${selected.length}) meals to ${widget.customerIds.length == 1 ? "this" : "selected"} customer?",
    );

    if (!confirmed) {
      return;
    }

    List<Map<String, String>> data = [];
    for (OrderTemplate e in selected) {
      for (int cxId in widget.customerIds) {
        data.add({
          "time": e.time,
          "items": e.items,
          "price": e.price.toString(),
          "type": e.type,
          "template_id": e.id.toString(),
          "is_veg": e.isVegan ? "1" : "0",
          "is_active": "1",
          "client_id": cxId.toString(),
          "frequency": e.frequency,
          "created_on": DateTime.now().toIso8601String()
        });
      }
    }

    if (data.length > 1) {
      await Database.addMany(Tables.orders, data);
    } else {
      await Database.add(Tables.orders, data.first);
    }

    if (context.mounted) {
      Utility.showMessage("Meal Plan Added Successfully!");
      AppRouter.goBack(true);
    }
  }
}
