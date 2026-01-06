import 'dart:io';

// import 'package:android_play_install_referrer/android_play_install_referrer.dart'; // Discontinued package
import 'package:country_picker/country_picker.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:tiffincrm/components/form/bool_input.dart';
import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/values/numbers.dart';

import '../values/countries.dart';
import '../values/styles.dart';

final GlobalKey<ScaffoldMessengerState> snackbarKey =
    GlobalKey<ScaffoldMessengerState>();

class Utility {
  static Future<bool> isOnline() async {
    try {
      final response = await InternetAddress.lookup("example.com");
      if (response.isNotEmpty && response[0].rawAddress.isNotEmpty) {
        return true;
      }
    } on SocketException catch (_) {
      return false;
    }
    return false;
  }

  // Commented out due to discontinued package
  // static Future<String> getReferrer() async {
  //   try {
  //     ReferrerDetails referrerDetails =
  //         await AndroidPlayInstallReferrer.installReferrer;
  //     return referrerDetails.toString();
  //   } catch (e) {
  //     return e.toString();
  //   }
  // }

  static Future<void> showMessage(String message, {int duration = 3}) async {
    snackbarKey.currentState?.removeCurrentSnackBar();

    final SnackBar snackBar = SnackBar(
      content: Text(message),
      elevation: 2,
      duration: Duration(seconds: duration),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(Numbers.borderRadius),
      ),
      behavior: SnackBarBehavior.floating,
    );
    await snackbarKey.currentState?.showSnackBar(snackBar).closed;
  }

  static Future<T?> tryCatch<T>(Future<T> Function() param0,
      {String? errorMessage,
      bool silent = false,
      Function(dynamic e)? onException}) async {
    try {
      return await param0();
    } catch (e, s) {
      if (!silent) {
        Utility.showMessage(errorMessage ?? e.toString());
      }
      FirebaseCrashlytics.instance.recordError(e, s);
      if (onException != null) {
        onException(e);
      }
    }
    return null;
  }

  static void showLoaderDialog() {
    showDialog(
      // barrierColor: Colors.transparent,
      barrierDismissible: false,
      context: navigatorKey.currentContext!,
      builder: (BuildContext context) {
        return AlertDialog(
            content: Row(
          children: [
            const CircularProgressIndicator(),
            const SizedBox(width: 10),
            Container(
                margin: const EdgeInsets.only(left: 7),
                child: const Text("Loading...")),
          ],
        ));
      },
    );
  }

  static Future<T?> showBottomSheet<T>(
    String title,
    Widget content, {
    String? infoTag,
    String? inputDescription,
    bool isForm = false,
    String? showMoreTag,
    double? height,
    Widget? topRightButton,
    List<FormInput>? inputs,
    Function()? onValueChanged,
  }) async {
    return await showModalBottomSheet<T>(
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      context: navigatorKey.currentContext!,
      builder: (BuildContext context) {
        return Padding(
          padding: MediaQuery.of(context).viewInsets.bottom > 0
              ? MediaQuery.of(context).viewInsets
              : MediaQuery.of(context).padding,
          child: Container(
            height: height,
            constraints: BoxConstraints(
              maxHeight: MediaQuery.of(context).size.height * 0.85,
            ),
            decoration: const BoxDecoration(
              borderRadius: BorderRadius.only(
                  topLeft: Radius.circular(Numbers.borderRadius),
                  topRight: Radius.circular(Numbers.borderRadius)),
              color: Colors.white,
            ),
            child: SingleChildScrollView(
              physics: const ScrollPhysics(),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(15, 15, 15, 40),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (title.isNotEmpty)
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(title,
                              style: const TextStyle(
                                  fontSize: 20, fontWeight: FontWeight.bold)),
                          if (infoTag != null) learnMoreIcon(infoTag, size: 20),
                          if (topRightButton != null) topRightButton
                        ],
                      ),
                    const SizedBox(height: 10),
                    if (isForm) ...[
                      if (inputDescription != null)
                        Text.rich(
                          TextSpan(text: inputDescription, children: [
                            const TextSpan(text: " "),
                            if (showMoreTag != null)
                              TextSpan(
                                  text: "Learn more",
                                  style: const TextStyle(
                                      decoration: TextDecoration.underline),
                                  recognizer: TapGestureRecognizer()
                                    ..onTap = () {
                                      openLearnMore(showMoreTag);
                                    })
                          ]),
                        ),
                      const SizedBox(height: 15),
                      // FormView(title, inputs)
                      Column(
                        children: inputs!.map((input) {
                          return Padding(
                              padding: const EdgeInsets.only(bottom: 20),
                              child: input.type == "bool"
                                  ? BoolInput(input)
                                  : TextField(
                                      autofocus: true,
                                      controller: input.controller,
                                      keyboardType: input.keyboardType,
                                      decoration: InputDecoration(
                                          hintText: "Enter ${input.name}"),
                                    ));
                        }).toList(),
                      ),
                      ElevatedButton(
                          style: Styles.mainButton,
                          onPressed: () async {
                            await onValueChanged!();
                            AppRouter.goBack();
                          },
                          child: const Text("Update")),
                    ] else
                      content
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  static String getCountryByCode(String code) {
    return Countries.countries.containsKey(code)
        ? Countries.countries[code]![0]
        : "";
  }

  static void openCountryPicker(Function(Country) onSelect) {
    showCountryPicker(
        context: navigatorKey.currentContext!,
        favorite: ["IN", "CA", "AE", "PK"],
        countryListTheme: CountryListThemeData(
          bottomSheetHeight:
              MediaQuery.of(navigatorKey.currentContext!).size.height * 0.7,
          margin: EdgeInsets.zero,
          borderRadius: BorderRadius.circular(0),
          backgroundColor: Colors.white,
        ),
        showPhoneCode: true,
        onSelect: onSelect);
  }

  static Future<T?> showCustomDialog<T>(
    String title,
    String message,
    Map<String, Function> buttons, {
    Widget? contentWidget,
    String? infoTag,
    bool barrierDismissible = false,
  }) async {
    return await showDialog<T>(
      barrierDismissible: barrierDismissible,
      context: navigatorKey.currentContext!,
      builder: (BuildContext context) {
        return AlertDialog.adaptive(
          title: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(title, style: const TextStyle(fontSize: 22)),
              if (infoTag != null) learnMoreIcon(infoTag, size: 20)
            ],
          ),
          content: contentWidget ?? Text(message),
          actions: buttons.entries.map((MapEntry<String, Function> entry) {
            return TextButton(
                onPressed: () async {
                  if (!context.mounted) return;
                  Navigator.of(context).pop(await entry.value());
                },
                child: Text(entry.key));
          }).toList(),
        );
      },
    );
  }

  static Future<bool> getConfirmation(
    String title,
    String content, {
    Widget? contentWidget,
    String okText = "Okay",
    bool showCancel = true,
  }) async {
    Map<String, Function> actions = {};
    if (showCancel) {
      actions["Cancel"] = () => false;
    }
    actions[okText] = () => true;
    return await Utility.showCustomDialog<bool>(title, content, actions,
            contentWidget: contentWidget) ??
        false;
  }

  static void closeCurrentDialog() {
    AppRouter.goBack();
  }
}
