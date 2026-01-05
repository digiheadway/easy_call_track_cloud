
import os

path = '/Users/ygs/Documents/Code/Android/miniclick-trackcalls/app/src/main/java/com/miniclick/calltrackmanage/ui/home/HomeScreen.kt'
with open(path, 'r') as f:
    lines = f.readlines()

# Fix CallLogList tail
header_idx = -1
for i, l in enumerate(lines):
    if "fun DateSectionHeader" in l:
        header_idx = i
        break

if header_idx != -1:
    # Find the end of CallLogItem call (the closing ')' at line 2716 approx)
    call_end_idx = -1
    for j in range(header_idx - 1, 0, -1):
        if "                )" in lines[j]:
            call_end_idx = j
            break
    
    if call_end_idx != -1:
        # We need: } (item), } (headerLogs), } (groupedLogs), } (LazyColumn), } (fun)
        tail = [
            "                }\n",
            "            }\n",
            "        }\n",
            "    }\n",
            "}\n",
            "\n"
        ]
        lines[call_end_idx + 1 : header_idx - 1] = tail

# Fix CallLogItem tail
action_idx = -1
for i, l in enumerate(lines):
    if "fun ActionIconButton" in l:
        action_idx = i
        break

if action_idx != -1:
    # Find the end of View Interaction Button (ArrowForwardIos)
    arrow_idx = -1
    for j in range(action_idx - 1, 0, -1):
        if "                                modifier = Modifier.size(12.dp)" in lines[j]:
            arrow_idx = j
            break
    
    if arrow_idx != -1:
        # Sequence:
        # 3185: } (Icon)
        # 3186: } (Row)
        # 3187: } (TextButton)
        # 3188: } (if personGroup)
        # 3189: } (if isExpanded)
        # 3190: } (Column main)
        # 3191: } (Box)
        # 3192: } (Card)
        # 3193: } (Func)
        
        # We start REPLACING from arrow_idx + 2?
        # Let's count from } of Icon (arrow_idx + 2 usually)
        # Actually, let's just use the brace count.
        
        # Finding the first } after 3186
        start_replace = -1
        for j in range(arrow_idx + 1, action_idx):
            if "}" in lines[j]:
                start_replace = j
                break
        
        if start_replace != -1:
            tail = [
                "                            }\n",
                "                        }\n",
                "                    }\n",
                "                }\n",
                "            }\n",
                "        }\n",
                "        }\n",
                "    }\n",
                "}\n",
                "\n"
            ]
            lines[start_replace : action_idx - 1] = tail

with open(path, 'w') as f:
    f.writelines(lines)
