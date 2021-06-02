# Mapping

Most of the work is done inside the BankLoader class.

At `BankLoader.java`, in the method `GetMapCol` we can get the tiles collision information for each map

The following `TimeMeta` values have been discovered
```javascript
{
    1: "blocked",
    12: "can pass",
    13: "blocked and interactable(?)" //seen on signs
}
```

Map connections are gotten at the `GetConnections` methods at `BankLoader.java`.

All map information is saved on individual files called `MapName (bank.map).txt` and are stored at `MEH/collision_map`.

If you wish to compile this then good luck (this only works on Windows due to GBAUtils, btw).