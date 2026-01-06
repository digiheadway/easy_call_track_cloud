
import os

path = '/Users/ygs/Documents/Code/Android/miniclick-trackcalls/app/src/main/java/com/miniclick/calltrackmanage/ui/home/HomeScreen.kt'
with open(path, 'r') as f:
    lines = f.readlines()

# Line 3191 (index 3190) is current function end.
# We need to turn it into Box Close, Card Close, Func Close.
# We also need to PRESERVE the Column Close (3190: index 3189).
# Wait, let's look at 3188-3191 again.
# 3188: if personGroup end
# 3189: if isExpanded end
# 3190: Column end?
# 3191: Function end?

# Let's verify indexing.
# 3191 in my view was `}`. 
# I want it to be:
# 3191:         } (Box end)
# 3192:     } (Card end)
# 3193: } (Func end)

# And line 3221 (the stray one) must be removed.
# 3221 is index 3220.

# Reverse order to avoid index shift
del lines[3220]
lines[3190] = "        }\n    }\n}\n"

with open(path, 'w') as f:
    f.writelines(lines)
