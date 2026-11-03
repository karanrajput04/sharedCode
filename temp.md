
import json
import re

text = editor.getText()

def decode_rawmsg(match):
    value = json.loads('"' + match.group(1) + '"')

    try:
        decoded = json.loads(value)
        return '"rawMsg": ' + json.dumps(decoded, indent=4)
    except:
        return '"rawMsg": ' + json.dumps(value)

pattern = r'"rawMsg"\s*:\s*"((?:\\.|[^"\\])*)"'

result = re.sub(pattern, decode_rawmsg, text)

editor.setText(result)

notepad.messageBox("All rawMsg fields processed!", "Decode Complete")
