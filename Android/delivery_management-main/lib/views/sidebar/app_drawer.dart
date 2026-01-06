import 'package:flutter/material.dart';
import 'package:in_app_review/in_app_review.dart';
import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/components/form/form_view.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/tables.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/strings.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

class MyAppDrawer extends StatefulWidget {
  const MyAppDrawer({super.key});

  @override
  State<MyAppDrawer> createState() => _MyAppDrawerState();
}

class _MyAppDrawerState extends State<MyAppDrawer> {
  App app = App();

  @override
  void initState() {
    Database.get(Tables.transactionsView,
            silent: true,
            fields: "count",
            where:
                "vendor_id = ${app.vendorId} AND type = 'order_delivered' AND date = CURDATE()")
        .then((value) {
      int count = 0;
      if (value.isNotEmpty) {
        count = int.tryParse(value[0]['count'].toString()) ?? 0;
      }
      if (!mounted) return;
      setState(() {
        if (app.dailyUsuageCount > app.vendor.dailyOrderLimit) {
          app.prefs.setString("planTitle", "Free Trial");
        }
        app.dailyUsuageCount = count;
      });
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Drawer(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(0),
        ),
        child: Column(mainAxisSize: MainAxisSize.max, children: [
          DrawerHeader(
            margin: EdgeInsets.zero,
            padding: const EdgeInsets.fromLTRB(0, 6, 0, 1),
            decoration: const BoxDecoration(
              color: ThemeColors.primary,
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Row(
                    mainAxisSize: MainAxisSize.max,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Image.asset(
                        'assets/icon/splash_dark_big.png',
                        height: 60,
                      ),
                      const SizedBox(width: 15),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(app.vendor.name,
                                softWrap: false,
                                maxLines: 1,
                                style: const TextStyle(
                                    fontSize: 25,
                                    color: Colors.white,
                                    overflow: TextOverflow.ellipsis)),
                            Text(
                              (app.vendor.businessName.isNotEmpty
                                  ? app.vendor.businessName
                                  : "Welcome to TiffinCRM"),
                              style: const TextStyle(color: Colors.white),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 4),
                Container(
                  color: Colors.white,
                  child: ListTile(
                    minLeadingWidth: 40,
                    onTap: () => openLearnMore("pricing"),
                    visualDensity: VisualDensity.compact,
                    leading: app.dailyUsuageCount < app.vendor.dailyOrderLimit
                        ? Padding(
                            padding: const EdgeInsets.only(top: 5),
                            child: SizedBox(
                              height: 30,
                              width: 30,
                              child: Center(
                                child: CircularProgressIndicator(
                                  value: app.dailyUsuageCount /
                                      app.vendor.dailyOrderLimit,
                                  backgroundColor: Colors.black12,
                                  color: ThemeColors.primary,
                                ),
                              ),
                            ),
                          )
                        : const Icon(
                            Icons.shopping_cart_outlined,
                            color: ThemeColors.primary,
                            size: 26,
                          ),
                    title: Text(
                      app.vendor.dailyOrderLimit > 25
                          ? "Free Trial"
                          : app.planTitle,
                      style: const TextStyle(
                          fontWeight: FontWeight.bold, fontSize: 16),
                    ),
                    trailing: const Icon(Icons.chevron_right),
                    subtitle: Text(
                      "${app.dailyUsuageCount == -1 ? "-" : app.dailyUsuageCount}/${app.vendor.dailyOrderLimit} Orders Processed Today",
                      style: const TextStyle(
                          color: ThemeColors.primary,
                          fontWeight: FontWeight.bold,
                          fontSize: 14),
                    ),
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: SingleChildScrollView(
              child: Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Column(
                  mainAxisSize: MainAxisSize.max,
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    ListTile(
                      leading: const Icon(Icons.edit_square),
                      title: const Text('Profile Info'),
                      onTap: () async {
                        await handleProfileEdit("profile_info");
                      },
                    ),
                    const Divider(thickness: 0.5),
                    ListTile(
                      leading: const Icon(Icons.web),
                      title: const Text('iMeals (Customer Login)'),
                      onTap: () async {
                        await app.setVendor();
                        await AppRouter.navigateTo("/imeals");
                        setState(() {});
                      },
                    ),
                    ListTile(
                      leading: const Icon(Icons.group),
                      title: const Text('Delivery Men'),
                      onTap: () {
                        AppRouter.navigateTo("/deliverymen");
                      },
                    ),
                    ListTile(
                        leading: const Icon(Icons.edit_document),
                        title: const Text('Standard Meal Plans'),
                        onTap: () async {
                          await AppRouter.navigateTo("/order_templates");
                        }),
                    ListTile(
                      leading: const Icon(Icons.settings),
                      title: const Text('Settings'),
                      onTap: () async {
                        await AppRouter.navigateTo("/settings");
                      },
                    ),
                    const Divider(thickness: 0.5),
                    ListTile(
                      leading: const Icon(Icons.upload),
                      title: const Text('Import Data'),
                      onTap: () async {
                        await openLearnMore("import_data");
                      },
                    ),
                    ListTile(
                      leading: const Icon(Icons.download),
                      title: const Text('Export Data'),
                      onTap: () async {
                        await app.fetchCustomers();
                        await app.downloadCustomers();
                      },
                    ),
                    const Divider(thickness: 0.5),
                    ListTile(
                      leading: const Icon(Icons.info),
                      title: const Text('Learn More'),
                      onTap: () async {
                        AppRouter.navigateTo("/learn");
                      },
                    ),
                    ListTile(
                      leading: const Icon(Icons.share_outlined),
                      title: const Text('Share TiffinCRM App'),
                      onTap: () async {
                        await Share.share(Strings.appShareLink);
                      },
                    ),
                    ListTile(
                      leading: const Icon(Icons.stars),
                      title: const Text('Rate This App!'),
                      onTap: () async {
                        Utility.tryCatch(() async {
                          Logger.logFirebaseEvent("App_Review_Btn_Click", {});
                          final InAppReview inAppReview = InAppReview.instance;

                          await inAppReview.openStoreListing();
                        });
                      },
                    ),
                    ListTile(
                      leading: const Icon(Icons.support_agent),
                      title: const Text('Support'),
                      onTap: () async {
                        Utility.tryCatch(
                            () async {
                              await launchUrl(Uri.parse(Strings.whatsappLink
                                  .replaceFirst(
                                      "__text__", "Hi, I have a query!")
                                  .replaceFirst(
                                      "__phone__", Strings.supportPhone)));
                            },
                            silent: true,
                            onException: (e) async {
                              await launchUrl(Uri.parse(
                                  "https://tiffincrm.com/contact-us.php"));
                            });
                      },
                    ),
                    ListTile(
                        leading: const Icon(Icons.logout),
                        title: const Text(
                          'Logout',
                        ),
                        onTap: () async {
                          await app.logout();
                        })
                  ],
                ),
              ),
            ),
          ),
        ]));
  }

  Future<void> handleProfileEdit(String type,
      {bool fromOutside = false}) async {
    App app = App();
    await app.setVendor();
    List<FormInput> profileForm = [
      FormInput("Your Name", "text", value: app.vendor.name),
      FormInput("Phone", "phone", value: app.vendor.phone, readOnly: true),
      FormInput("Email", "text",
          isRequired: app.vendor.email != null,
          value: app.vendor.email ?? "",
          readOnly: app.vendor.email != null),
      FormInput("City", "text",
          isRequired: false,
          value: app.vendor.city ?? "",
          prefixIcon: const Icon(Icons.location_city)),
    ];

    dynamic formData =
        await AppRouter.navigateTo(MaterialPageRoute(builder: (context) {
      return FormView("Profile Info", profileForm);
    }));

    if (formData == null) {
      return;
    }

    await app.updateVendor({
      "name": formData["Your Name"],
      "phone": formData["Phone"],
      "email": formData["Email"],
      "city": formData["City"],
    });
    await app.setVendor(update: true);
    await Logger.logFirebaseEvent("app_drawer_$type", {});

    app.changeInDrawer = true;
    Utility.showMessage("Settings Updated Successfully");
    AppRouter.goBack();

    setState(() {});
  }
}
