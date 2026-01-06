import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:tiffincrm/utils/get_location.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:url_launcher/url_launcher.dart';

Future<void> navigate(LatLng location) async {
  Position position;
  try {
    position = await determinePosition();
  } catch (e) {
    return;
  }

  String url =
      'https://www.google.com/maps/dir/?api=1&origin=${position.latitude},${position.longitude} &destination=${location.latitude},${location.longitude}&travelmode=driving&dir_action=navigate';

  await launchUrl(Uri.parse(url));
  await Logger.logFirebaseEvent("navigation_used", {});
}
