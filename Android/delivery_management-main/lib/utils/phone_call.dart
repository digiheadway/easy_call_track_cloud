import 'package:flutter_phone_direct_caller/flutter_phone_direct_caller.dart';
import 'package:url_launcher/url_launcher.dart';

Future<void> call(String number) async {
  // String url = "tel:+91$number";

  bool? result = await FlutterPhoneDirectCaller.callNumber(number);
  if (result == false) {
    await launchUrl(Uri.parse("tel:$number"));
  }
}
