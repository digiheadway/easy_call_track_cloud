
import os

path = '/Users/ygs/Documents/Code/Android/miniclick-trackcalls/app/src/main/java/com/miniclick/calltrackmanage/ui/home/HomeScreen.kt'
with open(path, 'r') as f:
    lines = f.readlines()

# Locate the CallLogItem call in CallLogList
for i in range(len(lines)):
    if "CallLogItem(" in lines[i] and (i+1 < len(lines)) and "log = callLogItem" in lines[i+1]:
        # Define captures
        captures = [
            "                val cid = callLogItem.compositeId\n",
            "                val isRev = callLogItem.reviewed\n",
            "                val pNum = callLogItem.phoneNumber\n",
            "                val cName = callLogItem.contactName\n",
            "                val cDate = callLogItem.callDate\n",
            "                val cType = callLogItem.callType\n",
            "                val lPath = callLogItem.localRecordingPath\n"
        ]
        # Insert them (in reverse order to keep positions)
        for cap in reversed(captures):
            lines.insert(i, cap)
        
        # New offset for k search
        call_start_idx = i + len(captures)
        
        # Now replace all usages of callLogItem properties within this CallLogItem(...) call
        # We must be careful not to replace callLogItem itself when passed as 'log = callLogItem'
        for k in range(call_start_idx, call_start_idx + 150):
            if k >= len(lines): break
            if "LazyColumn" in lines[k]: break # Safety stop
            
            # Replace properties with captured vars
            lines[k] = lines[k].replace("callLogItem.compositeId", "cid")
            lines[k] = lines[k].replace("callLogItem.reviewed", "isRev")
            lines[k] = lines[k].replace("callLogItem.phoneNumber", "pNum")
            lines[k] = lines[k].replace("callLogItem.contactName", "cName")
            lines[k] = lines[k].replace("callLogItem.callDate", "cDate")
            lines[k] = lines[k].replace("callLogItem.callType", "cType")
            lines[k] = lines[k].replace("callLogItem.localRecordingPath", "lPath")
        break

with open(path, 'w') as f:
    f.writelines(lines)
