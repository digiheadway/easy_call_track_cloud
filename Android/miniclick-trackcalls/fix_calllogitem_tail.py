
import os

path = '/Users/ygs/Documents/Code/Android/miniclick-trackcalls/app/src/main/java/com/miniclick/calltrackmanage/ui/home/HomeScreen.kt'
with open(path, 'r') as f:
    lines = f.readlines()

# Fix CallLogItem tail (around index 3185 to 3191)
# Replacement: Row end (24s), TextButton end (20s), if personGroup end (16s), if isExpanded end (12s), Column end (8s), Box end (8s), Card end (4s), Func end (0s).
new_tail = [
    "                        }\n",
    "                    }\n",
    "                }\n",
    "            }\n",
    "        }\n",
    "        }\n",
    "    }\n",
    "}\n"
]
lines[3185:3192] = new_tail

with open(path, 'w') as f:
    f.writelines(lines)
