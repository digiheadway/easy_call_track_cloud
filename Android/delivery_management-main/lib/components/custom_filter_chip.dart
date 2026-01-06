import 'package:flutter/material.dart';
import 'package:tiffincrm/utils/app_router.dart';

import '../utils/utility.dart';
import '../values/my_colors.dart';
import '../values/numbers.dart';
import '../values/styles.dart';

class CustomFilterChip extends StatelessWidget {
  final bool isSelected;
  final List<String> selectedOptions;
  final List<String> options;
  final String label;
  final String labelWithCount;
  final Map<String, int> optionCounts;
  final Function({bool selected}) updateState;
  final bool multipleSelect;
  const CustomFilterChip({
    required this.isSelected,
    required this.selectedOptions,
    required this.options,
    required this.label,
    required this.labelWithCount,
    required this.optionCounts,
    required this.updateState,
    this.multipleSelect = true,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return FilterChip(
      labelPadding: EdgeInsets.only(right: isSelected ? 7 : 2),
      visualDensity: VisualDensity.compact,
      label: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            multipleSelect
                ? labelWithCount
                : (label +
                    (selectedOptions.isNotEmpty
                        ? " (${selectedOptions[0]})"
                        : "")),
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 12,
            ),
          ),
          if (!isSelected && options.isNotEmpty)
            Transform.scale(
              scale: 2,
              child: const Padding(
                padding: EdgeInsets.only(left: 6),
                child: Icon(
                  Icons.arrow_drop_down,
                  size: 13,
                  color: Colors.black45,
                ),
              ),
            )
        ],
      ),
      selected: isSelected,
      selectedColor: ThemeColors.primary,
      deleteIconColor: Colors.white,
      labelStyle: TextStyle(color: isSelected ? Colors.white : Colors.black54),
      deleteIcon: const Icon(Icons.close, size: 15),
      showCheckmark: false,
      shape: RoundedRectangleBorder(
          side: const BorderSide(color: Colors.black12),
          borderRadius: BorderRadius.circular(Numbers.borderRadius)),
      onDeleted: isSelected
          ? () {
              if (options.isNotEmpty) selectedOptions.clear();
              updateState(selected: false);
            }
          : null,
      onSelected: (value) {
        if (options.isEmpty) {
          updateState(selected: value);
          return;
        }

        Utility.showBottomSheet(
            "Select $label",
            StatefulBuilder(
              builder: (context, setState) => SingleChildScrollView(
                child: ListView.builder(
                    physics: const NeverScrollableScrollPhysics(),
                    shrinkWrap: true,
                    itemExtent: 60,
                    itemCount: options.length,
                    itemBuilder: (context, index) {
                      String count = label != "Sort By" || multipleSelect
                          ? " (${optionCounts[options[index]] ?? 0})"
                          : "";

                      return Card(
                        shape: Styles.cardShape(
                            selectedOptions.contains(options[index])),
                        child: ListTile(
                          onTap: () {
                            if (selectedOptions.contains(options[index])) {
                              selectedOptions.remove(options[index]);
                            } else {
                              if (!multipleSelect) {
                                selectedOptions.clear();
                              }
                              selectedOptions.add(options[index]);
                            }

                            updateState();
                            setState(() {});

                            if (options.length == 1 || !multipleSelect) {
                              Navigator.pop(context);
                            }
                          },
                          visualDensity: VisualDensity.compact,
                          title: Text("${options[index]}$count"),
                        ),
                      );
                    }),
              ),
            ),
            topRightButton: options.isEmpty && !isSelected
                ? null
                : TextButton(
                    onPressed: () {
                      selectedOptions.clear();
                      updateState();

                      AppRouter.goBack();
                    },
                    child: const Text(
                      "Clear Selection",
                    ),
                  ));
      },
    );
  }
}
