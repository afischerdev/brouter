---
parent: Using BRouter
---

# Download Routing Segments

Routing data files are organised as 5\*5 degree files, with the filename
containing the south-west corner of the square, which means:

- You want to route near West48/North37 -> you need `W50_N35.rd5`
- You want to route near East7/North47 -> you need `E5_N45.rd5`

These data files, called _segments_ across BRouter, are generated from
[OpenStreetMap](https://www.openstreetmap.org/) data and stored in a custom
binary format (rd5) for improved efficiency of BRouter routing.

## Download them from brouter.de

### Note:

```
We offer no guarantee for this service.
Changes are possible at any time,
for example to the IP address, the domain, the version number for lookups.dat, or ...
```

Segments files from the whole planet are generated weekly at
[https://brouter.de/brouter/segments4/](http://brouter.de/brouter/segments4/).

You can download one or more segments files, covering the area of the planet
your want to route, into the `segments4` directory.

It is highly recommended to use the synchronization client for downloading.

Single files: The lookups.dat file is checked; when downloading multiple files, this can be ignored in the other steps.
```
>java  -jar brouter-sync.jar ./segments4 ./profiles2 W20_N30
>java  -DignoreCheckProfile=true -jar brouter-sync.jar ./segments4 ./profiles2 E5_N45
>java  -DignoreCheckProfile=true -jar brouter-sync.jar ./segments4 ./profiles2 E10_N45
....
```

All files from your local segment folder
```
>java -jar brouter-sync.jar ./segments4 ./profiles2 all
```

