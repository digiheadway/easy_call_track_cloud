
import os

path = '/Users/ygs/Documents/Code/Android/miniclick-trackcalls/app/src/main/java/com/miniclick/calltrackmanage/ui/home/HomeScreen.kt'
with open(path, 'r') as f:
    lines = f.readlines()

# Locate the CallLogItem call in CallLogList
for i in range(len(lines)):
    if "CallLogItem(" in lines[i] and (i+1 < len(lines)) and "log = callLogItem" in lines[i+1]:
        # Insert the capture variables
        lines.insert(i, "                val cid = callLogItem.compositeId\n")
        lines.insert(i+1, "                val isRev = callLogItem.reviewed\n")
        
        # Now find and update onReviewedToggle within this call
        for k in range(i+2, i+150):
            if k < len(lines) and "onReviewedToggle = {" in lines[k]:
                lines[k] = "                    onReviewedToggle = { viewModel.updateReviewed(cid, !isRev) }\n"
                break
        break

with open(path, 'w') as f:
    f.writelines(lines)
