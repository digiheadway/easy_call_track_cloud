import 'dart:async';

import 'package:flutter_keyboard_visibility/flutter_keyboard_visibility.dart';
import 'package:tiffincrm/components/form/form_input.dart';
import 'package:flutter/material.dart';
import 'package:textfield_tags/textfield_tags.dart';
import 'package:tiffincrm/values/my_colors.dart';
import 'package:tiffincrm/values/numbers.dart';

//String Tags with AutoComplete
class StringAutoCompleteTags extends StatefulWidget {
  final FormInput input;
  const StringAutoCompleteTags(this.input, {super.key});

  @override
  State<StringAutoCompleteTags> createState() => _StringAutoCompleteTagsState();
}

class _StringAutoCompleteTagsState extends State<StringAutoCompleteTags> {
  late StringTagController _stringTagController;
  late List<String> _initialTags;
  late FormInput input;
  bool remove = false;
  Map<String, int> items = {};
  FocusNode keyboardFocusNode = FocusNode();

  late StreamSubscription<bool> keyboardSubscription;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
  }

  @override
  void initState() {
    super.initState();

    input = widget.input;
    _stringTagController = input.controller;

    _initialTags = input.options;
    for (var opt in input.options) {
      items[opt] = 1;
    }

    var keyboardVisibilityController = KeyboardVisibilityController();
    // Subscribe
    keyboardSubscription =
        keyboardVisibilityController.onChange.listen((bool visible) {
      if (!visible) {
        _stringTagController.getFocusNode?.unfocus();
      }
    });
  }

  @override
  void dispose() {
    keyboardFocusNode.dispose();
    keyboardSubscription.cancel();

    super.dispose();
    //_stringTagController.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Column(
        children: [
          Autocomplete<String>(
            optionsViewBuilder: optionsViewBuilder,
            optionsBuilder: (TextEditingValue textEditingValue) {
              return _initialTags
                  .map((opt) => opt.trim())
                  .toSet()
                  .where((String option) {
                return option != "null" &&
                    !_stringTagController.getTags!.join(" ").contains(option) &&
                    option
                        .toLowerCase()
                        .contains(textEditingValue.text.toLowerCase());
              });
            },
            onSelected: (String selectedTag) {
              _stringTagController.onTagSubmitted(selectedTag);
            },
            fieldViewBuilder:
                (context, textEditingController, focusNode, onFieldSubmitted) {
              return TextFieldTags<String>(
                textEditingController: textEditingController,
                focusNode: focusNode,
                textfieldTagsController: _stringTagController,
                initialTags: input.value.isEmpty ? [] : input.value.split(', '),
                textSeparators: const [",", "\n"],
                letterCase: LetterCase.normal,
                validator: (String tag) {
                  if (tag.isEmpty) {
                    return 'Field cannot be empty';
                  }
                  if (input.quantitativeTags &&
                      tag.contains(" ") &&
                      int.tryParse(tag.split(" ")[0]) != null) {
                    String tag1 = tag.split(" ").sublist(1).join(" ");
                    if (_stringTagController.getTags!
                        .join(" ")
                        .contains(tag1)) {
                      return 'You\'ve already entered that';
                    }
                  }
                  if (_stringTagController.getTags!.contains(tag)) {
                    return 'You\'ve already entered that';
                  }
                  if (_stringTagController.getTags!.length == input.tagsLimit) {
                    return 'You can\'t add more options';
                  }
                  return null;
                },
                inputFieldBuilder: (context, inputFieldValues) {
                  return FocusScope(
                    onFocusChange: (value) {
                      if (!inputFieldValues.focusNode.hasFocus) {
                        if (inputFieldValues
                            .textEditingController.text.isNotEmpty) {
                          inputFieldValues.onTagSubmitted(
                              inputFieldValues.textEditingController.text);
                        } else {
                          FocusManager.instance.primaryFocus?.unfocus();
                        }
                      }
                    },
                    child: Container(
                      decoration: BoxDecoration(
                          color: Colors.white,
                          border: Border.all(color: Colors.black12),
                          borderRadius:
                              BorderRadius.circular(Numbers.borderRadius)),
                      padding: const EdgeInsets.all(8),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          inputFieldValues.tags.isEmpty
                              ? const SizedBox(height: 10)
                              : tags(inputFieldValues),
                          inputField(inputFieldValues),
                        ],
                      ),
                    ),
                  );
                },
              );
            },
          ),
        ],
      ),
    );
  }

  Widget optionsViewBuilder(context, onSelected, options) {
    return Align(
      alignment: Alignment.topLeft,
      child: Material(
          color: Colors.grey.shade100,
          elevation: 4.0,
          child: ConstrainedBox(
            constraints: BoxConstraints(
              maxHeight: 100,
              maxWidth: MediaQuery.of(context).size.width * 0.9,
            ),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Wrap(
                  direction: Axis.horizontal,
                  spacing: 10,
                  children: List<Widget>.from(options.map((opt) => Card(
                        child: InkWell(
                            onTap: () {
                              onSelected(opt);
                            },
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 12, vertical: 8),
                              child: Text(
                                opt,
                                style: const TextStyle(
                                    color: ThemeColors.primary,
                                    fontWeight: FontWeight.bold),
                              ),
                            )),
                      ))),
                ),
              ),
            ),
          )),
    );
  }

  Widget inputField(inputFieldValues) {
    return KeyboardListener(
      focusNode: keyboardFocusNode,
      onKeyEvent: (value) {
        inputFieldValues.focusNode.unfocus();

        remove = !remove;
        // if (value.logicalKey.keyLabel == 'Backspace' &&
        //     inputFieldValues.tags.isNotEmpty &&
        //     remove) {
        //   inputFieldValues
        //       .onTagRemoved(inputFieldValues.tags.last);
        // }
        // setState(() {});
      },
      child: TextField(
        onTapOutside: (PointerDownEvent event) {
          FocusManager.instance.primaryFocus?.unfocus();
        },
        readOnly: _stringTagController.getTags!.length == input.tagsLimit,
        textInputAction: TextInputAction.newline,
        controller: inputFieldValues.textEditingController,
        focusNode: inputFieldValues.focusNode,
        textCapitalization: TextCapitalization.sentences,
        maxLines: null,
        scrollPadding: const EdgeInsets.only(bottom: 120),
        decoration: InputDecoration(
          contentPadding: EdgeInsets.zero,
          suffixIcon: input.canAddOption
              ? InkWell(
                  onTap: () {
                    inputFieldValues.onTagSubmitted(
                        inputFieldValues.textEditingController.text.trim());
                    inputFieldValues.textEditingController.clear();
                  },
                  child: const Icon(Icons.add))
              : null,
          prefixIcon: input.prefixIcon,
          helperText: input.helperText.isNotEmpty ? input.helperText : null,
          hintText: _stringTagController.getTags!.length == input.tagsLimit
              ? "You can't add more options"
              : input.hintText.isNotEmpty
                  ? input.hintText
                  : "Enter ${input.name}",
          hintStyle: const TextStyle(color: Colors.black45),
          isDense: true,
          border: const OutlineInputBorder(),
          helperStyle: const TextStyle(color: Colors.black54),
          // labelText: "${input.name}${input.isRequired ? "" : " (optional)"}",
          errorText: inputFieldValues.error,
        ),
        onChanged: inputFieldValues.onTagChanged,
        onSubmitted: inputFieldValues.onTagSubmitted,
      ),
    );
  }

  Widget tags(InputFieldValues<String> inputFieldValues) {
    return SingleChildScrollView(
      controller: inputFieldValues.tagScrollController,
      scrollDirection: Axis.vertical,
      padding: const EdgeInsets.only(top: 6, bottom: 10),
      child: Wrap(
          runSpacing: 7,
          children: List<Widget>.from(inputFieldValues.tags.map((String tag) {
            List<String> splitted = tag.split(" ");
            String tag2 = tag;

            int count = 1;

            if (tag.contains(" ") && int.tryParse(splitted[0]) != null) {
              count = int.parse(splitted[0]);
              tag2 = splitted.sublist(1).join(" ");
            }
            return Container(
              width: input.quantitativeTags ? double.infinity : null,
              decoration: BoxDecoration(
                border: input.quantitativeTags
                    ? Border(bottom: BorderSide(color: Colors.grey.shade100))
                    : Border.all(color: Colors.black, width: 0.5),
                borderRadius: const BorderRadius.all(
                  Radius.circular(Numbers.borderRadius),
                ),
                color: Colors.white,
              ),
              margin: const EdgeInsets.only(right: 10.0),
              padding:
                  const EdgeInsets.symmetric(horizontal: 10.0, vertical: 4.0),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Flexible(
                    child: Text(
                      input.quantitativeTags ? tag2 : tag,
                      style: const TextStyle(
                          // color: Colors.white,
                          fontWeight: FontWeight.bold),
                    ),
                  ),
                  const SizedBox(width: 4.0),
                  if (!input.quantitativeTags)
                    InkWell(
                      child: const Icon(Icons.cancel, size: 18.0),
                      onTap: () {
                        inputFieldValues.onTagRemoved(tag);
                      },
                    ),
                  if (input.quantitativeTags)
                    Card(
                      color: Colors.grey.shade100,
                      child: SizedBox(
                        width: 100,
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            InkWell(
                              child: Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: Icon(
                                    count == 1 ? Icons.delete : Icons.remove,
                                    size: 20.0),
                              ),
                              onTap: () {
                                count--;

                                if (count <= 0) {
                                  inputFieldValues.onTagRemoved(tag);
                                } else {
                                  List<String> tags = inputFieldValues.tags;
                                  _stringTagController.clearTags();
                                  for (var tag1 in tags) {
                                    if (tag1.contains(tag2)) {
                                      tag1 = "$count $tag2";
                                    }
                                    inputFieldValues.onTagSubmitted(tag1);
                                  }
                                  if (!inputFieldValues.focusNode.hasFocus) {
                                    inputFieldValues.focusNode.unfocus();
                                  }
                                  setState(() {});
                                }
                              },
                            ),
                            const SizedBox(width: 4.0),
                            Text(count.toString(),
                                style: const TextStyle(
                                    fontSize: 18.0,
                                    fontWeight: FontWeight.bold)),
                            const SizedBox(width: 4.0),
                            InkWell(
                              child: const Padding(
                                padding: EdgeInsets.all(8.0),
                                child: Icon(Icons.add, size: 20.0),
                              ),
                              onTap: () {
                                count++;

                                List<String> tags = inputFieldValues.tags;
                                _stringTagController.clearTags();
                                for (var tag1 in tags) {
                                  if (tag1.contains(tag2)) {
                                    tag1 = "$count $tag2";
                                  }
                                  inputFieldValues.onTagSubmitted(tag1);
                                }
                                if (!inputFieldValues.focusNode.hasFocus) {
                                  inputFieldValues.focusNode.unfocus();
                                }
                                setState(() {});
                              },
                            ),
                          ],
                        ),
                      ),
                    )
                ],
              ),
            );
          })).toList()),
    );
  }
}
