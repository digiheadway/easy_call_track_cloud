import 'package:flutter/material.dart';
import 'package:tiffincrm/components/learn_more_button.dart';
import 'package:tiffincrm/values/icons.dart';

class CustomListTile extends StatelessWidget {
  final String title;
  final List<Widget> actions;
  final String learnMoretag;
  final IconData? leadingIcon;
  final Function? onTap;
  final bool? isProperty;

  const CustomListTile(
      {required this.title,
      this.actions = const [],
      this.learnMoretag = "",
      this.leadingIcon,
      this.isProperty = false,
      this.onTap,
      super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
        child: ListTile(
      minLeadingWidth: 30,
      leading: Icon(leadingIcon ?? MyIcons.get(title)),
      title: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Flexible(child: Text(title)),
          if (learnMoretag.isNotEmpty) ...[
            const SizedBox(width: 10),
            learnMoreIcon(learnMoretag)
          ]
        ],
      ),
      onTap: onTap as void Function()?,
      trailing: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        mainAxisSize: MainAxisSize.min,
        children: [
          ...actions,
          if (isProperty ?? false)
            const Padding(
              padding: EdgeInsets.only(left: 6),
              child: Icon(
                Icons.chevron_right,
                color: Colors.grey,
              ),
            )
        ],
      ),
    ));
  }
}
