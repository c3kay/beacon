# Beacon Plugin

A simple plugin to extend the beacon area of effect. The area is extended as sphere.

Build for MC 1.21.1.

### Build

```shell
mvn install
mvn package
```

The `.jar` file is then available in the `target` folder.

### Plugin Commands

- `/beacon distance <value>`: Set the distance/radius to extend.
- `/beacon tier <value>`: Set the min. tier for the beacon to be extended.
- `/beacon measure`: Get the distance to the closest beacon.
