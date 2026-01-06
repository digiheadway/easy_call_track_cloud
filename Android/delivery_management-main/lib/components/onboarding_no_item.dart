import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/values/my_colors.dart';

import '../values/styles.dart';

Widget onboardingNoItem(
  String title,
  String description,
  IconData icon, {
  Widget button = const SizedBox(),
  String? linkTag,
}) =>
    Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(height: 20),
          Stack(children: [
            SvgPicture.asset(
              "assets/images/egg_shape.svg",
              colorFilter: const ColorFilter.mode(
                  Color.fromARGB(255, 207, 218, 244), BlendMode.srcIn),
            ),
            Positioned(
                left: 48,
                top: 15,
                child: Icon(icon, color: ThemeColors.primary, size: 50))
          ]),
          const SizedBox(height: 20),
          Text(title, style: Styles.textHeading),
          const SizedBox(height: 8),
          Text(
            description,
            textAlign: TextAlign.center,
            style: const TextStyle(
                fontWeight: FontWeight.w500, color: ThemeColors.normalBlack),
          ),
          if (linkTag != null)
            Padding(
              padding: const EdgeInsets.only(top: 10),
              child: GestureDetector(
                  onTap: () async {
                    await openLearnMore(linkTag);
                  },
                  child: const Text(
                    "Check How It Works",
                    style: TextStyle(decoration: TextDecoration.underline),
                  )),
            ),
          const SizedBox(height: 20),
          button
        ],
      ),
    );
