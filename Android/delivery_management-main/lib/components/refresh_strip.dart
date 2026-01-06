import 'dart:async';
import 'package:flutter/material.dart';

class RefreshStrip extends StatefulWidget {
  final DateTime dataDownloadedAt;
  final Future<dynamic> Function() update;

  const RefreshStrip({
    required this.update,
    required this.dataDownloadedAt,
    super.key,
  });

  @override
  State<RefreshStrip> createState() => _RefreshStripState();
}

class _RefreshStripState extends State<RefreshStrip> {
  late Timer timer;

  @override
  void initState() {
    timer = Timer.periodic(const Duration(minutes: 1), (t) {
      setState(() {});
    });
    super.initState();
  }

  @override
  void dispose() {
    timer.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    int minutes = DateTime.now().difference(widget.dataDownloadedAt).inMinutes;
    return minutes < 1
        ? Container()
        : Container(
            margin: const EdgeInsets.only(bottom: 0),
            padding: const EdgeInsets.all(3),
            width: double.infinity,
            color: Colors.red[300],
            child: Row(
              mainAxisSize: MainAxisSize.min,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Last Updated: ${minutes > 1 ? '$minutes mins ago' : minutes < 1 ? 'now' : '1 min ago'}',
                  style: const TextStyle(color: Colors.white),
                ),
                const SizedBox(width: 10),
                GestureDetector(
                  onTap: onRefreshTap,
                  child: const Icon(
                    weight: 10,
                    color: Colors.white,
                    Icons.refresh,
                    size: 16,
                  ),
                ),
              ],
            ),
          );
  }

  void onRefreshTap() async {
    await widget.update();
  }
}
