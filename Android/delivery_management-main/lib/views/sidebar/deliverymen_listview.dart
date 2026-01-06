import 'package:flutter/material.dart';
import 'package:share_plus/share_plus.dart';
import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/components/form/form_view.dart';
import 'package:tiffincrm/models/delivery_man.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/strings.dart';
import 'package:tiffincrm/values/styles.dart';
import 'package:tiffincrm/values/tables.dart';

import '../../components/onboarding_no_item.dart';
import '../../utils/db.dart';

class DeliveryMenList extends StatefulWidget {
  const DeliveryMenList({super.key});

  @override
  State<DeliveryMenList> createState() => _DeliveryMenListState();
}

class _DeliveryMenListState extends State<DeliveryMenList> {
  List<DeliveryMan> deliveryMen = [];
  bool loading = true;
  App app = App();
  String countryCode = "IN";

  @override
  void initState() {
    openLearnMore("deliverymen", firstTime: true);
    countryCode = app.prefs.getString("country_code") ?? "IN";
    updateDeliveryMen().then((_) async {
      await app.getInitialData();
    });
    super.initState();
  }

  updateDeliveryMen() async {
    dynamic result = await Database.get(Tables.deliveryMen,
        where: {"vendor_id": app.vendorId}, silent: true);
    deliveryMen = List<DeliveryMan>.from(
        result.map((item) => DeliveryMan.fromJson(item)));
    deliveryMen.sort((a, b) => b.id.compareTo(a.id));

    if (mounted) {
      setState(() {
        loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          titleSpacing: 0,
          title: const Text("Delivery Men"),
          actions: [learnMoreButton("deliverymen")],
        ),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : deliveryMen.isEmpty
                ? onboardingNoItem(
                    "Add Your First Delivery Man!",
                    "You can Add Multiple Delivery men to deliver\norders based on their assigned zones.",
                    Icons.delivery_dining_outlined,
                    linkTag: "deliverymen",
                  )
                : SingleChildScrollView(
                    child: Padding(
                      padding: const EdgeInsets.all(12.0),
                      child: Column(
                        mainAxisSize: MainAxisSize.max,
                        children: [
                          const SizedBox(height: 10),
                          ElevatedButton.icon(
                              icon: const Icon(Icons.share, size: 18),
                              iconAlignment: IconAlignment.end,
                              style: Styles.normalButton,
                              onPressed: () {
                                Share.shareUri(Uri.parse(Strings.appShareLink));
                              },
                              label: const Text("Share App With Deliverymen")),
                          const SizedBox(height: 20),
                          ...deliveryMen.map((deliveryMan) => Card(
                                  child: ListTile(
                                title: Text(
                                    "${deliveryMan.name} (${deliveryMan.phone.isNotEmpty ? deliveryMan.phone : deliveryMan.email})",
                                    style: const TextStyle(
                                        fontWeight: FontWeight.bold)),
                                subtitle: Text(
                                    "Zones: ${deliveryMan.zones.isEmpty ? "All" : deliveryMan.zones.join(",")}"),
                                trailing: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    IconButton(
                                        icon: const Icon(Icons.edit),
                                        onPressed: () async {
                                          await handleManAddUpdate(deliveryMan);
                                        }),
                                    IconButton(
                                        icon: const Icon(Icons.delete),
                                        onPressed: () async {
                                          bool result =
                                              await Utility.getConfirmation(
                                            "Delete Delivery Man",
                                            "Are you sure?",
                                          );
                                          if (!result) {
                                            return;
                                          }
                                          await Database.delete(
                                              Tables.deliveryMen,
                                              deliveryMan.id);
                                          Utility.showMessage(
                                              "Delivery Man deleted");
                                          updateDeliveryMen();
                                        }),
                                  ],
                                ),
                              )))
                        ],
                      ),
                    ),
                  ),
        floatingActionButton: FloatingActionButton(
          onPressed: () async {
            await handleManAddUpdate(null);
          },
          child: const Icon(Icons.add),
        ));
  }

  Future<void> handleManAddUpdate(
    DeliveryMan? deliveryMan,
  ) async {
    List<FormInput> inputs = [
      FormInput("Name", "text",
          value: deliveryMan == null ? "" : deliveryMan.name),
      countryCode == "IN"
          ? FormInput("Phone", "phone",
              value: deliveryMan == null ? "" : deliveryMan.phone,
              maxLength: 10,
              prefixIcon: const SizedBox(
                  width: 45,
                  child: Center(
                      child: Text("+91",
                          style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w400,
                              color: Color(0xff343434))))))
          : FormInput("Email", "text",
              value: deliveryMan == null ? "" : deliveryMan.email),
      FormInput("Zones", "select_multiple",
          canAddOption: false,
          isRequired: false,
          options: app.zones,
          value: deliveryMan == null ? "" : deliveryMan.zones.join(",")),
    ];

    dynamic formData = await Navigator.push(
        context,
        MaterialPageRoute(
            builder: (context) => FormView(
                "${deliveryMan == null ? "Add" : "Update"} Delivery Man",
                inputs)));

    if (formData == null) return;

    Map<String, String> data = {
      "vendor_id": app.vendorId.toString(),
      "name": formData["Name"],
      "phone": formData["Phone"] ?? "0",
      "zones": formData["Zones"],
      "email": formData["Email"] ?? ""
    };

    if (deliveryMan == null) {
      await Database.add(Tables.deliveryMen, data);
    } else {
      await Database.update(Tables.deliveryMen, deliveryMan.id, data);
    }

    Utility.showMessage(
        "${deliveryMan == null ? "Added" : "Updated"} Successfully");
    updateDeliveryMen();
  }
}
