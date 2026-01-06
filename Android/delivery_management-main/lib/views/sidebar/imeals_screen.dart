import 'package:flutter/material.dart';
import 'package:share_plus/share_plus.dart';
import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/values/icons.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/strings.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../components/form/form_input.dart';
import '../../utils/utility.dart';

class ImealsScreen extends StatefulWidget {
  const ImealsScreen({super.key});

  @override
  State<ImealsScreen> createState() => _ImealsScreenState();
}

class _ImealsScreenState extends State<ImealsScreen> {
  App app = App();

  @override
  void initState() {
    openLearnMore("imeals", firstTime: true);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        title: const Text('Customer Login Setting'),
        actions: [learnMoreButton("imeals")],
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 10),
              Card(
                child: ListTile(
                  leading: const Icon(Icons.login),
                  title: const Text('Allow Customers Login'),
                  trailing: SizedBox(
                    width: 30,
                    child: FittedBox(
                      fit: BoxFit.fill,
                      child: Switch(
                        activeColor: ThemeColors.primary,
                        value: app.vendor.canCustomerLogin,
                        onChanged: (value) async {
                          await app.updateVendor(
                              {"can_customer_login": value ? "1" : "0"});
                          Utility.showMessage("Updated Successfully!");
                          setState(() {
                            app.vendor.canCustomerLogin =
                                !app.vendor.canCustomerLogin;
                          });
                        },
                      ),
                    ),
                  ),
                ),
              ),
              ...["iMeals.in - Check Demo", "Share With Customers"].map((e) {
                return Card(
                  child: ListTile(
                    leading: Icon(MyIcons.get(e), size: 24),
                    trailing: GestureDetector(
                      onTap: () async {
                        if (e.contains("Demo")) {
                          await launchUrl(Uri.parse("https://imeals.in?demo"));
                        } else {
                          await Share.share(Strings.tiffincrmShareText
                              .replaceAll(
                                  "vendor_id", app.vendorId.toString()));
                        }
                      },
                      child: Icon(
                        e.contains("Demo") ? Icons.open_in_new : Icons.share,
                        size: 20,
                      ),
                    ),
                    title: Text(e),
                  ),
                );
              }),
              const SizedBox(height: 20),
              const Text("Configuration for Imeals",
                  style: TextStyle(
                      color: Colors.black45,
                      fontSize: 16,
                      fontWeight: FontWeight.bold)),
              const SizedBox(height: 10),
              ...[
                "Imeal Announcement",
                "Low Balance Reminder Limit",
                "Business Details",
                "Business Name",
                "Support Phone",
                "UPI ID",
              ].map((e) {
                if (e == "Business Details") {
                  return const Padding(
                    padding: EdgeInsets.only(bottom: 10),
                    child: Text("Business Details",
                        style: TextStyle(
                            color: Colors.black45,
                            fontSize: 16,
                            fontWeight: FontWeight.bold)),
                  );
                }
                String key = e.toLowerCase().replaceAll(" ", "_");
                String value = app.vendor.toMap()[key] ?? "";
                return Card(
                  child: ListTile(
                    leading:
                        Icon(MyIcons.get(e), size: 24, color: Colors.black87),
                    title: Text(e),
                    trailing: GestureDetector(
                      onTap: () async {
                        FormInput input = FormInput(
                            e,
                            e.contains("Phone")
                                ? "phone"
                                : e.contains("Balance")
                                    ? "number"
                                    : "text",
                            value: value);
                        FormInput boolInput = FormInput(
                            "Notify All Customers", "bool",
                            value: "Yes");

                        await Utility.showBottomSheet(e, const SizedBox(),
                            isForm: true,
                            inputDescription: {
                              "Business Name":
                                  "Set your business name for imeal customers",
                              "Support Phone":
                                  "Set support number to let customers whatsapp you.",
                              "UPI ID":
                                  "Set your UPI ID to get payments from your customers directly.",
                              "Imeal Announcement":
                                  "Set a text to be shown to your all customers on imeals.in.",
                              "Low Balance Reminder Limit":
                                  "Set a limit to show low-balance banner on imeals.",
                            }[e],
                            showMoreTag: "imeals",
                            inputs: [
                              input,
                              if (e == "Imeal Announcement") boolInput
                            ], onValueChanged: () async {
                          await app.updateVendor(
                              {key: input.controller.text.trim()});

                          Utility.showMessage("Updated Successfully!");
                          if (e == "Imeal Announcement" &&
                              boolInput.controller.text == "Yes") {
                            await app.fetchCustomers();
                            await app.showNotification(
                                app.customers.map((e) => e.id).toList(),
                                input.controller.text.trim());
                            Utility.showMessage("Notification Sent!");
                          }
                        });
                        await app.setVendor(update: true);
                        setState(() {});
                        app.changeInDrawer = true;
                      },
                      child: Container(
                        constraints: BoxConstraints(
                            maxWidth: MediaQuery.of(context).size.width * 0.4),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.end,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Flexible(
                              child: Text(
                                value.isNotEmpty ? value : "Not Set",
                                style: TextStyle(
                                    fontWeight: FontWeight.w400,
                                    overflow: TextOverflow.ellipsis,
                                    color: value.isEmpty
                                        ? Colors.red
                                        : Colors.black87),
                              ),
                            ),
                            const SizedBox(width: 2),
                            const Icon(Icons.chevron_right)
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              }),
            ],
          ),
        ),
      ),
    );
  }
}
