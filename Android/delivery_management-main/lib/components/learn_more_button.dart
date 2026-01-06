import 'package:flutter/material.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/utils/logger.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:url_launcher/url_launcher.dart';

import '../values/tables.dart';

Widget learnMoreIcon(String tag, {double size = 15}) => InkWell(
    onTap: () async {
      openLearnMore(tag);
    },
    child: Icon(Icons.info_outlined, size: size));

Widget learnMoreButton(String tag) => TextButton(
    onPressed: () async {
      openLearnMore(tag);
    },
    child: const Row(
      children: [
        Text('Learn More', style: TextStyle(color: Colors.white)),
        SizedBox(width: 5),
        Icon(Icons.info, color: Colors.white, size: 15)
      ],
    ));

Future<void> openLearnMore(String tag, {bool firstTime = false}) async {
  if (firstTime) {
    App app = App();
    if (app.prefs.getBool("show_learn_more_$tag") ?? true) {
      await Future.delayed(const Duration(milliseconds: 800));
      app.prefs.setBool("show_learn_more_$tag", false);
    } else {
      return;
    }
  }

  List<dynamic> data = [];
  Utility.showBottomSheet("", StatefulBuilder(builder: (context, setState) {
    if (data.isEmpty) {
      Database.get(Tables.settings,
              where: {"name": 'learn_more_$tag'}, fields: "value", silent: true)
          .then((List<dynamic> result) {
        if (result.isEmpty) {
          AppRouter.goBack();
        } else if (!context.mounted) {
          return;
        } else {
          setState(() {
            data = result;
          });
        }
      });

      return const SizedBox(
          height: 300, child: Center(child: CircularProgressIndicator()));
    }
    return HtmlWidget(
      data.first["value"],
      onTapUrl: (url) =>
          launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication),
    );
  }));
  Logger.logFirebaseEvent("learn_more_$tag", {});
}
