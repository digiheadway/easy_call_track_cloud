import 'package:country_picker/country_picker.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:tiffincrm/components/form/resizable_switch.dart';
import 'package:tiffincrm/utils/notification_manager.dart';
import 'package:tiffincrm/values/icons.dart';

import '../../components/learn_more_button.dart';
import '../../components/form/form_input.dart';
import '../../utils/app.dart';
import '../../utils/utility.dart';

class SettingsView extends StatefulWidget {
  const SettingsView({super.key});

  @override
  State<SettingsView> createState() => _SettingsViewState();
}

class _SettingsViewState extends State<SettingsView> {
  App app = App();
  bool notificationsPermanentlyDisabled = false;
  bool notificationsAllowed = false;

  @override
  void initState() {
    super.initState();
    Permission.notification.isPermanentlyDenied.then((value) async {
      notificationsAllowed = await Permission.notification.isGranted;
      setState(() {
        notificationsPermanentlyDisabled = value;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text("Settings"),
        ),
        body: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 16),
          child: Column(children: [
            ...[
              "Country",
              "Credit Limit",
              "Track Tiffin Boxes",
              "App Notifications"
            ].map((e) {
              String key = e.toLowerCase().replaceAll(" ", "_");
              String value = (app.vendor.toMap()[key] ?? "Not Set").toString();
              if (e == "Country") {
                value = "${Utility.getCountryByCode(value)} ($value)";
              }
              return Card(
                child: ListTile(
                  leading: Icon(MyIcons.get(e)),
                  title: Row(
                    children: [
                      Text("${e == "Credit Limit" ? "Default " : ""}$e"),
                      const SizedBox(width: 5),
                      if (!["Country", "App Notifications"].contains(e))
                        learnMoreIcon(e == "Credit Limit"
                            ? "credit_limit"
                            : "tiffin_collection")
                    ],
                  ),
                  trailing: e.contains("Notifications")
                      ? Padding(
                          padding: const EdgeInsets.only(right: 6),
                          child: GestureDetector(
                            onTap: () async {
                              if (!notificationsAllowed) {
                                if (notificationsPermanentlyDisabled) {
                                  openAppSettings();
                                  return;
                                } else {
                                  final status =
                                      await Permission.notification.request();
                                  notificationsAllowed = status.isGranted;
                                  if (!status.isGranted) {
                                    openAppSettings();
                                  }
                                }
                              }
                              await NotificationManager.getInstance()
                                  .saveFCMToken(null);
                              Utility.showMessage(
                                  "Notification Preferences Updated!");
                              setState(() {});
                            },
                            child: Text(notificationsAllowed
                                ? "Allowed"
                                : notificationsPermanentlyDisabled
                                    ? "Open Settings"
                                    : "Allow Now"),
                          ),
                        )
                      : e.contains("Tiffin")
                          ? Padding(
                              padding: const EdgeInsets.only(right: 6),
                              child: ResizableSwitch(value == "1",
                                  (newValue) async {
                                await app.updateVendor({
                                  key: newValue ? "1" : "0",
                                });
                                app.vendor.trackTiffinboxes = newValue;
                                app.changeInDrawer = true;
                                Utility.showMessage(
                                    "Settings Updated Successfully");
                                setState(() {});
                              }, 30),
                            )
                          : SizedBox(
                              width: MediaQuery.of(context).size.width * 0.4,
                              child: InkWell(
                                onTap: () async {
                                  if (e == "Country") {
                                    Utility.openCountryPicker(
                                        (Country country) async {
                                      String countryCode =
                                          country.toJson()['iso2_cc'];
                                      app.updateVendor({
                                        "country_code": countryCode,
                                      });
                                      await app.prefs.setString(
                                          "country_code", countryCode);
                                      app.dialCode = "";
                                      app.currencySymbol = "";
                                      app.vendor.countryCode =
                                          country.toJson()['iso2_cc'];
                                      Utility.showMessage(
                                          "Settings Updated Successfully");
                                      setState(() {});
                                    });
                                  } else if (e == "Credit Limit") {
                                    FormInput input = FormInput(
                                        "Credit Limit", "number",
                                        value: value);
                                    Utility.showBottomSheet(
                                        "Default Credit Limit",
                                        const SizedBox(),
                                        inputDescription:
                                            "This limit will be used as default minimum balance your customers can have.",
                                        showMoreTag: "credit_limit",
                                        isForm: true,
                                        inputs: [
                                          input
                                        ], onValueChanged: () async {
                                      String newValue =
                                          input.controller.text.trim();
                                      if (newValue.isEmpty) {
                                        newValue = "NULL";
                                      }
                                      await app.updateVendor(
                                          {"credit_limit": newValue});
                                      app.vendor.creditLimit =
                                          newValue == "NULL"
                                              ? null
                                              : int.parse(newValue);
                                      Utility.showMessage(
                                          "Settings Updated Successfully");
                                      setState(() {});
                                    });
                                  }
                                },
                                child: Row(
                                  mainAxisAlignment: MainAxisAlignment.end,
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Flexible(
                                      child: Text(
                                        value,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                    const Icon(Icons.chevron_right),
                                  ],
                                ),
                              ),
                            ),
                ),
              );
            }),
          ]),
        ));
  }
}
