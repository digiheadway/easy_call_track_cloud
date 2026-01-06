import 'package:fast_contacts/fast_contacts.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/tables.dart';

class ContactsListView extends StatefulWidget {
  const ContactsListView({super.key});

  @override
  State<ContactsListView> createState() => _ContactsListViewState();
}

class _ContactsListViewState extends State<ContactsListView> {
  List<Contact> contacts = [];
  List<int> selected = [];
  bool loading = true;
  String searchValue = "";
  bool permissionDenied = false;
  App app = App();
  PermissionStatus status = PermissionStatus.denied;

  @override
  void initState() {
    getContacts();
    super.initState();
  }

  Future<void> getPermission() async {
    status = await Permission.contacts.request();
    if (status != PermissionStatus.granted) {
      Utility.showMessage("Permission denied");
    } else {
      Utility.showMessage("Contacts loaded successfully!");
      await getContacts();
    }
  }

  Future<void> getContacts() async {
    try {
      status = await Permission.contacts.status;
      if (status != PermissionStatus.granted) {
        setState(() {
          permissionDenied = true;
          loading = false;
        });
        return;
      }

      contacts = await FastContacts.getAllContacts();
      List<String> alreadyExistingPhones =
          app.customers.map((e) => e.phone).toList();
      contacts = contacts
          .where((element) =>
              element.phones.isNotEmpty &&
              !alreadyExistingPhones.contains(element.phones.first.number))
          .toList();
      contacts.sort((a, b) => a.displayName.compareTo(b.displayName));
      selected = [];

      setState(() {
        permissionDenied = false;
        loading = false;
      });
    } catch (e) {
      Utility.showMessage(e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          titleSpacing: 0,
          title: const Text("Add Customer(s)"),
        ),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : Padding(
                padding: const EdgeInsets.only(top: 16, left: 16, right: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (!permissionDenied)
                      TextField(
                        decoration: const InputDecoration(
                          suffixIcon: Icon(Icons.search),
                          border: OutlineInputBorder(),
                          labelText: 'Search Customer Details',
                        ),
                        onChanged: (value) {
                          setState(() {
                            searchValue = value.trim();
                          });
                        },
                      ),
                    ListTile(
                      dense: true,
                      contentPadding:
                          const EdgeInsets.only(right: 12, top: 10, bottom: 10),
                      leading: const Icon(Icons.add_circle_outline_outlined),
                      title: Text(
                        "Add ${searchValue.isNotEmpty ? "\"$searchValue\" as " : "new"} customer",
                        style: const TextStyle(fontSize: 18),
                      ),
                      onTap: () {
                        Navigator.pop(context,
                            {"action": "addFromSearch", "value": searchValue});
                      },
                      trailing: const Icon(Icons.chevron_right),
                    ),
                    permissionDenied
                        ? Expanded(
                            child: Center(
                              child: status.isPermanentlyDenied
                                  ? const Text(
                                      "Permission Denied!\nAllow permission to import customers from contacts!",
                                      textAlign: TextAlign.center,
                                    )
                                  : Column(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        ElevatedButton(
                                            onPressed: getPermission,
                                            child: const Row(
                                              mainAxisSize: MainAxisSize.min,
                                              children: [
                                                Icon(Icons.contacts),
                                                SizedBox(width: 8),
                                                Text(
                                                  "Use Phone Contacts",
                                                  style: TextStyle(
                                                      fontWeight:
                                                          FontWeight.bold),
                                                ),
                                              ],
                                            )),
                                        const SizedBox(height: 8),
                                        const Text("Require Permission")
                                      ],
                                    ),
                            ),
                          )
                        : contacts.isEmpty
                            ? const Expanded(
                                child: Center(
                                  child: Text("No Contacts Found"),
                                ),
                              )
                            : Expanded(
                                child: ListView.builder(
                                    padding: const EdgeInsets.all(0),
                                    shrinkWrap: true,
                                    itemCount: contacts.length,
                                    itemBuilder: (context, index) {
                                      Contact contact = contacts[index];
                                      return Visibility(
                                        visible: contact
                                            .toString()
                                            .toLowerCase()
                                            .contains(
                                                searchValue.toLowerCase()),
                                        child: ListTile(
                                          contentPadding: const EdgeInsets.only(
                                              left: 8, right: 2),
                                          onTap: () {
                                            handleCheckBoxChange(index);
                                          },
                                          trailing: Checkbox(
                                            onChanged: (value) {
                                              handleCheckBoxChange(index);
                                            },
                                            value: selected.contains(index),
                                          ),
                                          title: Text(
                                            contact.displayName,
                                            style: const TextStyle(
                                                fontSize: 16,
                                                fontWeight: FontWeight.bold),
                                          ),
                                          subtitle: Text(
                                              contact.phones.first.number
                                                  .toString(),
                                              style: const TextStyle(
                                                  fontSize: 16)),
                                        ),
                                      );
                                    })),
                  ],
                ),
              ),
        floatingActionButton: permissionDenied || selected.isEmpty
            ? null
            : TextButton(
                style: TextButton.styleFrom(
                  backgroundColor: ThemeColors.primary,
                  foregroundColor: Colors.white,
                ),
                onPressed: handleImportBtnClick,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Text("Import ${selected.length} Contacts",
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                )));
  }

  void handleCheckBoxChange(int index) async {
    setState(() {
      if (!selected.contains(index)) {
        selected.add(index);
      } else {
        selected.remove(index);
      }
    });
  }

  Future<void> handleImportBtnClick() async {
    if (!(await Utility.getConfirmation(
      "Add ${selected.length} Customer(s)",
      "Are you sure?",
    ))) {
      return;
    }

    List<Map<String, dynamic>> customers = selected.map((index) {
      Contact contact = contacts[index];
      return {
        "name": contact.displayName,
        "phone": contact.phones.first.number
            .replaceAll(RegExp(r"[ \-]"), "")
            .replaceFirst(app.dialCode, ""),
        "vendor_id": app.vendorId,
        "phone2": "",
        "auth_token": app.generateRandomToken()
      };
    }).toList();

    await Database.addMany(Tables.clients, customers);
    await app.fetchCustomers(update: true);
    Utility.showMessage("Contacts Added Successfully");
    AppRouter.goBack(selected.length > 1
        ? null
        : {
            "action": "openProfile",
            "value": customers.first["phone"].toString()
          });
  }
}
