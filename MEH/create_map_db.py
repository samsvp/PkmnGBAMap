### You should not run this code
### This can easily run malicious code if the .txt files are tampered with
### Instead, copy and paste its output file
# %%
import os
import json
from typing import Dict, List, Any

output_file = "map_info.json"
file_names = [f"collision_map/{f}" for f in os.listdir("collision_map/") if not f.startswith(" ")]
names = [f[:-4] for f in file_names]


def update_partial_map(events: Dict[str, Any], _partial_map: List[List[Any]], map_name="") -> List[List[Any]]:
    """
    Add events location into the map
    """
    partial_map = list(map(lambda lst: [str(i) for i in lst], _partial_map))

    def get_x_y(var):
        return var["x"], var["y"]

    for map_exit in events["exits"]:
        x, y = get_x_y(map_exit)
        # for some reason it seems like there are events outside the map
        # might be due to the rom
        if y >= len(partial_map) or x >= len(partial_map[0]): print(map_name, y, len(partial_map))
        else: partial_map[y][x] = "E"
    for map_npc in events["NPCs"]:
        x, y = get_x_y(map_npc)
        partial_map[y][x] = "N" if map_npc["is_trainer"] == 0 else "NT"
    for sign in events["signs"]:
        x, y = get_x_y(sign)
        if y >= len(partial_map): print(map_name, y, len(partial_map))
        else: partial_map[y][x] = "S"
    for trigger in events["triggers"]:
        x, y = get_x_y(trigger)
        partial_map[y][x] = "T"
    return partial_map

map_info = {}
events = None
connections = None
partial_map = None
for name, file_name in zip(names, file_names):
    with open(file_name) as file:
        exec(file.read()) # variables: connections, events and partial_map
    
    map_info[file_name] = {"connections": connections[0], "events": events[0], "partial_map": update_partial_map(events[0], partial_map, file_name)}
    

with open(output_file, "w") as file:
    json.dump(map_info, file, ensure_ascii=False, indent=4)


# %%

