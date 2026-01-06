import 'package:flutter/material.dart';
import 'package:tiffincrm/utils/app.dart';
import 'package:tiffincrm/utils/app_router.dart';
import 'package:tiffincrm/utils/db.dart';
import 'package:tiffincrm/utils/utility.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/tables.dart';
import 'package:youtube_player_flutter/youtube_player_flutter.dart';

class YoutubeVideoScreen extends StatefulWidget {
  const YoutubeVideoScreen({super.key});

  @override
  State<YoutubeVideoScreen> createState() => _YoutubeVideoScreenState();
}

class _YoutubeVideoScreenState extends State<YoutubeVideoScreen> {
  YoutubePlayerController _controller =
      YoutubePlayerController(initialVideoId: "");
  bool watched = false;
  bool idFetched = false;
  bool inHindi = false;
  App app = App();
  Map<String, String> ids = {};

  @override
  void initState() {
    super.initState();

    Database.get(Tables.settings,
            where: {"vendor_id": -1}, fields: "name, value", silent: true)
        .then((List<dynamic> value) {
      for (var row in value) {
        if (row["name"].toString().contains("hindi")) {
          ids["Hindi"] = row["value"];
        } else {
          ids["English"] = row["value"];
        }
      }

      inHindi = app.prefs.getString("country_code") == "IN";
      _controller = YoutubePlayerController(
        initialVideoId: ids[inHindi ? "Hindi" : "English"]!,
        flags: const YoutubePlayerFlags(autoPlay: true, mute: false),
      );

      setState(() {
        idFetched = true;
      });
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButtonLocation: FloatingActionButtonLocation.endTop,
      appBar: AppBar(toolbarHeight: 0),
      backgroundColor: Colors.white,
      floatingActionButton: idFetched
          ? Padding(
              padding: const EdgeInsets.all(12.0),
              child: GestureDetector(
                onTap: () {
                  setState(() {
                    inHindi = !inHindi;
                  });
                  _controller.load(ids[inHindi ? "Hindi" : "English"]!);
                },
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(8.0),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        const Icon(
                          Icons.record_voice_over,
                          size: 18,
                        ),
                        const SizedBox(width: 8),
                        Text(inHindi ? "Eng" : "Hindi"),
                      ],
                    ),
                  ),
                ),
              ),
            )
          : null,
      body: idFetched == false
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              child: YoutubePlayerBuilder(
              player: YoutubePlayer(
                aspectRatio: 9 / 14,
                showVideoProgressIndicator: true,
                controller: _controller,
                onEnded: (_) {
                  setState(() {
                    watched = true;
                  });
                  done();
                },
                onReady: () {
                  _controller.play();
                },
                topActions: [
                  IconButton(
                      onPressed: _controller.play,
                      icon: const Icon(
                        Icons.play_arrow,
                        shadows: [
                          BoxShadow(color: Colors.white, blurRadius: 10.0),
                        ],
                      ))
                ],
                progressColors: const ProgressBarColors(
                  playedColor: ThemeColors.primary,
                  handleColor: ThemeColors.primary,
                ),
                progressIndicatorColor: Colors.blueAccent,
              ),
              builder: (context, player) {
                return Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    player,
                    const SizedBox(height: 20),
                    SizedBox(
                      width: MediaQuery.of(context).size.width * 0.8,
                      child: ElevatedButton.icon(
                        iconAlignment: IconAlignment.end,
                        icon: Icon(watched ? Icons.chevron_right : Icons.lock),
                        onPressed: watched ? done : null,
                        style: ElevatedButton.styleFrom(
                            foregroundColor: Colors.white,
                            backgroundColor: ThemeColors.primary),
                        label: Padding(
                          padding: const EdgeInsets.only(right: 8.0),
                          child: Text(
                            watched
                                ? "Continue!"
                                : "Watch This Video to Continue!",
                            style: const TextStyle(
                                overflow: TextOverflow.visible, fontSize: 14),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 10),
                    if (!watched)
                      GestureDetector(
                          onTap: openBottomPopup,
                          child: const Text("Skip (Not Recommended)",
                              style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w400,
                                  color: Colors.black54))),
                    const SizedBox(height: 10),
                  ],
                );
              },
            )),
    );
  }

  void done() async {
    await App().prefs.setBool("show_intro_video", false);
    Utility.showMessage("Video Completed, Let's Continue!");
    await AppRouter.navigateTo("/", removeUntil: true);
  }

  void openBottomPopup() {
    showModalBottomSheet(
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(20),
          ),
        ),
        clipBehavior: Clip.antiAliasWithSaveLayer,
        constraints: const BoxConstraints(maxHeight: 200),
        context: context,
        builder: (modalContext) {
          return Scaffold(
            backgroundColor: Colors.white,
            body: Center(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text(
                    "Skipping is not recommended!",
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  const Text(
                    "Understanding the basics of TiffinCRM will make it easier for you to start using it for first time.!",
                    style: TextStyle(fontSize: 12),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 25),
                  SizedBox(
                    width: MediaQuery.of(context).size.width * 0.8,
                    child: ElevatedButton(
                      onPressed: () {
                        Navigator.pop(modalContext);
                      },
                      style: ElevatedButton.styleFrom(
                          foregroundColor: Colors.white,
                          backgroundColor: ThemeColors.primary),
                      child: const Padding(
                        padding: EdgeInsets.all(8.0),
                        child: Text("OK, continue watching!"),
                      ),
                    ),
                  ),
                  const SizedBox(height: 5),
                  GestureDetector(
                      onTap: done,
                      child: const Center(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text("I still want to skip!"),
                            SizedBox(width: 5),
                            Icon(Icons.chevron_right, size: 14),
                          ],
                        ),
                      )),
                ],
              ),
            ),
          );
        });
  }
}
